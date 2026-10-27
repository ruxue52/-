# frmSaleOut · 出货 / POS 收银

> 接口：`GET /sale/pos/search`（POS 检索）、`GET /sale/orders`、`POST /sale/orders`、
> `POST /sale/orders/{id}/outstock`、`POST /sale/orders/{id}/submit/cancel`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmSaleOut` |
| 记录源 | 无；购物车绑定本地表 `tmpPosCart`；单据列表绑定 `tmpSaleOrders` |
| 标题 | 出货 / POS 收银 |
| 模式 | 上半「POS 收银」，下半「出货单管理」（Tab 分隔） |

## 2. 控件清单（POS 收银页）

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `txtScan` | Text Box | — | 扫码枪输入（ISBN/书名/拼音码） |
| `cmdAdd` | Command Button | Default=True | 检索并加入购物车 |
| `cboWarehouse` | ComboBox | — | 出货仓库 |
| `cboCustomer` | ComboBox | — | 会员/客户（可空，零售） |
| `cboPayment` | ComboBox | "CASH;WECHAT;ALIPAY;CARD;CREDIT" | 支付方式 |
| `sfmCart` | Subform | SourceObject="sfmPosCart" | 购物车明细 |
| `txtDiscount` | Text Box | Default="0" | 折扣金额 |
| `txtPayAmount` | Text Box | — | 实收（默认=应收） |
| `lblTotal` | Label | — | 应收合计 |
| `cmdCheckout` | Command Button | Caption="结 算（出库）" | 建单+出库 |
| `cmdClearCart` | Command Button | Caption="清空" | 清购物车 |
| `lblPosMsg` | Label | — | 提示（含库存不足） |

## 3. 子窗体 sfmPosCart（绑定 tmpPosCart）

| 控件名 | 绑定字段 | 说明 |
|---|---|---|
| `txtBookName` | `book_name` | 书名 |
| `txtISBN` | `isbn` | ISBN |
| `txtQty` | `quantity` | 数量 |
| `txtPrice` | `price` | 单价 |
| `txtAmount` | `amount` | 金额 |
| `bookId` | `book_id` | 隐藏 |

`tmpPosCart`：`book_id LONG, book_name TEXT(255), isbn TEXT(32), quantity DOUBLE, price DOUBLE, amount DOUBLE`。
`tmpSaleOrders`：`id LONG, order_no TEXT(64), customer_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE, payment_type TEXT(32), status TEXT(16), create_time DATETIME`。

## 4. 布局描述

```
┌──────── 出货 / POS 收银 ────────────────────────────────────┐
│ 扫码/检索：[ 9787……            ] [加入]  仓库[主仓▼]         │
│ ┌──购物车───────────────────────────────────────────────┐  │
│ │ 书名   ISBN  数量  单价  金额                            │  │
│ └───────────────────────────────────────────────────────┘  │
│ 会员[散客▼] 支付[现金▼] 折扣[0.00] 应收 ¥0.00 实收[0.00]     │
│ [结 算（出库）] [清空]                                      │
│ ── 出货单管理 ──                                            │
│ 状态[全部▼] [查询]                                          │
│ ┌───────────────────────────────────────────────────────┐  │
│ │ 单号  客户  仓库  数量  金额  支付  状态                 │  │
│ └───────────────────────────────────────────────────────┘  │
│ [作废选中单]                                                │
└────────────────────────────────────────────────────────────┘
```

## 5. 事件过程

### POS 扫码加入购物车
```vba
Private Sub txtScan_KeyDown(KeyCode As Integer, Shift As Integer)
    If KeyCode = vbKeyReturn Then
        AddByKeyword
        KeyCode = 0
    End If
End Sub
Private Sub cmdAdd_Click(): AddByKeyword: End Sub

