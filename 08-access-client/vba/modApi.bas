Attribute VB_Name = "modApi"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modApi
'  通用 REST 客户端（WinHTTP）+ 轻量 JSON 解析器（无第三方依赖）
'  契约来源：01-docs/09-接口设计.md
'    - 基础路径 /api，认证头 Authorization: Bearer <token>
'    - 成功 {code:0,message:"ok",data:{...}}
'    - 分页 data:{total,page,size,records:[...]}
'  依赖：WinHttp.WinHttpRequest.5.1、ADODB.Stream、Scripting.Dictionary（均系统自带，晚期绑定）
' =====================================================================

' ---------------- 全局常量 ----------------
Public Const API_BASE As String = "http://localhost:8080/api"
Public Const API_TIMEOUT As Long = 60000

' ---------------- 轻量 JSON 解析器内部状态 ----------------
' 注意：模块级变量必须声明在 Declarations 段（首个过程之前）；若声明在过程之后，
'       VBA 会视为未定义，并在使用处报「变量未定义」。
Private m_jSrc As String
Private m_jPos As Long
Private m_jLen As Long

' ---------------- 全局会话状态 ----------------
Public gToken       As String   ' JWT
Public gUsername    As String   ' 登录账号
Public gRealName    As String   ' 姓名
Public gRoles       As String   ' 角色编码，形如 "|ADMIN|MANAGER|"
Public gPermissions As String   ' 权限编码，形如 "|stock:list|purchase:in|"
Public gEmpNo       As String   ' 工号
Public gUserId      As Long     ' 用户ID
Public gLastStatus  As Long     ' 最近一次 HTTP 状态码
Public gLastError   As String   ' 最近一次错误文本
Public gLoginIsGuard As Boolean ' True：frmLogin 正作为登录守卫弹出（避免递归打开 frmMain）

' 全局系统管理员实例（用户要求的全局变量 sysMan），记录当前用户 ID 及其会话
Public sysMan As sysManager

' 惰性取用全局 sysManager：标准模块的对象变量在工程重置后会失效，故统一走本函数取用
Public Function SysM() As sysManager
    If sysMan Is Nothing Then Set sysMan = New sysManager
    Set SysM = sysMan
End Function

' =====================================================================
'  一、HTTP 基础层
' =====================================================================

' 发送请求，返回解析后的 JSON 信封（Dictionary/Collection/标量）；失败返回 Empty
Private Function DoRequest(ByVal method As String, ByVal path As String, _
                           ByVal query As String, ByVal body As String) As Variant
    Dim url As String, http As Object, txt As String
    url = API_BASE & path
    If Len(query) > 0 Then url = url & "?" & query
    gLastError = ""
    On Error GoTo EH

    Set http = CreateObject("WinHttp.WinHttpRequest.5.1")
    http.Open method, url, False
    http.setRequestHeader "Content-Type", "application/json;charset=UTF-8"
    http.setRequestHeader "Accept", "application/json"
    If Len(gToken) > 0 Then http.setRequestHeader "Authorization", "Bearer " & gToken
    ' 解析/连接/发送/接收 超时（毫秒）
    http.SetTimeouts 5000, 5000, 10000, API_TIMEOUT

    ' 注意：与 ResponseBody 同理，不能把返回 Byte() 的函数结果直接作为实参传给
    '       http.Send（VBA 会报「错误的参数号或无效的属性赋值」450）；必须先赋给
    '       局部 Byte() 变量再传入。
    Dim reqBody() As Byte
    If Len(body) > 0 Then
        reqBody = Utf8Bytes(body)
        http.Send reqBody
    Else
        http.Send
    End If

    gLastStatus = http.Status
    ' 注意：不能把 Variant 形态的 http.ResponseBody 直接传给 Utf8String(ByRef b() As Byte)，
    '       否则编译期报「类型不匹配: 缺少数组或用户定义类型」，导致整个 HTTP 层无法编译。
    '       必须先赋给 Byte() 局部变量再传入。
    Dim rawBody() As Byte
    rawBody = http.ResponseBody
    txt = Utf8String(rawBody)
    If Len(Trim$(txt)) = 0 Then
        If http.Status >= 400 Then gLastError = "HTTP " & http.Status
        DoRequest = Empty
        Exit Function
    End If
    AssignVariant DoRequest, JsonParse(txt)
    Exit Function

