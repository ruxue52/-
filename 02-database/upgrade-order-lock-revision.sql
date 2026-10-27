-- =====================================================================
--  晓镜书阁 · 书店进销存管理系统  升级脚本：单据确认锁定 + 修订审批 + 报表分级授权
--  适用：已建库的 MySQL 8.x 环境（schema.sql + data.sql 已执行过）
--  说明：全新初始化无需执行本脚本，schema.sql / data.sql 已含本次全部变更。
--        MySQL 8 的 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS，本脚本只能执行一次；
--        重跑前请先执行文末「收尾校验」确认列与表尚不存在。
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) 5 张单据主表新增「确认锁定」4 列（位置紧随 status）
-- ---------------------------------------------------------------------
ALTER TABLE psi_stock_in_order
  ADD COLUMN is_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否' AFTER status,
  ADD COLUMN confirm_user_id BIGINT NULL COMMENT '确认人ID' AFTER is_locked,
  ADD COLUMN confirm_user_name VARCHAR(64) NULL COMMENT '确认人姓名' AFTER confirm_user_id,
  ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间' AFTER confirm_user_name;

ALTER TABLE psi_stock_out_order
  ADD COLUMN is_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否' AFTER status,
  ADD COLUMN confirm_user_id BIGINT NULL COMMENT '确认人ID' AFTER is_locked,
  ADD COLUMN confirm_user_name VARCHAR(64) NULL COMMENT '确认人姓名' AFTER confirm_user_id,
  ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间' AFTER confirm_user_name;

ALTER TABLE psi_stock_check_order
  ADD COLUMN is_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否' AFTER status,
  ADD COLUMN confirm_user_id BIGINT NULL COMMENT '确认人ID' AFTER is_locked,
  ADD COLUMN confirm_user_name VARCHAR(64) NULL COMMENT '确认人姓名' AFTER confirm_user_id,
  ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间' AFTER confirm_user_name;

ALTER TABLE psi_stock_transfer_order
  ADD COLUMN is_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否' AFTER status,
  ADD COLUMN confirm_user_id BIGINT NULL COMMENT '确认人ID' AFTER is_locked,
  ADD COLUMN confirm_user_name VARCHAR(64) NULL COMMENT '确认人姓名' AFTER confirm_user_id,
  ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间' AFTER confirm_user_name;

ALTER TABLE psi_return_order
  ADD COLUMN is_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认锁定 1是 0否' AFTER status,
  ADD COLUMN confirm_user_id BIGINT NULL COMMENT '确认人ID' AFTER is_locked,
  ADD COLUMN confirm_user_name VARCHAR(64) NULL COMMENT '确认人姓名' AFTER confirm_user_id,
  ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间' AFTER confirm_user_name;

-- ---------------------------------------------------------------------
-- 2) 新建 单据修订申请 表
--    快照用 LONGTEXT 存 JSON 字符串（不用 MySQL 原生 JSON 类型，保持到达梦 DM8 的迁移兼容性）
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 3) 新建 单据操作流水 表（5 类单据统一台账，取代 psi_stock_in_log 的新增写入）
-- ---------------------------------------------------------------------
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
-- 4) 存量回填：历史终态单据一律置为已锁定
-- ---------------------------------------------------------------------
UPDATE psi_stock_in_order       SET is_locked = 1 WHERE status IN ('INSTOCK','CANCELLED');
UPDATE psi_stock_out_order      SET is_locked = 1 WHERE status IN ('OUTSTOCK','CANCELLED');
UPDATE psi_stock_check_order    SET is_locked = 1 WHERE status IN ('FINISHED','CANCELLED');
UPDATE psi_stock_transfer_order SET is_locked = 1 WHERE status IN ('TRANSFERRED','CANCELLED');
UPDATE psi_return_order         SET is_locked = 1 WHERE status IN ('FINISHED','CANCELLED');

-- ---------------------------------------------------------------------
-- 5) 历史流水搬迁：psi_stock_in_log -> psi_order_action_log
--    旧表保留不删（降低基线风险），此后新写入只进统一表
-- ---------------------------------------------------------------------
INSERT INTO psi_order_action_log (order_type, order_id, order_no, action, operator_id, operator_name, remark, create_time)
SELECT 'IN', l.order_id, o.order_no, l.action, l.operator_id, l.operator_name, l.remark, l.create_time
FROM psi_stock_in_log l
LEFT JOIN psi_stock_in_order o ON o.id = l.order_id;

