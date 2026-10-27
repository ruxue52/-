﻿﻿# =====================================================================
#  build-accdb.ps1  ·  晓镜书阁 Access 客户端 .accdb 生成脚本
#  作用：用 Access COM 自动化，从 vba/*.bas 真正生成可打开的二进制
#        ShuHaiGe-PSI.accdb（模块 + 窗体 + 原生报表 + 启动窗体）。
#  幂等：可重复运行，覆盖重建。
#  用法：powershell -NoProfile -ExecutionPolicy Bypass -File .\build-accdb.ps1
#  日志：tools\build-accdb.log
#  说明：Access 用 COM 建库/导入/建对象；数据访问走 REST（本脚本不连数据库）。
#        经实测：LoadFromText 三参签名为 (类型, 对象名, 文件)；
#        CreateControl 仅支持窗体（报表改用「窗体文本→报表文本」换写法）。
# =====================================================================
param(
    [switch]$SkipRegistry   # 跳过注册表信任设置（已设过时可加）
)

$ErrorActionPreference = 'Continue'
$Root     = Split-Path -Parent $PSScriptRoot     # 08-access-client
$ToolsDir = $PSScriptRoot
$LogFile  = Join-Path $ToolsDir 'build-accdb.log'
$DbPath   = Join-Path $Root 'ShuHaiGe-PSI.accdb'
$TmpDir   = Join-Path $env:TEMP ('shg_build_' + [Guid]::NewGuid().ToString('N'))

# Access 常量
$AcModule = 5; $AcForm = 2; $AcReport = 3
$AcLabel = 100; $AcButton = 104; $AcTextBox = 109; $AcListBox = 110; $AcComboBox = 111; $AcCheckBox = 106

New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null

function Log($msg) {
    $line = "[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $msg
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

# 写 UTF-8 无 BOM 文本（供 Access LoadFromText 读取，避免 BOM 干扰首行）
function Write-Utf8NoBom($path, $text) {
    $enc = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $text, $enc)
}

Set-Content -Path $LogFile -Value ("=== 晓镜书阁 Access 构建日志 {0} ===" -f (Get-Date)) -Encoding UTF8
Log "输出数据库：$DbPath"
Log "临时目录：$TmpDir"

# ---------------------------------------------------------------------
# 0. 数据库信任：启用 VBA 工程对象模型访问 + 允许宏（HKCU 用户级，可逆）
# ---------------------------------------------------------------------
if (-not $SkipRegistry) {
    $sec = 'HKCU:\Software\Microsoft\Office\16.0\Access\Security'
    try {
        if (-not (Test-Path $sec)) { New-Item -Path $sec -Force | Out-Null }
        New-ItemProperty -Path $sec -Name 'AccessVBOM'  -PropertyType DWord -Value 1 -Force | Out-Null
        New-ItemProperty -Path $sec -Name 'VBAWarnings' -PropertyType DWord -Value 1 -Force | Out-Null
        Log "已写入信任设置：AccessVBOM=1, VBAWarnings=1"
    } catch { Log "写注册表失败（可忽略）：$($_.Exception.Message)" }
}

# ---------------------------------------------------------------------
# 0.1 清理：结束遗留 Access 进程，删除旧库
# ---------------------------------------------------------------------
Get-Process MSACCESS -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Milliseconds 500
if (Test-Path $DbPath) { Remove-Item $DbPath -Force; Log "已删除旧 .accdb" }

# ---------------------------------------------------------------------
# 1. 打开 Access，创建数据库
# ---------------------------------------------------------------------
$procBefore = @(Get-Process MSACCESS -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id)
$app = New-Object -ComObject Access.Application
Start-Sleep -Milliseconds 400
$appPid = @(Get-Process MSACCESS -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id | Where-Object { $procBefore -notcontains $_ } | Select-Object -First 1)
$app.Visible = $true
$app.NewCurrentDatabase($DbPath)
try { $app.DoCmd.SetWarnings($false) } catch {}
Log "已创建数据库（Access PID=$appPid）"

# 验证 VBE 可访问
try { $null = $app.VBE.VBProjects.Item(1); Log "VBE 工程可访问（VBOM 已生效）" }
catch { Log "警告：VBE 不可访问：$($_.Exception.Message)" }

# ---------------------------------------------------------------------
# 2. 导入 10 个标准模块
#    注意：不要用 LoadFromText(acModule, ..., 文件) 直接读 .bas —— Access 按 ANSI(GBK) 解码，
#          而仓库里的 .bas 是 UTF-8，中文会整体变乱码；乱码字节还可能吞掉字符串的结尾引号，
#          导致整个 VBA 工程编译失败（症状是报错行号附近一句看起来正常的字符串赋值）。
#          改为：先用 PowerShell 按 UTF-8 把文件读成 Unicode 字符串，再用 CodeModule.AddFromString
#          注入（走 BSTR 通道，Unicode 安全）。注入前整体清空 CodeModule，避免与新建模块
#          自带的 Option 语句重复；同时剥离 VBE 导出的 Attribute VB_Name 头行。
# ---------------------------------------------------------------------
$modules = @('modApi','modUtil','modLogin','modStock','modCheck','modPurchase','modSale','modScan','modReport','modReportBuild')
$failedModules = @()
foreach ($m in $modules) {
    $p = Join-Path $Root "vba\$m.bas"
    if (-not (Test-Path $p)) { Log "跳过：缺少 $p"; $failedModules += $m; continue }
    try {
        $lines = Get-Content -LiteralPath $p -Encoding UTF8
        $body = (($lines | Where-Object { $_ -notmatch '^Attribute\s+VB_Name' }) -join "`r`n")
        $comp = $app.VBE.VBProjects.Item(1).VBComponents.Add(1)   # 1 = vbext_ct_StdModule
        $comp.Name = $m
        $cm = $comp.CodeModule
        if ($cm.CountOfLines -gt 0) { $cm.DeleteLines(1, $cm.CountOfLines) }
        $cm.AddFromString($body)
        Log "已导入标准模块 $m（type=$($comp.Type)，$($cm.CountOfLines) 行）"
        if ($comp.Type -ne 1) { Log "警告：$m 未建成标准模块（type=$($comp.Type)）"; $failedModules += $m }
    } catch { Log "导入模块 $m 失败：$($_.Exception.Message)"; $failedModules += $m }
}