Private Sub AddByKeyword()
    Dim kw As String: kw = Trim$(Me.txtScan & "")
    If Len(kw) = 0 Then Exit Sub
    Dim code As String: code = kw
    Dim isbn As String: isbn = modScan.ExtractISBN(kw)
    If Len(isbn) > 0 Then kw = isbn            ' 扫码内容含 ISBN 时优先用 ISBN
    Dim r As Variant: r = PosSearch(kw, ComboValue(Me.cboWarehouse))
    If Not ApiSuccess(r) Then MsgErr ApiMessage(r): Exit Sub
    Dim d As Variant: d = ApiData(r)
    Dim bid As Long: bid = CLng(JNum(JsonPath(d, "bookId")))
    If bid = 0 Then MsgErr "未检索到图书：" & kw: Exit Sub

    ' 已在购物车则数量+1
    Dim rs As DAO.Recordset
    Set rs = CurrentDb.OpenRecordset("SELECT * FROM tmpPosCart WHERE book_id=" & bid, dbOpenDynaset)
    If Not rs.EOF Then
        rs.Edit
        rs!quantity = rs!quantity + 1
        rs!amount = rs!quantity * rs!price
        rs.Update
    Else
        rs.AddNew
        rs!book_id = bid
        rs!book_name = JStr(JsonPath(d, "bookName"))
        rs!isbn = JStr(JsonPath(d, "isbn"))
        rs!quantity = 1
        rs!price = JNum(JsonPath(d, "salePrice"))
        rs!amount = rs!price
        rs.Update
    End If
    rs.Close
    Me.sfmCart.Form.Requery
    CalcTotal
    modScan.LogScan code, "BOOK", isbn, "SUCCESS"
    Me.txtScan = "": Me.txtScan.SetFocus
End Sub

Private Sub CalcTotal()
    Dim rs As DAO.Recordset, t As Double
    Set rs = CurrentDb.OpenRecordset("SELECT Sum(amount) AS s FROM tmpPosCart")
    If Not rs.EOF Then t = Nz(rs!s, 0)
    rs.Close
    Me.lblTotal = "¥" & FmtMoney(t)
    Me.txtPayAmount = FmtMoney(t - Val(Me.txtDiscount & "0"))
End Sub
```

### 结算出库
```vba
Private Sub cmdCheckout_Click()
    Dim rs As DAO.Recordset, items As String, cnt As Long
    Set rs = CurrentDb.OpenRecordset("tmpPosCart")
    Do Until rs.EOF
        items = items & IIf(Len(items) > 0, ",", "") & SaleItemJSON(rs!book_id, rs!quantity, rs!price)
        cnt = cnt + 1
        rs.MoveNext
    Loop
    rs.Close
    If cnt = 0 Then MsgErr "购物车为空": Exit Sub
    If Not RequirePerm("sale:out") Then Exit Sub
    If Not MsgConfirm("共 " & cnt & " 项，确认结算并出库？") Then Exit Sub

    Dim r As Variant
    r = CreateSaleOrder(ComboValue(Me.cboCustomer), CLng(ComboValue(Me.cboWarehouse)), _
                        Me.cboPayment, Val(Me.txtDiscount & "0"), Val(Me.txtPayAmount & "0"), "", "[" & items & "]")
    If Not ApiSuccess(r) Then
        ' 库存不足：code=1002
        If CLng(JNum(JsonPath(r, "code"))) = 1002 Then
            MsgErr "库存不足，结算已回滚：" & ApiMessage(r)
        Else
            MsgErr ApiMessage(r)
        End If
        Exit Sub
    End If
    Dim oid As Long: oid = CLng(JNum(JsonPath(r, "data.id")))
    ' 新建后如需确认出库
    r = OutstockOrder(oid)
    If ApiCheckOrWarn(r, "确认出库") Then
        MsgInfo "收款完成，单号：" & JStr(JsonPath(r, "data.orderNo"))
        CurrentDb.Execute "DELETE FROM tmpPosCart"
        Me.sfmCart.Form.Requery: CalcTotal
        LoadSaleOrders
    End If
End Sub
```
> 若后端 `POST /sale/orders` 已直接完成出库，则跳过 `OutstockOrder`；以实际后端行为为准（契约允许「草稿/直接完成」）。

### 出货单管理
```vba
Private Sub LoadSaleOrders()
    Dim r As Variant: r = ListSaleOrders("", "", "", "", "", "", 1, 100)
    ' …… 同 frmPurchaseIn 的列表填充写法，写入 tmpSaleOrders 后 Requery
End Sub
```

## 6. 对应 VBA

`modSale.*`（PosSearch/CreateSaleOrder/OutstockOrder/ListSaleOrders）、`modScan.ExtractISBN/LogScan`、`modUtil.*`。

## 7. 说明

- POS 检索与 Web 端一致：`GET /sale/pos/search?keyword=`，支持 ISBN/书名/拼音码。
- 出库校验库存：不足返回 `code=1002` 并回滚，客户端提示不落单。
- 会员积分由服务端在 `outstock` 时累计，客户端无需处理。