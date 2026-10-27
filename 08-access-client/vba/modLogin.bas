Attribute VB_Name = "modLogin"
Option Compare Database
Option Explicit
' =====================================================================
'  晓镜书阁 · Access 客户端 · modLogin
'  说明：登录 / 权限 / 登录守卫的实现已迁移到 sysManager 类（vba/sysManager.cls）。
'        本模块只保留与原 API 同名的一行转发过程，以保证既有调用点零改动：
'        README.md、forms/*.md 文档，以及窗体事件绑定（如 =modLogin.DoLogout）。
'        新增代码请统一使用 modApi.SysM()（返回全局 sysManager 单例）。
'  契约：POST /auth/login、GET /auth/me、POST /auth/logout（09-接口设计 §2）
' =====================================================================

' 登录。account 可填工号或登录账号，成功返回 True
Public Function DoLogin(ByVal account As String, ByVal password As String) As Boolean
    DoLogin = SysM.Login(account, password)
End Function

' 拉取当前用户（刷新权限）
Public Function RefreshMe() As Boolean
    RefreshMe = SysM.RefreshMe()
End Function

' 注销。以 Function 形式提供：Access 窗体表达式事件只能绑定有返回值的函数。
Public Function DoLogout() As Boolean
    DoLogout = SysM.Logout()
End Function

Public Function IsLoggedIn() As Boolean
    IsLoggedIn = SysM.IsLoggedIn()
End Function

Public Function CurrentToken() As String
    CurrentToken = SysM.CurrentToken()
End Function

Public Function HasPerm(ByVal code As String) As Boolean
    HasPerm = SysM.HasPerm(code)
End Function

Public Function HasRole(ByVal code As String) As Boolean
    HasRole = SysM.HasRole(code)
End Function

Public Function RequirePerm(ByVal code As String) As Boolean
    RequirePerm = SysM.RequirePerm(code)
End Function

' 登录守卫：打开窗体 / 报表前调用（已登录 True；未登录弹登录框，失败 False）
Public Function RequireLogin() As Boolean
    RequireLogin = SysM.RequireLogin()
End Function

Public Sub ClearSession()
    SysM.ClearSession
End Sub