# ---------------------------------------------------------------------
# 2.1 导入类模块 vba\sysManager.cls
#     注意：LoadFromText(acModule, ...) 与 VBComponents.Import 都会把类模块建成「标准模块」
#           （type=1）且把头部的 VERSION/BEGIN/Attribute 当作代码行写入，导致 `As sysManager`
#           无法解析、整个 VBA 工程编译失败。可靠做法是用 VBComponents.Add(2) 显式建类模块，
#           再剥离 VBE 头部（VERSION/BEGIN/END/MultiUse/Attribute VB_*）后注入正文。
#           源文件仍保留完整 VBE 导出头，便于在 VBE 里「导出/导入」人工维护。
# ---------------------------------------------------------------------
$classModules = @('sysManager')
foreach ($c in $classModules) {
    $p = Join-Path $Root "vba\$c.cls"
    if (-not (Test-Path $p)) { Log "跳过：缺少 $p"; $failedModules += $c; continue }
    try {
        $lines = Get-Content -LiteralPath $p -Encoding UTF8
        $h = 0
        while ($h -lt $lines.Count -and ($lines[$h] -match '^(VERSION\s|BEGIN\s*$|END\s*$|\s+MultiUse|Attribute\s+VB_)')) { $h++ }
        $body = ($lines[$h..($lines.Count - 1)]) -join "`r`n"
        $comp = $app.VBE.VBProjects.Item(1).VBComponents.Add(2)   # 2 = vbext_ct_ClassModule
        $comp.Name = $c
        $cm = $comp.CodeModule
        if ($cm.CountOfLines -gt 0) { $cm.DeleteLines(1, $cm.CountOfLines) }
        $cm.AddFromString($body)
        Log "已导入类模块 $c（type=$($comp.Type)，剥离头部 $h 行，注入 $($cm.CountOfLines) 行）"
        if ($comp.Type -ne 2) { Log "警告：$c 未建成类模块（type=$($comp.Type)）"; $failedModules += $c }
    } catch { Log "导入类模块 $c 失败：$($_.Exception.Message)"; $failedModules += $c }
}

# ---------------------------------------------------------------------
# 3. 建本地辅助临时表（DDL 见 assets/create-accdb-steps.md）
# ---------------------------------------------------------------------
$ddl = @(
 "CREATE TABLE tblSession (skey TEXT(64), val TEXT(255))",
 "CREATE TABLE tmpStockList (book_id LONG, book_name TEXT(255), isbn TEXT(32), warehouse_id LONG, warehouse_name TEXT(128), category_id LONG, category_name TEXT(64), shelf_no TEXT(64), quantity DOUBLE, safety_stock DOUBLE, avg_cost DOUBLE, stock_amount DOUBLE)",
 "CREATE TABLE tmpCheckOrders (id LONG, check_no TEXT(64), warehouse_id LONG, warehouse_name TEXT(128), check_type TEXT(16), status TEXT(16), total_items LONG, diff_items LONG, profit_qty DOUBLE, loss_qty DOUBLE, create_time DATETIME)",
 "CREATE TABLE tmpCheckItems (item_id LONG, book_id LONG, book_name TEXT(255), isbn TEXT(32), book_qty DOUBLE, real_qty DOUBLE, diff_qty DOUBLE, diff_amount DOUBLE)",
 "CREATE TABLE tmpPurchaseOrders (id LONG, order_no TEXT(64), supplier_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE, status TEXT(16), operator_name TEXT(64), create_time DATETIME)",
 "CREATE TABLE tmpPurchaseItems (book_id LONG, book_name TEXT(255), isbn TEXT(32), quantity DOUBLE, price DOUBLE, amount DOUBLE)",
 "CREATE TABLE tmpSaleOrders (id LONG, order_no TEXT(64), customer_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE, payment_type TEXT(32), status TEXT(16), create_time DATETIME)",
 "CREATE TABLE tmpPosCart (book_id LONG, book_name TEXT(255), isbn TEXT(32), quantity DOUBLE, price DOUBLE, amount DOUBLE)",
 "CREATE TABLE tmpDocQuery (id LONG, doc_no TEXT(64), doc_type TEXT(16), partner_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, amount DOUBLE, status TEXT(16), create_time DATETIME)"
)
foreach ($s in $ddl) {
    try { $app.DoCmd.RunSQL($s) } catch { Log "建表失败：$($_.Exception.Message)" }
}
Log "已建立 $($ddl.Count) 张本地临时表"

# ---------------------------------------------------------------------
# 4. 程序化创建窗体
#    控件类型：100 Label / 104 Button / 109 TextBox / 110 ListBox / 111 ComboBox / 106 CheckBox
#    多数按钮用「表达式」绑定到标准模块公有过程；需写回事件参数的场景（登录守卫的 Form_Open、
#    frmLogin 的登录/退出按钮、frmMain 的扫码 KeyDown）改用窗体代码模块（见 module / onOpen / onLoad）。
# ---------------------------------------------------------------------
function New-AccessForm($app, $spec) {
    # 说明：Access 在本环境对连续 CreateForm 会随机抛 RPC_E_SERVERFAULT，
    #       实测「失败后立即重试」即可成功（1 次重试通常足够），故整体重试。
    $maxTry = 6
    for ($attempt = 1; $attempt -le $maxTry; $attempt++) {
        $old = $null
        try {
            $frm = $app.CreateForm()
            $old = $frm.Name
            Start-Sleep -Milliseconds 120
            try { $frm.Caption = $spec.caption } catch {}
            if ($spec.ContainsKey('width')) { try { $frm.Width = $spec.width } catch {} }
            try { $frm.RecordSelectors = $false } catch {}
            try { $frm.NavigationButtons = $false } catch {}
            # 对话框窗体属性（frmLogin 用）
            if ($spec.ContainsKey('popup'))       { try { $frm.PopUp = $true } catch {} }
            if ($spec.ContainsKey('modal'))       { try { $frm.Modal = $true } catch {} }
            if ($spec.ContainsKey('borderStyle')) { try { $frm.BorderStyle = $spec.borderStyle } catch {} }
            if ($spec.ContainsKey('autoCenter'))  { try { $frm.AutoCenter = $true } catch {} }
            # 窗体代码模块：仅个别场景需要（KeyDown 之类必须写回 Event 参数，表达式做不到）
            if ($spec.ContainsKey('module')) {
                try {
                    $frm.HasModule = $true
                    Start-Sleep -Milliseconds 250
                    $comp = $app.VBE.VBProjects.Item(1).VBComponents("Form_$old")
                    # 注意：新建窗体模块已自带 Option Compare Database，必须「整体替换」而不是追加，
                    #       否则重复的 Option 语句会让整个 VBA 工程编译失败（后续 Application.Run 全部找不到过程）。
                    $cm = $comp.CodeModule
                    if ($cm.CountOfLines -gt 0) { $cm.DeleteLines(1, $cm.CountOfLines) }
                    $cm.AddFromString($spec.module)
                    Log "  已注入窗体代码模块：Form_$old（$($cm.CountOfLines) 行）"
                } catch {
                    Log "  窗体代码模块注入失败 $($spec.name)：$($_.Exception.Message)"
                }
            }
            # 事件绑定：模块注入后，把 Form_Open / Form_Load 从「表达式」切到「[Event Procedure]」
            if ($spec.ContainsKey('onOpen')) {
                try { $frm.OnOpen = '[Event Procedure]'; Log "  已绑定 Form_Open = [Event Procedure]" }
                catch { Log "  Form_Open 绑定失败 $($spec.name)：$($_.Exception.Message)" }
            }
            if ($spec.ContainsKey('onLoad')) {
                try { $frm.OnLoad = '[Event Procedure]'; Log "  已绑定 Form_Load = [Event Procedure]" }
                catch { Log "  Form_Load 绑定失败 $($spec.name)：$($_.Exception.Message)" }
            }
            foreach ($c in $spec.controls) {
                $ctl = $app.CreateControl($old, $c.t, 0, $null, $null, $c.l, $c.p, $c.w, $c.h)
                try { $ctl.Name = $c.n } catch {}
                if ($c.ContainsKey('cap')) { try { $ctl.Caption = $c.cap } catch {} }
                if ($c.ContainsKey('def')) { try { $ctl.DefaultValue = $c.def } catch {} }
                if ($c.ContainsKey('im'))  { try { $ctl.InputMask = $c.im } catch {} }
                if ($c.ContainsKey('fc'))  { try { $ctl.ForeColor = $c.fc } catch {} }
                if ($c.ContainsKey('rst')) { try { $ctl.RowSourceType = $c.rst } catch {} }
                if ($c.ContainsKey('rs'))  { try { $ctl.RowSource = $c.rs } catch {} }
                # 事件用「表达式」绑定到标准模块的公有过程（见 vba/*.bas）
                if ($c.ContainsKey('ev'))  { try { $pn = $c.ev; $ctl.$pn = $c.evv } catch { Log "  事件绑定跳过 $($spec.name).$($c.n)：$($_.Exception.Message)" } }
            }
            $app.DoCmd.Close($AcForm, $old, 1)      # 关闭时保存
            $app.DoCmd.Rename($spec.name, $AcForm, $old)
            Log "已创建窗体 $($spec.name)（$($spec.controls.Count) 个控件）"
            return
        } catch {
            if ($old) { try { $app.DoCmd.Close($AcForm, $old, 2) } catch {} }   # 丢弃未保存的临时窗体
            if ($attempt -eq $maxTry) {
                Log "创建窗体 $($spec.name) 失败（已重试 $maxTry 次）：$($_.Exception.Message)"
            } else {
                Start-Sleep -Milliseconds 500
            }
        }
    }
}

