VERSION 1.0 CLASS
BEGIN
  MultiUse = -1  'True
END
Attribute VB_Name = "sysManager"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = True
Attribute VB_PredeclaredId = False
Attribute VB_Exposed = False
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · sysManager（系统管理员类）
'  职责：登录 / 注销 / 会话状态 / 权限判断 / 登录守卫 / 当前操作人
'  说明：会话数据统一存放在 modApi 的全局会话变量中，本类只做读写封装；
'        全局单例由 modApi.sysMan 持有，业务与守卫代码统一通过 modApi.SysM() 取用。
'  契约：POST /auth/login、GET /auth/me、POST /auth/logout（09-接口设计 §2）
' =====================================================================

' =====================================================================
'  一、只读属性：会话状态
' =====================================================================

Public Property Get UserId() As Long
    UserId = gUserId
End Property

Public Property Get UserName() As String
    UserName = gUsername
End Property

Public Property Get RealName() As String
    RealName = gRealName
End Property

Public Property Get EmployeeNo() As String
    EmployeeNo = gEmpNo
End Property

Public Property Get Token() As String
    Token = gToken
End Property

Public Property Get Roles() As String
    Roles = gRoles
End Property

Public Property Get Permissions() As String
    Permissions = gPermissions
End Property

' 当前操作人显示名："姓名(工号)"——用于写入单据、显示欢迎语
Public Property Get OperatorName() As String
    Dim s As String
    s = gRealName
    If Len(s) = 0 Then s = gUsername
    If Len(gEmpNo) > 0 Then
        OperatorName = s & "(" & gEmpNo & ")"
    Else
        OperatorName = s
    End If
End Property

' =====================================================================
'  二、登录 / 注销
' =====================================================================

' 登录：account 可填工号，也可填登录账号（服务端两者均能匹配）
Public Function Login(ByVal account As String, ByVal password As String) As Boolean
    Dim body As String, resp As Variant
    body = "{""username"":""" & JsonEsc(Trim$(account)) & """,""password"":""" & JsonEsc(password) & """}"
    AssignVariant resp, ApiPost("/auth/login", body)
    If Not ApiSuccess(resp) Then
        gLastError = ApiMessage(resp)
        Login = False
        Exit Function
    End If
    gToken = JStr(JsonPath(resp, "data.token"))
    gUserId = CLng(JNum(JsonPath(resp, "data.userInfo.id")))
    gUsername = JStr(JsonPath(resp, "data.userInfo.username"))
    gRealName = JStr(JsonPath(resp, "data.userInfo.realName"))
    gEmpNo = JStr(JsonPath(resp, "data.userInfo.employeeNo"))
    If Len(gEmpNo) = 0 Then gEmpNo = Trim$(account)   ' 兼容服务端未返回工号的情形
    gRoles = PackList(JsonPath(resp, "data.userInfo.roles"))
    gPermissions = PackList(JsonPath(resp, "data.permissions"))
    Login = True
End Function

' 刷新当前用户会话（权限变更后调用）
Public Function RefreshMe() As Boolean
    Dim resp As Variant
    AssignVariant resp, ApiGet("/auth/me")
    If Not ApiSuccess(resp) Then RefreshMe = False: Exit Function
    gUsername = JStr(JsonPath(resp, "data.username"))
    gRealName = JStr(JsonPath(resp, "data.realName"))
    gEmpNo = JStr(JsonPath(resp, "data.employeeNo"))
    gRoles = PackList(JsonPath(resp, "data.roles"))
    gPermissions = PackList(JsonPath(resp, "data.permissions"))
    RefreshMe = True
End Function

' 注销。以 Function 形式提供：Access 窗体表达式事件只能绑定有返回值的函数。
Public Function Logout() As Boolean
    On Error Resume Next
    ApiPost "/auth/logout", "{}"
    On Error GoTo 0
    ClearSession
    Logout = True
End Function

Public Sub ClearSession()
    gToken = ""
    gUsername = ""
    gRealName = ""
    gEmpNo = ""
    gRoles = ""
    gPermissions = ""
    gUserId = 0
End Sub

Public Function IsLoggedIn() As Boolean
    IsLoggedIn = (Len(gToken) > 0)
End Function

Public Function CurrentToken() As String
    CurrentToken = gToken
End Function

' =====================================================================
'  三、登录守卫：打开窗体 / 报表前调用
' =====================================================================
'  已登录 -> 直接返回 True；
'  未登录 -> 弹模态登录框（frmLogin 以 acDialog 打开，此处阻塞至其关闭），
'            登录成功返回 True，取消或失败返回 False，调用方据此取消打开。
Public Function RequireLogin() As Boolean
    If IsLoggedIn() Then
        RequireLogin = True
        Exit Function
    End If
    Dim alreadyLoaded As Boolean
    alreadyLoaded = False
    On Error Resume Next
    alreadyLoaded = CurrentProject.AllForms("frmLogin").IsLoaded
    Err.Clear
    On Error GoTo 0
    If Not alreadyLoaded Then
        ' 置位守卫标记：frmLogin 据此判断不必再打开 frmMain（避免与调用方递归）
        gLoginIsGuard = True
        On Error Resume Next
        DoCmd.OpenForm "frmLogin", , , , , acDialog
        Err.Clear
        On Error GoTo 0
        gLoginIsGuard = False
    End If
    RequireLogin = IsLoggedIn()
End Function

' =====================================================================
'  四、权限
' =====================================================================

Public Function HasPerm(ByVal code As String) As Boolean
    If Len(code) = 0 Then HasPerm = True: Exit Function
    HasPerm = (InStr(1, gPermissions, "|" & code & "|", vbTextCompare) > 0)
End Function

Public Function HasRole(ByVal code As String) As Boolean
    HasRole = (InStr(1, gRoles, "|" & code & "|", vbTextCompare) > 0)
End Function

' 无权限时提示并返回 False
Public Function RequirePerm(ByVal code As String) As Boolean
    RequirePerm = HasPerm(code)
    If Not RequirePerm Then MsgErr "当前账号无该操作权限（" & code & "），请联系管理员。"
End Function

' =====================================================================
'  五、私有辅助
' =====================================================================

' Collection -> "|a|b|"
Private Function PackList(ByVal arr As Variant) As String
    Dim i As Long, s As String
    If IsObject(arr) Then
        If TypeName(arr) = "Collection" Then
            For i = 1 To arr.Count
                s = s & "|" & JStr(arr(i)) & "|"
            Next
        End If
    End If
    PackList = s
End Function
