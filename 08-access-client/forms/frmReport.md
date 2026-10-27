# frmReport · 报表中心

> 15 张报表（R01~R15）统一入口。接口：`GET /api/report/{code}?<params>`、
> `GET /api/report/{code}/export?format=xlsx|csv`。
> 取数后由 `modReportBuild` 落临时表并用 `rptR01~rptR15` 预览/打印。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmReport` |
| 记录源 | 无 |
| 标题 | 报表中心 |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `cboReport` | ComboBox | RowSource=ReportCatalog 文本 | 报表选择（R01~R15） |
| `lblParams` | Label | — | 显示所选报表参数说明 |
| `txtParam1..4` | Text Box | 可见性按报表联动 | 参数输入 |
| `lblParamName1..4` | Label | — | 参数名标签 |
| `dtDate` | Text Box | Format="Short Date" | 日期类参数（配合 txtParam） |
| `cmdPreview` | Command Button | Caption="预览" | 取数+打开报表 |
| `cmdPrint` | Command Button | Caption="打印" | 直接打印 |
| `cmdExportXlsx` | Command Button | Caption="导出 Excel" | xlsx |
| `cmdExportCsv` | Command Button | Caption="导出 CSV" | csv |
| `lblStatus` | Label | — | 状态/条数 |
| `lblSummary` | Label | — | 汇总文本 |

## 3. 布局描述

```
┌──────── 报表中心 ───────────────────────────────────────────┐
│ 报表：[R01 销售日报表 ▼]                                     │
│ 参数：date [2026-10-12]                                      │
│                                                             │
│ [预览]  [打印]  [导出 Excel]  [导出 CSV]                     │
│                                                             │
│ 状态：取数成功，共 128 行                                     │
│ 汇总：单据数：32  总数量：210  销售总额：¥8,900 …            │
│                                                             │
│ （预览将在报表窗口 rptRxx 中打开）                           │
└─────────────────────────────────────────────────────────────┘
```

## 4. 事件过程

### Form_Load：填充报表清单
```vba
Private Sub Form_Load()
    Dim cat As Variant, i As Long
    cat = ReportCatalog()
    Me.cboReport.RowSourceType = "Value List"
    Me.cboReport.RowSource = ""
    For i = 1 To cat.Count
        Me.cboReport.AddItem JStr(DictGet(cat(i), "code")) & "  " & JStr(DictGet(cat(i), "label"))
    Next
    Me.cboReport = "R01  销售日报表"
    Me.cboReport_AfterUpdate
End Sub
```

### 报表切换：展示参数提示、配置输入框
```vba
Private Sub cboReport_AfterUpdate()
    Dim code As String: code = ReportCodeOf(Me.cboReport)
    Me.lblParams = ParamHint(code)
    ConfigParamInputs code
    Me.lblStatus = "": Me.lblSummary = ""
End Sub

Private Function ParamHint(ByVal code As String) As String
    Dim cat As Variant, i As Long
    cat = ReportCatalog()
    For i = 1 To cat.Count
        If JStr(DictGet(cat(i), "code")) = code Then
            ParamHint = "参数：" & JStr(DictGet(cat(i), "params"))
            Exit Function
        End If
    Next
End Function

' 按报表参数显隐输入框（示例：R01 仅 date；R03 用 start/end/limit）
Private Sub ConfigParamInputs(ByVal code As String)
    Dim names() As String, i As Long
    Select Case code
        Case "R01": names = Split("date", ",")
        Case "R02": names = Split("month", ",")
        Case "R03": names = Split("startDate,endDate,limit", ",")
        Case "R04", "R06", "R11": names = Split("startDate,endDate", ",")
        Case "R05": names = Split("startDate,endDate,supplierId", ",")
        Case "R07": names = Split("warehouseId,categoryId", ",")
        Case "R08", "R09": names = Split("warehouseId", ",")
        Case "R10": names = Split("checkNo,startDate,endDate", ",")
        Case "R12": names = Split("customerId,startDate,endDate", ",")
        Case "R13": names = Split("levelId", ",")
        Case "R14": names = Split("bookId,warehouseId,startDate,endDate", ",")
        Case "R15": names = Split("month,warehouseId", ",")
    End Select
    For i = 0 To 3
        If i <= UBound(names) Then
            Me("lblParamName" & (i + 1)).Caption = names(i) & "："
            Me("lblParamName" & (i + 1)).Visible = True
            Me("txtParam" & (i + 1)).Visible = True
            Me("txtParam" & (i + 1)).Tag = names(i)
        Else
            Me("lblParamName" & (i + 1)).Visible = False
            Me("txtParam" & (i + 1)).Visible = False
            Me("txtParam" & (i + 1)).Tag = ""
        End If
    Next
