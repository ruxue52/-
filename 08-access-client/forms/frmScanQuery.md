# frmScanQuery · 扫码查询（扫码办事核心窗）

> 「扫码办事」核心界面。扫码枪录入 ISBN（或 `SHG:BOOK:<isbn>` 二维码内容）→
> 调用 `GET /api/mobile/stock/quick?keyword=<isbn>` 取「图书 + 库存 + 货架位」，
> 并展示 `GET /api/qrcode/book/{isbn}` 返回的二维码图片。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmScanQuery` |
| 记录源 | 无 |
| 标题 | 扫码查询 |
| 弹出式 | 否（可被 frmMain 以 acDialog+OpenArgs 打开） |
| OpenArgs | 传入扫码内容（可选，来自主控台全局扫码框） |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `txtScan` | Text Box | — | 扫码枪输入框（**保持焦点**） |
| `lblScanHint` | Label | — | "将光标置于此处，用扫码枪扫描 ISBN（自动回车）" |
| `cmdQuery` | Command Button | Default=True | 手动查询 |
| `imgQR` | Image | PictureType=嵌入，SizeMode=Zoom | 二维码图片 |
| `lblBookName` `lblAuthor` `lblPublisher` `lblCategory` | Label | — | 图书信息 |
| `lblISBN` `lblShelfNo` `lblPrice` `lblSalePrice` | Label | — | 出版/价格/货架位 |
| `lblStockQty` `lblSafety` `lblWarehouse` | Label | — | 库存信息 |
| `lblStatus` | Label | ForeColor 变化 | 有货/无货/预警 |
| `cmdSaveQR` | Command Button | Caption="保存二维码图片" | 另存 PNG |
| `cmdOpenCheck` | Command Button | Caption="加入盘点" | 跳 frmCheck（传入该 ISBN） |
| `txtLog` | Text Box | MultiLine | 本次会话扫码历史（追加） |

## 3. 布局描述

```
┌──────── 扫码查询 ───────────────────────────────────────┐
│ 扫码：[ 9787……                        ] [ 查询 ]         │
│ 将光标置于此处，用扫码枪扫描 ISBN（自动回车）             │
│ ┌──图书信息────────────────┐ ┌──二维码──────┐          │
│ │ 书名：……                  │ │  ▓▓▓▓▓▓▓▓    │          │
│ │ 作者：……  出版社：……      │ │  ▓ QR ▓      │          │
│ │ 分类：……  ISBN：……        │ │  ▓▓▓▓▓▓▓▓    │          │
│ │ 定价：¥…  零售：¥…        │ └──────────────┘          │
│ │ 货架位：A-03-2                                     │   │
│ └──────────────────────────┘ [保存二维码] [加入盘点]    │
│ ┌──库存────────────────────┐                            │
│ │ 仓库：主仓  在库：12  安全：5  状态：有货             │ │
│ └──────────────────────────┘                            │
│ 扫码历史：……                                            │
└─────────────────────────────────────────────────────────┘
```

## 4. 事件过程

### Form_Load
```vba
Private Sub Form_Load()
    Me.lblScanHint = "将光标置于此处，用扫码枪扫描 ISBN（自动回车）"
    If Not IsNull(Me.OpenArgs) Then
        Me.txtScan = Me.OpenArgs
        DoQuery
    End If
    Me.txtScan.SetFocus
End Sub
```

### txtScan_KeyDown（关键：扫码枪=键盘+回车）
```vba
Private Sub txtScan_KeyDown(KeyCode As Integer, Shift As Integer)
    If KeyCode = vbKeyReturn Then
        DoQuery
        KeyCode = 0                    ' 吞掉回车，防误触
    End If
End Sub
Private Sub txtScan_AfterUpdate()       ' 兜底：某些枪触发 AfterUpdate
    ' 若需要可 DoQuery
End Sub
```

### 核心查询
```vba
Private Sub cmdQuery_Click(): DoQuery: End Sub

