-- =====================================================================
--  晓镜书阁 · 书店进销存管理系统  —— 达梦 DM8 建表脚本（信创适配版）
--  来源：02-database/schema.sql（MySQL 8）转写
--  目标：DM8（达梦数据库）—— 政务/信创环境
--  ---------------------------------------------------------------------
--  【DM 与 MySQL 差异点汇总】（脚本内对应位置亦有标注）
--   1) AUTO_INCREMENT      -> IDENTITY(1,1)                 [MIG-01]
--   2) DATETIME            -> TIMESTAMP                     [MIG-02]
--   3) TINYINT             -> TINYINT（DM 支持，保留）       [MIG-03]
--   4) DEFAULT CURRENT_TIMESTAMP -> DEFAULT SYSDATE          [MIG-04]
--   5) ON UPDATE CURRENT_TIMESTAMP -> DM 不支持，改用触发器 [MIG-05]
--   6) 行内 COMMENT '...'  -> 独立 COMMENT ON TABLE/COLUMN    [MIG-06]
--   7) KEY idx_xxx(...)    -> 独立 CREATE INDEX               [MIG-07]
--   8) UNIQUE KEY uk_xxx   -> CONSTRAINT uk_xxx UNIQUE(...)   [MIG-08]
--   9) ENGINE/CHARSET 选项 -> DM 由库级配置指定，无需表级声明 [MIG-09]
--  10) 无自增回填 -> 数据迁移时需 SET IDENTITY_INSERT ON/OFF
--  执行前请确认：库字符集为 UTF-8；已切换 DM 兼容模式（如需要）。
-- =====================================================================

-- =====================================================================
-- 一、系统管理域
-- =====================================================================

-- MIG-01: AUTO_INCREMENT -> IDENTITY(1,1)；MIG-02: DATETIME -> TIMESTAMP
-- MIG-04: DEFAULT CURRENT_TIMESTAMP -> DEFAULT SYSDATE；MIG-06: 注释独立
CREATE TABLE sys_role (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  role_code     VARCHAR(64)  NOT NULL,
  role_name     VARCHAR(64)  NOT NULL,
  description   VARCHAR(255),
  status        TINYINT      DEFAULT 1 NOT NULL,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  update_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sys_role PRIMARY KEY (id),
  CONSTRAINT uk_role_code UNIQUE (role_code)          -- MIG-08
);
COMMENT ON TABLE  sys_role IS '系统角色';                 -- MIG-06
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.description IS '描述';
COMMENT ON COLUMN sys_role.status IS '状态 1启用 0停用';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';

CREATE TABLE sys_user (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  username      VARCHAR(64)  NOT NULL,
  employee_no   VARCHAR(32),
  password      VARCHAR(128) NOT NULL,
  real_name     VARCHAR(64),
  phone         VARCHAR(32),
  email         VARCHAR(128),
  avatar        VARCHAR(255),
  gender        TINYINT      DEFAULT 0 NOT NULL,
  status        TINYINT      DEFAULT 1 NOT NULL,
  last_login    TIMESTAMP,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  update_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sys_user PRIMARY KEY (id),
  CONSTRAINT uk_username UNIQUE (username),
  CONSTRAINT uk_employee_no UNIQUE (employee_no)
);
COMMENT ON TABLE  sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '登录账号';
COMMENT ON COLUMN sys_user.employee_no IS '工号';
COMMENT ON COLUMN sys_user.password IS '密码(BCrypt)';
COMMENT ON COLUMN sys_user.real_name IS '姓名';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.avatar IS '头像';
COMMENT ON COLUMN sys_user.gender IS '性别 0未知 1男 2女';
COMMENT ON COLUMN sys_user.status IS '状态 1启用 0停用';
COMMENT ON COLUMN sys_user.last_login IS '最后登录时间';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

CREATE TABLE sys_permission (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  perm_code     VARCHAR(128) NOT NULL,
  perm_name     VARCHAR(64)  NOT NULL,
  perm_type     VARCHAR(16)  DEFAULT 'MENU' NOT NULL,
  parent_id     BIGINT       DEFAULT 0 NOT NULL,
  path          VARCHAR(255),
  icon          VARCHAR(64),
  sort_no       INT          DEFAULT 0 NOT NULL,
  CONSTRAINT pk_sys_permission PRIMARY KEY (id),
  CONSTRAINT uk_perm_code UNIQUE (perm_code)
);
COMMENT ON TABLE  sys_permission IS '系统权限';
COMMENT ON COLUMN sys_permission.id IS '权限ID';
COMMENT ON COLUMN sys_permission.perm_code IS '权限编码(如 psi:stockIn:add)';
COMMENT ON COLUMN sys_permission.perm_name IS '权限名称';
COMMENT ON COLUMN sys_permission.perm_type IS '类型 MENU/BUTTON/API';
COMMENT ON COLUMN sys_permission.parent_id IS '父级ID';
COMMENT ON COLUMN sys_permission.path IS '前端路由/接口';
COMMENT ON COLUMN sys_permission.icon IS '图标';
COMMENT ON COLUMN sys_permission.sort_no IS '排序';

CREATE TABLE sys_role_permission (
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  CONSTRAINT pk_sys_role_permission PRIMARY KEY (role_id, permission_id)
);
COMMENT ON TABLE  sys_role_permission IS '角色-权限';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限ID';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id)
);
COMMENT ON TABLE  sys_user_role IS '用户-角色';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