# 登录守卫代码模块生成器：为窗体生成「未登录则取消打开」的 Form_Open 事件过程
# 注意：注入一律「整体替换」CodeModule 内容，绝不追加——追加会出现重复 Option 语句导致整个工程编译失败
# （报表不在此生成：报表的开关统一经由 modReportBuild.BuildReportTable，守卫放在该入口）
function New-GuardModule($name) {
    return @"
Option Compare Database
Option Explicit

Private Sub Form_Open(Cancel As Integer)
    If Not SysM.RequireLogin() Then
        Cancel = True
    End If
End Sub
"@
}

# frmLogin 的窗体代码模块：登录对话框（Form_Load 恢复上次工号；登录/退出按钮）
# frmLogin 豁免登录守卫（否则守卫会递归弹自己）
$frmLoginModule = @'
Option Compare Database
Option Explicit

Private Sub Form_Load()
    Me.Caption = "晓镜书阁 · 登录"
    Dim nm As String
    nm = Nz(DLookup("val", "tblSession", "skey='lastEmpNo'"), "")
    If Len(nm) > 0 Then
        Me.txtUsername = nm
        Me.chkRemember = True
    End If
End Sub

Private Sub cmdLogin_Click()
    If Len(Trim$(Me.txtUsername & "")) = 0 Then
        Me.lblMsg = "请输入工号": Exit Sub
    End If
    Me.cmdLogin.Enabled = False
    Me.lblMsg = "正在登录…"
    If SysM.Login(Me.txtUsername, Me.txtPassword) Then
        SaveEmpNo IIf(Me.chkRemember, Me.txtUsername, "")
        DoCmd.Close acForm, Me.Name
        ' 由守卫弹出时不再打开 frmMain（调用方会自行继续其打开流程）
        If Not gLoginIsGuard Then DoCmd.OpenForm "frmMain"
    Else
        Me.lblMsg = "登录失败：" & gLastError
        Me.txtPassword = ""
        Me.txtPassword.SetFocus
    End If
    Me.cmdLogin.Enabled = True
End Sub

Private Sub cmdCancel_Click()
    DoCmd.Quit
End Sub

' 记住上次工号（tblSession: skey TEXT, val TEXT）
Private Sub SaveEmpNo(ByVal v As String)
    On Error Resume Next
    CurrentDb.Execute "DELETE FROM tblSession WHERE skey='lastEmpNo'"
    If Len(v) > 0 Then
        CurrentDb.Execute "INSERT INTO tblSession(skey,val) VALUES('lastEmpNo','" & Replace(v, "'", "''") & "')"
    End If
End Sub
'@

# frmMain 的窗体代码模块：登录守卫 + 显示当前操作人（姓名(工号)） + 扫码录入
# （KeyDown 必须把 KeyCode 置 0，表达式事件做不到，故必须用代码模块）
$frmMainModule = @'
Option Compare Database
Option Explicit

Private Sub Form_Open(Cancel As Integer)
    If Not SysM.RequireLogin() Then
        Cancel = True
    End If
End Sub

Private Sub Form_Load()
    Me.lblWelcome = "欢迎，" & SysM.OperatorName
End Sub

Private Sub txtGlobalScan_KeyDown(KeyCode As Integer, Shift As Integer)
    If KeyCode = vbKeyReturn Then
        Dim kw As String
        kw = Trim$(Me.txtGlobalScan & "")
        If Len(kw) > 0 Then
            DoCmd.OpenForm "frmScanQuery", , , , , acDialog, kw
            Me.txtGlobalScan = ""
        End If
        KeyCode = 0
    End If
End Sub
'@

