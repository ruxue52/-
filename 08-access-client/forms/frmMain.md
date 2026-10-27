# frmMain · 主控台 / 导航窗体

> 登录成功后的总导航。聚合「库存概览 KPI」，提供各功能窗体入口。数据源：`GET /api/stock/summary`、`GET /api/mobile/overview`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmMain` |
| 记录源 | 无 |
| 标题 | 晓镜书阁 · 主控台 |
| 边框 | Sizable |
| 导航按钮/记录选择器 | 否 |
| 初始视图 | Single Form（最大化） |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `lblWelcome` | Label | — | "欢迎，<真实姓名>（角色）" |
| `lblKpi1..4` | Label ×4 | — | SKU / 在库量 / 库存金额 / 预警数 |
| `cmdStockQuery` | Command Button | Caption="库存在线查询" | → `frmStockQuery` |
| `cmdScanQuery` | Command Button | Caption="扫码查询" | → `frmScanQuery` |
| `cmdCheck` | Command Button | Caption="库存盘点" | → `frmCheck` |
| `cmdPurchaseIn` | Command Button | Caption="进货入库" | → `frmPurchaseIn` |
| `cmdSaleOut` | Command Button | Caption="出货 / POS" | → `frmSaleOut` |
| `cmdOrderQuery` | Command Button | Caption="单据查询" | → `frmOrderQuery` |
| `cmdReport` | Command Button | Caption="报表中心" | → `frmReport` |
| `cmdLogout` | Command Button | Caption="注销" | 清会话并回登录 |
| `txtGlobalScan` | Text Box | — | 全局扫码框：扫码枪输入 ISBN 回车 → 跳扫码查询 |
| `lblTime` | Label | — | 当前时间（Timer 刷新） |

## 3. 布局描述

```
┌──────── 晓镜书阁 · 主控台 ─────────────── 欢迎，系统管理员（管理员）  [注销] ┐
│  全局扫码：[ 扫 ISBN 回车即查 ……………………… ]                             │
│ ┌──────────┐┌──────────┐┌──────────┐┌──────────┐                        │
│ │ SKU      ││ 在库数量  ││ 库存金额  ││ 预警项   │                        │
│ │ 1,234    ││ 8,900.00 ││ ¥ 123,456││ 12       │                        │
│ └──────────┘└──────────┘└──────────┘└──────────┘                        │
│ [库存在线查询] [扫码查询] [库存盘点] [进货入库]                          │
│ [出货 / POS  ] [单据查询] [报表中心]                                     │
│                                         2026-10-12 09:30:00             │
└──────────────────────────────────────────────────────────────────────────┘
```

## 4. 事件过程

### Form_Load
```vba
Private Sub Form_Load()
    If Not IsLoggedIn() Then
        DoCmd.Close acForm, Me.Name
        DoCmd.OpenForm "frmLogin"
        Exit Sub
    End If
    Me.lblWelcome = "欢迎，" & gRealName & "（" & gUsername & "）"
    RefreshKpi
    Me.TimerInterval = 1000
End Sub

Private Sub RefreshKpi()
    Dim r As Variant
    r = StockSummary()
    If ApiSuccess(r) Then
        Me.lblKpi1 = "SKU  " & FmtNum(JsonPath(r, "data.totalSku"))
        Me.lblKpi2 = "在库  " & FmtNum(JsonPath(r, "data.totalQty"), 2)
        Me.lblKpi3 = "金额  ¥" & FmtMoney(JsonPath(r, "data.totalAmount"))
        Me.lblKpi4 = "预警  " & FmtNum(JsonPath(r, "data.warningCount")) & " 项"
    Else
        Me.lblKpi1 = "概览读取失败：" & ApiMessage(r)
    End If
End Sub
```

### cmd*_Click（导航统一写法）
```vba
Private Sub cmdStockQuery_Click():  DoCmd.OpenForm "frmStockQuery":  End Sub
Private Sub cmdScanQuery_Click():   DoCmd.OpenForm "frmScanQuery":   End Sub
Private Sub cmdCheck_Click():       DoCmd.OpenForm "frmCheck":       End Sub
Private Sub cmdPurchaseIn_Click():  DoCmd.OpenForm "frmPurchaseIn":  End Sub
Private Sub cmdSaleOut_Click():     DoCmd.OpenForm "frmSaleOut":     End Sub
Private Sub cmdOrderQuery_Click():  DoCmd.OpenForm "frmOrderQuery":  End Sub
Private Sub cmdReport_Click():      DoCmd.OpenForm "frmReport":      End Sub
```

### txtGlobalScan_KeyDown（扫码枪 = 键盘回车）
```vba
Private Sub txtGlobalScan_KeyDown(KeyCode As Integer, Shift As Integer)
    If KeyCode = vbKeyReturn Then
        Dim kw As String: kw = Trim$(Me.txtGlobalScan & "")
        If Len(kw) > 0 Then
            DoCmd.OpenForm "frmScanQuery", , , , , acDialog, kw ' OpenArgs 传入扫码内容
            Me.txtGlobalScan = ""
        End If
        KeyCode = 0
    End If
End Sub
```

### cmdLogout_Click
```vba
Private Sub cmdLogout_Click()
    If MsgConfirm("确认注销当前账号？") Then
        DoLogout
        DoCmd.Close acForm, Me.Name
        DoCmd.OpenForm "frmLogin"
    End If
End Sub

Private Sub Form_Timer()
    Me.lblTime = Format$(Now, "yyyy-mm-dd hh:nn:ss")
End Sub
```

## 5. 对应 VBA

`modStock.StockSummary`、`modLogin.IsLoggedIn/DoLogout`、`modUtil.FmtNum/FmtMoney`、`modApi.ApiSuccess/JsonPath`。

## 6. 备注

全局扫码框是本端「扫码办事」的统一入口：无论图书码、单据码、库存位码，扫码枪扫入后回车即在此捕获并分发。