End Sub
```

### 组装查询串
```vba
Private Function BuildQuery() As String
    Dim i As Long, qs As String, nm As String, val As String
    For i = 1 To 4
        nm = Nz(Me("txtParam" & i).Tag, "")
        val = Trim$(Me("txtParam" & i) & "")
        If Len(nm) > 0 And Len(val) > 0 Then
            qs = qs & IIf(Len(qs) > 0, "&", "") & nm & "=" & UrlEnc(val)
        End If
    Next
    BuildQuery = qs
End Function
```

### 预览 / 打印 / 导出
```vba
Private Sub cmdPreview_Click()
    Dim code As String: code = ReportCodeOf(Me.cboReport)
    If Not RequirePerm("report") Then Exit Sub
    Dim qs As String: qs = BuildQuery()
    If Not modReportBuild.BuildReportTable(code, qs) Then Exit Sub
    Me.lblStatus = "取数成功，标题：" & gRptTitle & "，临时表：tmp_" & code
    Me.lblSummary = gRptSummary
    On Error Resume Next
    DoCmd.OpenReport "rpt" & code, acViewPreview
    If Err.Number <> 0 Then MsgErr "未找到报表对象 rpt" & code & "，请按 reports/ 规格创建。": Err.Clear
    On Error GoTo 0
End Sub

Private Sub cmdPrint_Click()
    Dim code As String: code = ReportCodeOf(Me.cboReport)
    If Not modReportBuild.BuildReportTable(code, BuildQuery()) Then Exit Sub
    DoCmd.OpenReport "rpt" & code, acViewNormal
End Sub

Private Sub cmdExportXlsx_Click(): DoExport "xlsx": End Sub
Private Sub cmdExportCsv_Click(): DoExport "csv": End Sub

Private Sub DoExport(ByVal fmt As String)
    Dim code As String: code = ReportCodeOf(Me.cboReport)
    Dim ext As String: ext = IIf(fmt = "xlsx", ".xlsx", ".csv")
    Dim f As String: f = CurrentProject.Path & "\" & code & "_" & Format$(Now, "yyyymmddhhnn") & ext
    If ExportReport(code, fmt, BuildQuery(), f) Then
        MsgInfo "已导出：" & f
    Else
        MsgErr "导出失败：" & gLastError
    End If
End Sub
```

## 5. 报表对象命名与绑定

| code | 报表对象 | 记录源 | 预览入口 |
|---|---|---|---|
| R01 | `rptR01` | `tmp_R01` | `DoCmd.OpenReport "rptR01"` |
| R02 | `rptR02` | `tmp_R02` | … |
| … | … | … | … |
| R15 | `rptR15` | `tmp_R15` | `DoCmd.OpenReport "rptR15"` |

每个报表的字段名 = 该报表 `columns[].key`（见 `reports/reports-spec.md`）。`modReportBuild.BuildReportTable` 会按返回的 `columns` 动态 `CREATE TABLE`，字段名与契约一致，无需手工建表。

## 6. 对应 VBA

`modReport.GetReport/ExportReport/ReportCatalog/ReportCodeOf`、
`modReportBuild.BuildReportTable/PreviewReport/PrintReport`、`modUtil.*`。

## 7. 备注

- 报表打印设置（页边距、页眉、A4/小票、分组汇总）见 `reports/reports-spec.md` 各报表条目。
- 导出直接落地为文件，与 Web 端导出的列/顺序一致（同一 `/report/{code}` 契约）。