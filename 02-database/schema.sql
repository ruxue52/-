-- =====================================================================
--  晓镜书阁 · 书店进销存管理系统  (Bookstore ERP / PSI System)
--  数据库结构脚本 (MySQL 8.x)  -- 亦作为达梦/信创库的结构基线
--  说明：
--    1) 本脚本使用“通用 SQL 子集”，便于通过 02-database/dm 适配层迁移到达梦(DM8)。
--    2) 所有表统一前缀，按业务域分组：sys_* 系统，base_* 基础数据，psi_* 进销存。
--    3) 金额统一 DECIMAL(14,2)，数量 DECIMAL(14,3)（图书可按册/套/包计）。
--    4) 时间统一 DATETIME；不使用数据库专有类型，便于国产库兼容。
--  字符集：utf8mb4（达梦使用 UTF-8）
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 一、系统管理域
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code     VARCHAR(64)  NOT NULL COMMENT '角色编码',
  role_name     VARCHAR(64)  NOT NULL COMMENT '角色名称',
  description   VARCHAR(255)          COMMENT '描述',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) COMMENT='系统角色';

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username      VARCHAR(64)  NOT NULL COMMENT '登录账号',
  employee_no   VARCHAR(32)           COMMENT '工号',
  password      VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
  real_name     VARCHAR(64)           COMMENT '姓名',
  phone         VARCHAR(32)           COMMENT '手机号',
  email         VARCHAR(128)          COMMENT '邮箱',
  avatar        VARCHAR(255)          COMMENT '头像',
  gender        TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  last_login    DATETIME              COMMENT '最后登录时间',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_employee_no (employee_no)
) COMMENT='系统用户';

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  perm_code     VARCHAR(128) NOT NULL COMMENT '权限编码(如 psi:stockIn:add)',
  perm_name     VARCHAR(64)  NOT NULL COMMENT '权限名称',
  perm_type     VARCHAR(16)  NOT NULL DEFAULT 'MENU' COMMENT '类型 MENU/BUTTON/API',
  parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID',
  path          VARCHAR(255)          COMMENT '前端路由/接口',
  icon          VARCHAR(64)           COMMENT '图标',
  sort_no       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code)
) COMMENT='系统权限';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  role_id       BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (role_id, permission_id)
) COMMENT='角色-权限';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) COMMENT='用户-角色';

DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id       BIGINT                COMMENT '操作人ID',
  username      VARCHAR(64)           COMMENT '操作人账号',
  module        VARCHAR(64)           COMMENT '模块',
  operation     VARCHAR(64)           COMMENT '操作类型',
  method        VARCHAR(255)          COMMENT '请求方法',
  params        VARCHAR(2000)         COMMENT '请求参数',
  ip            VARCHAR(64)           COMMENT '来源IP',
  cost_ms       BIGINT                COMMENT '耗时(ms)',
  success       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功',
  error_msg     VARCHAR(1000)         COMMENT '错误信息',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_log_user (user_id),
  KEY idx_log_time (create_time)
) COMMENT='操作日志';

DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  dict_code     VARCHAR(64)  NOT NULL COMMENT '字典编码',
  dict_name     VARCHAR(64)  NOT NULL COMMENT '字典名称',
  remark        VARCHAR(255)          COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_code (dict_code)
) COMMENT='数据字典';

DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
  dict_code     VARCHAR(64)  NOT NULL COMMENT '字典编码',
  item_value    VARCHAR(64)  NOT NULL COMMENT '字典值',
  item_label    VARCHAR(64)  NOT NULL COMMENT '字典标签',
  sort_no       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (id),
  KEY idx_dict_item_code (dict_code)
) COMMENT='字典项';

-- ---------------------------------------------------------------------
-- 二、基础数据域
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS base_category;
CREATE TABLE base_category (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID(0为根)',
  category_name VARCHAR(64)  NOT NULL COMMENT '分类名称',
  category_code VARCHAR(64)           COMMENT '分类编码',
  sort_no       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_category_parent (parent_id)
) COMMENT='图书分类(树形)';