CREATE TABLE sys_operation_log (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  user_id       BIGINT,
  username      VARCHAR(64),
  module        VARCHAR(64),
  operation     VARCHAR(64),
  method        VARCHAR(255),
  params        VARCHAR(2000),
  ip            VARCHAR(64),
  cost_ms       BIGINT,
  success       TINYINT      DEFAULT 1 NOT NULL,
  error_msg     VARCHAR(1000),
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sys_operation_log PRIMARY KEY (id)
);
COMMENT ON TABLE  sys_operation_log IS '操作日志';
COMMENT ON COLUMN sys_operation_log.id IS '日志ID';
COMMENT ON COLUMN sys_operation_log.user_id IS '操作人ID';
COMMENT ON COLUMN sys_operation_log.username IS '操作人账号';
COMMENT ON COLUMN sys_operation_log.module IS '模块';
COMMENT ON COLUMN sys_operation_log.operation IS '操作类型';
COMMENT ON COLUMN sys_operation_log.method IS '请求方法';
COMMENT ON COLUMN sys_operation_log.params IS '请求参数';
COMMENT ON COLUMN sys_operation_log.ip IS '来源IP';
COMMENT ON COLUMN sys_operation_log.cost_ms IS '耗时(ms)';
COMMENT ON COLUMN sys_operation_log.success IS '是否成功';
COMMENT ON COLUMN sys_operation_log.error_msg IS '错误信息';
COMMENT ON COLUMN sys_operation_log.create_time IS '操作时间';
-- MIG-07: KEY -> CREATE INDEX
CREATE INDEX idx_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_log_time ON sys_operation_log(create_time);

CREATE TABLE sys_dict (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  dict_code     VARCHAR(64)  NOT NULL,
  dict_name     VARCHAR(64)  NOT NULL,
  remark        VARCHAR(255),
  CONSTRAINT pk_sys_dict PRIMARY KEY (id),
  CONSTRAINT uk_dict_code UNIQUE (dict_code)
);
COMMENT ON TABLE  sys_dict IS '数据字典';
COMMENT ON COLUMN sys_dict.id IS '字典ID';
COMMENT ON COLUMN sys_dict.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dict.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict.remark IS '备注';

CREATE TABLE sys_dict_item (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  dict_code     VARCHAR(64)  NOT NULL,
  item_value    VARCHAR(64)  NOT NULL,
  item_label    VARCHAR(64)  NOT NULL,
  sort_no       INT          DEFAULT 0 NOT NULL,
  status        TINYINT      DEFAULT 1 NOT NULL,
  CONSTRAINT pk_sys_dict_item PRIMARY KEY (id)
);
COMMENT ON TABLE  sys_dict_item IS '字典项';
COMMENT ON COLUMN sys_dict_item.id IS '字典项ID';
COMMENT ON COLUMN sys_dict_item.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dict_item.item_value IS '字典值';
COMMENT ON COLUMN sys_dict_item.item_label IS '字典标签';
COMMENT ON COLUMN sys_dict_item.sort_no IS '排序';
COMMENT ON COLUMN sys_dict_item.status IS '状态';
CREATE INDEX idx_dict_item_code ON sys_dict_item(dict_code);

-- =====================================================================
-- 二、基础数据域
-- =====================================================================

CREATE TABLE base_category (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  parent_id     BIGINT       DEFAULT 0 NOT NULL,
  category_name VARCHAR(64)  NOT NULL,
  category_code VARCHAR(64),
  sort_no       INT          DEFAULT 0 NOT NULL,
  status        TINYINT      DEFAULT 1 NOT NULL,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_category PRIMARY KEY (id)
);
COMMENT ON TABLE  base_category IS '图书分类(树形)';
COMMENT ON COLUMN base_category.id IS '分类ID';
COMMENT ON COLUMN base_category.parent_id IS '父级ID(0为根)';
COMMENT ON COLUMN base_category.category_name IS '分类名称';
COMMENT ON COLUMN base_category.category_code IS '分类编码';
COMMENT ON COLUMN base_category.sort_no IS '排序';
COMMENT ON COLUMN base_category.status IS '状态';
COMMENT ON COLUMN base_category.create_time IS '创建时间';
CREATE INDEX idx_category_parent ON base_category(parent_id);

CREATE TABLE base_publisher (
  id             BIGINT       IDENTITY(1,1) NOT NULL,
  publisher_name VARCHAR(128) NOT NULL,
  contact        VARCHAR(64),
  phone          VARCHAR(32),
  address        VARCHAR(255),
  status         TINYINT      DEFAULT 1 NOT NULL,
  create_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_publisher PRIMARY KEY (id),
  CONSTRAINT uk_publisher_name UNIQUE (publisher_name)
);
COMMENT ON TABLE  base_publisher IS '出版社';
COMMENT ON COLUMN base_publisher.id IS '出版社ID';
COMMENT ON COLUMN base_publisher.publisher_name IS '出版社名称';
COMMENT ON COLUMN base_publisher.contact IS '联系人';
COMMENT ON COLUMN base_publisher.phone IS '联系电话';
COMMENT ON COLUMN base_publisher.address IS '地址';
COMMENT ON COLUMN base_publisher.status IS '状态';
COMMENT ON COLUMN base_publisher.create_time IS '创建时间';

CREATE TABLE base_supplier (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  supplier_code VARCHAR(64)  NOT NULL,
  supplier_name VARCHAR(128) NOT NULL,
  contact       VARCHAR(64),
  phone         VARCHAR(32),
  address       VARCHAR(255),
  settle_type   VARCHAR(32)  DEFAULT 'CASH' NOT NULL,
  credit_limit  DECIMAL(14,2) DEFAULT 0 NOT NULL,
  status        TINYINT      DEFAULT 1 NOT NULL,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_supplier PRIMARY KEY (id),
  CONSTRAINT uk_supplier_code UNIQUE (supplier_code)
);
COMMENT ON TABLE  base_supplier IS '供应商';
COMMENT ON COLUMN base_supplier.id IS '供应商ID';
COMMENT ON COLUMN base_supplier.supplier_code IS '供应商编码';
COMMENT ON COLUMN base_supplier.supplier_name IS '供应商名称';
COMMENT ON COLUMN base_supplier.contact IS '联系人';
COMMENT ON COLUMN base_supplier.phone IS '电话';
COMMENT ON COLUMN base_supplier.address IS '地址';
COMMENT ON COLUMN base_supplier.settle_type IS '结算方式 CASH/CREDIT/PERIOD';
COMMENT ON COLUMN base_supplier.credit_limit IS '授信额度';
COMMENT ON COLUMN base_supplier.status IS '状态';
COMMENT ON COLUMN base_supplier.create_time IS '创建时间';

