-- =====================================================================
--  晓镜书阁 · 书店进销存管理系统  升级脚本：单据归档
--  适用：已建库的 MySQL 8.x 环境（schema.sql + data.sql + upgrade-order-lock-revision.sql 已执行过）
--  说明：全新初始化无需执行本脚本，schema.sql / data.sql 已含本次全部变更。
--        MySQL 8 的 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS，本脚本只能执行一次。
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) 5 张单据主表新增归档 4 列（位置紧随 confirm_time）
--    归档语义：终态 + 已确认锁定的单据，超过保留期后可归档；
--              归档后列表默认隐藏（可显式查询），单据只读、不可再修改或修订。
-- ---------------------------------------------------------------------
ALTER TABLE psi_stock_in_order
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否' AFTER confirm_time,
  ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER is_archived,
  ADD COLUMN archive_user_id BIGINT NULL COMMENT '归档人ID' AFTER archive_time,
  ADD COLUMN archive_user_name VARCHAR(64) NULL COMMENT '归档人姓名' AFTER archive_user_id;

ALTER TABLE psi_stock_out_order
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否' AFTER confirm_time,
  ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER is_archived,
  ADD COLUMN archive_user_id BIGINT NULL COMMENT '归档人ID' AFTER archive_time,
  ADD COLUMN archive_user_name VARCHAR(64) NULL COMMENT '归档人姓名' AFTER archive_user_id;

ALTER TABLE psi_stock_check_order
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否' AFTER confirm_time,
  ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER is_archived,
  ADD COLUMN archive_user_id BIGINT NULL COMMENT '归档人ID' AFTER archive_time,
  ADD COLUMN archive_user_name VARCHAR(64) NULL COMMENT '归档人姓名' AFTER archive_user_id;

ALTER TABLE psi_stock_transfer_order
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否' AFTER confirm_time,
  ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER is_archived,
  ADD COLUMN archive_user_id BIGINT NULL COMMENT '归档人ID' AFTER archive_time,
  ADD COLUMN archive_user_name VARCHAR(64) NULL COMMENT '归档人姓名' AFTER archive_user_id;

ALTER TABLE psi_return_order
  ADD COLUMN is_archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否已归档 1是 0否' AFTER confirm_time,
  ADD COLUMN archive_time DATETIME NULL COMMENT '归档时间' AFTER is_archived,
  ADD COLUMN archive_user_id BIGINT NULL COMMENT '归档人ID' AFTER archive_time,
  ADD COLUMN archive_user_name VARCHAR(64) NULL COMMENT '归档人姓名' AFTER archive_user_id;

-- ---------------------------------------------------------------------
-- 2) 归档查询索引（列表默认按 is_archived=0 过滤，归档页按 1 过滤）
-- ---------------------------------------------------------------------
CREATE INDEX idx_in_archived ON psi_stock_in_order (is_archived, create_time);
CREATE INDEX idx_out_archived ON psi_stock_out_order (is_archived, create_time);
CREATE INDEX idx_check_archived ON psi_stock_check_order (is_archived, create_time);
CREATE INDEX idx_transfer_archived ON psi_stock_transfer_order (is_archived, create_time);
CREATE INDEX idx_return_archived ON psi_return_order (is_archived, create_time);

-- ---------------------------------------------------------------------
-- 3) 权限：单据归档（授予 ADMIN / MANAGER / STOCK）
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, path, icon, sort_no) VALUES
 (104,'order:archive','单据归档','BUTTON',100,NULL,NULL,4);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
 (1,104),(2,104),(5,104);

-- ---------------------------------------------------------------------
-- 4) 收尾校验（is_archived 应恰好 5 条）
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.columns
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'is_archived' ORDER BY TABLE_NAME;
