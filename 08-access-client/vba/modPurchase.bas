Attribute VB_Name = "modPurchase"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modPurchase
'  进货管理：09-接口设计.md §5
'    GET  /purchase/orders           列表
'    GET  /purchase/orders/{id}      详情（含 items）
'    POST /purchase/orders           新建（草稿）
'    PUT  /purchase/orders/{id}      修改（仅 DRAFT）
'    POST /purchase/orders/{id}/submit   提交
'    POST /purchase/orders/{id}/instock  确认入库
'    POST /purchase/orders/{id}/cancel   作废
'    GET  /purchase/orders/{id}/logs     操作流水
' =====================================================================

Public Function ListPurchaseOrders(ByVal status As String, ByVal supplierId As String, _
                                   ByVal warehouseId As String, ByVal keyword As String, _
                                   ByVal startDate As String, ByVal endDate As String, _
                                   ByVal page As Long, ByVal size As Long) As Variant
    AssignVariant ListPurchaseOrders, ApiGet("/purchase/orders", Q("status=" & status, "supplierId=" & supplierId, _
        "warehouseId=" & warehouseId, "keyword=" & keyword, "startDate=" & startDate, _
        "endDate=" & endDate, "page=" & page, "size=" & size))
End Function

Public Function GetPurchaseOrder(ByVal id As Long) As Variant
    AssignVariant GetPurchaseOrder, ApiGet("/purchase/orders/" & id)
End Function

' 新建进货单（草稿）。itemsJSON 形如 [{"bookId":1,"quantity":10,"price":35.00,"batchNo":"","remark":""}]
Public Function CreatePurchaseOrder(ByVal supplierId As Long, ByVal warehouseId As Long, _
                                    ByVal settleType As String, ByVal discount As Double, _
                                    ByVal paidAmount As Double, ByVal remark As String, _
                                    ByVal itemsJSON As String) As Variant
    Dim body As String
    If Len(itemsJSON) = 0 Then itemsJSON = "[]"
    body = "{" & JNumPair("supplierId", supplierId) & "," & JNumPair("warehouseId", warehouseId) & "," & _
           JStrPair("settleType", settleType) & "," & JNumPair("discount", discount) & "," & _
           JNumPair("paidAmount", paidAmount) & "," & JStrPair("remark", remark) & "," & _
           """items"":" & itemsJSON & "}"
    AssignVariant CreatePurchaseOrder, ApiPost("/purchase/orders", body)
End Function

Public Function SubmitPurchaseOrder(ByVal id As Long) As Variant
    AssignVariant SubmitPurchaseOrder, ApiPost("/purchase/orders/" & id & "/submit", "{}")
End Function

' 确认入库：写入库存与移动加权成本、流水，状态->INSTOCK
Public Function InstockOrder(ByVal id As Long) As Variant
    AssignVariant InstockOrder, ApiPost("/purchase/orders/" & id & "/instock", "{}")
End Function

Public Function CancelPurchaseOrder(ByVal id As Long) As Variant
    AssignVariant CancelPurchaseOrder, ApiPost("/purchase/orders/" & id & "/cancel", "{}")
End Function

Public Function PurchaseLogs(ByVal id As Long) As Variant
    AssignVariant PurchaseLogs, ApiGet("/purchase/orders/" & id & "/logs")
End Function

' 便捷：构造进货明细项 JSON 片段
Public Function PurchaseItemJSON(ByVal bookId As Long, ByVal qty As Double, ByVal price As Double) As String
    PurchaseItemJSON = "{" & JNumPair("bookId", bookId) & "," & JNumPair("quantity", qty) & "," & _
        JNumPair("price", price) & ",""batchNo"":"""",""remark"":"""" & "}"
End Function