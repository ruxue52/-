Attribute VB_Name = "modScan"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modScan
'  扫码：09-接口设计.md §10
'    POST /scan/resolve   body {code} -> {bizType, entity, extra}
'    POST /scan/log       {code,bizType,bizId,device,result}
'    GET  /qrcode/book/{isbn}     图书二维码 PNG
'    GET  /qrcode/order/{orderNo} 单据二维码 PNG
'    GET  /qrcode/content/{code}  二维码内容字符串
'  说明：扫码枪=键盘设备，扫到内容后自动“回车”。窗体用文本框 + OnKeyDown(vbKeyReturn)
'        触发本模块。无需任何驱动/中间件。
'  二维码内容规范：
'    图书 SHG:BOOK:<isbn>      单据 SHG:ORDER:<orderNo>
'    库存位 SHG:STOCK:<warehouseId>:<isbn>   盘点 SHG:CHECK:<checkNo>:<isbn>
' =====================================================================

' 服务端解析扫码内容
Public Function ResolveScan(ByVal code As String) As Variant
    AssignVariant ResolveScan, ApiPost("/scan/resolve", "{""code"":""" & JsonEsc(code) & """}")
End Function

' 记录扫码（device 传 "PC"）
Public Function LogScan(ByVal code As String, ByVal bizType As String, ByVal bizId As String, _
                        ByVal result As String) As Variant
    Dim body As String
    body = "{" & JStrPair("code", code) & "," & JStrPair("bizType", bizType) & "," & _
           JStrPair("bizId", bizId) & ",""device"":""PC""," & JStrPair("result", result) & "}"
    AssignVariant LogScan, ApiPost("/scan/log", body)
End Function

' 由图书 ISBN 取二维码 PNG 并保存到本地文件
Public Function SaveBookQRCode(ByVal isbn As String, ByVal savePath As String) As Boolean
    Dim b() As Byte
    If ApiGetBinary("/qrcode/book/" & isbn, "", b) Then
        modApi.SaveBytes b, savePath
        SaveBookQRCode = True
    End If
End Function

Public Function SaveOrderQRCode(ByVal orderNo As String, ByVal savePath As String) As Boolean
    Dim b() As Byte
    If ApiGetBinary("/qrcode/order/" & orderNo, "", b) Then
        modApi.SaveBytes b, savePath
        SaveOrderQRCode = True
    End If
End Function

' 取二维码内容字符串（本地解析失败时可用于比对）
Public Function QRContent(ByVal code As String) As String
    Dim r As Variant
    AssignVariant r, ApiGet("/qrcode/content/" & code)
    If ApiSuccess(r) Then QRContent = JStr(ApiData(r))
End Function

' ---------------- 本地解析（离线兜底，不依赖服务端） ----------------
' 返回 Dictionary：kind(类型 BOOK/ORDER/STOCK/CHECK/UNKNOWN), isbn, orderNo, warehouseId, checkNo, raw
Public Function ParseScanCode(ByVal code As String) As Object
    Dim d As Object, parts() As String
    Set d = CreateObject("Scripting.Dictionary")
    d("raw") = code
    d("kind") = "UNKNOWN"
    d("isbn") = ""
    d("orderNo") = ""
    d("warehouseId") = ""
    d("checkNo") = ""
    parts = Split(Trim$(code), ":")
    If UBound(parts) >= 2 And UCase$(parts(0)) = "SHG" Then
        Select Case UCase$(parts(1))
            Case "BOOK"
                d("kind") = "BOOK": d("isbn") = parts(2)
            Case "ORDER"
                d("kind") = "ORDER": d("orderNo") = parts(2)
            Case "STOCK"
                d("kind") = "STOCK"
                If UBound(parts) >= 3 Then d("warehouseId") = parts(2): d("isbn") = parts(3)
            Case "CHECK"
                d("kind") = "CHECK"
                If UBound(parts) >= 3 Then d("checkNo") = parts(2): d("isbn") = parts(3)
        End Select
    ElseIf Len(Trim$(code)) >= 10 Then
        ' 纯条码：视为 ISBN/EAN
        d("kind") = "BOOK": d("isbn") = Trim$(code)
    End If
    Set ParseScanCode = d
End Function

' 便捷：从扫码内容里取 ISBN（图书/库存位/盘点均含 ISBN，可直接检索）
Public Function ExtractISBN(ByVal code As String) As String
    ExtractISBN = JStr(ParseScanCode(code)("isbn"))
End Function