CREATE TABLE base_member_level (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  level_code    VARCHAR(32)  NOT NULL,
  level_name    VARCHAR(64)  NOT NULL,
  discount      DECIMAL(5,2) DEFAULT 100 NOT NULL,
  point_rate    DECIMAL(5,2) DEFAULT 1 NOT NULL,
  CONSTRAINT pk_base_member_level PRIMARY KEY (id),
  CONSTRAINT uk_level_code UNIQUE (level_code)
);
COMMENT ON TABLE  base_member_level IS '会员等级';
COMMENT ON COLUMN base_member_level.id IS '等级ID';
COMMENT ON COLUMN base_member_level.level_code IS '等级编码';
COMMENT ON COLUMN base_member_level.level_name IS '等级名称';
COMMENT ON COLUMN base_member_level.discount IS '折扣率(%)，100为不打折';
COMMENT ON COLUMN base_member_level.point_rate IS '积分倍率';

CREATE TABLE base_customer (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  customer_code VARCHAR(64)  NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  customer_type VARCHAR(16)  DEFAULT 'RETAIL' NOT NULL,
  phone         VARCHAR(32),
  address       VARCHAR(255),
  level_id      BIGINT,
  points        INT          DEFAULT 0 NOT NULL,
  balance       DECIMAL(14,2) DEFAULT 0 NOT NULL,
  status        TINYINT      DEFAULT 1 NOT NULL,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_customer PRIMARY KEY (id),
  CONSTRAINT uk_customer_code UNIQUE (customer_code)
);
COMMENT ON TABLE  base_customer IS '客户/会员';
COMMENT ON COLUMN base_customer.id IS '客户ID';
COMMENT ON COLUMN base_customer.customer_code IS '客户编码';
COMMENT ON COLUMN base_customer.customer_name IS '客户名称';
COMMENT ON COLUMN base_customer.customer_type IS '类型 RETAIL零售/WHOLESALE批发/MEMBER会员';
COMMENT ON COLUMN base_customer.phone IS '电话';
COMMENT ON COLUMN base_customer.address IS '地址';
COMMENT ON COLUMN base_customer.level_id IS '会员等级ID';
COMMENT ON COLUMN base_customer.points IS '积分';
COMMENT ON COLUMN base_customer.balance IS '账户余额';
COMMENT ON COLUMN base_customer.status IS '状态';
COMMENT ON COLUMN base_customer.create_time IS '创建时间';
CREATE INDEX idx_customer_phone ON base_customer(phone);

CREATE TABLE base_warehouse (
  id             BIGINT       IDENTITY(1,1) NOT NULL,
  warehouse_code VARCHAR(64)  NOT NULL,
  warehouse_name VARCHAR(128) NOT NULL,
  location       VARCHAR(255),
  manager        VARCHAR(64),
  status         TINYINT      DEFAULT 1 NOT NULL,
  create_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_warehouse PRIMARY KEY (id),
  CONSTRAINT uk_warehouse_code UNIQUE (warehouse_code)
);
COMMENT ON TABLE  base_warehouse IS '仓库';
COMMENT ON COLUMN base_warehouse.id IS '仓库ID';
COMMENT ON COLUMN base_warehouse.warehouse_code IS '仓库编码';
COMMENT ON COLUMN base_warehouse.warehouse_name IS '仓库名称';
COMMENT ON COLUMN base_warehouse.location IS '位置';
COMMENT ON COLUMN base_warehouse.manager IS '负责人';
COMMENT ON COLUMN base_warehouse.status IS '状态';
COMMENT ON COLUMN base_warehouse.create_time IS '创建时间';

CREATE TABLE base_book (
  id             BIGINT       IDENTITY(1,1) NOT NULL,
  isbn           VARCHAR(32)  NOT NULL,
  book_name      VARCHAR(255) NOT NULL,
  author         VARCHAR(128),
  publisher_id   BIGINT,
  category_id    BIGINT,
  publish_date   VARCHAR(16),
  price          DECIMAL(14,2) DEFAULT 0 NOT NULL,
  purchase_price DECIMAL(14,2) DEFAULT 0 NOT NULL,
  sale_price     DECIMAL(14,2) DEFAULT 0 NOT NULL,
  unit           VARCHAR(16)  DEFAULT '册' NOT NULL,
  shelf_no       VARCHAR(64),
  cover_url      VARCHAR(255),
  summary        VARCHAR(1000),
  status         TINYINT      DEFAULT 1 NOT NULL,
  create_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  update_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_base_book PRIMARY KEY (id),
  CONSTRAINT uk_book_isbn UNIQUE (isbn)
);
COMMENT ON TABLE  base_book IS '图书(商品)';
COMMENT ON COLUMN base_book.id IS '图书ID';
COMMENT ON COLUMN base_book.isbn IS 'ISBN(条形码)';
COMMENT ON COLUMN base_book.book_name IS '书名';
COMMENT ON COLUMN base_book.author IS '作者';
COMMENT ON COLUMN base_book.publisher_id IS '出版社ID';
COMMENT ON COLUMN base_book.category_id IS '分类ID';
COMMENT ON COLUMN base_book.publish_date IS '出版日期(YYYY-MM)';
COMMENT ON COLUMN base_book.price IS '定价';
COMMENT ON COLUMN base_book.purchase_price IS '参考进价';
COMMENT ON COLUMN base_book.sale_price IS '零售价';
COMMENT ON COLUMN base_book.unit IS '单位';
COMMENT ON COLUMN base_book.shelf_no IS '货架位';
COMMENT ON COLUMN base_book.cover_url IS '封面图';
COMMENT ON COLUMN base_book.summary IS '简介';
COMMENT ON COLUMN base_book.status IS '状态 1在售 0停售';
COMMENT ON COLUMN base_book.create_time IS '创建时间';
COMMENT ON COLUMN base_book.update_time IS '更新时间';
CREATE INDEX idx_book_name ON base_book(book_name);
CREATE INDEX idx_book_publisher ON base_book(publisher_id);
CREATE INDEX idx_book_category ON base_book(category_id);