DROP TABLE IF EXISTS base_publisher;
CREATE TABLE base_publisher (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出版社ID',
  publisher_name VARCHAR(128) NOT NULL COMMENT '出版社名称',
  contact        VARCHAR(64)           COMMENT '联系人',
  phone          VARCHAR(32)           COMMENT '联系电话',
  address        VARCHAR(255)          COMMENT '地址',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_publisher_name (publisher_name)
) COMMENT='出版社';

DROP TABLE IF EXISTS base_supplier;
CREATE TABLE base_supplier (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  supplier_code VARCHAR(64)  NOT NULL COMMENT '供应商编码',
  supplier_name VARCHAR(128) NOT NULL COMMENT '供应商名称',
  contact       VARCHAR(64)           COMMENT '联系人',
  phone         VARCHAR(32)           COMMENT '电话',
  address       VARCHAR(255)          COMMENT '地址',
  settle_type   VARCHAR(32)  NOT NULL DEFAULT 'CASH' COMMENT '结算方式 CASH/CREDIT/PERIOD',
  credit_limit  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '授信额度',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_supplier_code (supplier_code)
) COMMENT='供应商';

DROP TABLE IF EXISTS base_member_level;
CREATE TABLE base_member_level (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  level_code    VARCHAR(32)  NOT NULL COMMENT '等级编码',
  level_name    VARCHAR(64)  NOT NULL COMMENT '等级名称',
  discount      DECIMAL(5,2) NOT NULL DEFAULT 100 COMMENT '折扣率(%)，100为不打折',
  point_rate    DECIMAL(5,2) NOT NULL DEFAULT 1 COMMENT '积分倍率',
  PRIMARY KEY (id),
  UNIQUE KEY uk_level_code (level_code)
) COMMENT='会员等级';

DROP TABLE IF EXISTS base_customer;
CREATE TABLE base_customer (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  customer_code VARCHAR(64)  NOT NULL COMMENT '客户编码',
  customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
  customer_type VARCHAR(16)  NOT NULL DEFAULT 'RETAIL' COMMENT '类型 RETAIL零售/WHOLESALE批发/MEMBER会员',
  phone         VARCHAR(32)           COMMENT '电话',
  address       VARCHAR(255)          COMMENT '地址',
  level_id      BIGINT                COMMENT '会员等级ID',
  points        INT          NOT NULL DEFAULT 0 COMMENT '积分',
  balance       DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '账户余额',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_customer_code (customer_code),
  KEY idx_customer_phone (phone)
) COMMENT='客户/会员';

DROP TABLE IF EXISTS base_warehouse;
CREATE TABLE base_warehouse (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  warehouse_code VARCHAR(64)  NOT NULL COMMENT '仓库编码',
  warehouse_name VARCHAR(128) NOT NULL COMMENT '仓库名称',
  location       VARCHAR(255)          COMMENT '位置',
  manager        VARCHAR(64)           COMMENT '负责人',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_warehouse_code (warehouse_code)
) COMMENT='仓库';

DROP TABLE IF EXISTS base_book;
CREATE TABLE base_book (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  isbn           VARCHAR(32)  NOT NULL COMMENT 'ISBN(条形码)',
  book_name      VARCHAR(255) NOT NULL COMMENT '书名',
  author         VARCHAR(128)          COMMENT '作者',
  publisher_id   BIGINT                COMMENT '出版社ID',
  category_id    BIGINT                COMMENT '分类ID',
  publish_date   VARCHAR(16)           COMMENT '出版日期(YYYY-MM)',
  price          DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '定价',
  purchase_price DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '参考进价',
  sale_price     DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '零售价',
  unit           VARCHAR(16)  NOT NULL DEFAULT '册' COMMENT '单位',
  shelf_no       VARCHAR(64)           COMMENT '货架位',
  cover_url      VARCHAR(255)          COMMENT '封面图',
  summary        VARCHAR(1000)         COMMENT '简介',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1在售 0停售',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_isbn (isbn),
  KEY idx_book_name (book_name),
  KEY idx_book_publisher (publisher_id),
  KEY idx_book_category (category_id)
) COMMENT='图书(商品)';