$forms = @(
  @{ name='frmLogin'; caption='晓镜书阁 · 登录'; width=9000; module=$frmLoginModule; onLoad=$true; popup=$true; modal=$true; borderStyle=3; autoCenter=$true; controls=@(
      @{t=$AcLabel;  n='lblTitle';    l=400;  p=300;  w=8000; h=400; cap='晓镜书阁 · 书店进销存  Access 客户端 v1.0'; fc=8388608},
      @{t=$AcLabel;  n='lblUser';     l=400;  p=900;  w=1200; h=300; cap='工号'},
      @{t=$AcTextBox;n='txtUsername'; l=1800; p=880;  w=3000; h=300; def='E001'},
      @{t=$AcLabel;  n='lblPwd';      l=400;  p=1300; w=1200; h=300; cap='密码'},
      @{t=$AcTextBox;n='txtPassword'; l=1800; p=1280; w=3000; h=300; im='Password'},
      @{t=$AcCheckBox;n='chkRemember';l=1800; p=1700; w=2500; h=300; cap='记住工号'},
      @{t=$AcButton; n='cmdLogin';    l=1800; p=2100; w=1500; h=400; cap='登 录'; ev='OnClick'; evv='[Event Procedure]'},
      @{t=$AcButton; n='cmdCancel';   l=3500; p=2100; w=1500; h=400; cap='退 出'; ev='OnClick'; evv='[Event Procedure]'},
      @{t=$AcLabel;  n='lblMsg';      l=400;  p=2650; w=7000; h=300; cap=''; fc=255},
      @{t=$AcLabel;  n='lblServer';   l=400;  p=3000; w=8000; h=300; cap='服务：http://localhost:8080/api'},
      @{t=$AcLabel;  n='lblVer';      l=400;  p=3300; w=2000; h=300; cap='v1.0'}
  )},
  @{ name='frmMain'; caption='晓镜书阁 · 主控台'; width=13000; module=$frmMainModule; onOpen=$true; onLoad=$true; controls=@(
      @{t=$AcLabel;  n='lblWelcome';    l=400; p=300;  w=7000; h=300; cap='欢迎'},
      @{t=$AcTextBox;n='txtGlobalScan'; l=400; p=700;  w=9000; h=300; ev='OnKeyDown'; evv='[Event Procedure]'},
      @{t=$AcLabel;  n='lblScanHint';   l=9500;p=700;  w=3000; h=300; cap='全局扫码：扫 ISBN 回车即查'},
      @{t=$AcLabel;  n='lblKpi1';       l=400; p=1200; w=2800; h=400; cap='SKU'},
      @{t=$AcLabel;  n='lblKpi2';       l=3400;p=1200; w=2800; h=400; cap='在库'},
      @{t=$AcLabel;  n='lblKpi3';       l=6400;p=1200; w=2800; h=400; cap='库存金额'},
      @{t=$AcLabel;  n='lblKpi4';       l=9400;p=1200; w=2800; h=400; cap='预警'},
      @{t=$AcButton; n='cmdStockQuery'; l=400; p=1800; w=2200; h=450; cap='库存在线查询'; ev='OnClick'; evv='=modUtil.NavOpenForm("frmStockQuery")'},
      @{t=$AcButton; n='cmdScanQuery';  l=2700;p=1800; w=2200; h=450; cap='扫码查询';     ev='OnClick'; evv='=modUtil.NavOpenForm("frmScanQuery")'},
      @{t=$AcButton; n='cmdCheck';      l=5000;p=1800; w=2200; h=450; cap='库存盘点';     ev='OnClick'; evv='=modUtil.NavOpenForm("frmCheck")'},
      @{t=$AcButton; n='cmdPurchaseIn'; l=7300;p=1800; w=2200; h=450; cap='进货入库';     ev='OnClick'; evv='=modUtil.NavOpenForm("frmPurchaseIn")'},
      @{t=$AcButton; n='cmdSaleOut';    l=400; p=2350; w=2200; h=450; cap='出货 / POS';   ev='OnClick'; evv='=modUtil.NavOpenForm("frmSaleOut")'},
      @{t=$AcButton; n='cmdOrderQuery'; l=2700;p=2350; w=2200; h=450; cap='单据查询';     ev='OnClick'; evv='=modUtil.NavOpenForm("frmOrderQuery")'},
      @{t=$AcButton; n='cmdReport';     l=5000;p=2350; w=2200; h=450; cap='报表中心';     ev='OnClick'; evv='=modUtil.NavOpenForm("frmReport")'},
      @{t=$AcButton; n='cmdLogout';     l=11000;p=300; w=1500; h=400; cap='注销'; ev='OnClick'; evv='=modLogin.DoLogout'},
      @{t=$AcLabel;  n='lblTime';       l=9500;p=2900; w=3000; h=300; cap=''}
  )},
  @{ name='frmStockQuery'; caption='库存在线查询'; width=13000; module=(New-GuardModule 'frmStockQuery'); onOpen=$true; controls=@(
      @{t=$AcComboBox;n='cboWarehouse'; l=400; p=300; w=2200; h=300; rst='Value List'; rs='全部'},
      @{t=$AcComboBox;n='cboCategory';  l=2700;p=300; w=2200; h=300; rst='Value List'; rs='全部'},
      @{t=$AcTextBox; n='txtKeyword';   l=5000;p=300; w=3000; h=300},
      @{t=$AcCheckBox;n='chkOnlyLow';   l=8100;p=300; w=2000; h=300; cap='仅低库存'},
      @{t=$AcButton;  n='cmdSearch';    l=400; p=700; w=1500; h=400; cap='查 询'},
      @{t=$AcButton;  n='cmdReset';     l=2000;p=700; w=1500; h=400; cap='重 置'},
      @{t=$AcButton;  n='cmdExport';    l=3600;p=700; w=1500; h=400; cap='导 出'},
      @{t=$AcLabel;   n='lblSummary';   l=5300;p=700; w=7000; h=300; cap='概览：—'},
      @{t=$AcListBox; n='lstStockList'; l=400; p=1200;w=12300;h=4000;rst='Value List'},
      @{t=$AcComboBox;n='cboPageSize';  l=400; p=5400;w=1200; h=300; rst='Value List'; rs='10;20;50;100'},
      @{t=$AcButton;  n='cmdPrev';      l=1800;p=5400;w=1200; h=400; cap='上一页'},
      @{t=$AcButton;  n='cmdNext';      l=3100;p=5400;w=1200; h=400; cap='下一页'},
      @{t=$AcLabel;   n='lblPage';      l=4500;p=5400;w=2000; h=300; cap='第 1/1 页'},
      @{t=$AcLabel;   n='lblCount';     l=6700;p=5400;w=3000; h=300; cap='共 0 条'}
  )},
  @{ name='frmScanQuery'; caption='扫码查询'; width=11000; module=(New-GuardModule 'frmScanQuery'); onOpen=$true; controls=@(
      @{t=$AcTextBox;n='txtScan';    l=400;  p=300;  w=6000; h=350},
      @{t=$AcButton; n='cmdQuery';   l=6500; p=300;  w=1500; h=400; cap='查 询'},
      @{t=$AcLabel;  n='lblScanHint';l=400;  p=700;  w=9000; h=300; cap='将光标置于扫码框，用扫码枪扫描 ISBN（自动回车）'},
      @{t=$AcLabel;  n='lblBookName';l=400;  p=1150; w=7000; h=300; cap='书名：'},
      @{t=$AcLabel;  n='lblAuthor';  l=400;  p=1500; w=4000; h=300; cap='作者：'},
      @{t=$AcLabel;  n='lblPublisher';l=4500;p=1500; w=4000; h=300; cap='出版社：'},
      @{t=$AcLabel;  n='lblCategory';l=400;  p=1850; w=4000; h=300; cap='分类：'},
      @{t=$AcLabel;  n='lblISBN';    l=4500; p=1850; w=4000; h=300; cap='ISBN：'},
      @{t=$AcLabel;  n='lblShelfNo'; l=400;  p=2200; w=4000; h=300; cap='货架位：'},
      @{t=$AcLabel;  n='lblPrice';   l=4500; p=2200; w=2000; h=300; cap='定价：'},
      @{t=$AcLabel;  n='lblSalePrice';l=6500;p=2200; w=2000; h=300; cap='零售：'},
      @{t=$AcLabel;  n='lblWarehouse';l=400; p=2600; w=3000; h=300; cap='仓库：'},
      @{t=$AcLabel;  n='lblStockQty';l=3400; p=2600; w=2500; h=300; cap='在库：'},
      @{t=$AcLabel;  n='lblSafety';  l=5900; p=2600; w=2500; h=300; cap='安全：'},
      @{t=$AcLabel;  n='lblStatus';  l=8400; p=2600; w=2000; h=300; cap='状态：'},
      @{t=$AcButton; n='cmdSaveQR';  l=400;  p=3050; w=2000; h=400; cap='保存二维码图片'},
      @{t=$AcButton; n='cmdOpenCheck';l=2500;p=3050; w=2000; h=400; cap='加入盘点'},
      @{t=$AcTextBox;n='txtLog';     l=400;  p=3600; w=10000;h=1800}
  )},
  @{ name='frmCheck'; caption='库存盘点'; width=13000; module=(New-GuardModule 'frmCheck'); onOpen=$true; controls=@(
      @{t=$AcComboBox;n='cboCheckStatus'; l=400; p=300; w=2000; h=300; rst='Value List'; rs='全部;DRAFT;FINISHED;CANCELLED'},
      @{t=$AcButton;  n='cmdReload';      l=2600;p=300; w=1600; h=400; cap='刷 新'},
      @{t=$AcButton;  n='cmdNewCheck';    l=4300;p=300; w=2000; h=400; cap='新建盘点单'},
      @{t=$AcListBox; n='lstOrders';      l=400; p=800; w=12300;h=2200;rst='Value List'},
      @{t=$AcComboBox;n='cboWarehouseNew';l=400; p=3150;w=2200; h=300; rst='Value List'},
      @{t=$AcComboBox;n='cboCheckType';   l=2700;p=3150;w=1600; h=300; rst='Value List'; rs='FULL;PART;CYCLE'},
      @{t=$AcTextBox; n='txtRemarkNew';   l=4400;p=3150;w=3500; h=300},
      @{t=$AcButton;  n='cmdCreateCheck'; l=8000;p=3150;w=1600; h=400; cap='创建'},
      @{t=$AcTextBox; n='txtScanCheck';   l=400; p=3650;w=5000; h=350},
      @{t=$AcTextBox; n='txtRealQty';     l=5500;p=3650;w=1200; h=300; def='1'},
      @{t=$AcComboBox;n='cboEntryMode';   l=6800;p=3650;w=1500; h=300; rst='Value List'; rs='累加;覆写'},
      @{t=$AcListBox; n='lstCheckItems';  l=400; p=4100;w=12300;h=2200;rst='Value List'},
      @{t=$AcLabel;   n='lblStats';       l=400; p=6400;w=8000; h=300; cap='品种 0 · 差异 0 · 盘盈 0 · 盘亏 0'},
      @{t=$AcButton;  n='cmdSubmitItems'; l=400; p=6750;w=2000; h=400; cap='提交实盘数'},
      @{t=$AcButton;  n='cmdFinish';      l=2500;p=6750;w=2000; h=400; cap='完成盘点'},
      @{t=$AcButton;  n='cmdCancelCheck'; l=4600;p=6750;w=1500; h=400; cap='作 废'}
  )},
  @{ name='frmPurchaseIn'; caption='进货入库'; width=13000; module=(New-GuardModule 'frmPurchaseIn'); onOpen=$true; controls=@(
      @{t=$AcComboBox;n='cboStatus';  l=400; p=300; w=2000; h=300; rst='Value List'; rs='全部;DRAFT;SUBMITTED;INSTOCK;CANCELLED'},
      @{t=$AcComboBox;n='cboSupplier';l=2600;p=300; w=2200; h=300; rst='Value List'},
      @{t=$AcComboBox;n='cboWarehouse';l=5000;p=300;w=2000; h=300; rst='Value List'},
      @{t=$AcTextBox; n='txtKeyword'; l=7200;p=300; w=2500; h=300},
      @{t=$AcTextBox; n='dtStart';    l=400; p=700; w=1500; h=300},
      @{t=$AcTextBox; n='dtEnd';      l=2000;p=700; w=1500; h=300},
      @{t=$AcButton;  n='cmdSearch';  l=3700;p=650; w=1500; h=400; cap='查 询'},
      @{t=$AcTextBox; n='txtScanIn';  l=5400;p=700; w=3000; h=300},
      @{t=$AcListBox; n='lstOrders';  l=400; p=1200;w=12300;h=2600;rst='Value List'},
      @{t=$AcListBox; n='lstItems';   l=400; p=3900;w=12300;h=2000;rst='Value List'},
      @{t=$AcLabel;   n='lblStat';    l=400; p=6000;w=8000; h=300; cap='单数 0 · 数量 0 · 金额 ¥0.00'},
      @{t=$AcButton;  n='cmdInstock'; l=400; p=6350;w=1800; h=400; cap='确认入库'},
      @{t=$AcButton;  n='cmdSubmit';  l=2300;p=6350;w=1500; h=400; cap='提 交'},
      @{t=$AcButton;  n='cmdCancel';  l=3900;p=6350;w=1500; h=400; cap='作 废'}
  )},
  @{ name='frmSaleOut'; caption='出货 / POS 收银'; width=13000; module=(New-GuardModule 'frmSaleOut'); onOpen=$true; controls=@(
      @{t=$AcTextBox; n='txtScan';     l=400; p=300; w=4000; h=350},
      @{t=$AcButton;  n='cmdAdd';      l=4500;p=300; w=1500; h=400; cap='加 入'},
      @{t=$AcComboBox;n='cboWarehouse';l=6200;p=300; w=2200; h=300; rst='Value List'},
      @{t=$AcListBox; n='lstCart';     l=400; p=800; w=12300;h=2200;rst='Value List'},
      @{t=$AcComboBox;n='cboCustomer'; l=400; p=3150;w=2000; h=300; rst='Value List'},
      @{t=$AcComboBox;n='cboPayment';  l=2600;p=3150;w=2000; h=300; rst='Value List'; rs='CASH;WECHAT;ALIPAY;CARD;CREDIT'},
      @{t=$AcTextBox; n='txtDiscount'; l=4800;p=3150;w=1200; h=300; def='0'},
      @{t=$AcLabel;   n='lblTotal';    l=6200;p=3150;w=2500; h=300; cap='应收：¥0.00'},
      @{t=$AcTextBox; n='txtPayAmount';l=8800;p=3150;w=1500; h=300},
      @{t=$AcButton;  n='cmdCheckout'; l=400; p=3650;w=2200; h=450; cap='结 算（出库）'},
      @{t=$AcButton;  n='cmdClearCart';l=2700;p=3650;w=1500; h=450; cap='清 空'},
      @{t=$AcLabel;   n='lblPosMsg';   l=4400;p=3650;w=6000; h=300; cap=''},
      @{t=$AcListBox; n='lstSaleOrders';l=400;p=4200;w=12300;h=2000;rst='Value List'}
  )},
  @{ name='frmOrderQuery'; caption='单据查询'; width=13000; module=(New-GuardModule 'frmOrderQuery'); onOpen=$true; controls=@(
      @{t=$AcComboBox;n='cboDocType'; l=400; p=300; w=2000; h=300; rst='Value List'; rs='进货单;出货单;盘点单'},
      @{t=$AcComboBox;n='cboStatus';  l=2600;p=300; w=2200; h=300; rst='Value List'},
      @{t=$AcTextBox; n='txtKeyword'; l=5000;p=300; w=3000; h=300},
      @{t=$AcTextBox; n='dtStart';    l=8200;p=300; w=1500; h=300},
      @{t=$AcTextBox; n='dtEnd';      l=9800;p=300; w=1500; h=300},
      @{t=$AcButton;  n='cmdSearch';  l=400; p=700; w=1500; h=400; cap='查 询'},
      @{t=$AcListBox; n='lstDocs';    l=400; p=1200;w=12300;h=4200;rst='Value List'},
      @{t=$AcLabel;   n='lblCount';   l=400; p=5500;w=3000; h=300; cap='共 0 条'},
      @{t=$AcButton;  n='cmdSaveQR';  l=3600;p=5500;w=2000; h=400; cap='保存二维码'}
  )},
  @{ name='frmReport'; caption='报表中心'; width=13000; module=(New-GuardModule 'frmReport'); onOpen=$true; controls=@(
      @{t=$AcComboBox;n='cboReport';  l=400; p=300; w=5000; h=300; rst='Value List'},
      @{t=$AcLabel;   n='lblParams';  l=5600;p=300; w=7000; h=300; cap='参数：'},
      @{t=$AcLabel;   n='lblParamName1';l=400;p=800; w=1600; h=300; cap='参数1：'},
      @{t=$AcTextBox; n='txtParam1';  l=2100;p=780; w=2200; h=300},
      @{t=$AcLabel;   n='lblParamName2';l=4400;p=800;w=1600; h=300; cap='参数2：'},
      @{t=$AcTextBox; n='txtParam2';  l=6100;p=780; w=2200; h=300},
      @{t=$AcLabel;   n='lblParamName3';l=8400;p=800;w=1600; h=300; cap='参数3：'},
      @{t=$AcTextBox; n='txtParam3';  l=10100;p=780;w=2200; h=300},
      @{t=$AcLabel;   n='lblParamName4';l=400;p=1200;w=1600; h=300; cap='参数4：'},
      @{t=$AcTextBox; n='txtParam4';  l=2100;p=1180;w=2200; h=300},
      @{t=$AcButton;  n='cmdPreview'; l=400; p=1700;w=1800; h=450; cap='预 览'; ev='OnClick'; evv='=modReportBuild.PreviewReport(modReport.ReportCodeOf([cboReport]),'''')'},
      @{t=$AcButton;  n='cmdPrint';   l=2300;p=1700;w=1800; h=450; cap='打 印'; ev='OnClick'; evv='=modReportBuild.PrintReport(modReport.ReportCodeOf([cboReport]),'''')'},
      @{t=$AcButton;  n='cmdExportXlsx';l=4200;p=1700;w=2200;h=450; cap='导出 Excel'},
      @{t=$AcButton;  n='cmdExportCsv'; l=6500;p=1700;w=2200;h=450; cap='导出 CSV'},
      @{t=$AcLabel;   n='lblStatus';  l=400; p=2300;w=10000;h=300; cap='状态：'},
      @{t=$AcLabel;   n='lblSummary'; l=400; p=2650;w=12000;h=300; cap='汇总：'}
  )}
)
foreach ($spec in $forms) {
    try { New-AccessForm $app $spec }
    catch { Log "创建窗体 $($spec.name) 失败：$($_.Exception.Message)" }
}