CREATE TABLE psi_stock (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  book_id       BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  quantity      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  locked_qty    DECIMAL(14,3) DEFAULT 0 NOT NULL,
  safety_stock  DECIMAL(14,3) DEFAULT 0 NOT NULL,
  avg_cost      DECIMAL(14,4) DEFAULT 0 NOT NULL,
  last_in_time  TIMESTAMP,
  last_out_time TIMESTAMP,
  update_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_psi_stock PRIMARY KEY (id),
  CONSTRAINT uk_stock_book_wh UNIQUE (book_id, warehouse_id)
);
COMMENT ON TABLE  psi_stock IS '库存(按图书+仓库)';
COMMENT ON COLUMN psi_stock.id IS '库存ID';
COMMENT ON COLUMN psi_stock.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock.warehouse_id IS '仓库ID';
COMMENT ON COLUMN psi_stock.quantity IS '在库数量';
COMMENT ON COLUMN psi_stock.locked_qty IS '锁定数量';
COMMENT ON COLUMN psi_stock.safety_stock IS '安全库存';
COMMENT ON COLUMN psi_stock.avg_cost IS '移动加权平均成本';
COMMENT ON COLUMN psi_stock.last_in_time IS '最后入库时间';
COMMENT ON COLUMN psi_stock.last_out_time IS '最后出库时间';
COMMENT ON COLUMN psi_stock.update_time IS '更新时间';
CREATE INDEX idx_stock_book ON psi_stock(book_id);

-- =====================================================================
-- 三、进货域 (采购入库)
-- =====================================================================

CREATE TABLE psi_stock_in_order (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_no      VARCHAR(64)  NOT NULL,
  supplier_id   BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  total_qty     DECIMAL(14,3) DEFAULT 0 NOT NULL,
  total_amount  DECIMAL(14,2) DEFAULT 0 NOT NULL,
  discount      DECIMAL(14,2) DEFAULT 0 NOT NULL,
  pay_amount    DECIMAL(14,2) DEFAULT 0 NOT NULL,
  paid_amount   DECIMAL(14,2) DEFAULT 0 NOT NULL,
  settle_type   VARCHAR(32)  DEFAULT 'CASH' NOT NULL,
  status        VARCHAR(16)  DEFAULT 'DRAFT' NOT NULL,
  source        VARCHAR(16)  DEFAULT 'PC' NOT NULL,
  remark        VARCHAR(500),
  operator_id   BIGINT,
  operator_name VARCHAR(64),
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  in_time       TIMESTAMP,
  update_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_psi_stock_in_order PRIMARY KEY (id),
  CONSTRAINT uk_in_order_no UNIQUE (order_no)
);
COMMENT ON TABLE  psi_stock_in_order IS '进货单';
COMMENT ON COLUMN psi_stock_in_order.id IS '进货单ID';
COMMENT ON COLUMN psi_stock_in_order.order_no IS '进货单号';
COMMENT ON COLUMN psi_stock_in_order.supplier_id IS '供应商ID';
COMMENT ON COLUMN psi_stock_in_order.warehouse_id IS '入货仓库ID';
COMMENT ON COLUMN psi_stock_in_order.total_qty IS '总数量';
COMMENT ON COLUMN psi_stock_in_order.total_amount IS '总金额';
COMMENT ON COLUMN psi_stock_in_order.discount IS '优惠金额';
COMMENT ON COLUMN psi_stock_in_order.pay_amount IS '应付金额';
COMMENT ON COLUMN psi_stock_in_order.paid_amount IS '已付金额';
COMMENT ON COLUMN psi_stock_in_order.settle_type IS '结算方式';
COMMENT ON COLUMN psi_stock_in_order.status IS '状态 DRAFT/SUBMITTED/INSTOCK/CANCELLED';
COMMENT ON COLUMN psi_stock_in_order.source IS '来源 PC/APP/MINI/SCAN';
COMMENT ON COLUMN psi_stock_in_order.remark IS '备注';
COMMENT ON COLUMN psi_stock_in_order.operator_id IS '经办人';
COMMENT ON COLUMN psi_stock_in_order.operator_name IS '经办人姓名';
COMMENT ON COLUMN psi_stock_in_order.create_time IS '制单时间';
COMMENT ON COLUMN psi_stock_in_order.in_time IS '实际入库时间';
COMMENT ON COLUMN psi_stock_in_order.update_time IS '更新时间';
CREATE INDEX idx_in_supplier ON psi_stock_in_order(supplier_id);
CREATE INDEX idx_in_status ON psi_stock_in_order(status);
CREATE INDEX idx_in_time ON psi_stock_in_order(create_time);

