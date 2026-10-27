# frmStockQuery · 库存在线查询

> 对应接口：`GET /api/stock/list`（09-接口设计 §7）；预警 `GET /api/stock/warnings`；概览 `GET /api/stock/summary`。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 窗体名 | `frmStockQuery` |
| 记录源 | 无（数据经 REST 取回后填入连续窗体 `frmStockSub`，或直接填充子窗体 `sfmStockList` 的临时表） |
| 子窗体 | `sfmStockList`（记录源：本地表 `tmpStockList`） |
| 标题 | 库存在线查询 |

**设计取舍**：Access 表单不便直接绑定 JSON。做法是——REST 取回 `records` 后写入本地临时表 `tmpStockList`，子窗体绑定该表；每次查询前 `DELETE FROM tmpStockList` 再插入，子窗体随之刷新。这样保留 Access 原生表格式浏览、排序、筛选与导出能力。

## 2. 主窗体控件清单

| 控件名 | 类型 | 主要属性 | 说明 |
|---|---|---|---|
| `cboWarehouse` | ComboBox | RowSource="Value List" | 仓库筛选（取 `/warehouses`） |
| `cboCategory` | ComboBox | — | 分类筛选（取 `/categories`） |
| `txtKeyword` | Text Box | — | 书名/ISBN 关键字 |
| `chkOnlyLow` | Check Box | Caption="仅低库存" | onlyLow=true |
| `cmdSearch` | Command Button | Default=True | 查询 |
| `cmdReset` | Command Button | — | 重置 |
| `cmdExport` | Command Button | — | 导出 CSV/Excel |
| `lblSummary` | Label | — | 概览条：SKU/在库/金额/预警 |
| `lblCount` | Label | — | "共 N 条" |
| `sfmStockList` | Subform | SourceObject="sfmStockList" | 结果列表 |
| `cboPageSize` | ComboBox | RowSource="10;20;50;100" | 每页条数 |
| `cmdPrev` / `cmdNext` | Command Button | — | 翻页 |
| `lblPage` | Label | — | "第 x/y 页" |

## 3. 子窗体 sfmStockList 控件（绑定 tmpStockList）

| 控件名 | 类型 | 绑定字段 | 列宽 |
|---|---|---|---|
| `bookName` | Text Box | `book_name` | 3cm |
| `isbn` | Text Box | `isbn` | 3cm |
| `warehouseName` | Text Box | `warehouse_name` | 2cm |
| `categoryName` | Text Box | `category_name` | 2cm |
| `shelfNo` | Text Box | `shelf_no` | 1.5cm |
| `quantity` | Text Box | `quantity` | 1.5cm |
| `safetyStock` | Text Box | `safety_stock` | 1.5cm |
| `avgCost` | Text Box | `avg_cost` | 1.5cm |
| `stockAmount` | Text Box | `stock_amount` | 2cm |

`tmpStockList` 表结构（本地表，Access 建表）：
`book_id LONG, book_name TEXT(255), isbn TEXT(32), warehouse_id LONG, warehouse_name TEXT(128), category_id LONG, category_name TEXT(64), shelf_no TEXT(64), quantity DOUBLE, safety_stock DOUBLE, avg_cost DOUBLE, stock_amount DOUBLE`。

## 4. 布局描述

```
┌──────── 库存在线查询 ────────────────────────────────┐
│ 仓库[全部▼] 分类[全部▼] 关键字[______] [✓]仅低库存     │
│ [ 查询 ] [ 重置 ] [ 导出 ]     概览：SKU 1234 · 预警 12 │
│ ┌──────────────────────────────────────────────────┐ │
│ │ 书名   ISBN   仓库  分类  货架  数量 安全 成本 金额 │ │
│ │ ……（可排序/筛选）……                               │ │
│ └──────────────────────────────────────────────────┘ │
│ 每页[20▼] 第 1/7 页  [上一页] [下一页]     共 132 条   │
└──────────────────────────────────────────────────────┘
```

## 5. 事件过程

### Form_Load
```vba
Private Sub Form_Load()
    LoadWarehouses Me.cboWarehouse
    LoadCategories Me.cboCategory
    Me.lblSummary = StockSummaryText()
    RunSearch 1
End Sub
```

### cmdSearch_Click / cmdReset_Click / cmdPrev / cmdNext
```vba
Private Sub cmdSearch_Click()
    RunSearch 1
End Sub
Private Sub cmdReset_Click()
    Me.txtKeyword = "": Me.chkOnlyLow = False
    Me.cboWarehouse = "": Me.cboCategory = ""
    RunSearch 1
End Sub
Private Sub cmdPrev_Click(): If mPage > 1 Then RunSearch mPage - 1: End Sub
Private Sub cmdNext_Click(): If mPage < mPages Then RunSearch mPage + 1: End Sub
```

