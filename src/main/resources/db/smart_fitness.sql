CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` TINYINT NULL COMMENT '性别：0未知，1男，2女',
  `phone` VARCHAR(20) NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL COMMENT '邮箱',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role_status` (`role`, `status`),
  KEY `idx_user_phone` (`phone`),
  KEY `idx_user_email` (`email`),
  CONSTRAINT `chk_user_role` CHECK (`role` IN ('STUDENT', 'TEACHER', 'ADMIN')),
  CONSTRAINT `chk_user_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS `tb_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(32) NOT NULL COMMENT '学院编码',
  `name` VARCHAR(100) NOT NULL COMMENT '学院名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_code` (`code`),
  KEY `idx_department_status` (`status`),
  CONSTRAINT `chk_department_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院';

CREATE TABLE IF NOT EXISTS `tb_fitness_class` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `department_id` BIGINT NOT NULL COMMENT '所属学院 ID',
  `name` VARCHAR(100) NOT NULL COMMENT '班级名称',
  `grade_year` SMALLINT NOT NULL COMMENT '入学年级',
  `teacher_id` BIGINT NULL COMMENT '负责教师用户 ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_department_name_grade` (`department_id`, `name`, `grade_year`),
  KEY `idx_class_department` (`department_id`),
  KEY `idx_class_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政班级';

CREATE TABLE IF NOT EXISTS `tb_student_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `student_no` VARCHAR(32) NOT NULL COMMENT '学号',
  `department_id` BIGINT NOT NULL COMMENT '学院 ID',
  `class_id` BIGINT NOT NULL COMMENT '班级 ID',
  `birth_date` DATE NULL COMMENT '出生日期',
  `enrollment_year` SMALLINT NULL COMMENT '入学年份',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_profile_user` (`user_id`),
  UNIQUE KEY `uk_student_profile_student_no` (`student_no`),
  KEY `idx_student_profile_department` (`department_id`),
  KEY `idx_student_profile_class` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生档案';

CREATE TABLE IF NOT EXISTS `tb_test_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '体测项目名称',
  `description` VARCHAR(500) NULL COMMENT '项目说明',
  `unit` VARCHAR(20) NOT NULL COMMENT '成绩计量单位',
  `created_by` BIGINT NOT NULL COMMENT '创建人用户 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_item_name` (`name`),
  KEY `idx_test_item_status` (`status`),
  KEY `idx_test_item_created_by` (`created_by`),
  CONSTRAINT `chk_test_item_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体测项目';

CREATE TABLE IF NOT EXISTS `tb_test_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '被测学生用户 ID',
  `item_id` BIGINT NOT NULL COMMENT '体测项目 ID',
  `score` DECIMAL(10,2) NOT NULL COMMENT '体测成绩',
  `video_url` VARCHAR(500) NULL COMMENT '体测视频地址',
  `data_url` VARCHAR(500) NULL COMMENT '体测原始数据地址',
  `test_time` DATETIME NOT NULL COMMENT '实际体测时间',
  `created_by` BIGINT NOT NULL COMMENT '录入教师或管理员用户 ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_test_record_user_item_time` (`user_id`, `item_id`, `test_time`),
  KEY `idx_test_record_item_time` (`item_id`, `test_time`),
  KEY `idx_test_record_created_by` (`created_by`),
  CONSTRAINT `chk_test_record_score` CHECK (`score` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体测记录';

CREATE TABLE IF NOT EXISTS `tb_test_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id` BIGINT NOT NULL COMMENT '体测项目 ID',
  `title` VARCHAR(100) NOT NULL COMMENT '场次标题',
  `start_time` DATETIME NOT NULL COMMENT '场次开始时间',
  `end_time` DATETIME NOT NULL COMMENT '场次结束时间',
  `appointment_start_time` DATETIME NOT NULL COMMENT '预约开始时间',
  `appointment_end_time` DATETIME NOT NULL COMMENT '预约结束时间',
  `capacity` INT NOT NULL COMMENT '总名额',
  `available_quota` INT NOT NULL COMMENT '剩余名额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿，1已发布，2已关闭，3已结束，4已取消',
  `created_by` BIGINT NOT NULL COMMENT '创建人用户 ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_test_session_item_status_start` (`item_id`, `status`, `start_time`),
  KEY `idx_test_session_status_appointment` (`status`, `appointment_start_time`, `appointment_end_time`),
  CONSTRAINT `chk_test_session_status` CHECK (`status` IN (0, 1, 2, 3, 4)),
  CONSTRAINT `chk_test_session_capacity` CHECK (`capacity` > 0),
  CONSTRAINT `chk_test_session_quota` CHECK (`available_quota` >= 0 AND `available_quota` <= `capacity`),
  CONSTRAINT `chk_test_session_time` CHECK (`start_time` < `end_time`),
  CONSTRAINT `chk_test_session_appointment_time` CHECK (`appointment_start_time` < `appointment_end_time` AND `appointment_end_time` <= `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体测场次';

CREATE TABLE IF NOT EXISTS `tb_test_appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` VARCHAR(64) NOT NULL COMMENT '异步预约请求 ID',
  `user_id` BIGINT NOT NULL COMMENT '预约学生用户 ID',
  `session_id` BIGINT NOT NULL COMMENT '体测场次 ID',
  `status` TINYINT NOT NULL COMMENT '1已预约，2已取消',
  `booked_at` DATETIME NOT NULL COMMENT '预约时间',
  `cancelled_at` DATETIME NULL COMMENT '取消时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_request_id` (`request_id`),
  UNIQUE KEY `uk_appointment_user_session` (`user_id`, `session_id`),
  KEY `idx_appointment_session_status` (`session_id`, `status`),
  KEY `idx_appointment_user_status` (`user_id`, `status`),
  CONSTRAINT `chk_appointment_status` CHECK (`status` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体测预约';
