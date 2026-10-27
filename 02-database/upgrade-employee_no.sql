-- =====================================================================
--  晓镜书阁 · 书店进销存管理系统  升级脚本：sys_user 新增工号字段
--  适用：已建库的 MySQL 8.x 环境（schema.sql + data.sql 已执行过）
--  说明：全新初始化无需执行本脚本，schema.sql 已含 employee_no 列与唯一键。
--        本脚本可重复安全执行前请确认列/索引尚不存在。
-- =====================================================================
SET NAMES utf8mb4;

-- 1) 新增工号列（可空，兼容存量数据；位置紧随 username）
ALTER TABLE sys_user
  ADD COLUMN employee_no VARCHAR(32) NULL COMMENT '工号' AFTER username;

-- 2) 工号唯一约束
ALTER TABLE sys_user
  ADD UNIQUE KEY uk_employee_no (employee_no);

-- 3) 回填种子用户工号（按 username 精确匹配，避免误改其他账号）
UPDATE sys_user SET employee_no = 'E001' WHERE username = 'admin';
UPDATE sys_user SET employee_no = 'E002' WHERE username = 'manager';
UPDATE sys_user SET employee_no = 'E003' WHERE username = 'buyer01';
UPDATE sys_user SET employee_no = 'E004' WHERE username = 'cashier01';
UPDATE sys_user SET employee_no = 'E005' WHERE username = 'stock01';
UPDATE sys_user SET employee_no = 'E006' WHERE username = 'sales02';