### 核心：取数并填临时表
```vba
Private mPage As Long, mPages As Long

Private Sub RunSearch(ByVal page As Long)
    If Not RequirePerm("stock:list") Then Exit Sub
    Dim size As Long: size = Val(Me.cboPageSize & "20")
    Dim r As Variant
    r = StockList(ComboValue(Me.cboWarehouse), ComboValue(Me.cboCategory), _
                  Trim$(Me.txtKeyword & ""), Me.chkOnlyLow, page, size)
    If Not ApiSuccess(r) Then MsgErr "库存查询失败：" & ApiMessage(r): Exit Sub

    Dim total As Long: total = CLng(JNum(JsonPath(r, "data.total")))
    mPage = page: mPages = IIf(total = 0, 1, (total + size - 1) \ size)
    Me.lblPage = "第 " & mPage & "/" & mPages & " 页"
    Me.lblCount = "共 " & total & " 条"

    Dim recs As Variant: recs = ApiRecords(r)
    CurrentDb.Execute "DELETE FROM tmpStockList"
    Dim rs As DAO.Recordset, i As Long
    Set rs = CurrentDb.OpenRecordset("tmpStockList", dbOpenDynaset)
    If IsObject(recs) Then
        For i = 1 To recs.Count
            rs.AddNew
            rs!book_id = CLng(JNum(JsonPath(recs(i), "bookId")))
            rs!book_name = JStr(JsonPath(recs(i), "bookName"))
            rs!isbn = JStr(JsonPath(recs(i), "isbn"))
            rs!warehouse_id = CLng(JNum(JsonPath(recs(i), "warehouseId")))
            rs!warehouse_name = JStr(JsonPath(recs(i), "warehouseName"))
            rs!category_id = CLng(JNum(JsonPath(recs(i), "categoryId")))
            rs!category_name = JStr(JsonPath(recs(i), "categoryName"))
            rs!shelf_no = JStr(JsonPath(recs(i), "shelfNo"))
            rs!quantity = JNum(JsonPath(recs(i), "quantity"))
            rs!safety_stock = JNum(JsonPath(recs(i), "safetyStock"))
            rs!avg_cost = JNum(JsonPath(recs(i), "avgCost"))
            rs!stock_amount = JNum(JsonPath(recs(i), "stockAmount"))
            rs.Update
        Next
    End If
    rs.Close
    Me.sfmStockList.Form.Requery
End Sub
```

### 下拉填充辅助（调用基础数据接口）
```vba
Private Sub LoadWarehouses(ByRef cbo As Object)
    Dim r As Variant: r = ApiGet("/warehouses")
    cbo.RowSourceType = "Value List": cbo.RowSource = ""
    cbo.AddItem "全部" & vbTab & ""
    Dim recs As Variant, i As Long: recs = ApiData(r)
    If IsObject(recs) Then
        For i = 1 To recs.Count
            cbo.AddItem JStr(JsonPath(recs(i), "warehouseName")) & vbTab & JStr(JsonPath(recs(i), "id"))
        Next
    End If
End Sub

Private Sub LoadCategories(ByRef cbo As Object)
    Dim r As Variant: r = ApiGet("/categories")
    cbo.RowSourceType = "Value List": cbo.RowSource = ""
    cbo.AddItem "全部" & vbTab & ""
    Dim recs As Variant, i As Long: recs = ApiData(r)  ' 树形数组，此处取一级
    If IsObject(recs) Then
        For i = 1 To recs.Count
            cbo.AddItem JStr(JsonPath(recs(i), "categoryName")) & vbTab & JStr(JsonPath(recs(i), "id"))
        Next
    End If
End Sub
```

### cmdExport_Click
```vba
Private Sub cmdExport_Click()
    DoCmd.TransferSpreadsheet acExport, acSpreadsheetTypeExcel12Xml, "tmpStockList", _
        CurrentProject.Path & "\库存查询_导出.xlsx", True
    MsgInfo "已导出到程序目录：库存查询_导出.xlsx"
End Sub
```

## 6. 对应 VBA

`modStock.StockList / StockWarnings / StockSummary / StockSummaryText`、`modApi.ApiRecords`、`modUtil.ComboValue`。

## 7. 备注

- 预警视图：勾选「仅低库存」即等价于 `/stock/warnings`；也可单独按钮直接 `StockWarnings()`。
- 金额列子控件 Format="¥#,##0.00"，数量列 Format="#,##0.00"。
- 与 Web 端一致：字段名、分页参数、排序语义完全对齐 `/stock/list`。