# frmOrderQuery · 单据查询

> 统一查询进货单 / 出货单 / 盘点单 / 退货单（含单据二维码）。
> 接口：`GET /purchase/orders`、`GET /sale/orders`、`GET /stock/check/orders`、
> `GET /qrcode/order/{orderNo}`（单据二维码）。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmOrderQuery` |
| 记录源 | 无；结果绑定本地表 `tmpDocQuery` |
| 标题 | 单据查询 |

## 2. 控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `cboDocType` | ComboBox | "进货单;出货单;盘点单" | 单据类型；Default=进货单 |
| `cboStatus` | ComboBox | — | 状态（随类型联动） |
| `txtKeyword` | Text Box | — | 单号/关键字 |
| `dtStart` `dtEnd` | Text Box | Format="Short Date" | 日期范围 |
| `cmdSearch` | Command Button | Default=True | 查询 |
| `lstDocs` | List Box | RowSource=tmpDocQuery | 结果（多列） |
| `imgDocQR` | Image | SizeMode=Zoom | 选中单的二维码 |
| `cmdSaveQR` | Command Button | — | 保存二维码 |
| `lblCount` | Label | — | 条数 |

`tmpDocQuery`：`id LONG, doc_no TEXT(64), doc_type TEXT(16), partner_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE, amount DOUBLE, status TEXT(16), create_time DATETIME`。

## 3. 布局描述

```
┌──────── 单据查询 ───────────────────────────────────────────┐
│ 类型[进货单▼] 状态[全部▼] 关键字[____] 起[__] 止[__] [查询]  │
│ ┌──结果────────────────────────────┐ ┌──二维码──────┐      │
│ │ 单号  类型  往来  仓库  数量 金额  │ │  ▓▓▓▓▓▓      │      │
│ │ ……                                │ │  ▓ QR ▓      │      │
│ └──────────────────────────────────┘ └──────────────┘      │
│ 共 32 条                                  [保存二维码]      │
└────────────────────────────────────────────────────────────┘
```

## 4. 事件过程

### 查询（按类型分发）
```vba
Private Sub cmdSearch_Click()
    Dim dt As String: dt = Me.cboDocType
    Dim r As Variant, recs As Variant, i As Long, docTypeKey As String
    CurrentDb.Execute "DELETE FROM tmpDocQuery"
    Dim rs As DAO.Recordset
    Set rs = CurrentDb.OpenRecordset("tmpDocQuery", dbOpenDynaset)

    If dt = "进货单" Then
        docTypeKey = "PURCHASE"
        r = ListPurchaseOrders(ComboValue(Me.cboStatus), "", "", Trim$(Me.txtKeyword & ""), _
                               FmtDate(Me.dtStart), FmtDate(Me.dtEnd), 1, 200)
        recs = ApiRecords(r)
        If IsObject(recs) Then
            For i = 1 To recs.Count
                rs.AddNew
                rs!id = CLng(JNum(JsonPath(recs(i), "id")))
                rs!doc_no = JStr(JsonPath(recs(i), "orderNo"))
                rs!doc_type = docTypeKey
                rs!partner_name = JStr(JsonPath(recs(i), "supplierName"))
                rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
                rs!total_qty = JNum(JsonPath(recs(i), "totalQty"))
                rs!amount = JNum(JsonPath(recs(i), "payAmount"))
                rs!status = JStr(JsonPath(recs(i), "status"))
                rs.Update
            Next
        End If
    ElseIf dt = "出货单" Then
        docTypeKey = "SALE"
        r = ListSaleOrders(ComboValue(Me.cboStatus), "", "", Trim$(Me.txtKeyword & ""), _
                           FmtDate(Me.dtStart), FmtDate(Me.dtEnd), 1, 200)
        recs = ApiRecords(r)
        If IsObject(recs) Then
            For i = 1 To recs.Count
                rs.AddNew
                rs!id = CLng(JNum(JsonPath(recs(i), "id")))
                rs!doc_no = JStr(JsonPath(recs(i), "orderNo"))
                rs!doc_type = docTypeKey
                rs!partner_name = JStr(JsonPath(recs(i), "customerName"))
                rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
                rs!total_qty = JNum(JsonPath(recs(i), "totalQty"))
                rs!amount = JNum(JsonPath(recs(i), "payAmount"))
                rs!status = JStr(JsonPath(recs(i), "status"))
                rs.Update
            Next
        End If
    Else
        docTypeKey = "CHECK"
        r = ListCheckOrders(ComboValue(Me.cboStatus), "", Trim$(Me.txtKeyword & ""), 1, 200)
        recs = ApiRecords(r)
        If IsObject(recs) Then
            For i = 1 To recs.Count
                rs.AddNew
                rs!id = CLng(JNum(JsonPath(recs(i), "id")))
                rs!doc_no = JStr(JsonPath(recs(i), "checkNo"))
                rs!doc_type = docTypeKey
                rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
                rs!total_qty = JNum(JsonPath(recs(i), "totalItems"))
                rs!status = JStr(JsonPath(recs(i), "status"))
                rs.Update
            Next
        End If
    End If
    rs.Close
    Me.lstDocs.Requery
    Me.lblCount = "共 " & DCount("*", "tmpDocQuery") & " 条"
End Sub
```

### 选中 -> 显示单据二维码
```vba
Private Sub lstDocs_Click()
    Dim no As String: no = Nz(Me.lstDocs.Column(1), "")
    If Len(no) = 0 Then Exit Sub
    Dim p As String: p = Environ$("TEMP") & "\shg_order_" & no & ".png"
    If SaveOrderQRCode(no, p) Then Me.imgDocQR.Picture = p Else Me.imgDocQR.Picture = ""
End Sub

Private Sub cmdSaveQR_Click()
    Dim no As String: no = Nz(Me.lstDocs.Column(1), "")
    If Len(no) = 0 Then Exit Sub
    Dim f As String: f = CurrentProject.Path & "\qrcode_" & no & ".png"
    If SaveOrderQRCode(no, f) Then MsgInfo "已保存：" & f
End Sub
```

### 类型切换时重置状态下拉
```vba
Private Sub cboDocType_AfterUpdate()
    Me.cboStatus.RowSourceType = "Value List"
    If Me.cboDocType = "盘点单" Then
        Me.cboStatus.RowSource = "全部;DRAFT;FINISHED;CANCELLED"
    Else
        Me.cboStatus.RowSource = "全部;DRAFT;SUBMITTED;INSTOCK;OUTSTOCK;CANCELLED"
    End If
End Sub
```

## 5. 对应 VBA

`modPurchase.ListPurchaseOrders`、`modSale.ListSaleOrders`、`modCheck.ListCheckOrders`、`modScan.SaveOrderQRCode`、`modUtil.*`。

## 6. 备注

- 单据二维码内容规范：`SHG:ORDER:<orderNo>`，图片来自 `GET /qrcode/order/{orderNo}`。
- 各单据状态字典值见 `modUtil.LabelOf`（DRAFT/SUBMITTED/INSTOCK/OUTSTOCK/CANCELLED/FINISHED）。