Private Sub DoQuery()
    Dim code As String: code = Trim$(Me.txtScan & "")
    If Len(code) = 0 Then Exit Sub
    ' 内容解析：SHG:BOOK:<isbn> / SHG:STOCK:<wh>:<isbn> / 纯 ISBN
    Dim isbn As String: isbn = modScan.ExtractISBN(code)
    If Len(isbn) = 0 Then isbn = code
    Me.lblISBN = isbn

    Dim r As Variant
    r = MobileStockQuick(isbn)
    If Not ApiSuccess(r) Then
        modScan.LogScan code, "BOOK", isbn, "FAIL"
        MsgErr "查询失败：" & ApiMessage(r)
        Me.lblStatus = "查询失败"
        Exit Sub
    End If
    Dim d As Variant: d = ApiData(r)

    Me.lblBookName = JStr(JsonPath(d, "bookName"))
    Me.lblAuthor = JStr(JsonPath(d, "author"))
    Me.lblPublisher = JStr(JsonPath(d, "publisherName"))
    Me.lblCategory = JStr(JsonPath(d, "categoryName"))
    Me.lblShelfNo = JStr(JsonPath(d, "shelfNo"))
    Me.lblPrice = "¥" & FmtMoney(JsonPath(d, "price"))
    Me.lblSalePrice = "¥" & FmtMoney(JsonPath(d, "salePrice"))
    Me.lblWarehouse = JStr(JsonPath(d, "warehouseName"))
    Me.lblStockQty = FmtNum(JsonPath(d, "quantity"), 2)
    Me.lblSafety = FmtNum(JsonPath(d, "safetyStock"), 2)

    If JNum(JsonPath(d, "quantity")) <= 0 Then
        Me.lblStatus = "无 货"; Me.lblStatus.ForeColor = vbRed
    ElseIf JNum(JsonPath(d, "quantity")) < JNum(JsonPath(d, "safetyStock")) Then
        Me.lblStatus = "库存预警"; Me.lblStatus.ForeColor = 8388608
    Else
        Me.lblStatus = "有 货"; Me.lblStatus.ForeColor = vbGreen
    End If

    LoadQRCode isbn
    modScan.LogScan code, "BOOK", isbn, "SUCCESS"
    Me.txtLog = Me.txtLog & vbCrLf & Format$(Now, "hh:nn:ss") & "  " & code
    Me.txtScan = "": Me.txtScan.SetFocus
End Sub
```

### 加载二维码图片（下载 PNG 到本地再绑定 Image）
```vba
Private Sub LoadQRCode(ByVal isbn As String)
    Dim p As String: p = Environ$("TEMP") & "\shg_qr_" & isbn & ".png"
    If SaveBookQRCode(isbn, p) Then
        Me.imgQR.Picture = p
    Else
        Me.imgQR.Picture = ""
    End If
End Sub
```

### cmdSaveQR_Click
```vba
Private Sub cmdSaveQR_Click()
    Dim f As String
    f = CurrentProject.Path & "\qrcode_" & Me.lblISBN & ".png"
    If SaveBookQRCode(Me.lblISBN, f) Then MsgInfo "已保存：" & f
End Sub
```

### cmdOpenCheck_Click
```vba
Private Sub cmdOpenCheck_Click()
    If Len(Me.lblISBN & "") = 0 Then Exit Sub
    DoCmd.OpenForm "frmCheck", , , , , acDialog, Me.lblISBN
End Sub
```

## 5. 对应 VBA

`modStock.MobileStockQuick`、`modScan.ExtractISBN / LogScan / SaveBookQRCode / QRContent`、`modUtil.*`。

## 6. 备注

- **扫码枪即键盘**：无需驱动，扫码内容直接进入 `txtScan` 并以回车结束，`KeyDown(vbKeyReturn)` 捕获。
- 内容兼容 `SHG:BOOK:<isbn>` 与纯条码；`SHG:STOCK:<warehouseId>:<isbn>` 也可解析出 ISBN，附带仓库用于定位货架。
- 二维码来源：`GET /api/qrcode/book/{isbn}`（`Content-Type: image/png`）。