# 清理重试过程中可能残留的自动命名窗体（如「窗体1」/「Form1」）
foreach ($f in @($app.CurrentProject.AllForms)) {
    if ($f.Name -match '^(窗体|Form)\d+$') {
        try { $app.DoCmd.DeleteObject($AcForm, $f.Name); Log "清理残留窗体 $($f.Name)" } catch {}
    }
}

# ---------------------------------------------------------------------
# 5. 创建原生报表 rptR01 / rptR03 / rptR07 / rptR11
#    做法：先建一张「绑定 tmp_<code> 的辅助窗体」，SaveAsText 导出其定义文本，
#    再把文本改写为报表定义（Begin Form→Begin Report、改名节），LoadFromText 导入，
#    最后删除辅助窗体。（本例 CreateControl 不支持报表，故用换写法）
# ---------------------------------------------------------------------
$reportDefs = @(
  @{ code='R01'; src='tmp_R01'; keys=@('orderNo','outTime','bookName','isbn','quantity','price','amount','costPrice','paymentType','operatorName') },
  @{ code='R03'; src='tmp_R03'; keys=@('rank','bookName','isbn','categoryName','qty','amount','profit') },
  @{ code='R07'; src='tmp_R07'; keys=@('warehouseName','categoryName','bookName','isbn','shelfNo','quantity','safetyStock','avgCost','stockAmount') },
  @{ code='R11'; src='tmp_R11'; keys=@('bookName','isbn','qty','amount','cost','profit','profitRate') }
)

