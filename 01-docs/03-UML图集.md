# 03 · UML 图集

> 本章用 ```mermaid 绘制系统的完整 UML 模型：用例图、类图、ER 图、活动图、时序图、状态图、组件图、部署图、分层包图，每图配文字说明。
> 模型与 `02-database/schema.sql`（30 张表）、[02-需求分析与用例](02-需求分析与用例.md)、[09-接口设计](09-接口设计.md) 严格一致。

---

## 1. 用例图

> 用例图见 [02-需求分析与用例 §2](02-需求分析与用例.md)（总用例图 + 5 张分模块用例图），本节引用不再重复。
> 共 **40 个用例（UC01~UC40）**，参与者：系统管理员、店长、采购员、销售/收银、库管员、供应商、客户/会员。

---

## 2. 类图（领域模型）

### 2.1 领域类图

```mermaid
classDiagram
    class BaseEntity {
        +Long id
        +Date createTime
        +Date updateTime
    }

    class SysUser {
        +Long id
        +String username
        +String password
        +String realName
        +String phone
        +String email
        +Integer gender
        +Integer status
        +Date lastLogin
        +login() String
        +logout() void
    }
    class SysRole {
        +Long id
        +String roleCode
        +String roleName
        +Integer status
    }
    class SysPermission {
        +Long id
        +String permCode
        +String permName
        +String permType
        +Long parentId
        +String path
    }
    class SysOperationLog {
        +Long id
        +String username
        +String module
        +String operation
        +Long costMs
        +Integer success
    }

    class BaseBook {
        +Long id
        +String isbn
        +String bookName
        +String author
        +Long publisherId
        +Long categoryId
        +BigDecimal price
        +BigDecimal purchasePrice
        +BigDecimal salePrice
        +String unit
        +String shelfNo
        +Integer status
    }
    class BaseCategory {
        +Long id
        +Long parentId
        +String categoryName
        +String categoryCode
    }
    class BasePublisher {
        +Long id
        +String publisherName
        +String contact
        +String phone
    }
    class BaseSupplier {
        +Long id
        +String supplierCode
        +String supplierName
        +String settleType
        +BigDecimal creditLimit
    }
    class BaseCustomer {
        +Long id
        +String customerCode
        +String customerName
        +String customerType
        +Long levelId
        +Integer points
        +BigDecimal balance
    }
    class BaseMemberLevel {
        +Long id
        +String levelCode
        +String levelName
        +BigDecimal discount
        +BigDecimal pointRate
    }
    class BaseWarehouse {
        +Long id
        +String warehouseCode
        +String warehouseName
        +String location
    }

    class PsiStock {
        +Long id
        +Long bookId
        +Long warehouseId
        +BigDecimal quantity
        +BigDecimal lockedQty
        +BigDecimal safetyStock
        +BigDecimal avgCost
        +Date lastInTime
        +Date lastOutTime
        +inbound(qty, price) void
        +outbound(qty) void
        +adjust(diff) void
        +isLow() boolean
    }
    class PsiStockFlow {
        +Long id
        +String bizType
        +String bizNo
        +Long bookId
        +Long warehouseId
        +Integer direction
        +BigDecimal quantity
        +BigDecimal beforeQty
        +BigDecimal afterQty
        +BigDecimal unitCost
    }

    class PsiStockInOrder {
        +Long id
        +String orderNo
        +Long supplierId
        +Long warehouseId
        +BigDecimal totalQty
        +BigDecimal totalAmount
        +BigDecimal payAmount
        +String status
        +Date inTime
        +submit() void
        +instock() void
        +cancel() void
    }
    class PsiStockInItem {
        +Long id
        +Long orderId
        +Long bookId
        +BigDecimal quantity
        +BigDecimal price
        +BigDecimal amount
    }
    class PsiStockInLog {
        +Long id
        +Long orderId
        +String action
        +String operatorName
    }

    class PsiStockOutOrder {
        +Long id
        +String orderNo
        +Long customerId
        +Long warehouseId
        +BigDecimal totalAmount
        +BigDecimal payAmount
        +String paymentType
        +String status
        +Date outTime
        +submit() void
        +outstock() void
        +cancel() void
    }
    class PsiStockOutItem {
        +Long id
        +Long orderId
        +Long bookId
        +BigDecimal quantity
        +BigDecimal price
        +BigDecimal amount
        +BigDecimal costPrice
    }

    class PsiStockCheckOrder {
        +Long id
        +String checkNo
        +Long warehouseId
        +String checkType
        +String status
        +Integer diffItems
        +BigDecimal profitQty
        +BigDecimal lossQty
        +finish() void
    }
    class PsiStockCheckItem {
        +Long id
        +Long orderId
        +Long bookId
        +BigDecimal bookQty
        +BigDecimal realQty
        +BigDecimal diffQty
        +BigDecimal diffAmount
    }

    class PsiStockTransferOrder {
        +Long id
        +String transferNo
        +Long fromWarehouse
        +Long toWarehouse
        +String status
        +transfer() void
    }
    class PsiStockTransferItem {
        +Long id
        +Long orderId
        +Long bookId
        +BigDecimal quantity
    }

    class PsiReturnOrder {
        +Long id
        +String returnNo
        +String returnType
        +Long partnerId
        +Long warehouseId
        +String status
        +String reason
        +finish() void
    }
    class PsiReturnItem {
        +Long id
        +Long orderId
        +Long bookId
        +BigDecimal quantity
        +BigDecimal price
        +BigDecimal amount
    }

    class Cart {
        +List~CartItem~ items
        +addItem(book, qty) void
        +removeItem(bookId) void
        +totalAmount() BigDecimal
    }
    class CartItem {
        +Long bookId
        +String bookName
        +BigDecimal price
        +BigDecimal quantity
        +BigDecimal amount
    }

    %% 继承（泛化）
    BaseEntity <|-- SysUser
    BaseEntity <|-- BaseBook
    BaseEntity <|-- PsiStock
    BaseEntity <|-- PsiStockInOrder
    BaseEntity <|-- PsiStockOutOrder

    %% 系统关系
    SysUser "1" -- "n" SysRole : 拥有(user_role)
    SysRole "1" -- "n" SysPermission : 授权(role_permission)
    SysUser "1" ..> "n" SysOperationLog : 产生

    %% 基础数据关系
    BaseCategory "1" o-- "n" BaseCategory : 父子
    BasePublisher "1" -- "n" BaseBook : 出版
    BaseCategory "1" -- "n" BaseBook : 归类
    BaseMemberLevel "1" -- "n" BaseCustomer : 等级

    %% 库存关系
    BaseBook "1" -- "n" PsiStock : 库存
    BaseWarehouse "1" -- "n" PsiStock : 存放
    PsiStock "1" ..> "n" PsiStockFlow : 产生流水

    %% 进货关系
    PsiStockInOrder "1" *-- "n" PsiStockInItem : 组合
    PsiStockInOrder "1" *-- "n" PsiStockInLog : 组合
    BaseSupplier "1" -- "n" PsiStockInOrder : 供货
    PsiStockInItem "n" -- "1" BaseBook : 引用

    %% 出货关系
    PsiStockOutOrder "1" *-- "n" PsiStockOutItem : 组合
    BaseCustomer "1" -- "n" PsiStockOutOrder : 购买
    PsiStockOutItem "n" -- "1" BaseBook : 引用
    Cart "1" *-- "n" CartItem : 含

    %% 盘点/调拨/退货
    PsiStockCheckOrder "1" *-- "n" PsiStockCheckItem : 组合
    PsiStockTransferOrder "1" *-- "n" PsiStockTransferItem : 组合
    PsiReturnOrder "1" *-- "n" PsiReturnItem : 组合
