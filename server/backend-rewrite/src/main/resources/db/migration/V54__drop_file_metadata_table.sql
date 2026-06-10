-- 磁盘文件存储模块（com.hospital.file，/api/v1/files）下线：
-- 前端统一走 /api/v1/photos（数据库存储，V34），file_metadata 表从未有生产数据。
DROP TABLE IF EXISTS file_metadata;
