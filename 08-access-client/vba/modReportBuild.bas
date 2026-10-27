Attribute VB_Name = "modReportBuild"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modReportBuild
'  将 /report/{code} 返回的 {columns,rows,summary} 动态落成本地临时表，
'  再以该表为记录源打开同名 Access 报表（rptR01 ~ rptR15）。
'  依赖：DAO（Access 内置）。
'  约定：临时表名 = "tmp_" & code（如 tmp_R01），报表 rptR01 的记录源即 "tmp_R01"。
' =====================================================================

' ---------------- 模块级状态 ----------------
' 注意：模块级变量必须声明在 Declarations 段（首个过程之前）；若声明在过程之后，
'       VBA 会视为未定义，并在使用处报「变量未定义」。
Private m_colKeys() As String
Private m_colLabels() As String

' 主入口：取数 -> 建表 -> 填数。返回 True 表示已就绪，可 DoCmd.OpenReport
' 登录守卫：所有报表预览/打印都经由本函数，未登录则弹登录框，失败直接中止。
Public Function BuildReportTable(ByVal code As String, ByVal query As String) As Boolean
    If Not SysM.RequireLogin() Then Exit Function
    Dim resp As Variant, cols As Variant, rows As Variant
    AssignVariant resp, GetReport(code, query)
    If Not ApiSuccess(resp) Then
        MsgErr "报表取数失败：" & ApiMessage(resp)
        Exit Function
    End If
    gRptTitle = JStr(JsonPath(resp, "data.title"))
    gRptSummary = SummaryToText(JsonPath(resp, "data.summary"))
    AssignVariant cols, JsonPath(resp, "data.columns")
    AssignVariant rows, JsonPath(resp, "data.rows")

    Dim tbl As String: tbl = "tmp_" & code
    ReDim m_colKeys(0 To 0) As String
    ReDim m_colLabels(0 To 0) As String

    ' 1) 建（重建）临时表
    If Not CreateTable(tbl, cols, rows) Then Exit Function
    ' 2) 写入数据
    If Not FillTable(tbl, cols, rows) Then Exit Function
    BuildReportTable = True
End Function

Private Function CreateTable(ByVal tbl As String, ByVal cols As Variant, ByVal rows As Variant) As Boolean
    Dim ddl As String, i As Long, n As Long, v As Variant
    On Error GoTo EH
    ' 删除同名旧表
    On Error Resume Next
    CurrentDb.Execute "DROP TABLE [" & tbl & "]", dbFailOnError
    Err.Clear
    On Error GoTo EH

    If Not IsObject(cols) Then MsgErr "报表缺少列定义": Exit Function
    n = cols.Count
    ReDim m_colKeys(0 To n - 1)
    ReDim m_colLabels(0 To n - 1)
    ddl = "CREATE TABLE [" & tbl & "] ([RptTitle] TEXT(255),"
    For i = 1 To n
        m_colKeys(i - 1) = JStr(DictGet(cols(i), "key"))
        m_colLabels(i - 1) = JStr(DictGet(cols(i), "label"))
        ' 字段类型：取首行值推断（数字->DOUBLE，日期串->DATETIME，其余->TEXT）
        ddl = ddl & "[" & m_colKeys(i - 1) & "] " & InferType(rows, m_colKeys(i - 1)) & ","
    Next
    ddl = Left$(ddl, Len(ddl) - 1) & ")"
    CurrentDb.Execute ddl, dbFailOnError
    CreateTable = True
    Exit Function
EH:
    MsgErr "建临时表失败：" & Err.Description
End Function

Private Function InferType(ByVal rows As Variant, ByVal key As String) As String
    Dim v As Variant
    If IsObject(rows) Then
        If TypeName(rows) = "Collection" Then
            If rows.Count > 0 Then
                AssignVariant v, Jinspect(rows(1), key)
                If VarType(v) = vbDouble Or VarType(v) = vbLong Or VarType(v) = vbInteger Then
                    InferType = "DOUBLE": Exit Function
                End If
                If IsDate(v) And InStr(CStr(v), "-") > 0 Then
                    InferType = "DATETIME": Exit Function
                End If
            End If
        End If
    End If
    InferType = "TEXT(255)"
End Function

Private Function Jinspect(ByVal row As Variant, ByVal key As String) As Variant
    Jinspect = Null
    If IsObject(row) Then
        If TypeName(row) = "Dictionary" Then
            If row.Exists(key) Then Jinspect = row(key)
        End If
    End If
End Function

Private Function FillTable(ByVal tbl As String, ByVal cols As Variant, ByVal rows As Variant) As Boolean
    Dim rs As DAO.Recordset, i As Long, r As Long, v As Variant, f As DAO.Field
    On Error GoTo EH
    Set rs = CurrentDb.OpenRecordset("SELECT * FROM [" & tbl & "]", dbOpenDynaset)
    If IsObject(rows) Then
        If TypeName(rows) = "Collection" Then
            For r = 1 To rows.Count
                rs.AddNew
                rs.Fields("RptTitle").Value = gRptTitle
                For i = LBound(m_colKeys) To UBound(m_colKeys)
                    AssignVariant v, Jinspect(rows(r), m_colKeys(i))
                    If IsNull(v) Or IsEmpty(v) Then
                        ' 保持默认空
                    ElseIf IsObject(v) Then
                        rs.Fields(m_colKeys(i)).Value = JStr(v)
                    Else
                        Set f = rs.Fields(m_colKeys(i))
                        If f.Type = dbText Or f.Type = dbMemo Then
                            f.Value = CStr(v)
                        ElseIf f.Type = dbDouble Or f.Type = dbDate Then
                            ' 数字/日期：容错转换
                            If IsNumeric(v) Then f.Value = CDbl(v) Else f.Value = CStr(v)
                        Else
                            f.Value = CStr(v)
                        End If
                    End If
                Next
                rs.Update
            Next
        End If
    End If
    rs.Close
    FillTable = True
    Exit Function
EH:
    MsgErr "填充报表数据失败：" & Err.Description
End Function

Private Function SummaryToText(ByVal sm As Variant) As String
    Dim s As String, k As Variant
    If IsObject(sm) Then
        If TypeName(sm) = "Dictionary" Then
            For Each k In sm.Keys
                s = s & k & "：" & JStr(sm(k)) & "   "
            Next
        End If
    End If
    SummaryToText = Trim$(s)
End Function

' 打开报表（预览）。若报表对象不存在则给出提示
Public Sub PreviewReport(ByVal code As String, ByVal query As String)
    If Not BuildReportTable(code, query) Then Exit Sub
    Dim rptName As String: rptName = "rpt" & code
    On Error Resume Next
    DoCmd.OpenReport rptName, acViewPreview
    If Err.Number <> 0 Then
        MsgErr "未找到报表对象「" & rptName & "」，请先按 reports/ 规格创建。" & vbCrLf & Err.Description
        Err.Clear
    End If
    On Error GoTo 0
End Sub

' 直接打印
Public Sub PrintReport(ByVal code As String, ByVal query As String)
    If Not BuildReportTable(code, query) Then Exit Sub
    On Error Resume Next
    DoCmd.OpenReport "rpt" & code, acViewNormal
    On Error GoTo 0
End Sub