-- ---------------------------------------------------------------------
-- 6) 权限数据
-- ---------------------------------------------------------------------

-- 6.1 报表菜单码改名：report -> report:view
--     （hasPerm 有「父级码隐含子码」规则，若保留 report 会等于放行 report:R01~R15；引用按 id 故授权行无需改）
UPDATE sys_permission SET perm_code = 'report:view', perm_name = '报表中心(入口)' WHERE id = 21;

-- 6.2 单据确认/修订权限组（分组节点 100 不授予任何非 ADMIN 角色）
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, path, icon, sort_no) VALUES
 (100,'order','单据管理','MENU',0,NULL,NULL,10),
 (101,'order:confirm','单据确认锁定','BUTTON',100,NULL,NULL,1),
 (102,'order:revision:apply','修订申请','BUTTON',100,NULL,NULL,2),
 (103,'order:revision:approve','修订审批','BUTTON',100,NULL,NULL,3);

-- 6.3 15 张报表逐张授权（父节点 = 21 报表中心）
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, path, icon, sort_no) VALUES
 (201,'report:R01','R01销售日报表','BUTTON',21,NULL,NULL,1),
 (202,'report:R02','R02销售月报表','BUTTON',21,NULL,NULL,2),
 (203,'report:R03','R03商品销售排行榜','BUTTON',21,NULL,NULL,3),
 (204,'report:R04','R04分类销售汇总表','BUTTON',21,NULL,NULL,4),
 (205,'report:R05','R05采购到货明细表','BUTTON',21,NULL,NULL,5),
 (206,'report:R06','R06供应商采购汇总表','BUTTON',21,NULL,NULL,6),
 (207,'report:R07','R07库存结存表','BUTTON',21,NULL,NULL,7),
 (208,'report:R08','R08库存预警表','BUTTON',21,NULL,NULL,8),
 (209,'report:R09','R09库龄分析表','BUTTON',21,NULL,NULL,9),
 (210,'report:R10','R10盘点差异表','BUTTON',21,NULL,NULL,10),
 (211,'report:R11','R11商品毛利分析表','BUTTON',21,NULL,NULL,11),
 (212,'report:R12','R12客户消费明细表','BUTTON',21,NULL,NULL,12),
 (213,'report:R13','R13会员积分报表','BUTTON',21,NULL,NULL,13),
 (214,'report:R14','R14库存流水台账','BUTTON',21,NULL,NULL,14),
 (215,'report:R15','R15进销存汇总表','BUTTON',21,NULL,NULL,15);

-- 6.4 角色授权（INSERT IGNORE 保证可重复执行）
--     ADMIN：全部新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id >= 100;

--     MANAGER：确认+申请+审批 + 全部报表
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
 (2,101),(2,102),(2,103),
 (2,201),(2,202),(2,203),(2,204),(2,205),(2,206),(2,207),(2,208),(2,209),(2,210),
 (2,211),(2,212),(2,213),(2,214),(2,215);

--     PURCHASER：确认+申请 + 采购/库存类报表，并放行报表中心入口(21) 与调拨/退货用到的库存报表
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
 (3,21),(3,101),(3,102),(3,205),(3,206),(3,207),(3,214),(3,215);

--     SALES：确认+申请 + 销售/会员类报表（不含 R11 商品毛利，属敏感报表）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
 (4,21),(4,101),(4,102),(4,201),(4,202),(4,203),(4,204),(4,212),(4,213);

--     STOCK：确认+申请 + 库存/盘点类报表
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
 (5,21),(5,101),(5,102),(5,207),(5,208),(5,209),(5,210),(5,214),(5,215);

-- ---------------------------------------------------------------------
-- 7) 收尾校验（应恰好返回 5 条 is_locked 列 + 2 张新表）
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'is_locked' ORDER BY TABLE_NAME;

SELECT TABLE_NAME FROM information_schema.tables
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('psi_order_revision','psi_order_action_log');

SELECT COUNT(*) AS migrated_action_logs FROM psi_order_action_log;