EH:
    gLastStatus = -1
    gLastError = "网络请求失败：" & Err.Description & "  [" & url & "]"
    DoRequest = Empty
End Function

Public Function ApiGet(ByVal path As String, Optional ByVal query As String = "") As Variant
    AssignVariant ApiGet, DoRequest("GET", path, query, "")
End Function

Public Function ApiPost(ByVal path As String, Optional ByVal body As String = "{}") As Variant
    AssignVariant ApiPost, DoRequest("POST", path, "", body)
End Function

Public Function ApiPut(ByVal path As String, Optional ByVal body As String = "{}") As Variant
    AssignVariant ApiPut, DoRequest("PUT", path, "", body)
End Function

Public Function ApiDel(ByVal path As String, Optional ByVal body As String = "") As Variant
    AssignVariant ApiDel, DoRequest("DELETE", path, "", body)
End Function

' 下载二进制（二维码 PNG / 报表 xlsx）。成功返回 True 并回填 bytes
Public Function ApiGetBinary(ByVal path As String, ByVal query As String, ByRef bytes() As Byte) As Boolean
    Dim url As String, http As Object
    url = API_BASE & path
    If Len(query) > 0 Then url = url & "?" & query
    gLastError = ""
    On Error GoTo EH
    Set http = CreateObject("WinHttp.WinHttpRequest.5.1")
    http.Open "GET", url, False
    If Len(gToken) > 0 Then http.setRequestHeader "Authorization", "Bearer " & gToken
    http.SetTimeouts 5000, 5000, 10000, API_TIMEOUT
    http.Send
    gLastStatus = http.Status
    If http.Status = 200 Then
        bytes = http.ResponseBody
        ApiGetBinary = True
    Else
        gLastError = "下载失败 HTTP " & http.Status
    End If
    Exit Function
EH:
    gLastStatus = -1
    gLastError = "下载失败：" & Err.Description
End Function

' =====================================================================
'  二、响应信封辅助
' =====================================================================

Public Function ApiSuccess(ByVal resp As Variant) As Boolean
    If IsObject(resp) Then
        If TypeName(resp) = "Dictionary" Then
            If resp.Exists("code") Then
                ApiSuccess = (resp("code") = 0)
                Exit Function
            End If
        End If
    End If
End Function

Public Function ApiMessage(ByVal resp As Variant) As String
    If IsObject(resp) Then
        If TypeName(resp) = "Dictionary" Then
            If resp.Exists("message") Then ApiMessage = CStr(resp("message"))
        End If
    End If
    If Len(ApiMessage) = 0 Then ApiMessage = gLastError
End Function

' 取 data 节点（可能是 Dictionary / Collection / 标量）
Public Function ApiData(ByVal resp As Variant) As Variant
    Dim v As Variant
    If IsObject(resp) Then
        If TypeName(resp) = "Dictionary" Then
            If resp.Exists("data") Then AssignVariant v, resp("data"): AssignVariant ApiData, v
        End If
    End If
End Function

' 分页响应里取 records（Collection）
Public Function ApiRecords(ByVal resp As Variant) As Variant
    Dim v As Variant
    AssignVariant v, JsonPath(resp, "data.records")
    AssignVariant ApiRecords, v
End Function

' 统一错误提示（供窗体调用）
Public Function ApiCheckOrWarn(ByVal resp As Variant, Optional ByVal action As String = "") As Boolean
    ApiCheckOrWarn = ApiSuccess(resp)
    If Not ApiCheckOrWarn Then
        MsgBox "操作失败" & IIf(Len(action) > 0, "（" & action & "）", "") & "：" & _
               vbCrLf & ApiMessage(resp), vbExclamation, "晓镜书阁 · Access"
    End If
End Function

' =====================================================================
'  三、字符编码（JSON 请求体 / 响应体走 UTF-8）
' =====================================================================

Public Function Utf8Bytes(ByVal s As String) As Byte()
    Dim st As Object
    Set st = CreateObject("ADODB.Stream")
    st.Type = 2              ' adTypeText
    st.Charset = "utf-8"
    st.Open
    st.WriteText s
    st.Position = 0
    st.Type = 1              ' adTypeBinary
    Utf8Bytes = st.Read
    st.Close