# 为报表临时表建占位表（运行时由 modReportBuild 重建；此处仅供设计期绑定）
foreach ($rd in $reportDefs) {
    $cols = ($rd.keys | ForEach-Object { "[$_] TEXT(255)" }) -join ','
    try { $app.DoCmd.RunSQL("CREATE TABLE $($rd.src) ($cols)") } catch { Log "建占位表 $($rd.src) 失败：$($_.Exception.Message)" }
}

# 报表换写：需剔除的「窗体专有」属性（报表级，缩进 4）
$skipKeys = @('Checksum','DividingLines','DefaultView','NavigationButtons','RecordSelectors','ScrollBars',
  'BorderStyle','RecordLocks','AllowDesignChanges','DatasheetFontName','DatasheetFontHeight','DatasheetFontWeight',
  'DatasheetGridlinesBehavior','DatasheetGridlinesColor','DatasheetBackColor','DatasheetAlternateBackColor',
  'DatasheetGridlinesColor12','DatasheetForeColor','TabularCharSet','TabularFamily','ShowPageMargins',
  'DisplayOnSharePointSite','FilterOnLoad','FitToScreen','GridY','ItemSuffix','PictureAlignment','AutoResize',
  'Picture','PictureType','PictureTiling','MaxRecButton','MinMaxButtons','OnLoad','OnOpen','Name','RecordSource')

