# frmCheck · 库存盘点（桌面/移动盘点一体）

> 关键场景三：盘点。流程：选盘点单 → 扫码录入实盘 → 提交完成。
> 接口：`GET /stock/check/orders`、`GET /stock/check/orders/{id}`、
> `POST /mobile/check/scan`、`PUT /stock/check/orders/{id}/items`、
> `POST /stock/check/orders/{id}/finish`、`POST /stock/check/orders/{id}/cancel`、
> `POST /stock/check/orders`（新建）、`GET /mobile/check/pending/{checkId}`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmCheck` |
| 记录源 | 无；主列表绑定本地表 `tmpCheckOrders`；明细子窗体 `sfmCheckItems` 绑定本地表 `tmpCheckItems` |
| 标题 | 库存盘点 |
| OpenArgs | 可选传入 ISBN（从扫码查询加入盘点） |
| 分隔 | 上方盘点单列表、下方明细录入（Tab 控件） |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `cboCheckStatus` | ComboBox | RowSource="DRAFT;FINISHED;CANCELLED" | 状态筛选 |
| `cmdReload` | Command Button | — | 刷新盘点单 |
| `cmdNewCheck` | Command Button | Caption="新建盘点单" | 打开新建区 |
| `lstOrders` | List Box | RowSource=tmpCheckOrders | 盘点单列表（列：单号/仓库/类型/状态/品种/差异） |
| `cboWarehouseNew` | ComboBox | — | 新建时选仓库 |
| `cboCheckType` | ComboBox | RowSource="FULL;PART;CYCLE" | 全盘/抽盘/循环盘 |
| `txtRemarkNew` | Text Box | — | 备注 |
| `cmdCreateCheck` | Command Button | — | 提交新建 |
| `txtScanCheck` | Text Box | — | **扫码盘点输入框**（扫码枪回车） |
| `txtRealQty` | Text Box | Default="1" | 实盘数量（配合扫码累加/覆盖） |
| `cboEntryMode` | ComboBox | RowSource="累加;覆写" | 扫码录入方式 |
| `sfmCheckItems` | Subform | SourceObject="sfmCheckItems" | 明细（账面/实盘/差异） |
| `cmdSubmitItems` | Command Button | Caption="提交实盘数" | `PUT .../items` |
| `cmdFinish` | Command Button | Caption="完成盘点" | `POST .../finish` |
| `cmdCancelCheck` | Command Button | Caption="作废" | `POST .../cancel` |
| `lblStats` | Label | — | 品种数/差异品种/盘盈/盘亏 |

## 3. 子窗体 sfmCheckItems 控件（绑定 tmpCheckItems）

| 控件名 | 类型 | 绑定字段 | 说明 |
|---|---|---|---|
| `txtBookName` | Text Box | `book_name` | 书名 |
| `txtISBN` | Text Box | `isbn` | ISBN |
| `txtBookQty` | Text Box | `book_qty` | 账面数量（只读） |
| `txtRealQtyItem` | Text Box | `real_qty` | 实盘数量（可编辑） |
| `txtDiffQty` | Text Box | `diff_qty` | 差异（只读，=实盘-账面） |
| `txtDiffAmount` | Text Box | `diff_amount` | 差异金额（只读） |
| `itemId` | Text Box | `item_id` | 隐藏（明细 ID） |

`tmpCheckItems` 结构：`item_id LONG, book_id LONG, book_name TEXT(255), isbn TEXT(32), book_qty DOUBLE, real_qty DOUBLE, diff_qty DOUBLE, diff_amount DOUBLE`。
`tmpCheckOrders` 结构：`id LONG, check_no TEXT(64), warehouse_id LONG, warehouse_name TEXT(128), check_type TEXT(16), status TEXT(16), total_items LONG, diff_items LONG, profit_qty DOUBLE, loss_qty DOUBLE, create_time DATETIME`。

## 4. 布局描述

```
┌──────── 库存盘点 ─────────────────────────────────────────────┐
│ 状态[全部▼] [刷新] [新建盘点单]                                 │
│ ┌──盘点单列表───────────────────────────────────────────────┐ │
│ │ 盘点单号     仓库  类型  状态    品种  差异                │ │
│ │ CK20261012001 主仓 全盘  盘点中   120   3   ← 单击选中     │ │
│ └───────────────────────────────────────────────────────────┘ │
│ 扫码盘点：[ 扫 ISBN/盘点码 ………… ] 实盘[1] 方式[累加▼]         │
│ ┌──明细─────────────────────────────────────────────────────┐ │
│ │ 书名   ISBN   账面  实盘  差异  差异金额                    │ │
│ │ ……                                                         │ │
│ └───────────────────────────────────────────────────────────┘ │
│ 品种 120 · 差异 3 · 盘盈 5 · 盘亏 -2                           │
│ [提交实盘数]  [完成盘点]  [作废]                                │
└───────────────────────────────────────────────────────────────┘
```

## 5. 事件过程

### Form_Load
```vba
Private mCheckId As Long