End Function

Public Function Utf8String(ByRef b() As Byte) As String
    Dim st As Object
    If UBound(b) < 0 Then Exit Function
    Set st = CreateObject("ADODB.Stream")
    st.Type = 1
    st.Open
    st.Write b
    st.Position = 0
    st.Type = 2
    st.Charset = "utf-8"
    Utf8String = st.ReadText
    st.Close
End Function

Public Sub SaveBytes(ByRef b() As Byte, ByVal filePath As String)
    Dim st As Object
    Set st = CreateObject("ADODB.Stream")
    st.Type = 1
    st.Open
    st.Write b
    st.SaveToFile filePath, 2   ' adSaveCreateOverWrite
    st.Close
End Sub

' =====================================================================
'  四、轻量 JSON 解析器（递归下降，1-based 光标）
'    对象 -> Scripting.Dictionary（键区分大小写关闭）
'    数组 -> VBA.Collection（1-based）
'    字符串-> String，数字-> Double，true/false-> Boolean，null-> Null
'    （内部状态 m_jSrc/m_jPos/m_jLen 声明在文件顶部 Declarations 段）
' =====================================================================

Public Function JsonParse(ByVal s As String) As Variant
    Dim v As Variant
    m_jSrc = s
    m_jLen = Len(s)
    m_jPos = 1
    SkipWs
    ParseValue v
    AssignVariant JsonParse, v
End Function

