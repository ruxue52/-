# frmPurchaseIn · 进货入库

> 接口：`GET /purchase/orders`、`GET /purchase/orders/{id}`、`POST /purchase/orders`、
> `POST /purchase/orders/{id}/submit`、`POST /purchase/orders/{id}/instock`、`POST /purchase/orders/{id}/cancel`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmPurchaseIn` |
| 记录源 | 无；列表绑定 `tmpPurchaseOrders`；明细子窗体绑定 `tmpPurchaseItems` |
| 标题 | 进货入库 |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `cboStatus` | ComboBox | "DRAFT;SUBMITTED;INSTOCK;CANCELLED" | 状态筛选 |
| `cboSupplier` | ComboBox | — | 供应商（`/suppliers`） |
| `txtKeyword` | Text Box | — | 单号关键字 |
| `dtStart` `dtEnd` | Text Box | Format="Short Date" | 日期范围 |
| `cmdSearch` | Command Button | Default=True | 查询 |
| `lstOrders` | List Box | RowSource=tmpPurchaseOrders | 进货单列表 |
| `cmdInstock` | Command Button | Caption="确认入库" | 选中单入库 |
| `cmdSubmit` | Command Button | Caption="提交" | 草稿提交 |
| `cmdCancel` | Command Button | Caption="作废" | 作废 |
| `sfmPurchaseItems` | Subframe | SourceObject="sfmPurchaseItems" | 明细 |
| `lblStat` | Label | — | 单数/数量/金额 |
| `txtScanIn` | Text Box | — | 扫码加书（可选，按 ISBN 定位图书） |
| `cboWarehouse` | ComboBox | — | 入库仓库（新建时） |

## 3. 子窗体 sfmPurchaseItems 控件（绑定 tmpPurchaseItems）

| 控件名 | 绑定字段 | 说明 |
|---|---|---|
| `txtBookName` | `book_name` | 书名 |
| `txtISBN` | `isbn` | ISBN |
| `txtQty` | `quantity` | 数量 |
| `txtPrice` | `price` | 单价 |
| `txtAmount` | `amount` | 金额 |

`tmpPurchaseOrders`：`id LONG, order_no TEXT(64), supplier_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE, status TEXT(16), operator_name TEXT(64), create_time DATETIME`。
`tmpPurchaseItems`：`book_id LONG, book_name TEXT(255), isbn TEXT(32), quantity DOUBLE, price DOUBLE, amount DOUBLE`。

## 4. 布局描述

```
┌──────── 进货入库 ───────────────────────────────────────────┐
│ 状态[全部▼] 供应商[全部▼] 单号[____] 起[__/__] 止[__/__] [查询]│
│ ┌──进货单────────────────────────────────────────────────┐ │
│ │ 单号         供应商  仓库  数量  金额     状态   经办      │ │
│ │ PI20261012001 …     主仓  100   ¥3,500 DRAFT 采购员      │ │
│ └────────────────────────────────────────────────────────┘ │
│ ┌──明细──────────────────────────────────────────────────┐ │
│ │ 书名  ISBN  数量  单价  金额                             │ │
│ └────────────────────────────────────────────────────────┘ │
│ 单数 5 · 数量 500 · 金额 ¥17,500                            │
│ [提交] [确认入库] [作废]                                     │
└────────────────────────────────────────────────────────────┘
```

## 5. 事件过程

### 查询列表
```vba
Private Sub cmdSearch_Click()
    Dim r As Variant
    r = ListPurchaseOrders(ComboValue(Me.cboStatus), ComboValue(Me.cboSupplier), "", _
        Trim$(Me.txtKeyword & ""), FmtDate(Me.dtStart), FmtDate(Me.dtEnd), 1, 200)
    If Not ApiSuccess(r) Then MsgErr ApiMessage(r): Exit Sub
    CurrentDb.Execute "DELETE FROM tmpPurchaseOrders"
    Dim rs As DAO.Recordset, recs As Variant, i As Long
    recs = ApiRecords(r)
    Set rs = CurrentDb.OpenRecordset("tmpPurchaseOrders", dbOpenDynaset)
    If IsObject(recs) Then
        For i = 1 To recs.Count
            rs.AddNew
            rs!id = CLng(JNum(JsonPath(recs(i), "id")))
            rs!order_no = JStr(JsonPath(recs(i), "orderNo"))
            rs!supplier_name = JStr(JsonPath(recs(i), "supplierName"))
            rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
            rs!total_qty = JNum(JsonPath(recs(i), "totalQty"))
            rs!pay_amount = JNum(JsonPath(recs(i), "payAmount"))
            rs!status = JStr(JsonPath(recs(i), "status"))
            rs!operator_name = JStr(JsonPath(recs(i), "operatorName"))
            rs.Update
        Next
    End If
    rs.Close
    Me.lstOrders.Requery
