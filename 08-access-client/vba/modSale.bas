Attribute VB_Name = "modSale"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modSale
'  出货管理：09-接口设计.md §6
'    GET  /sale/orders                    列表
'    GET  /sale/orders/{id}               详情
'    POST /sale/orders                    新建
'    PUT  /sale/orders/{id}               修改（仅 DRAFT）
'    POST /sale/orders/{id}/submit        提交
'    POST /sale/orders/{id}/outstock      确认出库（校验库存、积分）
'    POST /sale/orders/{id}/cancel        作废
'    GET  /sale/pos/search                 POS 扫码/检索 keyword -> 图书+库存
' =====================================================================

Public Function ListSaleOrders(ByVal status As String, ByVal customerId As String, _
                               ByVal warehouseId As String, ByVal keyword As String, _
                               ByVal startDate As String, ByVal endDate As String, _
                               ByVal page As Long, ByVal size As Long) As Variant
    AssignVariant ListSaleOrders, ApiGet("/sale/orders", Q("status=" & status, "customerId=" & customerId, _
        "warehouseId=" & warehouseId, "keyword=" & keyword, "startDate=" & startDate, _
        "endDate=" & endDate, "page=" & page, "size=" & size))
End Function

Public Function GetSaleOrder(ByVal id As Long) As Variant
    AssignVariant GetSaleOrder, ApiGet("/sale/orders/" & id)
End Function

' POS 扫码/检索：keyword 支持 ISBN / 书名 / 拼音码
Public Function PosSearch(ByVal keyword As String, Optional ByVal warehouseId As String = "") As Variant
    AssignVariant PosSearch, ApiGet("/sale/pos/search", Q("keyword=" & keyword, "warehouseId=" & warehouseId))
End Function

' 新建出货单。paymentType: CASH/WECHAT/ALIPAY/CARD/CREDIT
Public Function CreateSaleOrder(ByVal customerId As String, ByVal warehouseId As Long, _
                                ByVal paymentType As String, ByVal discount As Double, _
                                ByVal payAmount As Double, ByVal remark As String, _
                                ByVal itemsJSON As String) As Variant
    Dim body As String
    If Len(itemsJSON) = 0 Then itemsJSON = "[]"
    Dim custFrag As String
    If Len(customerId) > 0 Then custFrag = JNumPair("customerId", customerId) Else custFrag = """customerId"":null"
    body = "{" & custFrag & "," & JNumPair("warehouseId", warehouseId) & "," & _
           JStrPair("paymentType", paymentType) & "," & JNumPair("discount", discount) & "," & _
           JNumPair("payAmount", payAmount) & "," & JStrPair("remark", remark) & "," & _
           """items"":" & itemsJSON & "}"
    AssignVariant CreateSaleOrder, ApiPost("/sale/orders", body)
End Function

Public Function SubmitSaleOrder(ByVal id As Long) As Variant
    AssignVariant SubmitSaleOrder, ApiPost("/sale/orders/" & id & "/submit", "{}")
End Function

' 确认出库：扣减库存、写流水、累计会员积分；库存不足返回 code=1002
Public Function OutstockOrder(ByVal id As Long) As Variant
    AssignVariant OutstockOrder, ApiPost("/sale/orders/" & id & "/outstock", "{}")
End Function

Public Function CancelSaleOrder(ByVal id As Long) As Variant
    AssignVariant CancelSaleOrder, ApiPost("/sale/orders/" & id & "/cancel", "{}")
End Function

' 便捷：构造出货明细项 JSON 片段
Public Function SaleItemJSON(ByVal bookId As Long, ByVal qty As Double, ByVal price As Double) As String
    SaleItemJSON = "{" & JNumPair("bookId", bookId) & "," & JNumPair("quantity", qty) & "," & _
        JNumPair("price", price) & "}"
End Function