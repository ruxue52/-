# frmLogin · 登录窗体

> 对应接口：`POST /api/auth/login`（09-接口设计 §2.1）；配合 `modLogin.DoLogin`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmLogin` |
| 记录源 | 无（非绑定窗体，Record Source 留空） |
| 标题(Caption) | 晓镜书阁 · 登录 |
| 弹出式(Modal) | 是 |
| 边框(BorderStyle) | Dialog |
| 导航按钮/记录选择器 | 否 |
| 自动居中(Auto Center) | 是 |
| 启动窗体 | 是（数据库启动时首开） |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `txtUsername` | Text Box | Default="admin"，TabIndex=0 | 账号 |
| `txtPassword` | Text Box | InputMask="Password"，TabIndex=1 | 密码（掩码） |
| `chkRemember` | Check Box | Default=False | 记住账号（写入临时表 `tblSession`） |
| `cmdLogin` | Command Button | Default=True，Caption="登 录" | 触发登录 |
| `cmdCancel` | Command Button | Cancel=True，Caption="退 出" | 关闭数据库 |
| `lblMsg` | Label | ForeColor=红 | 错误提示 |
| `lblServer` | Label | — | 显示 `modApi.API_BASE`，便于排查 |
| `lblVer` | Label | Caption="v1.0" | 版本 |

## 3. 布局描述

```
┌───────────────────────────────────────┐
│           晓镜书阁 · 书店进销存            │
│       Access 桌面客户端  v1.0            │
│                                       │
│   账号  [ admin                    ]   │
│   密码  [ ********                 ]   │
│   [ ] 记住账号                          │
│                                       │
│        [  登 录  ]   [  退 出  ]        │
│   服务：http://localhost:8080/api       │
└───────────────────────────────────────┘
```

## 4. 事件过程

### Form_Load
```vba
Private Sub Form_Load()
    Me.Caption = "晓镜书阁 · 登录"
    Me.lblServer.Caption = "服务：" & API_BASE
    ' 恢复记住的账号
    Dim nm As String
    nm = Nz(DLookup("val", "tblSession", "skey='lastUser'"), "")
    If Len(nm) > 0 Then
        Me.txtUsername = nm
        Me.chkRemember = True
    End If
    Me.txtPassword.SetFocus
End Sub
```

### cmdLogin_Click
```vba
Private Sub cmdLogin_Click()
    If Len(Trim$(Me.txtUsername & "")) = 0 Then
        Me.lblMsg = "请输入账号": Exit Sub
    End If
    Me.cmdLogin.Enabled = False
    Me.lblMsg = "正在登录…"
    If DoLogin(Me.txtUsername, Me.txtPassword) Then
        ' 记住账号
        SaveSession "lastUser", IIf(Me.chkRemember, Me.txtUsername, "")
        DoCmd.Close acForm, Me.Name
        DoCmd.OpenForm "frmMain"
    Else
        Me.lblMsg = "登录失败：" & gLastError
        Me.txtPassword = ""
        Me.txtPassword.SetFocus
    End If
    Me.cmdLogin.Enabled = True
End Sub
```

### cmdCancel_Click
```vba
Private Sub cmdCancel_Click()
    DoCmd.Quit
End Sub
```

### 辅助：写会话临时表（tblSession: skey TEXT, val TEXT）
```vba
Private Sub SaveSession(ByVal k As String, ByVal v As String)
    On Error Resume Next
    CurrentDb.Execute "DELETE FROM tblSession WHERE skey='" & Replace(k, "'", "''") & "'"
    CurrentDb.Execute "INSERT INTO tblSession(skey,val) VALUES('" & Replace(k, "'", "''") & "','" & Replace(v, "'", "''") & "')"
End Sub
```

## 5. 调用链

`cmdLogin_Click` → `modLogin.DoLogin()` → `modApi.ApiPost("/auth/login")` → WinHTTP → 解析 JSON →
写入 `gToken / gRealName / gRoles / gPermissions` → 打开 `frmMain`。

## 6. 对应 VBA

- `modLogin.DoLogin`、`modLogin.ClearSession`
- `modApi.API_BASE`、`modApi.ApiPost/JsonParse`
- `modUtil.MsgErr`

## 7. 备注

- `tblSession` 为本地临时表，结构：`skey TEXT(64) 主键, val TEXT(255)`。
- 登录成功后 `gToken` 在进程内有效；Access 关闭即失效，符合安全要求。
- 若返回 401/`code=2001`，提示「账号或密码错误」；`code=2002` 提示「账号被停用」。