function New-AccessReport($app, $code, $src, $keys) {
    $rptName = "rpt$code"
    $helper  = "frmTmp_$code"
    $maxTry  = 6
    for ($attempt = 1; $attempt -le $maxTry; $attempt++) {
        $f = $null
        try {
            # 清理上一轮可能的残留
            try { $app.DoCmd.DeleteObject($AcReport, $rptName) } catch {}
            try { $app.DoCmd.DeleteObject($AcForm, $helper) } catch {}
            # 1) 辅助窗体（绑定源表 + 绑定字段控件）
            $frm = $app.CreateForm(); $f = $frm.Name
            Start-Sleep -Milliseconds 120
            $frm.RecordSource = $src
            $x = 200
            foreach ($k in $keys) {
                $c = $app.CreateControl($f, $AcTextBox, 0, $null, $null, $x, 100, 1900, 300)
                $c.Name = "col_$k"
                $c.ControlSource = $k
                $x += 1950
            }
            $app.DoCmd.Close($AcForm, $f, 1)
            $app.DoCmd.Rename($helper, $AcForm, $f)
            # 2) 导出定义文本
            $dump = Join-Path $TmpDir "$helper.txt"
            $app.SaveAsText($AcForm, $helper, $dump)
            # 3) 改写为报表定义
            $out = New-Object System.Collections.Generic.List[string]
            foreach ($ln in (Get-Content $dump)) {
                $t = $ln.Trim()
                if ($t -eq 'Begin Form') { $out.Add('Begin Report'); $out.Add(('    RecordSource ="{0}"' -f $src)); continue }
                if ($t -eq 'Begin FormHeader') { $out.Add('    Begin ReportHeader'); continue }
                if ($t -eq 'Begin FormFooter') { $out.Add('    Begin ReportFooter'); continue }
                $indent = $ln.Length - $ln.TrimStart().Length
                $key = (($t -split '=| <')[0]).Trim()
                if ($indent -eq 4 -and ($skipKeys -contains $key)) { continue }
                $out.Add($ln)
            }
            $rep = Join-Path $TmpDir "rpt$code.txt"
            Write-Utf8NoBom $rep (@($out) -join "`r`n")
            # 4) 导入为报表
            $app.LoadFromText($AcReport, $rptName, $rep)
            # 5) 删除辅助窗体
            $app.DoCmd.DeleteObject($AcForm, $helper)
            Log "已创建报表 $rptName（绑定 $src，$($keys.Count) 列）"
            return
        } catch {
            if ($f) { try { $app.DoCmd.Close($AcForm, $f, 2) } catch {} }
            if ($attempt -eq $maxTry) {
                Log "创建报表 $rptName 失败（已重试 $maxTry 次）：$($_.Exception.Message)"
            } else {
                Start-Sleep -Milliseconds 500
            }
        }
    }
}

foreach ($rd in $reportDefs) {
    try { New-AccessReport $app $rd.code $rd.src $rd.keys }
    catch { Log "创建报表 $($rd.code) 失败：$($_.Exception.Message)" }
}
# 说明：报表的登录守卫不在此处注入代码模块。实测在报表设计视图里置 HasModule=$true 会弹
#       「另存为」模态框并阻塞构建；报表的开关全部经由 modReportBuild.BuildReportTable，
#       故守卫统一放在该入口（见 vba/modReportBuild.bas）。

# ---------------------------------------------------------------------
# 6. 设置数据库属性（StartupForm / AppTitle 等）
#    背景：PowerShell 直调 CurrentDb 会因 DAO 类型库加载失败（TYPE_E_CANTLOADLIBRARY），
#          故把设置逻辑放进 Access 内执行。做法：把临时 VBA 代码【追加】到已存在的
#          标准模块 modUtil 上覆盖导入并调用，随后还原原始 modUtil —— 全程不新增/删除
#          模块，因此不会出现「删除模块不落盘」导致的残留组件（最终恰为 10 个模块）。
# ---------------------------------------------------------------------
$origModUtilPath = Join-Path $Root 'vba\modUtil.bas'
$origModUtil     = [System.IO.File]::ReadAllText($origModUtilPath)

function Invoke-VbaViaModUtil($app, $code, $proc) {
    $tmp = Join-Path $TmpDir ('modUtil_tmp_' + [guid]::NewGuid().ToString('N') + '.bas')
    Write-Utf8NoBom $tmp ($origModUtil + "`r`n" + $code)
    $app.LoadFromText($AcModule, 'modUtil', $tmp)          # 覆盖 modUtil（临时注入）
    try { return $app.Run($proc) }
    finally { $app.LoadFromText($AcModule, 'modUtil', $origModUtilPath) }   # 还原原始 modUtil
}

$setupCode = @'

' ==== 晓镜书阁构建临时注入（运行后即被还原） ====
Public Function ShuHaiGeSetup() As String
    Dim db As DAO.Database
    Set db = CurrentDb
    ShuHaiGeSetProp db, "StartupForm", "frmLogin"
    ShuHaiGeSetProp db, "AppTitle", "晓镜书阁 · 书店进销存（Access 客户端）"
    ShuHaiGeSetProp db, "StartupShowDBWindow", True
    ShuHaiGeSetProp db, "StartupShowStatusBar", True
    On Error Resume Next
    ShuHaiGeSetup = CStr(db.Properties("StartupForm"))   ' 回读校验
    db.Close
End Function

Private Sub ShuHaiGeSetProp(ByVal db As DAO.Database, ByVal nm As String, ByVal v As Variant)
    On Error Resume Next
    db.Properties(nm) = v
    If Err.Number <> 0 Then
        Err.Clear
        Dim p As DAO.Property
        Set p = db.CreateProperty(nm, IIf(VarType(v) = vbString, dbText, dbBoolean), v)
        db.Properties.Append p
    End If
    On Error GoTo 0
End Sub
'@
try {
    $sfSet = Invoke-VbaViaModUtil $app $setupCode 'ShuHaiGeSetup'
    Log "已设置数据库属性：StartupForm=$sfSet、AppTitle、StartupShowDBWindow（modUtil 已还原）"
} catch { Log "设置数据库属性失败：$($_.Exception.Message)" }

# ---------------------------------------------------------------------
# 7. 收尾：保存并退出
# ---------------------------------------------------------------------
try { $app.DoCmd.RunCommand(126) } catch {}   # 编译并保存所有模块
# 注意：实测本机 $app.CloseCurrentDatabase() 会长时间阻塞（RPC 无响应）；改用
#       DoCmd.Quit(acQuitSaveAll=1)「保存全部并退出」，能正确落盘 VBA 工程（模块增删）。
try { $app.DoCmd.Quit(1) } catch { try { $app.Quit() } catch {} }
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($app) | Out-Null
$app = $null
# 等待 Access 进程自然退出以完成落盘（最多 20 秒），避免中途强杀损坏 VBA 工程存储
if ($appPid) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ((Get-Process -Id $appPid -ErrorAction SilentlyContinue) -and $sw.Elapsed.TotalSeconds -lt 20) { Start-Sleep -Milliseconds 300 }
    if (Get-Process -Id $appPid -ErrorAction SilentlyContinue) { Stop-Process -Id $appPid -Force -ErrorAction SilentlyContinue; Log "Access 未自行退出，已强制结束 PID=$appPid" }
    else { Log "Access 已正常退出（PID=$appPid）" }
}
Start-Sleep -Seconds 2