DROP TABLE IF EXISTS psi_stock;
CREATE TABLE psi_stock (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  book_id       BIGINT       NOT NULL COMMENT '图书ID',
  warehouse_id  BIGINT       NOT NULL COMMENT '仓库ID',
  quantity      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '在库数量',
  locked_qty    DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '锁定数量',
  safety_stock  DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '安全库存',
  avg_cost      DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '移动加权平均成本',
  last_in_time  DATETIME              COMMENT '最后入库时间',
  last_out_time DATETIME              COMMENT '最后出库时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stock_book_wh (book_id, warehouse_id),
  KEY idx_stock_book (book_id)
) COMMENT='库存(按图书+仓库)';

-- ---------------------------------------------------------------------
-- 三、进货域 (采购入库)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS psi_stock_in_order;
CREATE TABLE psi_stock_in_order (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '进货单ID',
  order_no      VARCHAR(64)  NOT NULL COMMENT '进货单号',
  supplier_id   BIGINT       NOT NULL COMMENT '供应商ID',
  warehouse_id  BIGINT       NOT NULL COMMENT '入货仓库ID',
  total_qty     DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '总数量',
  total_amount  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '总金额',
  discount      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  pay_amount    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '应付金额',
  paid_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
  settle_type   VARCHAR(32)  NOT NULL DEFAULT 'CASH' COMMENT '结算方式',
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿/SUBMITTED已提交/INSTOCK已入库/CANCELLED已作废',
  is_locked     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否',
  confirm_user_id   BIGINT       NULL COMMENT '确认人ID',
  confirm_user_name VARCHAR(64)  NULL COMMENT '确认人姓名',
  confirm_time      DATETIME     NULL COMMENT '确认时间',
  is_archived       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否',
  archive_time      DATETIME     NULL COMMENT '归档时间',
  archive_user_id   BIGINT       NULL COMMENT '归档人ID',
  archive_user_name VARCHAR(64)  NULL COMMENT '归档人姓名',
  source        VARCHAR(16)  NOT NULL DEFAULT 'PC' COMMENT '来源 PC/APP/MINI/SCAN',
  remark        VARCHAR(500)          COMMENT '备注',
  operator_id   BIGINT                COMMENT '经办人',
  operator_name VARCHAR(64)           COMMENT '经办人姓名',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '制单时间',
  in_time       DATETIME              COMMENT '实际入库时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_in_order_no (order_no),
  KEY idx_in_supplier (supplier_id),
  KEY idx_in_status (status),
  KEY idx_in_time (create_time)
) COMMENT='进货单';

DROP TABLE IF EXISTS psi_stock_in_item;
CREATE TABLE psi_stock_in_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  order_id      BIGINT       NOT NULL COMMENT '进货单ID',
  book_id       BIGINT       NOT NULL COMMENT '图书ID',
  book_name     VARCHAR(255)          COMMENT '书名快照',
  isbn          VARCHAR(32)           COMMENT 'ISBN快照',
  quantity      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '数量',
  price         DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '进货单价',
  amount        DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '金额',
  batch_no      VARCHAR(64)           COMMENT '批次号',
  remark        VARCHAR(255)          COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_in_item_order (order_id),
  KEY idx_in_item_book (book_id)
) COMMENT='进货单明细';

