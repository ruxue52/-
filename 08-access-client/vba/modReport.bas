Attribute VB_Name = "modReport"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modReport
'  统计报表：09-接口设计.md §9（R01~R15）
'    GET /report/{code}?<params>  -> {title,columns:[{key,label}],rows:[...],summary:{...}}
'    GET /report/{code}/export?format=xlsx|csv&<params>  -> 文件流
'  报表的“取数”由本模块完成；“落表/绑定报表”由 modReportBuild 完成。
' =====================================================================

Public gRptTitle As String   ' 最近一次报表标题（供报表 Open 事件显示）
Public gRptSummary As String ' 最近一次汇总文本

' 取报表数据（信封）
Public Function GetReport(ByVal code As String, Optional ByVal query As String = "") As Variant
    AssignVariant GetReport, ApiGet("/report/" & code, query)
End Function

' 导出报表为文件；format = "xlsx" | "csv"。成功返回 True
Public Function ExportReport(ByVal code As String, ByVal format As String, _
                             ByVal query As String, ByVal savePath As String) As Boolean
    Dim b() As Byte
    If ApiGetBinary("/report/" & code & "/export", Q("format=" & format) & IIf(Len(query) > 0, "&" & query, ""), b) Then
        modApi.SaveBytes b, savePath
        ExportReport = True
    End If
End Function

' 15 张报表的 code/名称/参数清单（供报表中心下拉）
Public Function ReportCatalog() As Variant
    Dim col As Object
    Set col = New Collection
    AddRpt col, "R01", "销售日报表", "date（必填，yyyy-MM-dd）"
    AddRpt col, "R02", "销售月报表", "month（yyyy-MM）"
    AddRpt col, "R03", "商品销售排行榜", "startDate,endDate,limit"
    AddRpt col, "R04", "分类销售汇总表", "startDate,endDate"
    AddRpt col, "R05", "采购到货明细表", "startDate,endDate,supplierId"
    AddRpt col, "R06", "供应商采购汇总表", "startDate,endDate"
    AddRpt col, "R07", "库存结存表", "warehouseId,categoryId"
    AddRpt col, "R08", "库存预警表", "warehouseId"
    AddRpt col, "R09", "库龄分析表", "warehouseId"
    AddRpt col, "R10", "盘点差异表", "checkNo,startDate,endDate"
    AddRpt col, "R11", "商品毛利分析表", "startDate,endDate"
    AddRpt col, "R12", "客户消费明细表", "customerId,startDate,endDate"
    AddRpt col, "R13", "会员积分报表", "levelId"
    AddRpt col, "R14", "库存流水台账", "bookId,warehouseId,startDate,endDate"
    AddRpt col, "R15", "进销存汇总表", "month,warehouseId"
    Set ReportCatalog = col
End Function

Private Sub AddRpt(ByRef col As Object, ByVal code As String, ByVal nm As String, ByVal prm As String)
    Dim d As Object
    Set d = CreateObject("Scripting.Dictionary")
    d("code") = code: d("label") = nm: d("params") = prm
    col.Add d
End Sub

' 由选中项取报表 code（下拉行文本 "R01  销售日报表"）
Public Function ReportCodeOf(ByVal comboRowText As String) As String
    ReportCodeOf = Left$(Trim$(comboRowText), 3)
End Function