Private Sub Form_Load()
    LoadWarehouses Me.cboWarehouseNew
    Me.cboCheckType = "FULL"
    Me.txtRealQty = 1
    Me.cboEntryMode = "累加"
    ReloadOrders
End Sub
```

### 刷新盘点单列表
```vba
Private Sub ReloadOrders()
    Dim r As Variant: r = ListCheckOrders(ComboValue(Me.cboCheckStatus), "", "", 1, 200)
    If Not ApiSuccess(r) Then MsgErr "读取盘点单失败：" & ApiMessage(r): Exit Sub
    CurrentDb.Execute "DELETE FROM tmpCheckOrders"
    Dim rs As DAO.Recordset, recs As Variant, i As Long
    Set rs = CurrentDb.OpenRecordset("tmpCheckOrders", dbOpenDynaset)
    recs = ApiRecords(r)
    If IsObject(recs) Then
        For i = 1 To recs.Count
            rs.AddNew
            rs!id = CLng(JNum(JsonPath(recs(i), "id")))
            rs!check_no = JStr(JsonPath(recs(i), "checkNo"))
            rs!warehouse_id = CLng(JNum(JsonPath(recs(i), "warehouseId")))
            rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
            rs!check_type = JStr(JsonPath(recs(i), "checkType"))
            rs!status = JStr(JsonPath(recs(i), "status"))
            rs!total_items = CLng(JNum(JsonPath(recs(i), "totalItems")))
            rs!diff_items = CLng(JNum(JsonPath(recs(i), "diffItems")))
            rs!profit_qty = JNum(JsonPath(recs(i), "profitQty"))
            rs!loss_qty = JNum(JsonPath(recs(i), "lossQty"))
            rs.Update
        Next
    End If
    rs.Close
    Me.lstOrders.Requery
End Sub
```

### 选中盘点单 -> 载入明细
```vba
Private Sub lstOrders_Click()
    If IsNull(Me.lstOrders) Then Exit Sub
    mCheckId = CLng(Me.lstOrders.Column(0))     ' 第 0 列为 id（隐藏列）
    LoadCheckItems mCheckId
End Sub

Private Sub LoadCheckItems(ByVal checkId As Long)
    Dim r As Variant: r = GetCheckOrder(checkId)
    If Not ApiSuccess(r) Then MsgErr ApiMessage(r): Exit Sub
    Dim items As Variant: items = JsonPath(r, "data.items")
    CurrentDb.Execute "DELETE FROM tmpCheckItems"
    Dim rs As DAO.Recordset, i As Long
    Set rs = CurrentDb.OpenRecordset("tmpCheckItems", dbOpenDynaset)
    If IsObject(items) Then
        For i = 1 To items.Count
            rs.AddNew
            rs!item_id = CLng(JNum(JsonPath(items(i), "id")))
            rs!book_id = CLng(JNum(JsonPath(items(i), "bookId")))
            rs!book_name = JStr(JsonPath(items(i), "bookName"))
            rs!isbn = JStr(JsonPath(items(i), "isbn"))
            rs!book_qty = JNum(JsonPath(items(i), "bookQty"))
            rs!real_qty = JNum(JsonPath(items(i), "realQty"))
            rs!diff_qty = JNum(JsonPath(items(i), "diffQty"))
            rs!diff_amount = JNum(JsonPath(items(i), "diffAmount"))
            rs.Update
        Next
    End If
    rs.Close
    Me.sfmCheckItems.Form.Requery
    UpdateStats
End Sub
```

### 扫码盘点录入（关键）
```vba
Private Sub txtScanCheck_KeyDown(KeyCode As Integer, Shift As Integer)
    If KeyCode = vbKeyReturn Then
        DoScanCheck
        KeyCode = 0
    End If
End Sub