DROP TABLE IF EXISTS psi_stock_in_log;
CREATE TABLE psi_stock_in_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  order_id      BIGINT       NOT NULL COMMENT '进货单ID',
  action        VARCHAR(32)  NOT NULL COMMENT '动作 CREATE/SUBMIT/INSTOCK/CANCEL',
  operator_id   BIGINT                COMMENT '操作人',
  operator_name VARCHAR(64)           COMMENT '操作人姓名',
  remark        VARCHAR(255)          COMMENT '备注',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_in_log_order (order_id)
) COMMENT='进货单操作流水';

-- ---------------------------------------------------------------------
-- 四、出货域 (销售出库)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS psi_stock_out_order;
CREATE TABLE psi_stock_out_order (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出货单ID',
  order_no      VARCHAR(64)  NOT NULL COMMENT '出货单号',
  customer_id   BIGINT                COMMENT '客户ID(零售可空)',
  warehouse_id  BIGINT       NOT NULL COMMENT '出货仓库ID',
  total_qty     DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '总数量',
  total_amount  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '总金额(应收)',
  discount      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
  pay_amount    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '实收金额',
  payment_type  VARCHAR(32)  NOT NULL DEFAULT 'CASH' COMMENT '支付方式 CASH/WECHAT/ALIPAY/CARD/CREDIT',
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/SUBMITTED/OUTSTOCK已完成/CANCELLED',
  is_locked     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否',
  confirm_user_id   BIGINT       NULL COMMENT '确认人ID',
  confirm_user_name VARCHAR(64)  NULL COMMENT '确认人姓名',
  confirm_time      DATETIME     NULL COMMENT '确认时间',
  is_archived       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否',
  archive_time      DATETIME     NULL COMMENT '归档时间',
  archive_user_id   BIGINT       NULL COMMENT '归档人ID',
  archive_user_name VARCHAR(64)  NULL COMMENT '归档人姓名',
  source        VARCHAR(16)  NOT NULL DEFAULT 'PC' COMMENT '来源 PC/APP/MINI/SCAN',
  remark        VARCHAR(500)          COMMENT '备注',
  operator_id   BIGINT                COMMENT '收银/经办人',
  operator_name VARCHAR(64)           COMMENT '经办人姓名',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '制单时间',
  out_time      DATETIME              COMMENT '实际出库时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_out_order_no (order_no),
  KEY idx_out_customer (customer_id),
  KEY idx_out_status (status),
  KEY idx_out_time (create_time)
) COMMENT='出货单(销售单)';

DROP TABLE IF EXISTS psi_stock_out_item;
CREATE TABLE psi_stock_out_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  order_id      BIGINT       NOT NULL COMMENT '出货单ID',
  book_id       BIGINT       NOT NULL COMMENT '图书ID',
  book_name     VARCHAR(255)          COMMENT '书名快照',
  isbn          VARCHAR(32)           COMMENT 'ISBN快照',
  quantity      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '数量',
  price         DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '销售单价',
  amount        DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '金额',
  cost_price    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '成本单价(出库成本)',
  remark        VARCHAR(255)          COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_out_item_order (order_id),
  KEY idx_out_item_book (book_id)
) COMMENT='出货单明细';

-- ---------------------------------------------------------------------
-- 五、盘点域
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS psi_stock_check_order;
CREATE TABLE psi_stock_check_order (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  check_no       VARCHAR(64)  NOT NULL COMMENT '盘点单号',
  warehouse_id   BIGINT       NOT NULL COMMENT '仓库ID',
  check_type     VARCHAR(16)  NOT NULL DEFAULT 'FULL' COMMENT '类型 FULL全盘/PART抽盘/CYCLE循环盘',
  status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT盘点中/FINISHED已完成/CANCELLED',
  is_locked      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否',
  confirm_user_id    BIGINT       NULL COMMENT '确认人ID',
  confirm_user_name  VARCHAR(64)  NULL COMMENT '确认人姓名',
  confirm_time       DATETIME     NULL COMMENT '确认时间',
  is_archived        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否',
  archive_time       DATETIME     NULL COMMENT '归档时间',
  archive_user_id    BIGINT       NULL COMMENT '归档人ID',
  archive_user_name  VARCHAR(64)  NULL COMMENT '归档人姓名',
  total_items    INT          NOT NULL DEFAULT 0 COMMENT '盘点品种数',
  diff_items     INT          NOT NULL DEFAULT 0 COMMENT '差异品种数',
  profit_qty     DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '盘盈数量',
  loss_qty       DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '盘亏数量',
  remark         VARCHAR(500)          COMMENT '备注',
  operator_id    BIGINT                COMMENT '盘点人',
  operator_name  VARCHAR(64)           COMMENT '盘点人姓名',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  finish_time    DATETIME              COMMENT '完成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_check_no (check_no),
  KEY idx_check_wh (warehouse_id)
) COMMENT='盘点单';