```

**说明**：
- `BaseEntity` 为抽象基类（提供 `id/createTime/updateTime`），`SysUser`、`BaseBook`、`PsiStock` 及各单据继承之（泛化 `|--`）。
- **组合**（`*--`）：单据与其明细/流水为**强组合**，单据删除时明细级联删除（同生命周期）。
- **聚合/关联**：`BaseCategory` 自关联（`o--`）表达树形父子；`BaseBook` 与 `PsiStock`、`PsiStockInItem` 为普通关联。
- `PsiStock` 封装库存不变量方法 `inbound/outbound/adjust`，是库存一致性的领域入口。
- `Cart/CartItem` 为 POS 收银的**会话态领域对象**（不落库）。

### 2.2 分层类协作

```mermaid
classDiagram
    class AuthController
    class PurchaseController
    class SaleController
    class StockController
    class AuthService
    class PurchaseService
    class SaleService
    class StockService
    class StockCalculator
    class StockInOrderMapper
    class StockMapper
    class StockFlowMapper

    AuthController --> AuthService
    PurchaseController --> PurchaseService
    SaleController --> SaleService
    StockController --> StockService
    PurchaseService --> StockCalculator
    SaleService --> StockCalculator
    StockService --> StockCalculator
    PurchaseService --> StockInOrderMapper
    PurchaseService --> StockMapper
    PurchaseService --> StockFlowMapper
    SaleService --> StockMapper
    SaleService --> StockFlowMapper
    StockService --> StockMapper
    StockService --> StockFlowMapper
