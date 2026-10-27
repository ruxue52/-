Attribute VB_Name = "modCheck"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modCheck
'  库存盘点：09-接口设计.md §7 / §11
'    POST   /stock/check/orders              新建盘点单 {warehouseId,checkType,remark,bookIds:[]}
'    GET    /stock/check/orders              盘点单列表
'    GET    /stock/check/orders/{id}         盘点详情（items: bookQty/realQty/diffQty）
'    PUT    /stock/check/orders/{id}/items   批量录入实盘 {items:[{itemId,realQty}]}
'    POST   /stock/check/orders/{id}/finish  完成盘点（按差异调库存、写流水）
'    POST   /stock/check/orders/{id}/cancel  作废
'    POST   /mobile/check/scan               扫码录入 {checkId,code,realQty}
'    GET    /mobile/check/pending/{checkId}  待盘明细
' =====================================================================

Public Function ListCheckOrders(ByVal status As String, ByVal warehouseId As String, _
                                ByVal keyword As String, ByVal page As Long, ByVal size As Long) As Variant
    AssignVariant ListCheckOrders, ApiGet("/stock/check/orders", Q("status=" & status, "warehouseId=" & warehouseId, _
        "keyword=" & keyword, "page=" & page, "size=" & size))
End Function

Public Function GetCheckOrder(ByVal id As Long) As Variant
    AssignVariant GetCheckOrder, ApiGet("/stock/check/orders/" & id)
End Function

' 新建盘点单；bookIds 为空字符串表示全盘
Public Function CreateCheckOrder(ByVal warehouseId As Long, ByVal checkType As String, _
                                 ByVal remark As String, ByVal bookIdsJSON As String) As Variant
    Dim body As String
    If Len(bookIdsJSON) = 0 Then bookIdsJSON = "[]"
    body = "{" & JNumPair("warehouseId", warehouseId) & "," & _
           JStrPair("checkType", checkType) & "," & _
           JStrPair("remark", remark) & ",""bookIds"":" & bookIdsJSON & "}"
    AssignVariant CreateCheckOrder, ApiPost("/stock/check/orders", body)
End Function

' 扫码盘点录入：通过盘点标记(code=SHG:CHECK:<checkNo>:<isbn>)匹配明细并累加实盘数
Public Function ScanCheck(ByVal checkId As Long, ByVal code As String, ByVal realQty As Double) As Variant
    Dim body As String
    body = "{" & JNumPair("checkId", checkId) & "," & JStrPair("code", code) & "," & _
           JNumPair("realQty", realQty) & "}"
    AssignVariant ScanCheck, ApiPost("/mobile/check/scan", body)
End Function

' 批量录入实盘数（itemsJSON 形如 [{"itemId":1,"realQty":5},...]）
Public Function PutCheckItems(ByVal id As Long, ByVal itemsJSON As String) As Variant
    AssignVariant PutCheckItems, ApiPut("/stock/check/orders/" & id & "/items", "{""items"":" & itemsJSON & "}")
End Function

Public Function FinishCheck(ByVal id As Long) As Variant
    AssignVariant FinishCheck, ApiPost("/stock/check/orders/" & id & "/finish", "{}")
End Function

Public Function CancelCheck(ByVal id As Long) As Variant
    AssignVariant CancelCheck, ApiPost("/stock/check/orders/" & id & "/cancel", "{}")
End Function

Public Function PendingCheck(ByVal checkId As Long) As Variant
    AssignVariant PendingCheck, ApiGet("/mobile/check/pending/" & checkId)
End Function

' 便捷：由一条明细行构造 items JSON 片段
Public Function CheckItemJSON(ByVal itemId As Long, ByVal realQty As Double) As String
    CheckItemJSON = "{" & JNumPair("itemId", itemId) & "," & JNumPair("realQty", realQty) & "}"
End Function