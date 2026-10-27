Attribute VB_Name = "modUtil"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modUtil
'  通用工具：格式化、JSON 拼装/转义、URL 编码、消息提示、值提取
' =====================================================================

' ---------------- 格式化 ----------------
Public Function FmtMoney(ByVal v As Variant) As String
    If IsNull(v) Or IsEmpty(v) Then FmtMoney = "0.00": Exit Function
    If Not IsNumeric(v) Then FmtMoney = CStr(v): Exit Function
    FmtMoney = Format$(CDbl(v), "#,##0.00")
End Function

Public Function FmtNum(ByVal v As Variant, Optional ByVal digits As Integer = 0) As String
    If IsNull(v) Or IsEmpty(v) Then FmtNum = "0": Exit Function
    If Not IsNumeric(v) Then FmtNum = CStr(v): Exit Function
    FmtNum = Format$(CDbl(v), "#,##0" & IIf(digits > 0, "." & String(digits, "0"), ""))
End Function

Public Function FmtDate(ByVal v As Variant) As String
    If IsNull(v) Or IsEmpty(v) Then FmtDate = "": Exit Function
    If IsDate(v) Then FmtDate = Format$(CDate(v), "yyyy-mm-dd"): Else FmtDate = CStr(v)
End Function

Public Function FmtDateTime(ByVal v As Variant) As String
    If IsNull(v) Or IsEmpty(v) Then FmtDateTime = "": Exit Function
    If IsDate(v) Then FmtDateTime = Format$(CDate(v), "yyyy-mm-dd hh:nn:ss"): Else FmtDateTime = CStr(v)
End Function

Public Function TodayStr() As String
    TodayStr = Format$(Date, "yyyy-mm-dd")
End Function

Public Function NowStr() As String
    NowStr = Format$(Now, "yyyy-mm-dd hh:nn:ss")
End Function

' ---------------- JSON 值提取 ----------------
' 空值安全转字符串
Public Function JStr(ByVal v As Variant) As String
    If IsNull(v) Or IsEmpty(v) Then JStr = "": Else JStr = CStr(v)
End Function

' 空值安全转数值
Public Function JNum(ByVal v As Variant) As Double
    If IsNull(v) Or IsEmpty(v) Then JNum = 0: Exit Function
    If IsNumeric(v) Then JNum = CDbl(v)
End Function

' 从 Dictionary 取键（不存在返回 Null）
Public Function DictGet(ByVal d As Variant, ByVal key As String) As Variant
    If IsObject(d) Then
        If TypeName(d) = "Dictionary" Then
            If d.Exists(key) Then AssignVariant DictGet, d(key): Exit Function
        End If
    End If
    DictGet = Null
End Function

' 标签->值文本（用于字典映射显示）
Public Function LabelOf(ByVal v As Variant) As String
    Dim s As String
    s = UCase$(JStr(v))
    Select Case s
        Case "CASH": LabelOf = "现金"
        Case "WECHAT": LabelOf = "微信"
        Case "ALIPAY": LabelOf = "支付宝"
        Case "CARD": LabelOf = "银行卡"
        Case "CREDIT": LabelOf = "挂账"
        Case "DRAFT": LabelOf = "草稿"
        Case "SUBMITTED": LabelOf = "已提交"
        Case "INSTOCK": LabelOf = "已入库"
        Case "OUTSTOCK": LabelOf = "已完成"
        Case "CANCELLED": LabelOf = "已作废"
        Case "FINISHED": LabelOf = "已完成"
        Case "PURCHASE": LabelOf = "采购退货"
        Case "SALES": LabelOf = "销售退货"
        Case "RETAIL": LabelOf = "零售"
        Case "WHOLESALE": LabelOf = "批发"
        Case "MEMBER": LabelOf = "会员"
        Case "FULL": LabelOf = "全盘"
        Case "PART": LabelOf = "抽盘"
        Case "CYCLE": LabelOf = "循环盘"
        Case Else: LabelOf = JStr(v)
    End Select
End Function