DROP TABLE IF EXISTS psi_stock_check_item;
CREATE TABLE psi_stock_check_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  order_id      BIGINT       NOT NULL COMMENT '盘点单ID',
  book_id       BIGINT       NOT NULL COMMENT '图书ID',
  book_name     VARCHAR(255)          COMMENT '书名快照',
  isbn          VARCHAR(32)           COMMENT 'ISBN快照',
  book_qty      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '账面数量',
  real_qty      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '实盘数量',
  diff_qty      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '差异数量(实盘-账面)',
  diff_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '差异金额',
  scan_code     VARCHAR(64)           COMMENT '扫码/盘点标记',
  remark        VARCHAR(255)          COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_check_item_order (order_id)
) COMMENT='盘点单明细';

-- ---------------------------------------------------------------------
-- 六、调拨与退货域
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS psi_stock_transfer_order;
CREATE TABLE psi_stock_transfer_order (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '调拨单ID',
  transfer_no    VARCHAR(64)  NOT NULL COMMENT '调拨单号',
  from_warehouse BIGINT       NOT NULL COMMENT '调出仓库',
  to_warehouse   BIGINT       NOT NULL COMMENT '调入仓库',
  total_qty      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '总数量',
  status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/TRANSFERRED/CANCELLED',
  is_locked      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否',
  confirm_user_id    BIGINT       NULL COMMENT '确认人ID',
  confirm_user_name  VARCHAR(64)  NULL COMMENT '确认人姓名',
  confirm_time       DATETIME     NULL COMMENT '确认时间',
  is_archived        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否',
  archive_time       DATETIME     NULL COMMENT '归档时间',
  archive_user_id    BIGINT       NULL COMMENT '归档人ID',
  archive_user_name  VARCHAR(64)  NULL COMMENT '归档人姓名',
  remark         VARCHAR(500)          COMMENT '备注',
  operator_id    BIGINT                COMMENT '经办人',
  operator_name  VARCHAR(64)           COMMENT '经办人姓名',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  transfer_time  DATETIME              COMMENT '调拨完成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_transfer_no (transfer_no)
) COMMENT='库存调拨单';

DROP TABLE IF EXISTS psi_stock_transfer_item;
CREATE TABLE psi_stock_transfer_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  order_id    BIGINT       NOT NULL COMMENT '调拨单ID',
  book_id     BIGINT       NOT NULL COMMENT '图书ID',
  book_name   VARCHAR(255)          COMMENT '书名快照',
  isbn        VARCHAR(32)           COMMENT 'ISBN快照',
  quantity    DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '调拨数量',
  PRIMARY KEY (id),
  KEY idx_transfer_item_order (order_id)
) COMMENT='调拨单明细';

