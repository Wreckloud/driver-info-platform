ALTER TABLE driver_record
    ADD COLUMN project VARCHAR(100) NULL COMMENT '业务项目' AFTER destination,
    ADD COLUMN quantity VARCHAR(100) NULL COMMENT '数量描述' AFTER project,
    ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注' AFTER quantity;