' ---------------- JSON 拼装 ----------------
' 转义字符串中的特殊字符，用于拼 JSON
Public Function JsonEsc(ByVal s As String) As String
    Dim r As String
    r = s
    r = Replace(r, "\", "\\")
    r = Replace(r, """", "\""")
    r = Replace(r, vbCr, "\r")
    r = Replace(r, vbLf, "\n")
    r = Replace(r, vbTab, "\t")
    JsonEsc = r
End Function

' 拼 "k":"v" 片段（字符串值）
Public Function JStrPair(ByVal key As String, ByVal val As String) As String
    JStrPair = """" & key & """:""" & JsonEsc(val) & """"
End Function

' 拼 "k":n 片段（数值）
Public Function JNumPair(ByVal key As String, ByVal val As Variant) As String
    Dim n As String
    If IsNull(val) Or IsEmpty(val) Or Not IsNumeric(val) Then n = "0" Else n = CStr(CDbl(val))
    n = Replace(n, ",", ".")
    JNumPair = """" & key & """:" & n
End Function

' ---------------- URL 编码 ----------------
Public Function UrlEnc(ByVal s As String) As String
    Dim i As Long, c As String, code As Long, r As String
    For i = 1 To Len(s)
        c = Mid$(s, i, 1)
        code = AscW(c)
        If (code >= 48 And code <= 57) Or (code >= 65 And code <= 90) Or (code >= 97 And code <= 122) _
           Or c = "-" Or c = "_" Or c = "." Or c = "~" Then
            r = r & c
        ElseIf code >= 0 And code <= 127 Then
            r = r & "%" & Right$("0" & Hex$(code), 2)
        Else
            ' UTF-8 编码（中文等）
            Dim bytes() As Byte
            bytes = modApi.Utf8Bytes(c)
            Dim k As Long
            For k = LBound(bytes) To UBound(bytes)
                r = r & "%" & Right$("0" & Hex$(bytes(k)), 2)
            Next
        End If
    Next
    UrlEnc = r
End Function

' 拼查询串（跳过空值）：Q("keyword=abc","page=1") 
Public Function Q(ParamArray pairs() As Variant) As String
    Dim i As Long, s As String, p As String, eq As Long
    For i = LBound(pairs) To UBound(pairs)
        p = CStr(pairs(i))
        eq = InStr(p, "=")
        If eq > 0 Then
            If Len(Mid$(p, eq + 1)) > 0 Then
                s = s & IIf(Len(s) > 0, "&", "") & Mid$(p, 1, eq) & UrlEnc(Mid$(p, eq + 1))
            End If
        End If
    Next
    Q = s
End Function

' ---------------- 消息提示 ----------------
Public Sub MsgInfo(ByVal msg As String, Optional ByVal title As String = "晓镜书阁 · Access")
    MsgBox msg, vbInformation, title
End Sub

Public Sub MsgOk(ByVal msg As String, Optional ByVal title As String = "晓镜书阁 · Access")
    MsgBox msg, vbInformation, title
End Sub

Public Sub MsgErr(ByVal msg As String, Optional ByVal title As String = "晓镜书阁 · Access")
    MsgBox msg, vbCritical, title
End Sub

Public Function MsgConfirm(ByVal msg As String, Optional ByVal title As String = "晓镜书阁 · Access") As Boolean
    MsgConfirm = (MsgBox(msg, vbQuestion + vbYesNo, title) = vbYes)
End Function

' ---------------- 控件辅助 ----------------
' 给下拉框填充键值（分隔符用制表符）
Public Sub FillCombo(ByRef cbo As Object, ByVal items As Variant, Optional ByVal firstBlank As Boolean = False)
    Dim i As Long
    cbo.RowSourceType = "Value List"
    cbo.RowSource = ""
    If firstBlank Then cbo.AddItem ""
    If IsObject(items) Then
        For i = 1 To items.Count
            cbo.AddItem ModItemText(items(i))
        Next
    End If
End Sub

Private Function ModItemText(ByVal v As Variant) As String
    If IsObject(v) Then
        ModItemText = JStr(DictGet(v, "label")) & vbTab & JStr(DictGet(v, "value"))
    Else
        ModItemText = JStr(v)
    End If
End Function

' 取下拉框选中值（第 2 列）
Public Function ComboValue(ByRef cbo As Object) As String
    Dim i As Long
    i = InStr(cbo.Value, vbTab)
    If i > 0 Then ComboValue = Mid$(cbo.Value, i + 1) Else ComboValue = cbo.Value
End Function

' 取下拉框显示文本（第 1 列）
Public Function ComboText(ByRef cbo As Object) As String
    Dim i As Long
    i = InStr(cbo.Value, vbTab)
    If i > 0 Then ComboText = Left$(cbo.Value, i - 1) Else ComboText = cbo.Value
End Function

' 打开窗体（供窗体「表达式」事件调用：表达式中用不了 DoCmd，故在 VBA 内包一层）
' 用法：控件 OnClick = =modUtil.NavOpenForm("frmStockQuery")
Public Function NavOpenForm(ByVal formName As String) As Boolean
    Dim ok As Boolean
    On Error Resume Next
    Err.Clear
    DoCmd.OpenForm formName
    ok = (Err.Number = 0)
    On Error GoTo 0
    NavOpenForm = ok
End Function