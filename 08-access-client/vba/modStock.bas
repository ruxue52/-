Attribute VB_Name = "modStock"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modStock
'  库存查询：09-接口设计.md §7 / §11
'    GET /stock/list、/stock/by-book/{id}、/stock/warnings、/stock/summary、/stock/flows
'    GET /mobile/stock/quick
' =====================================================================

' 库存列表（分页）。返回完整信封，records 含 bookName,isbn,warehouseName,quantity,safetyStock,avgCost,stockAmount
Public Function StockList(ByVal warehouseId As String, ByVal categoryId As String, _
                          ByVal keyword As String, ByVal onlyLow As Boolean, _
                          ByVal page As Long, ByVal size As Long) As Variant
    Dim qs As String
    qs = Q("warehouseId=" & warehouseId, "categoryId=" & categoryId, "keyword=" & keyword, _
           "onlyLow=" & IIf(onlyLow, "true", "false"), "page=" & page, "size=" & size)
    AssignVariant StockList, ApiGet("/stock/list", qs)
End Function

' 某图书各仓库库存明细
Public Function StockByBook(ByVal bookId As Long) As Variant
    AssignVariant StockByBook, ApiGet("/stock/by-book/" & bookId)
End Function

' 低库存预警（quantity < safetyStock）
Public Function StockWarnings(ByVal warehouseId As String) As Variant
    AssignVariant StockWarnings, ApiGet("/stock/warnings", Q("warehouseId=" & warehouseId))
End Function

' 库存概览：totalSku,totalQty,totalAmount,warningCount
Public Function StockSummary() As Variant
    AssignVariant StockSummary, ApiGet("/stock/summary")
End Function

' 库存流水：bookId,warehouseId,bizType,startDate,endDate,page,size
Public Function StockFlows(ByVal bookId As String, ByVal warehouseId As String, ByVal bizType As String, _
                           ByVal startDate As String, ByVal endDate As String, _
                           ByVal page As Long, ByVal size As Long) As Variant
    AssignVariant StockFlows, ApiGet("/stock/flows", Q("bookId=" & bookId, "warehouseId=" & warehouseId, _
           "bizType=" & bizType, "startDate=" & startDate, "endDate=" & endDate, _
           "page=" & page, "size=" & size))
End Function

' 移动端快速查（扫码/输入 keyword -> 图书+库存+货架位）
Public Function MobileStockQuick(ByVal keyword As String) As Variant
    AssignVariant MobileStockQuick, ApiGet("/mobile/stock/quick", Q("keyword=" & keyword))
End Function

' 便捷：库存概览文本
Public Function StockSummaryText() As String
    Dim r As Variant
    AssignVariant r, StockSummary()
    If Not ApiSuccess(r) Then StockSummaryText = "读取失败：" & ApiMessage(r): Exit Function
    StockSummaryText = "SKU " & FmtNum(JNum(JsonPath(r, "data.totalSku"))) & _
        "  ·  在库 " & FmtNum(JNum(JsonPath(r, "data.totalQty")), 2) & _
        "  ·  库存金额 ¥" & FmtMoney(JsonPath(r, "data.totalAmount")) & _
        "  ·  预警 " & FmtNum(JNum(JsonPath(r, "data.warningCount"))) & " 项"
End Function