End Sub
```

### 选中 -> 载明细
```vba
Private mOrderId As Long
Private Sub lstOrders_Click()
    mOrderId = CLng(Me.lstOrders.Column(0))
    LoadItems mOrderId
End Sub

Private Sub LoadItems(ByVal id As Long)
    Dim r As Variant: r = GetPurchaseOrder(id)
    If Not ApiSuccess(r) Then Exit Sub
    Dim items As Variant: items = JsonPath(r, "data.items")
    CurrentDb.Execute "DELETE FROM tmpPurchaseItems"
    Dim rs As DAO.Recordset, i As Long
    Set rs = CurrentDb.OpenRecordset("tmpPurchaseItems", dbOpenDynaset)
    If IsObject(items) Then
        For i = 1 To items.Count
            rs.AddNew
            rs!book_id = CLng(JNum(JsonPath(items(i), "bookId")))
            rs!book_name = JStr(JsonPath(items(i), "bookName"))
            rs!isbn = JStr(JsonPath(items(i), "isbn"))
            rs!quantity = JNum(JsonPath(items(i), "quantity"))
            rs!price = JNum(JsonPath(items(i), "price"))
            rs!amount = JNum(JsonPath(items(i), "amount"))
            rs.Update
        Next
    End If
    rs.Close
    Me.sfmPurchaseItems.Form.Requery
End Sub
```

### 确认入库（关键操作）
```vba
Private Sub cmdInstock_Click()
    If mOrderId = 0 Then MsgErr "请先选择进货单": Exit Sub
    If Not RequirePerm("purchase:in") Then Exit Sub
    If Not MsgConfirm("确认入库？将写入库存、更新移动加权平均成本并生成流水。") Then Exit Sub
    Dim r As Variant: r = InstockOrder(mOrderId)
    If ApiCheckOrWarn(r, "确认入库") Then MsgInfo "已入库。": cmdSearch_Click
End Sub

Private Sub cmdSubmit_Click()
    If mOrderId = 0 Then Exit Sub
    If ApiCheckOrWarn(SubmitPurchaseOrder(mOrderId), "提交") Then cmdSearch_Click
End Sub

Private Sub cmdCancel_Click()
    If mOrderId = 0 Then Exit Sub
    If Not MsgConfirm("确认作废该进货单？") Then Exit Sub
    If ApiCheckOrWarn(CancelPurchaseOrder(mOrderId), "作废") Then cmdSearch_Click
End Sub
```

## 6. 对应 VBA

`modPurchase.*`、`modApi.*`、`modUtil.FmtDate/FmtMoney/ComboValue`。

## 7. 说明

- 新建进货单需「供应商 + 明细」，界面可用子窗体录入明细后调用
  `CreatePurchaseOrder(supplierId, warehouseId, settleType, discount, paidAmount, remark, itemsJSON)`。
- 入库为服务端事务：更新 `psi_stock`、写 `psi_stock_flow`（`biz_type=IN`），状态→`INSTOCK`；客户端仅触发。
- 字段快照（`book_name`/`isbn`）来自明细，显示与 Web 端一致。