if (-not (Test-Path $DbPath)) { Log "错误：未生成 $DbPath"; exit 1 }
$exists = Get-Item $DbPath
Log ("已生成文件：{0}  大小={1} 字节（{2:N0} KB）" -f $exists.Name, $exists.Length, ($exists.Length/1KB))

# ---------------------------------------------------------------------
# 8. 验证：重新打开 .accdb，枚举模块 / 窗体 / 报表 / 启动窗体
# ---------------------------------------------------------------------
$procBefore2 = @(Get-Process MSACCESS -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id)
$app2 = New-Object -ComObject Access.Application
Start-Sleep -Milliseconds 400
$app2Pid = @(Get-Process MSACCESS -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id | Where-Object { $procBefore2 -notcontains $_ } | Select-Object -First 1)
$app2.Visible = $false
$app2.OpenCurrentDatabase($DbPath)
try { $app2.DoCmd.SetWarnings($false) } catch {}

# 用「按索引」枚举，规避 AllModules 集合枚举器在本机偶发的 STG_E_FILENOTFOUND
function Get-ObjNames($coll) {
    $r = @()
    try { for ($i = 0; $i -lt $coll.Count; $i++) { $r += $coll.Item($i).Name } } catch {}
    return ,$r
}
$mods = Get-ObjNames $app2.CurrentProject.AllModules
$frms = Get-ObjNames $app2.CurrentProject.AllForms
$rpts = Get-ObjNames $app2.CurrentProject.AllReports
Log ("验证 · 模块 {0} 个：{1}" -f $mods.Count, ($mods -join ', '))
Log ("验证 · 窗体 {0} 个：{1}" -f $frms.Count, ($frms -join ', '))
Log ("验证 · 报表 {0} 个：{1}" -f $rpts.Count, ($rpts -join ', '))

# 验证 · VBA 组件清单（应含类模块 sysManager 与 9 个窗体 + 4 个报表模块）
$vbComps = @()
try {
    $vbp = $app2.VBE.VBProjects.Item(1)
    for ($i = 1; $i -le $vbp.VBComponents.Count; $i++) { $vbComps += $vbp.VBComponents.Item($i).Name }
} catch { Log "枚举 VBA 组件失败：$($_.Exception.Message)" }
Log ("验证 · VBA 组件 {0} 个：{1}" -f $vbComps.Count, ($vbComps -join ', '))
if ($vbComps -contains 'sysManager') { Log "验证 · 类模块 sysManager 存在 ✓" } else { Log "验证 · 类模块 sysManager 缺失 ✗" }

# 验证 · 用 SaveAsText 导出定义文本，检查窗体事件绑定
#   （不打开对象窗口——实测在报表设计视图置 HasModule 会弹「另存为」模态框并阻塞）
foreach ($nm in $frms) {
    if ($nm -match '^frmTmp_') { continue }
    try {
        $dump = Join-Path $TmpDir ("verify_" + $nm + ".txt")
        $app2.SaveAsText($AcForm, $nm, $dump)
        $txt = Get-Content -Raw $dump
        $mo = [regex]::Match($txt, 'OnOpen\s*=\s*"[^"]*"')
        $ml = [regex]::Match($txt, 'OnLoad\s*=\s*"[^"]*"')
        $moV = if ($mo.Success) { $mo.Value } else { '(未设置)' }
        $mlV = if ($ml.Success) { $ml.Value } else { '(未设置)' }
        Log ("验证 · {0}：{1}  {2}" -f $nm, $moV, $mlV)
    } catch { Log "验证 · $nm 导出失败：$($_.Exception.Message)" }
}
foreach ($nm in $rpts) {
    try {
        $dump = Join-Path $TmpDir ("verify_" + $nm + ".txt")
        $app2.SaveAsText($AcReport, $nm, $dump)
        $txt = Get-Content -Raw $dump
        $rs = [regex]::Match($txt, 'RecordSource\s*=\s*"[^"]*"')
        Log ("验证 · {0}：{1}（报表守卫在 modReportBuild.BuildReportTable 入口）" -f $nm, $(if ($rs.Success) { $rs.Value } else { '(无记录源)' }))
    } catch { Log "验证 · $nm 导出失败：$($_.Exception.Message)" }
}

# 验证 · 编译探针：Application.Run 只接受裸过程名（不接受「模块.过程」），且零参数函数不能带空括号
#   - 不探 SysM：返回对象，Application.Run 无法把对象回传给 COM 调用方（0x800A0062）；
#     其可用性由 IsLoggedIn / CurrentToken（内部均经由 SysM 取用）间接证明。
#   - 不探 RequireLogin：未登录时会弹模态登录框，阻塞构建脚本。
foreach ($proc in @('IsLoggedIn','CurrentToken')) {
    try { $app2.Run($proc) | Out-Null; Log "验证 · 编译探针 $proc ✓" }
    catch { Log "验证 · 编译探针 $proc ✗ $($_.Exception.Message)" }
}

# 读取启动窗体（同样借 modUtil 临时注入执行，随后还原）
$verifyCode = @'

' ==== 晓镜书阁校验临时注入（运行后即被还原） ====
Public Function StartupFormName() As String
    On Error Resume Next
    StartupFormName = CStr(CurrentDb.Properties("StartupForm"))
End Function
'@
try {
    $sf = Invoke-VbaViaModUtil $app2 $verifyCode 'StartupFormName'
    if ("$sf" -ne '') { Log "验证 · 启动窗体 = $sf" } else { Log "验证 · 启动窗体为空" }
} catch { Log "读取启动窗体失败：$($_.Exception.Message)" }

try { $app2.DoCmd.Quit(1) } catch { try { $app2.Quit() } catch {} }
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($app2) | Out-Null
if ($app2Pid) {
    $sw2 = [System.Diagnostics.Stopwatch]::StartNew()
    while ((Get-Process -Id $app2Pid -ErrorAction SilentlyContinue) -and $sw2.Elapsed.TotalSeconds -lt 20) { Start-Sleep -Milliseconds 300 }
    if (Get-Process -Id $app2Pid -ErrorAction SilentlyContinue) { Stop-Process -Id $app2Pid -Force -ErrorAction SilentlyContinue }
}

if ($failedModules.Count -gt 0) { Log ("注意：以下模块导入失败并跳过：{0}" -f ($failedModules -join ', ')) }
Log "构建完成。"
Remove-Item $TmpDir -Recurse -Force -ErrorAction SilentlyContinue