CREATE TABLE psi_stock_in_item (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_id      BIGINT       NOT NULL,
  book_id       BIGINT       NOT NULL,
  book_name     VARCHAR(255),
  isbn          VARCHAR(32),
  quantity      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  price         DECIMAL(14,2) DEFAULT 0 NOT NULL,
  amount        DECIMAL(14,2) DEFAULT 0 NOT NULL,
  batch_no      VARCHAR(64),
  remark        VARCHAR(255),
  CONSTRAINT pk_psi_stock_in_item PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_in_item IS '进货单明细';
COMMENT ON COLUMN psi_stock_in_item.id IS '明细ID';
COMMENT ON COLUMN psi_stock_in_item.order_id IS '进货单ID';
COMMENT ON COLUMN psi_stock_in_item.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock_in_item.book_name IS '书名快照';
COMMENT ON COLUMN psi_stock_in_item.isbn IS 'ISBN快照';
COMMENT ON COLUMN psi_stock_in_item.quantity IS '数量';
COMMENT ON COLUMN psi_stock_in_item.price IS '进货单价';
COMMENT ON COLUMN psi_stock_in_item.amount IS '金额';
COMMENT ON COLUMN psi_stock_in_item.batch_no IS '批次号';
COMMENT ON COLUMN psi_stock_in_item.remark IS '备注';
CREATE INDEX idx_in_item_order ON psi_stock_in_item(order_id);
CREATE INDEX idx_in_item_book ON psi_stock_in_item(book_id);

CREATE TABLE psi_stock_in_log (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_id      BIGINT       NOT NULL,
  action        VARCHAR(32)  NOT NULL,
  operator_id   BIGINT,
  operator_name VARCHAR(64),
  remark        VARCHAR(255),
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_psi_stock_in_log PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_in_log IS '进货单操作流水';
COMMENT ON COLUMN psi_stock_in_log.id IS 'ID';
COMMENT ON COLUMN psi_stock_in_log.order_id IS '进货单ID';
COMMENT ON COLUMN psi_stock_in_log.action IS '动作 CREATE/SUBMIT/INSTOCK/CANCEL';
COMMENT ON COLUMN psi_stock_in_log.operator_id IS '操作人';
COMMENT ON COLUMN psi_stock_in_log.operator_name IS '操作人姓名';
COMMENT ON COLUMN psi_stock_in_log.remark IS '备注';
COMMENT ON COLUMN psi_stock_in_log.create_time IS '时间';
CREATE INDEX idx_in_log_order ON psi_stock_in_log(order_id);

-- =====================================================================
-- 四、出货域 (销售出库)
-- =====================================================================

CREATE TABLE psi_stock_out_order (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_no      VARCHAR(64)  NOT NULL,
  customer_id   BIGINT,
  warehouse_id  BIGINT       NOT NULL,
  total_qty     DECIMAL(14,3) DEFAULT 0 NOT NULL,
  total_amount  DECIMAL(14,2) DEFAULT 0 NOT NULL,
  discount      DECIMAL(14,2) DEFAULT 0 NOT NULL,
  pay_amount    DECIMAL(14,2) DEFAULT 0 NOT NULL,
  payment_type  VARCHAR(32)  DEFAULT 'CASH' NOT NULL,
  status        VARCHAR(16)  DEFAULT 'DRAFT' NOT NULL,
  source        VARCHAR(16)  DEFAULT 'PC' NOT NULL,
  remark        VARCHAR(500),
  operator_id   BIGINT,
  operator_name VARCHAR(64),
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  out_time      TIMESTAMP,
  update_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_psi_stock_out_order PRIMARY KEY (id),
  CONSTRAINT uk_out_order_no UNIQUE (order_no)
);
COMMENT ON TABLE  psi_stock_out_order IS '出货单(销售单)';
COMMENT ON COLUMN psi_stock_out_order.id IS '出货单ID';
COMMENT ON COLUMN psi_stock_out_order.order_no IS '出货单号';
COMMENT ON COLUMN psi_stock_out_order.customer_id IS '客户ID(零售可空)';
COMMENT ON COLUMN psi_stock_out_order.warehouse_id IS '出货仓库ID';
COMMENT ON COLUMN psi_stock_out_order.total_qty IS '总数量';
COMMENT ON COLUMN psi_stock_out_order.total_amount IS '总金额(应收)';
COMMENT ON COLUMN psi_stock_out_order.discount IS '折扣金额';
COMMENT ON COLUMN psi_stock_out_order.pay_amount IS '实收金额';
COMMENT ON COLUMN psi_stock_out_order.payment_type IS '支付方式 CASH/WECHAT/ALIPAY/CARD/CREDIT';
COMMENT ON COLUMN psi_stock_out_order.status IS '状态 DRAFT/SUBMITTED/OUTSTOCK/CANCELLED';
COMMENT ON COLUMN psi_stock_out_order.source IS '来源 PC/APP/MINI/SCAN';
COMMENT ON COLUMN psi_stock_out_order.remark IS '备注';
COMMENT ON COLUMN psi_stock_out_order.operator_id IS '收银/经办人';
COMMENT ON COLUMN psi_stock_out_order.operator_name IS '经办人姓名';
COMMENT ON COLUMN psi_stock_out_order.create_time IS '制单时间';
COMMENT ON COLUMN psi_stock_out_order.out_time IS '实际出库时间';
COMMENT ON COLUMN psi_stock_out_order.update_time IS '更新时间';
CREATE INDEX idx_out_customer ON psi_stock_out_order(customer_id);
CREATE INDEX idx_out_status ON psi_stock_out_order(status);
CREATE INDEX idx_out_time ON psi_stock_out_order(create_time);

CREATE TABLE psi_stock_out_item (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_id      BIGINT       NOT NULL,
  book_id       BIGINT       NOT NULL,
  book_name     VARCHAR(255),
  isbn          VARCHAR(32),
  quantity      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  price         DECIMAL(14,2) DEFAULT 0 NOT NULL,
  amount        DECIMAL(14,2) DEFAULT 0 NOT NULL,
  cost_price    DECIMAL(14,2) DEFAULT 0 NOT NULL,
  remark        VARCHAR(255),
  CONSTRAINT pk_psi_stock_out_item PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_out_item IS '出货单明细';
COMMENT ON COLUMN psi_stock_out_item.id IS '明细ID';
COMMENT ON COLUMN psi_stock_out_item.order_id IS '出货单ID';
COMMENT ON COLUMN psi_stock_out_item.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock_out_item.book_name IS '书名快照';
COMMENT ON COLUMN psi_stock_out_item.isbn IS 'ISBN快照';
COMMENT ON COLUMN psi_stock_out_item.quantity IS '数量';
COMMENT ON COLUMN psi_stock_out_item.price IS '销售单价';
COMMENT ON COLUMN psi_stock_out_item.amount IS '金额';
COMMENT ON COLUMN psi_stock_out_item.cost_price IS '成本单价(出库成本)';
COMMENT ON COLUMN psi_stock_out_item.remark IS '备注';
CREATE INDEX idx_out_item_order ON psi_stock_out_item(order_id);
CREATE INDEX idx_out_item_book ON psi_stock_out_item(book_id);

-- =====================================================================
-- 五、盘点域
-- =====================================================================

CREATE TABLE psi_stock_check_order (
  id             BIGINT       IDENTITY(1,1) NOT NULL,
  check_no       VARCHAR(64)  NOT NULL,
  warehouse_id   BIGINT       NOT NULL,
  check_type     VARCHAR(16)  DEFAULT 'FULL' NOT NULL,
  status         VARCHAR(16)  DEFAULT 'DRAFT' NOT NULL,
  total_items    INT          DEFAULT 0 NOT NULL,
  diff_items     INT          DEFAULT 0 NOT NULL,
  profit_qty     DECIMAL(14,3) DEFAULT 0 NOT NULL,
  loss_qty       DECIMAL(14,3) DEFAULT 0 NOT NULL,
  remark         VARCHAR(500),
  operator_id    BIGINT,
  operator_name  VARCHAR(64),
  create_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  finish_time    TIMESTAMP,
  CONSTRAINT pk_psi_stock_check_order PRIMARY KEY (id),
  CONSTRAINT uk_check_no UNIQUE (check_no)
);
COMMENT ON TABLE  psi_stock_check_order IS '盘点单';
COMMENT ON COLUMN psi_stock_check_order.id IS '盘点单ID';
COMMENT ON COLUMN psi_stock_check_order.check_no IS '盘点单号';
COMMENT ON COLUMN psi_stock_check_order.warehouse_id IS '仓库ID';
COMMENT ON COLUMN psi_stock_check_order.check_type IS '类型 FULL全盘/PART抽盘/CYCLE循环盘';
COMMENT ON COLUMN psi_stock_check_order.status IS '状态 DRAFT盘点中/FINISHED已完成/CANCELLED';
COMMENT ON COLUMN psi_stock_check_order.total_items IS '盘点品种数';
COMMENT ON COLUMN psi_stock_check_order.diff_items IS '差异品种数';
COMMENT ON COLUMN psi_stock_check_order.profit_qty IS '盘盈数量';
COMMENT ON COLUMN psi_stock_check_order.loss_qty IS '盘亏数量';
COMMENT ON COLUMN psi_stock_check_order.remark IS '备注';
COMMENT ON COLUMN psi_stock_check_order.operator_id IS '盘点人';
COMMENT ON COLUMN psi_stock_check_order.operator_name IS '盘点人姓名';
COMMENT ON COLUMN psi_stock_check_order.create_time IS '创建时间';
COMMENT ON COLUMN psi_stock_check_order.finish_time IS '完成时间';
CREATE INDEX idx_check_wh ON psi_stock_check_order(warehouse_id);

CREATE TABLE psi_stock_check_item (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  order_id      BIGINT       NOT NULL,
  book_id       BIGINT       NOT NULL,
  book_name     VARCHAR(255),
  isbn          VARCHAR(32),
  book_qty      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  real_qty      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  diff_qty      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  diff_amount   DECIMAL(14,2) DEFAULT 0 NOT NULL,
  scan_code     VARCHAR(64),
  remark        VARCHAR(255),
  CONSTRAINT pk_psi_stock_check_item PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_check_item IS '盘点单明细';
COMMENT ON COLUMN psi_stock_check_item.id IS '明细ID';
COMMENT ON COLUMN psi_stock_check_item.order_id IS '盘点单ID';
COMMENT ON COLUMN psi_stock_check_item.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock_check_item.book_name IS '书名快照';
COMMENT ON COLUMN psi_stock_check_item.isbn IS 'ISBN快照';
COMMENT ON COLUMN psi_stock_check_item.book_qty IS '账面数量';
COMMENT ON COLUMN psi_stock_check_item.real_qty IS '实盘数量';
COMMENT ON COLUMN psi_stock_check_item.diff_qty IS '差异数量(实盘-账面)';
COMMENT ON COLUMN psi_stock_check_item.diff_amount IS '差异金额';
COMMENT ON COLUMN psi_stock_check_item.scan_code IS '扫码/盘点标记';
COMMENT ON COLUMN psi_stock_check_item.remark IS '备注';
CREATE INDEX idx_check_item_order ON psi_stock_check_item(order_id);

-- =====================================================================
-- 六、调拨与退货域
-- =====================================================================

CREATE TABLE psi_stock_transfer_order (
  id             BIGINT       IDENTITY(1,1) NOT NULL,
  transfer_no    VARCHAR(64)  NOT NULL,
  from_warehouse BIGINT       NOT NULL,
  to_warehouse   BIGINT       NOT NULL,
  total_qty      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  status         VARCHAR(16)  DEFAULT 'DRAFT' NOT NULL,
  remark         VARCHAR(500),
  operator_id    BIGINT,
  operator_name  VARCHAR(64),
  create_time    TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  transfer_time  TIMESTAMP,
  CONSTRAINT pk_psi_stock_transfer_order PRIMARY KEY (id),
  CONSTRAINT uk_transfer_no UNIQUE (transfer_no)
);
COMMENT ON TABLE  psi_stock_transfer_order IS '库存调拨单';
COMMENT ON COLUMN psi_stock_transfer_order.id IS '调拨单ID';
COMMENT ON COLUMN psi_stock_transfer_order.transfer_no IS '调拨单号';
COMMENT ON COLUMN psi_stock_transfer_order.from_warehouse IS '调出仓库';
COMMENT ON COLUMN psi_stock_transfer_order.to_warehouse IS '调入仓库';
COMMENT ON COLUMN psi_stock_transfer_order.total_qty IS '总数量';
COMMENT ON COLUMN psi_stock_transfer_order.status IS 'DRAFT/PENDING/TRANSFERRED/CANCELLED';
COMMENT ON COLUMN psi_stock_transfer_order.remark IS '备注';
COMMENT ON COLUMN psi_stock_transfer_order.operator_id IS '经办人';
COMMENT ON COLUMN psi_stock_transfer_order.operator_name IS '经办人姓名';
COMMENT ON COLUMN psi_stock_transfer_order.create_time IS '创建时间';
COMMENT ON COLUMN psi_stock_transfer_order.transfer_time IS '调拨完成时间';

CREATE TABLE psi_stock_transfer_item (
  id          BIGINT       IDENTITY(1,1) NOT NULL,
  order_id    BIGINT       NOT NULL,
  book_id     BIGINT       NOT NULL,
  book_name   VARCHAR(255),
  isbn        VARCHAR(32),
  quantity    DECIMAL(14,3) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_psi_stock_transfer_item PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_transfer_item IS '调拨单明细';
COMMENT ON COLUMN psi_stock_transfer_item.id IS '明细ID';
COMMENT ON COLUMN psi_stock_transfer_item.order_id IS '调拨单ID';
COMMENT ON COLUMN psi_stock_transfer_item.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock_transfer_item.book_name IS '书名快照';
COMMENT ON COLUMN psi_stock_transfer_item.isbn IS 'ISBN快照';
COMMENT ON COLUMN psi_stock_transfer_item.quantity IS '调拨数量';
CREATE INDEX idx_transfer_item_order ON psi_stock_transfer_item(order_id);

CREATE TABLE psi_return_order (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  return_no     VARCHAR(64)  NOT NULL,
  return_type   VARCHAR(16)  NOT NULL,
  partner_id    BIGINT,
  partner_name  VARCHAR(128),
  warehouse_id  BIGINT       NOT NULL,
  total_qty     DECIMAL(14,3) DEFAULT 0 NOT NULL,
  total_amount  DECIMAL(14,2) DEFAULT 0 NOT NULL,
  status        VARCHAR(16)  DEFAULT 'DRAFT' NOT NULL,
  reason        VARCHAR(255),
  operator_id   BIGINT,
  operator_name VARCHAR(64),
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  finish_time   TIMESTAMP,
  CONSTRAINT pk_psi_return_order PRIMARY KEY (id),
  CONSTRAINT uk_return_no UNIQUE (return_no)
);
COMMENT ON TABLE  psi_return_order IS '退货单(采购退货/销售退货)';
COMMENT ON COLUMN psi_return_order.id IS '退货单ID';
COMMENT ON COLUMN psi_return_order.return_no IS '退货单号';
COMMENT ON COLUMN psi_return_order.return_type IS '类型 PURCHASE采购退货/SALES销售退货';
COMMENT ON COLUMN psi_return_order.partner_id IS '供应商/客户ID';
COMMENT ON COLUMN psi_return_order.partner_name IS '往来单位名称';
COMMENT ON COLUMN psi_return_order.warehouse_id IS '仓库ID';
COMMENT ON COLUMN psi_return_order.total_qty IS '总数量';
COMMENT ON COLUMN psi_return_order.total_amount IS '总金额';
COMMENT ON COLUMN psi_return_order.status IS 'DRAFT/FINISHED/CANCELLED';
COMMENT ON COLUMN psi_return_order.reason IS '退货原因';
COMMENT ON COLUMN psi_return_order.operator_id IS '经办人';
COMMENT ON COLUMN psi_return_order.operator_name IS '经办人姓名';
COMMENT ON COLUMN psi_return_order.create_time IS '创建时间';
COMMENT ON COLUMN psi_return_order.finish_time IS '完成时间';
CREATE INDEX idx_return_type ON psi_return_order(return_type);

CREATE TABLE psi_return_item (
  id          BIGINT       IDENTITY(1,1) NOT NULL,
  order_id    BIGINT       NOT NULL,
  book_id     BIGINT       NOT NULL,
  book_name   VARCHAR(255),
  isbn        VARCHAR(32),
  quantity    DECIMAL(14,3) DEFAULT 0 NOT NULL,
  price       DECIMAL(14,2) DEFAULT 0 NOT NULL,
  amount      DECIMAL(14,2) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_psi_return_item PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_return_item IS '退货单明细';
COMMENT ON COLUMN psi_return_item.id IS '明细ID';
COMMENT ON COLUMN psi_return_item.order_id IS '退货单ID';
COMMENT ON COLUMN psi_return_item.book_id IS '图书ID';
COMMENT ON COLUMN psi_return_item.book_name IS '书名快照';
COMMENT ON COLUMN psi_return_item.isbn IS 'ISBN快照';
COMMENT ON COLUMN psi_return_item.quantity IS '数量';
COMMENT ON COLUMN psi_return_item.price IS '单价';
COMMENT ON COLUMN psi_return_item.amount IS '金额';
CREATE INDEX idx_return_item_order ON psi_return_item(order_id);

-- =====================================================================
-- 七、库存流水
-- =====================================================================

CREATE TABLE psi_stock_flow (
  id            BIGINT       IDENTITY(1,1) NOT NULL,
  biz_type      VARCHAR(32)  NOT NULL,
  biz_no        VARCHAR(64),
  book_id       BIGINT       NOT NULL,
  warehouse_id  BIGINT       NOT NULL,
  direction     TINYINT      NOT NULL,
  quantity      DECIMAL(14,3) DEFAULT 0 NOT NULL,
  before_qty    DECIMAL(14,3) DEFAULT 0 NOT NULL,
  after_qty     DECIMAL(14,3) DEFAULT 0 NOT NULL,
  unit_cost     DECIMAL(14,4) DEFAULT 0 NOT NULL,
  operator_id   BIGINT,
  operator_name VARCHAR(64),
  source        VARCHAR(16)  DEFAULT 'PC' NOT NULL,
  create_time   TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_psi_stock_flow PRIMARY KEY (id)
);
COMMENT ON TABLE  psi_stock_flow IS '库存变动流水';
COMMENT ON COLUMN psi_stock_flow.id IS '流水ID';
COMMENT ON COLUMN psi_stock_flow.biz_type IS '业务类型 IN/OUT/CHECK/TRANSFER_IN/TRANSFER_OUT/RET_PUR/RET_SAL/INIT';
COMMENT ON COLUMN psi_stock_flow.biz_no IS '业务单号';
COMMENT ON COLUMN psi_stock_flow.book_id IS '图书ID';
COMMENT ON COLUMN psi_stock_flow.warehouse_id IS '仓库ID';
COMMENT ON COLUMN psi_stock_flow.direction IS '方向 1入库 -1出库';
COMMENT ON COLUMN psi_stock_flow.quantity IS '变动数量(绝对值)';
COMMENT ON COLUMN psi_stock_flow.before_qty IS '变动前数量';
COMMENT ON COLUMN psi_stock_flow.after_qty IS '变动后数量';
COMMENT ON COLUMN psi_stock_flow.unit_cost IS '单位成本';
COMMENT ON COLUMN psi_stock_flow.operator_id IS '操作人';
COMMENT ON COLUMN psi_stock_flow.operator_name IS '操作人姓名';
COMMENT ON COLUMN psi_stock_flow.source IS '来源 PC/APP/MINI/SCAN';
COMMENT ON COLUMN psi_stock_flow.create_time IS '发生时间';
CREATE INDEX idx_flow_book ON psi_stock_flow(book_id);
CREATE INDEX idx_flow_wh ON psi_stock_flow(warehouse_id);
CREATE INDEX idx_flow_time ON psi_stock_flow(create_time);
CREATE INDEX idx_flow_biz ON psi_stock_flow(biz_type, biz_no);

-- =====================================================================
-- 八、二维码/条码域
-- =====================================================================

CREATE TABLE sys_qrcode (
  id          BIGINT       IDENTITY(1,1) NOT NULL,
  biz_type    VARCHAR(32)  NOT NULL,
  biz_id      VARCHAR(64)  NOT NULL,
  qr_content  VARCHAR(255) NOT NULL,
  qr_url      VARCHAR(255),
  scan_count  INT          DEFAULT 0 NOT NULL,
  create_time TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sys_qrcode PRIMARY KEY (id)
);
COMMENT ON TABLE  sys_qrcode IS '二维码记录';
COMMENT ON COLUMN sys_qrcode.id IS 'ID';
COMMENT ON COLUMN sys_qrcode.biz_type IS '业务类型 BOOK/ORDER/STOCK/CHECK';
COMMENT ON COLUMN sys_qrcode.biz_id IS '业务标识(如ISBN/单号)';
COMMENT ON COLUMN sys_qrcode.qr_content IS '二维码内容';
COMMENT ON COLUMN sys_qrcode.qr_url IS '二维码图片地址';
COMMENT ON COLUMN sys_qrcode.scan_count IS '扫码次数';
COMMENT ON COLUMN sys_qrcode.create_time IS '创建时间';
CREATE INDEX idx_qr_biz ON sys_qrcode(biz_type, biz_id);

CREATE TABLE sys_scan_log (
  id          BIGINT       IDENTITY(1,1) NOT NULL,
  code        VARCHAR(128) NOT NULL,
  biz_type    VARCHAR(32),
  biz_id      VARCHAR(64),
  device      VARCHAR(32)  DEFAULT 'APP' NOT NULL,
  user_id     BIGINT,
  result      VARCHAR(32),
  create_time TIMESTAMP    DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sys_scan_log PRIMARY KEY (id)
);
COMMENT ON TABLE  sys_scan_log IS '扫码记录';
COMMENT ON COLUMN sys_scan_log.id IS 'ID';
COMMENT ON COLUMN sys_scan_log.code IS '扫码内容';
COMMENT ON COLUMN sys_scan_log.biz_type IS '识别出的业务类型';
COMMENT ON COLUMN sys_scan_log.biz_id IS '业务标识';
COMMENT ON COLUMN sys_scan_log.device IS '设备 APP/MINI/PC';
COMMENT ON COLUMN sys_scan_log.user_id IS '扫码人';
COMMENT ON COLUMN sys_scan_log.result IS '结果 SUCCESS/FAIL';
COMMENT ON COLUMN sys_scan_log.create_time IS '扫码时间';
CREATE INDEX idx_scan_code ON sys_scan_log(code);
CREATE INDEX idx_scan_time ON sys_scan_log(create_time);

-- =====================================================================
-- MIG-05: MySQL 的 ON UPDATE CURRENT_TIMESTAMP 在达梦不支持，
--          以下触发器用于自动维护 update_time（按需启用）。
-- =====================================================================
CREATE OR REPLACE TRIGGER trg_sys_user_upd
BEFORE UPDATE ON sys_user
FOR EACH ROW
BEGIN
  :NEW.update_time := SYSDATE;
END;
/

CREATE OR REPLACE TRIGGER trg_base_book_upd
BEFORE UPDATE ON base_book
FOR EACH ROW
BEGIN
  :NEW.update_time := SYSDATE;
END;
/

CREATE OR REPLACE TRIGGER trg_psi_stock_in_order_upd
BEFORE UPDATE ON psi_stock_in_order
FOR EACH ROW
BEGIN
  :NEW.update_time := SYSDATE;
END;
/

CREATE OR REPLACE TRIGGER trg_psi_stock_out_order_upd
BEFORE UPDATE ON psi_stock_out_order
FOR EACH ROW
BEGIN
  :NEW.update_time := SYSDATE;
END;
/

-- =====================================================================
--  End of dm-schema.sql  (达梦 DM8)
--  提示：数据迁移时如需保留自增 ID，使用
--        SET IDENTITY_INSERT <table> ON; ... INSERT ...; SET IDENTITY_INSERT <table> OFF;
-- =====================================================================