Private Sub ParseValue(ByRef outVal As Variant)
    Dim c As String
    If m_jPos > m_jLen Then JsonErr "内容意外结束"
    c = PeekChar()
    Select Case c
        Case "{": Set outVal = ParseObject()
        Case "[": Set outVal = ParseArray()
        Case """": outVal = ParseString()
        Case "t", "f": outVal = ParseBool()
        Case "n": ParseLiteral "null": outVal = Null
        Case Else: outVal = ParseNumber()
    End Select
End Sub

Private Function ParseObject() As Object
    Dim d As Object, key As String, v As Variant, ch As String
    Set d = CreateObject("Scripting.Dictionary")
    d.CompareMode = 1                          ' TextCompare（键不区分大小写）
    Consume "{"
    SkipWs
    If PeekChar() = "}" Then Consume "}": Set ParseObject = d: Exit Function
    Do While m_jPos <= m_jLen
        SkipWs
        key = ParseString()
        SkipWs
        ExpectChar ":"
        SkipWs
        ParseValue v
        ' 注意：VBA 不能用 Let 把持有对象的 Variant 存入 Dictionary 项目（运行时报 450/438），对象必须走 Set
        If IsObject(v) Then
            Set d(key) = v
        Else
            d(key) = v
        End If
        SkipWs
        ch = PeekChar()
        If ch = "," Then
            Consume ","
        ElseIf ch = "}" Then
            Consume "}"
            Exit Do
        Else
            JsonErr "对象缺少 ',' 或 '}'"
        End If
    Loop
    Set ParseObject = d
End Function

Private Function ParseArray() As Collection
    Dim col As Object, v As Variant, ch As String
    Set col = New Collection
    Consume "["
    SkipWs
    If PeekChar() = "]" Then Consume "]": Set ParseArray = col: Exit Function
    Do While m_jPos <= m_jLen
        SkipWs
        ParseValue v
        col.Add v
        SkipWs
        ch = PeekChar()
        If ch = "," Then
            Consume ","
        ElseIf ch = "]" Then
            Consume "]"
            Exit Do
        Else
            JsonErr "数组缺少 ',' 或 ']'"
        End If
    Loop
    Set ParseArray = col
End Function

Private Function ParseString() As String
    Dim sb As String, c As String, e As String, h As String
    ExpectChar """"
    Do While m_jPos <= m_jLen
        c = Mid$(m_jSrc, m_jPos, 1)
        m_jPos = m_jPos + 1
        If c = """" Then
            ParseString = sb
            Exit Function
        ElseIf c = "\" Then
            e = Mid$(m_jSrc, m_jPos, 1)
            m_jPos = m_jPos + 1
            Select Case e
                Case """": sb = sb & """"
                Case "\": sb = sb & "\"
                Case "/": sb = sb & "/"
                Case "b": sb = sb & Chr(8)
                Case "f": sb = sb & Chr(12)
                Case "n": sb = sb & vbLf
                Case "r": sb = sb & vbCr
                Case "t": sb = sb & vbTab
                Case "u"
                    h = Mid$(m_jSrc, m_jPos, 4)
                    m_jPos = m_jPos + 4
                    sb = sb & ChrW(CLng("&H" & h))
                Case Else: sb = sb & e
            End Select
        Else
            sb = sb & c
        End If
    Loop
    JsonErr "字符串未闭合"
End Function

Private Function ParseNumber() As Double
    Dim sb As String, c As String
    Do While m_jPos <= m_jLen
        c = Mid$(m_jSrc, m_jPos, 1)
        If (c >= "0" And c <= "9") Or c = "-" Or c = "+" Or c = "." Or c = "e" Or c = "E" Then
            sb = sb & c
            m_jPos = m_jPos + 1
        Else
            Exit Do
        End If
    Loop
    If Len(sb) = 0 Then JsonErr "非法数字"
    ParseNumber = CDbl(sb)
End Function

Private Function ParseBool() As Boolean
    If Mid$(m_jSrc, m_jPos, 4) = "true" Then
        m_jPos = m_jPos + 4
        ParseBool = True
    ElseIf Mid$(m_jSrc, m_jPos, 5) = "false" Then
        m_jPos = m_jPos + 5
        ParseBool = False
    Else
        JsonErr "非法布尔值"
    End If
End Function

Private Sub ParseLiteral(ByVal lit As String)
    If Mid$(m_jSrc, m_jPos, Len(lit)) = lit Then
        m_jPos = m_jPos + Len(lit)
    Else
        JsonErr "非法字面量 " & lit
    End If
End Sub

Private Sub SkipWs()
    Dim c As String
    Do While m_jPos <= m_jLen
        c = Mid$(m_jSrc, m_jPos, 1)
        If c = " " Or c = vbTab Or c = vbCr Or c = vbLf Then
            m_jPos = m_jPos + 1
        Else
            Exit Do
        End If
    Loop
End Sub

Private Function PeekChar() As String
    If m_jPos <= m_jLen Then PeekChar = Mid$(m_jSrc, m_jPos, 1)
End Function

Private Sub Consume(ByVal c As String)
    If Mid$(m_jSrc, m_jPos, 1) = c Then
        m_jPos = m_jPos + 1
    Else
        JsonErr "期望 '" & c & "'"
    End If
End Sub

Private Sub ExpectChar(ByVal c As String)
    SkipWs
    Consume c
End Sub

Private Sub JsonErr(ByVal msg As String)
    Err.Raise 5121, "JsonParse", "JSON 解析错误：" & msg & "（位置 " & m_jPos & "）"
End Sub

' Variant 安全赋值：对象走 Set，值走 Let
Public Sub AssignVariant(ByRef dst As Variant, ByRef src As Variant)
    If IsObject(src) Then
        Set dst = src
    Else
        dst = src
    End If
End Sub

' -------- 路径导航：root 中按 "a.b.0.c" 取子节点（数组用 0 基下标） --------
Public Function JsonPath(ByVal root As Variant, ByVal path As String) As Variant
    Dim parts() As String, i As Long, cur As Variant, tmp As Variant, idx As Long
    AssignVariant cur, root
    If Len(path) = 0 Then AssignVariant JsonPath, cur: Exit Function
    parts = Split(path, ".")
    For i = LBound(parts) To UBound(parts)
        If Not IsObject(cur) Then JsonPath = Null: Exit Function
        If TypeName(cur) = "Dictionary" Then
            If cur.Exists(parts(i)) Then
                AssignVariant tmp, cur(parts(i))
                AssignVariant cur, tmp
            Else
                JsonPath = Null: Exit Function
            End If
        ElseIf TypeName(cur) = "Collection" Then
            If Not IsNumeric(parts(i)) Then JsonPath = Null: Exit Function
            idx = CLng(parts(i)) + 1          ' JSON 0 基 -> Collection 1 基
            If idx >= 1 And idx <= cur.Count Then
                AssignVariant tmp, cur(idx)
                AssignVariant cur, tmp
            Else
                JsonPath = Null: Exit Function
            End If
        Else
            JsonPath = Null: Exit Function
        End If
    Next
    AssignVariant JsonPath, cur
End Function