Private Sub DoScanCheck()
    If mCheckId = 0 Then MsgErr "请先选择一个盘点单": Exit Sub
    Dim code As String: code = Trim$(Me.txtScanCheck & "")
    If Len(code) = 0 Then Exit Sub
    Dim qty As Double: qty = Val(Me.txtRealQty & "")
    If qty <= 0 Then qty = 1
    If Me.cboEntryMode = "覆写" Then
        ' 覆写：定位明细并直接改实盘数
        Dim isbn As String: isbn = modScan.ExtractISBN(code)
        Dim rs As DAO.Recordset
        Set rs = CurrentDb.OpenRecordset("SELECT * FROM tmpCheckItems WHERE isbn='" & Replace(isbn, "'", "''") & "'", dbOpenDynaset)
        If Not rs.EOF Then
            rs.Edit: rs!real_qty = qty: rs!diff_qty = qty - rs!book_qty: rs.Update
        Else
            MsgErr "该 ISBN 不在本盘点单明细中：" & isbn
        End If
        rs.Close
        Me.sfmCheckItems.Form.Requery
    Else
        ' 累加：调用移动端扫码接口
        Dim r As Variant: r = ScanCheck(mCheckId, code, qty)
        If Not ApiSuccess(r) Then
            modScan.LogScan code, "CHECK", CStr(mCheckId), "FAIL"
            MsgErr "扫码盘点失败：" & ApiMessage(r)
            Exit Sub
        End If
        LoadCheckItems mCheckId           ' 刷新明细（服务端已累加）
    End If
    modScan.LogScan code, "CHECK", CStr(mCheckId), "SUCCESS"
    Me.txtScanCheck = "": Me.txtScanCheck.SetFocus
End Sub
```

### 提交实盘数（批量）
```vba
Private Sub cmdSubmitItems_Click()
    If mCheckId = 0 Then Exit Sub
    ' 收集明细（含用户手工修改的实盘数）→ items JSON
    Dim rs As DAO.Recordset, js As String, n As Long
    Set rs = CurrentDb.OpenRecordset("tmpCheckItems")
    Do Until rs.EOF
        js = js & IIf(Len(js) > 0, ",", "") & CheckItemJSON(rs!item_id, rs!real_qty)
        n = n + 1
        rs.MoveNext
    Loop
    rs.Close
    If Not MsgConfirm("确认提交本单 " & n & " 条实盘数？") Then Exit Sub
    Dim r As Variant: r = PutCheckItems(mCheckId, "[" & js & "]")
    If ApiCheckOrWarn(r, "提交实盘数") Then MsgInfo "实盘数已提交。": LoadCheckItems mCheckId
End Sub
```

### 完成 / 作废
```vba
Private Sub cmdFinish_Click()
    If mCheckId = 0 Then Exit Sub
    If Not MsgConfirm("完成后将按差异调整库存并写库存流水，确认？") Then Exit Sub
    Dim r As Variant: r = FinishCheck(mCheckId)
    If ApiCheckOrWarn(r, "完成盘点") Then
        MsgInfo "盘点已完成，库存已按差异调整。"
        ReloadOrders: LoadCheckItems mCheckId
    End If
End Sub

Private Sub cmdCancelCheck_Click()
    If mCheckId = 0 Then Exit Sub
    If Not MsgConfirm("确认作废本盘点单？") Then Exit Sub
    If ApiCheckOrWarn(CancelCheck(mCheckId), "作废盘点") Then ReloadOrders
End Sub
```

### 新建盘点单
```vba
Private Sub cmdNewCheck_Click()
    Dim wh As String: wh = ComboValue(Me.cboWarehouseNew)
    If Len(wh) = 0 Then MsgErr "请选择仓库": Exit Sub
    If Not RequirePerm("stock:check:add") Then Exit Sub
    Dim r As Variant: r = CreateCheckOrder(CLng(wh), Me.cboCheckType, Me.txtRemarkNew, "")
    If ApiCheckOrWarn(r, "新建盘点单") Then
        MsgInfo "已创建盘点单：" & JStr(JsonPath(r, "data.checkNo"))
        ReloadOrders
    End If
End Sub
```

### 统计条
```vba
Private Sub UpdateStats()
    Dim rs As DAO.Recordset, tot As Long, diff As Long, pp As Double, pl As Double
    Set rs = CurrentDb.OpenRecordset("tmpCheckItems")
    Do Until rs.EOF
        tot = tot + 1
        If rs!diff_qty <> 0 Then diff = diff + 1
        If rs!diff_qty > 0 Then pp = pp + rs!diff_qty
        If rs!diff_qty < 0 Then pl = pl + rs!diff_qty
        rs.MoveNext
    Loop
    rs.Close
    Me.lblStats = "品种 " & tot & " · 差异 " & diff & " · 盘盈 " & FmtNum(pp, 2) & " · 盘亏 " & FmtNum(pl, 2)
End Sub
```

## 6. 对应 VBA

`modCheck.*`、`modScan.ExtractISBN/LogScan`、`modApi.*`、`modUtil.*`。

## 7. 与 Web 端一致性

- 状态机一致：`DRAFT(盘点中) → FINISHED` 或 `CANCELLED`。
- `finish` 由服务端按差异调整库存并写 `psi_stock_flow`（`biz_type=CHECK`），客户端不自行改库。
- 扫码走 `/mobile/check/scan`，与 APP 盘点同一接口，保证多端数据同源。