DROP TABLE IF EXISTS psi_return_order;
CREATE TABLE psi_return_order (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '退货单ID',
  return_no     VARCHAR(64)  NOT NULL COMMENT '退货单号',
  return_type   VARCHAR(16)  NOT NULL COMMENT '类型 PURCHASE采购退货/SALES销售退货',
  partner_id    BIGINT                COMMENT '供应商/客户ID',
  partner_name  VARCHAR(128)          COMMENT '往来单位名称',
  warehouse_id  BIGINT       NOT NULL COMMENT '仓库ID',
  total_qty     DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '总数量',
  total_amount  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '总金额',
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/FINISHED/CANCELLED',
  is_locked     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否',
  confirm_user_id   BIGINT       NULL COMMENT '确认人ID',
  confirm_user_name VARCHAR(64)  NULL COMMENT '确认人姓名',
  confirm_time      DATETIME     NULL COMMENT '确认时间',
  is_archived       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否',
  archive_time      DATETIME     NULL COMMENT '归档时间',
  archive_user_id   BIGINT       NULL COMMENT '归档人ID',
  archive_user_name VARCHAR(64)  NULL COMMENT '归档人姓名',
  reason        VARCHAR(255)          COMMENT '退货原因',
  operator_id   BIGINT                COMMENT '经办人',
  operator_name VARCHAR(64)           COMMENT '经办人姓名',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  finish_time   DATETIME              COMMENT '完成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_return_no (return_no),
  KEY idx_return_type (return_type)
) COMMENT='退货单(采购退货/销售退货)';

DROP TABLE IF EXISTS psi_return_item;
CREATE TABLE psi_return_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  order_id    BIGINT       NOT NULL COMMENT '退货单ID',
  book_id     BIGINT       NOT NULL COMMENT '图书ID',
  book_name   VARCHAR(255)          COMMENT '书名快照',
  isbn        VARCHAR(32)           COMMENT 'ISBN快照',
  quantity    DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '数量',
  price       DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '单价',
  amount      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '金额',
  PRIMARY KEY (id),
  KEY idx_return_item_order (order_id)
) COMMENT='退货单明细';

-- ---------------------------------------------------------------------
-- 七、库存流水 (所有出入库动作的统一台账，报表与追溯的核心)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS psi_stock_flow;
CREATE TABLE psi_stock_flow (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  biz_type      VARCHAR(32)  NOT NULL COMMENT '业务类型 IN采购入库 OUT销售出库 CHECK盘点调整 TRANSFER_IN调拨入 TRANSFER_OUT调拨出 RET_PUR采购退货 RET_SAL销售退货 INIT期初',
  biz_no        VARCHAR(64)           COMMENT '业务单号',
  book_id       BIGINT       NOT NULL COMMENT '图书ID',
  warehouse_id  BIGINT       NOT NULL COMMENT '仓库ID',
  direction     TINYINT      NOT NULL COMMENT '方向 1入库 -1出库',
  quantity      DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '变动数量(绝对值)',
  before_qty    DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '变动前数量',
  after_qty     DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '变动后数量',
  unit_cost     DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '单位成本',
  operator_id   BIGINT                COMMENT '操作人',
  operator_name VARCHAR(64)           COMMENT '操作人姓名',
  source        VARCHAR(16)  NOT NULL DEFAULT 'PC' COMMENT '来源 PC/APP/MINI/SCAN',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (id),
  KEY idx_flow_book (book_id),
  KEY idx_flow_wh (warehouse_id),
  KEY idx_flow_time (create_time),
  KEY idx_flow_biz (biz_type, biz_no)
) COMMENT='库存变动流水';

-- ---------------------------------------------------------------------
-- 七之二、单据确认锁定与修订留痕
-- ---------------------------------------------------------------------