```

**说明**：Controller 仅做参数校验与编排，Service 承载业务规则，`StockCalculator` 封装移动加权平均成本与库存扣减算法，Mapper 负责持久化。

---

## 3. ER 图

```mermaid
erDiagram
    SYS_ROLE ||--o{ SYS_USER_ROLE : "被授予"
    SYS_USER ||--o{ SYS_USER_ROLE : "拥有"
    SYS_ROLE ||--o{ SYS_ROLE_PERMISSION : "包含"
    SYS_PERMISSION ||--o{ SYS_ROLE_PERMISSION : "被分配"
    SYS_USER ||--o{ SYS_OPERATION_LOG : "产生"
    SYS_DICT ||--o{ SYS_DICT_ITEM : "含"

    BASE_CATEGORY ||--o{ BASE_CATEGORY : "父子"
    BASE_PUBLISHER ||--o{ BASE_BOOK : "出版"
    BASE_CATEGORY ||--o{ BASE_BOOK : "归类"
    BASE_MEMBER_LEVEL ||--o{ BASE_CUSTOMER : "等级"
    BASE_BOOK ||--o{ PSI_STOCK : "库存"
    BASE_WAREHOUSE ||--o{ PSI_STOCK : "存放"

    BASE_SUPPLIER ||--o{ PSI_STOCK_IN_ORDER : "供货"
    PSI_STOCK_IN_ORDER ||--o{ PSI_STOCK_IN_ITEM : "明细"
    PSI_STOCK_IN_ORDER ||--o{ PSI_STOCK_IN_LOG : "流水"
    BASE_BOOK ||--o{ PSI_STOCK_IN_ITEM : "被引用"

    BASE_CUSTOMER ||--o{ PSI_STOCK_OUT_ORDER : "购买"
    PSI_STOCK_OUT_ORDER ||--o{ PSI_STOCK_OUT_ITEM : "明细"
    BASE_BOOK ||--o{ PSI_STOCK_OUT_ITEM : "被引用"

    PSI_STOCK_CHECK_ORDER ||--o{ PSI_STOCK_CHECK_ITEM : "明细"
    PSI_STOCK_TRANSFER_ORDER ||--o{ PSI_STOCK_TRANSFER_ITEM : "明细"
    PSI_RETURN_ORDER ||--o{ PSI_RETURN_ITEM : "明细"
    BASE_BOOK ||--o{ PSI_STOCK_FLOW : "产生流水"
    BASE_WAREHOUSE ||--o{ PSI_STOCK_FLOW : "发生"

    SYS_QRCODE }o--|| BASE_BOOK : "图书码"
    SYS_SCAN_LOG }o--o{ SYS_QRCODE : "扫码"

    SYS_ROLE {
        bigint id PK
        varchar role_code UK
        varchar role_name
        tinyint status
    }
    SYS_USER {
        bigint id PK
        varchar username UK
        varchar password
        varchar real_name
        tinyint status
    }
    SYS_PERMISSION {
        bigint id PK
        varchar perm_code UK
        varchar perm_type
        bigint parent_id
    }
    BASE_BOOK {
        bigint id PK
        varchar isbn UK
        varchar book_name
        bigint publisher_id FK
        bigint category_id FK
        decimal price
        decimal purchase_price
        decimal sale_price
    }
    BASE_CUSTOMER {
        bigint id PK
        varchar customer_code UK
        varchar customer_type
        bigint level_id FK
        int points
        decimal balance
    }
    PSI_STOCK {
        bigint id PK
        bigint book_id FK
        bigint warehouse_id FK
        decimal quantity
        decimal safety_stock
        decimal avg_cost
    }
    PSI_STOCK_IN_ORDER {
        bigint id PK
        varchar order_no UK
        bigint supplier_id FK
        bigint warehouse_id FK
        varchar status
    }
    PSI_STOCK_OUT_ORDER {
        bigint id PK
        varchar order_no UK
        bigint customer_id FK
        varchar status
    }
    PSI_STOCK_FLOW {
        bigint id PK
        varchar biz_type
        varchar biz_no
        bigint book_id FK
        int direction
        decimal before_qty
        decimal after_qty
    }
```

**说明**：ER 图完整呈现 30 张表的实体与关系，与 `02-database/schema.sql` 的 `UNIQUE KEY`（PK/UK）、`FOREIGN KEY` 语义（此处以外键标注，物理脚本未建 FK 约束，由应用层保证）一致。核心实体为 `BASE_BOOK`（商品）、`PSI_STOCK`（库存）、三类单据及其明细、`PSI_STOCK_FLOW`（台账）。

---

## 4. 活动图

### 4.1 活动图 1：进货入库

```mermaid
flowchart TD
    A([开始]) --> B[选择供应商/仓库/结算方式]
    B --> C[录入进货明细]
    C --> D{明细有效?}
    D -- 否 --> C
    D -- 是 --> E[计算总额/应付]
    E --> F[生成单号 PI+日期+流水]
    F --> G[保存单据 DRAFT]
    G --> H[提交 SUBMITTED]
    H --> I{店长审核?}
    I -- 驳回/作废 --> J[状态 CANCELLED]
    I -- 通过 --> K[[事务开始]]
    K --> L[逐条 inbound: quantity+=qty]
    L --> M[重算 avg_cost 移动加权]
    M --> N[写 psi_stock_flow IN]
    N --> O{全部成功?}
    O -- 否 --> P[[事务回滚]]
    O -- 是 --> Q[[事务提交]]
    Q --> R[状态 INSTOCK / 写 in_time]
    R --> S([结束])
    P --> S
    J --> S
```

**说明**：进货入库的关键是**事务内的库存批量更新 + 成本重算 + 流水记账**，任一步失败整体回滚，保证账实一致。

### 4.2 活动图 2：销售出库

```mermaid
flowchart TD
    A([开始]) --> B[选择客户/仓库/支付方式]
    B --> C[扫码/检索图书/加购物车]
    C --> D{库存充足?}
    D -- 否 --> E[提示库存不足]
    E --> C
    D -- 是 --> F[应用会员折扣/计算应收]
    F --> G[生成单号 SO+日期+流水]
    G --> H[保存单据+明细]
    H --> I[提交 SUBMITTED]
    I --> J[[事务开始]]
    J --> K[逐条 outbound: quantity-=qty]
    K --> L[记录 cost_price=avg_cost]
    L --> M[写 psi_stock_flow OUT]
    M --> N[会员累加积分]
    N --> O{全部成功?}
    O -- 否 --> P[[事务回滚]]
    P --> Q[提示失败]
    O -- 是 --> R[[事务提交]]
    R --> S[状态 OUTSTOCK / 写 out_time]
    S --> T[打印小票]
    T --> U([结束])
    Q --> U
```

**说明**：销售出库强调**库存校验先行**（`quantity ≥ qty`），不足即中断；成功后同步累计会员积分，体现会员运营。

### 4.3 活动图 3：库存盘点

```mermaid
flowchart TD
    A([开始]) --> B[选择仓库/盘点类型]
    B --> C{指定 bookIds?}
    C -- 是 --> D[按指定品种生成明细]
    C -- 否 --> E[按仓库全量生成明细]
    D --> F[快照账面数量 book_qty]
    E --> F
    F --> G[生成单号 CK+日期+流水]
    G --> H[保存盘点单 DRAFT]
    H --> I[扫码/录入实盘数 real_qty]
    I --> J[计算 diff_qty/diff_amount]
    J --> K{全部录入?}
    K -- 否 --> I
    K -- 是 --> L[[事务开始]]
    L --> M[按 diff_qty 调整库存]
    M --> N[写 psi_stock_flow CHECK]
    N --> O[汇总盘盈/盘亏/差异品种]
    O --> P[[事务提交]]
    P --> Q[状态 FINISHED / 写 finish_time]
    Q --> R([结束])
```

**说明**：盘点是"**账面快照 → 实盘录入 → 差异调整**"三段式，差异通过 `CHECK` 流水固化，`profit_qty/loss_qty` 汇总入单头。

---

## 5. 时序图

### 5.1 时序图 1：登录认证

```mermaid
sequenceDiagram
    autonumber
    actor U as 用户
    participant FE as 前端
    participant Auth as AuthController
    participant Svc as AuthService
    participant UM as UserMapper
    participant JWT as JwtUtil

    U->>FE: 输入账号/密码
    FE->>Auth: POST /api/auth/login {username,password}
    Auth->>Svc: login(username,password)
    Svc->>UM: selectByUsername(username)
    UM-->>Svc: sys_user
    alt 用户不存在
        Svc-->>Auth: 抛异常 2001
    else 密码校验
        Svc->>Svc: BCrypt.matches(password, hash)
        alt 密码错误
            Svc-->>Auth: 抛异常 2001
        else 账号停用
            Svc-->>Auth: 抛异常 2002
        else 成功
            Svc->>UM: 查询角色/权限
            UM-->>Svc: roles,permissions
            Svc->>JWT: sign(userId,roles)
            JWT-->>Svc: token
            Svc->>UM: update last_login
            Svc-->>Auth: {token,userInfo,permissions}
        end
    end
    Auth-->>FE: {code:0,data:...}
    FE-->>U: 进入系统
```

**说明**：认证链路包含**查库 → BCrypt 校验 → 状态校验 → 装载权限 → 签发 JWT → 更新登录时间**，异常统一由全局处理器转为错误码。

### 5.2 时序图 2：进货入库

```mermaid
sequenceDiagram
    autonumber
    actor M as 店长
    participant C as PurchaseController
    participant S as PurchaseService
    participant OM as StockInOrderMapper
    participant SM as StockMapper
    participant FM as StockFlowMapper
    participant Calc as StockCalculator

    M->>C: POST /api/purchase/orders/{id}/instock
    C->>S: instock(orderId, operator)
    S->>OM: selectById(orderId)
    OM-->>S: order(status=SUBMITTED)
    S->>S: 校验状态合法
    S->>OM: selectItems(orderId)
    OM-->>S: items[]
    loop 每条明细
        S->>SM: selectForUpdate(bookId,warehouseId)
        SM-->>S: stock 或 null
        S->>Calc: calcAvgCost(oldQty,oldCost,inQty,inPrice)
        Calc-->>S: newAvgCost
        S->>SM: update quantity+=qty, avg_cost, last_in_time
        S->>FM: insert flow(bizType=IN, before,after)
    end
    S->>OM: update status=INSTOCK, in_time=now
    S->>OM: insert in_log(action=INSTOCK)
    S-->>C: 成功
    C-->>M: {code:0,data:null}
```

**说明**：库存行使用**悲观锁 `selectForUpdate`** 读取，保证并发下成本与数量计算不丢失更新；所有写入在同一事务内。

### 5.3 时序图 3：销售出库

```mermaid
sequenceDiagram
    autonumber
    actor K as 收银员
    participant C as SaleController
    participant S as SaleService
    participant OM as StockOutOrderMapper
    participant SM as StockMapper
    participant FM as StockFlowMapper
    participant CM as CustomerMapper

    K->>C: POST /api/sale/orders/{id}/outstock
    C->>S: outstock(orderId, operator)
    S->>OM: selectById(orderId)
    OM-->>S: order(status=SUBMITTED)
    S->>OM: selectItems(orderId)
    OM-->>S: items[]
    loop 每条明细
        S->>SM: selectForUpdate(bookId,warehouseId)
        SM-->>S: stock
        alt quantity < qty
            S-->>C: 抛异常 1002 库存不足
            C-->>K: {code:1002}
        else 足够
            S->>SM: update quantity-=qty, last_out_time
            S->>FM: insert flow(bizType=OUT, direction=-1)
            S->>OM: update item.cost_price=avg_cost
        end
    end
    opt 会员客户
        S->>CM: update points += amount*pointRate
    end
    S->>OM: update status=OUTSTOCK, out_time=now
    S-->>C: 成功
    C-->>K: {code:0}
```

**说明**：出库以**库存校验为闸门**，不足时抛业务异常 `1002`，事务回滚，`cost_price` 固化出库成本用于毛利分析。

### 5.4 时序图 4：扫码盘点

```mermaid
sequenceDiagram
    autonumber
    actor S as 库管员
    participant APP as 移动端/PC扫码
    participant SC as ScanController
    participant QC as QrCodeService
    participant CC as CheckService
    participant SM as StockMapper
    participant CM as CheckItemMapper

    S->>APP: 扫描图书码
    APP->>SC: POST /api/scan/resolve {code:"SHG:BOOK:9787020002207"}
    SC->>QC: resolve(code)
    QC-->>SC: {bizType:"BOOK", entity:isbn}
    SC-->>APP: 识别结果
    APP->>SC: POST /api/mobile/check/scan {checkId,code,realQty}
    SC->>CC: scanInput(checkId,isbn,realQty)
    CC->>CM: selectItemByBook(checkId,bookId)
    CM-->>CC: item(book_qty=120)
    CC->>SM: select(bookId,warehouseId)
    SM-->>CC: avg_cost
    CC->>CC: diff_qty=realQty-book_qty; diff_amount=diff*qty*avg_cost
    CC->>CM: update real_qty,diff_qty,diff_amount,scan_code
    CC-->>SC: 明细更新
    SC->>QC: log scan + scan_count++
    SC-->>APP: {code:0,data:item}
    APP-->>S: 显示差异
```

**说明**：扫码盘点复用 `SHG:BOOK:<isbn>` 规范，服务端负责**码解析 → 明细定位 → 差异计算 → 记录扫码日志**，实现免手输的高效盘点。

---

## 6. 状态图

### 6.1 状态图 1：进货单状态机

```mermaid
stateDiagram-v2
    [*] --> DRAFT : 开单
    DRAFT --> SUBMITTED : submit() 提交
    DRAFT --> CANCELLED : cancel() 作废
    SUBMITTED --> INSTOCK : instock() 确认入库
    SUBMITTED --> CANCELLED : cancel() 作废
    INSTOCK --> [*] : 完成
    CANCELLED --> [*] : 结束
```

**说明**：`DRAFT→SUBMITTED→INSTOCK` 为主干；`CANCELLED` 为终态。仅 `DRAFT` 可改可删，`INSTOCK` 为不可逆终态（如需冲正走退货单）。

### 6.2 状态图 2：出货单状态机

```mermaid
stateDiagram-v2
    [*] --> DRAFT : 开单/POS加购
    DRAFT --> SUBMITTED : submit() 提交
    DRAFT --> OUTSTOCK : outstock() 直接出库
    DRAFT --> CANCELLED : cancel() 作废
    SUBMITTED --> OUTSTOCK : outstock() 确认出库
    SUBMITTED --> CANCELLED : cancel() 作废
    OUTSTOCK --> [*] : 完成
    CANCELLED --> [*] : 结束
```

**说明**：出货单支持 `DRAFT` 直接 `OUTSTOCK`（POS 一步收银）与 `DRAFT→SUBMITTED→OUTSTOCK`（先开单后出库）两条路径，`OUTSTOCK` 为终态。

---

## 7. 组件图

```mermaid
flowchart TB
    subgraph Client["客户端组件"]
      W["Web 前端<br/>Vue3+Element Plus"]
      P["PC 端<br/>Electron"]
      M["移动 APP<br/>uni-app"]
      X["微信小程序"]
    end
    subgraph Gateway["接入组件"]
      N["Nginx 反向代理/静态资源"]
      API["Spring Boot API<br/>/api"]
    end
    subgraph App["应用服务组件"]
      AUTH["认证鉴权<br/>JWT+RBAC"]
      PU["进货服务"]
      SA["出货服务"]
      ST["库存服务"]
      RFC["报表/统计组件<br/>POI+SQL"]
      QR["二维码组件<br/>Hutool"]
      LOG["日志审计组件"]
    end
    subgraph Infra["基础设施组件"]
      DB[("MySQL 8 / 达梦 DM8")]
      RD[("Redis 缓存*")]
      FS["文件存储<br/>导出文件/图片"]
    end
    W --> N
    P --> N
    M --> API
    X --> API
    N --> API
    API --> AUTH
    API --> PU & SA & ST & RFC & QR & LOG
    PU & SA & ST & RFC --> DB
    AUTH --> RD
    RFC --> FS
    QR --> FS
```

> 注：`Redis 缓存*` 为**可选增强组件**（用于 token 黑名单、热点缓存），基础实现可不依赖。

**说明**：组件图刻画四端经 Nginx/API 网关进入应用服务，服务组件按业务域拆分，统一访问数据库与文件存储。

---

## 8. 部署图

```mermaid
flowchart TB
    subgraph Dev["浏览器/客户端设备"]
      B1["PC 浏览器<br/>Chrome/国产浏览器"]
      B2["PC 客户端<br/>Electron (Win/麒麟/UOS)"]
      B3["移动设备<br/>Android/iOS/鸿蒙 APP"]
      B4["微信小程序"]
    end
    subgraph Svr["应用服务器（麒麟/统信 UOS/Windows Server）"]
      NG["Nginx :80/443"]
      subgraph JVM["JVM（毕昇/龙井 JDK 21）"]
        APP["Spring Boot 应用 :8080<br/>Docker/裸机"]
      end
    end
    subgraph DBSvr["数据库服务器"]
      MY["MySQL 8 :3306"]
      DM["达梦 DM8 :5236"]
      OG["openGauss :5432"]
    end
    FS["文件/导出服务器"]
    B1 -->|HTTPS| NG
    B2 -->|HTTPS| NG
    B3 -->|HTTPS| NG
    B4 -->|WSS/HTTPS| NG
    NG -->|反向代理| APP
    APP -->|JDBC| MY
    APP -.->|信创切换| DM
    APP -.->|信创切换| OG
    APP --> FS
```

**说明**：应用服务器与数据库服务器分离部署；应用通过 JDBC 连接数据库，通过**数据源配置**在 MySQL/达梦/openGauss 间切换；四端统一经 HTTPS 访问 Nginx。

---

## 9. 分层包图

```mermaid
flowchart TB
    subgraph com.shuhaige
      subgraph common["common"]
        C1["Result / 统一响应"]
        C2["异常 / GlobalExceptionHandler"]
        C3["工具 / JwtUtil, QrCodeUtil, ExcelUtil"]
        C4["常量 / 状态、错误码"]
      end
      subgraph config["config"]
        G1["SecurityConfig / MyBatisPlusConfig"]
        G2["DataSourceConfig / RedisConfig"]
      end
      subgraph controller["controller"]
        K1["AuthController"]
        K2["PurchaseController / SaleController"]
        K3["StockController / StatController / ReportController"]
        K4["MobileController / ScanController"]
      end
      subgraph service["service"]
        S1["impl.*"]
      end
      subgraph domain["domain"]
        D1["entity.*（30 表实体）"]
        D2["dto / vo"]
        D3["StockCalculator（领域算法）"]
      end
      subgraph mapper["mapper"]
        MP["MyBatis-Plus Mapper 接口 + XML"]
      end
      subgraph security["security"]
        JE["JWT Filter / 权限注解 / 拦截器"]
      end
    end
    controller --> service --> domain
    service --> mapper
    mapper --> domain
    controller --> common
    service --> security
```

**说明**：采用经典**五层包结构**：`controller（表现/接入）→ service（应用）→ domain（领域模型）`，向下依赖 `mapper（基础设施）`；`common/config/security` 为横切支撑。依赖方向单向向下，避免循环依赖。