-- 单据修订申请（锁定后修订的唯一在途凭据）
DROP TABLE IF EXISTS psi_order_revision;
CREATE TABLE psi_order_revision (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '修订申请ID',
  order_type       VARCHAR(16)  NOT NULL COMMENT '单据类型 IN/OUT/CHECK/TRANSFER/RETURN',
  order_id         BIGINT       NOT NULL COMMENT '单据ID',
  order_no         VARCHAR(64)           COMMENT '单据号快照',
  reason           VARCHAR(500) NOT NULL COMMENT '修订原因(必填)',
  status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审批/APPROVED已批准已解锁/REJECTED已驳回/CANCELLED已撤销/CLOSED修订完成',
  before_snapshot  LONGTEXT              COMMENT '修改前快照(JSON)，审批解锁瞬间采集',
  after_snapshot   LONGTEXT              COMMENT '修改后快照(JSON)，重新确认瞬间采集',
  apply_user_id    BIGINT                COMMENT '申请人',
  apply_user_name  VARCHAR(64)           COMMENT '申请人姓名',
  apply_time       DATETIME              COMMENT '申请时间',
  approve_user_id  BIGINT                COMMENT '审批人',
  approve_user_name VARCHAR(64)          COMMENT '审批人姓名',
  approve_time     DATETIME              COMMENT '审批时间',
  approve_remark   VARCHAR(500)          COMMENT '审批意见',
  close_time       DATETIME              COMMENT '闭环时间(重新确认)',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_rev_order (order_type, order_id),
  KEY idx_rev_status (status, create_time)
) COMMENT='单据修订申请';

-- 单据操作流水（5 类单据统一台账，取代 psi_stock_in_log 的新增写入）
DROP TABLE IF EXISTS psi_order_action_log;
CREATE TABLE psi_order_action_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  order_type    VARCHAR(16)  NOT NULL COMMENT '单据类型 IN/OUT/CHECK/TRANSFER/RETURN',
  order_id      BIGINT       NOT NULL COMMENT '单据ID',
  order_no      VARCHAR(64)           COMMENT '单据号',
  action        VARCHAR(32)  NOT NULL COMMENT '动作 CREATE/SUBMIT/INSTOCK/OUTSTOCK/FINISH/CANCEL/TRANSFER/CONFIRM/REVISION_*',
  operator_id   BIGINT                COMMENT '操作人',
  operator_name VARCHAR(64)           COMMENT '操作人姓名',
  remark        VARCHAR(500)          COMMENT '备注',
  ext_json      LONGTEXT              COMMENT '扩展信息(JSON)',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_act_order (order_type, order_id),
  KEY idx_act_time (create_time)
) COMMENT='单据操作流水(全类型统一)';

-- ---------------------------------------------------------------------
-- 八、二维码/条码域 (移动扫码与盘点)
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS sys_qrcode;
CREATE TABLE sys_qrcode (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  biz_type    VARCHAR(32)  NOT NULL COMMENT '业务类型 BOOK/ORDER/STOCK/CHECK',
  biz_id      VARCHAR(64)  NOT NULL COMMENT '业务标识(如ISBN/单号)',
  qr_content  VARCHAR(255) NOT NULL COMMENT '二维码内容',
  qr_url      VARCHAR(255)          COMMENT '二维码图片地址',
  scan_count  INT          NOT NULL DEFAULT 0 COMMENT '扫码次数',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_qr_biz (biz_type, biz_id)
) COMMENT='二维码记录';

DROP TABLE IF EXISTS sys_scan_log;
CREATE TABLE sys_scan_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  code        VARCHAR(128) NOT NULL COMMENT '扫码内容',
  biz_type    VARCHAR(32)           COMMENT '识别出的业务类型',
  biz_id      VARCHAR(64)           COMMENT '业务标识',
  device      VARCHAR(32)  NOT NULL DEFAULT 'APP' COMMENT '设备 APP/MINI/PC',
  user_id     BIGINT                COMMENT '扫码人',
  result      VARCHAR(32)           COMMENT '结果 SUCCESS/FAIL',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
  PRIMARY KEY (id),
  KEY idx_scan_code (code),
  KEY idx_scan_time (create_time)
) COMMENT='扫码记录';

SET FOREIGN_KEY_CHECKS = 1;
-- =====================================================================
--  End of schema.sql
-- =====================================================================