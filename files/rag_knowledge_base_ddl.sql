-- ============================================================================
-- 企业智能文档问答助手（RAG知识库系统）- 完整数据库初始化脚本
-- ============================================================================
-- 数据库字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 创建日期: 2025-05-21
-- ============================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `rag_knowledge_base` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `rag_knowledge_base`;

-- ============================================================================
-- 第一层：用户与权限体系（3张表）
-- ============================================================================

-- 2.1 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` BIGINT NOT NULL COMMENT '部门ID',
  `name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `parent_id` BIGINT NULL COMMENT '父部门ID，支持树形结构',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(200) NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 2.2 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成）',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号，唯一',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密的密码',
  `nickname` VARCHAR(50) NULL COMMENT '显示名称',
  `email` VARCHAR(100) NULL COMMENT '邮箱',
  `avatar_url` VARCHAR(255) NULL COMMENT '头像URL（MinIO地址）',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色：0=超管 1=KB管理员 2=普通员工',
  `dept_id` BIGINT NULL COMMENT '所属部门ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0=禁用 1=正常',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_role` (`role`),
  KEY `idx_email` (`email`),
  FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2.3 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `user_id` BIGINT NULL COMMENT '操作用户ID',
  `module` VARCHAR(50) NOT NULL COMMENT '模块名：doc/qa/ticket等',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型：upload/delete/ask等',
  `target_id` BIGINT NULL COMMENT '操作对象ID（可能是doc_id、msg_id等）',
  `ip` VARCHAR(50) NULL COMMENT '客户端IP地址',
  `user_agent` VARCHAR(300) NULL COMMENT '浏览器User Agent信息',
  `cost_ms` INT NULL COMMENT '请求耗时（毫秒）',
  `result` TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果：0=失败 1=成功',
  `error_msg` TEXT NULL COMMENT '失败原因详情',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_create_time` (`create_time`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================================
-- 第二层：知识库与文档体系（3张表）
-- ============================================================================

-- 3.1 知识库表
CREATE TABLE IF NOT EXISTS `doc_knowledge_base` (
  `id` BIGINT NOT NULL COMMENT '知识库ID',
  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
  `description` VARCHAR(500) NULL COMMENT '知识库描述',
  `owner_id` BIGINT NOT NULL COMMENT '创建人/拥有者ID',
  `visibility` TINYINT NOT NULL DEFAULT 2 COMMENT '可见范围：0=私有 1=部门 2=全员',
  `dept_id` BIGINT NULL COMMENT '可见部门ID（visibility=1时生效）',
  `doc_count` INT DEFAULT 0 COMMENT '文档数量（冗余字段，用于快速计数）',
  `embed_model` VARCHAR(50) DEFAULT 'text-embedding-v3' COMMENT '使用的Embedding模型名',
  `chunk_size` INT DEFAULT 512 COMMENT '分片token大小',
  `chunk_overlap` INT DEFAULT 64 COMMENT '分片重叠token数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_visibility` (`visibility`),
  KEY `idx_dept_id` (`dept_id`),
  FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 3.2 文档表
CREATE TABLE IF NOT EXISTS `doc_document` (
  `id` BIGINT NOT NULL COMMENT '文档ID',
  `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `file_name` VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_type` VARCHAR(20) NOT NULL COMMENT '文件格式：pdf/docx/txt',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `minio_path` VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径（kb_id/doc_id/filename）',
  `parse_status` TINYINT NOT NULL DEFAULT 0 COMMENT '解析状态：0=待处理 1=解析中 2=完成 3=失败',
  `parse_error` TEXT NULL COMMENT '解析失败原因',
  `chunk_count` INT DEFAULT 0 COMMENT '生成的分片数量',
  `upload_user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `word_count` INT DEFAULT 0 COMMENT '文档总字数',
  `page_count` INT NULL COMMENT '页数（仅PDF）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_parse_status` (`parse_status`),
  KEY `idx_create_time` (`create_time`),
  FOREIGN KEY (`kb_id`) REFERENCES `doc_knowledge_base` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`upload_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 3.3 文档分片表（RAG的核心数据表）
CREATE TABLE IF NOT EXISTS `doc_chunk` (
  `id` BIGINT NOT NULL COMMENT '分片ID',
  `doc_id` BIGINT NOT NULL COMMENT '所属文档ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID（冗余，用于加速过滤）',
  `chunk_index` INT NOT NULL COMMENT '分片序号（从0开始递增）',
  `content` LONGTEXT NOT NULL COMMENT '分片文本内容',
  `content_length` INT DEFAULT 0 COMMENT '文本字数',
  `vector_id` VARCHAR(100) NULL COMMENT 'Qdrant Point ID，关联向量库记录',
  `embed_status` TINYINT NOT NULL DEFAULT 0 COMMENT '向量化状态：0=待向量化 1=完成 2=失败',
  `page_num` INT NULL COMMENT '来源页码（PDF文档）',
  `source_type` VARCHAR(20) NULL COMMENT '来源类型：paragraph/table/image_text',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_embed_status` (`embed_status`),
  KEY `idx_vector_id` (`vector_id`),
  FULLTEXT KEY `ft_content` (`content`) WITH PARSER ngram COMMENT '全文索引用于BM25关键词检索',
  FOREIGN KEY (`doc_id`) REFERENCES `doc_document` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`kb_id`) REFERENCES `doc_knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分片表（核心：存储分片文本和向量ID）';

-- ============================================================================
-- 第三层：问答与对话体系（4张表）
-- ============================================================================

-- 4.1 对话会话表
CREATE TABLE IF NOT EXISTS `qa_conversation` (
  `id` BIGINT NOT NULL COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '发起用户ID',
  `kb_id` BIGINT NOT NULL COMMENT '问答所在的知识库ID',
  `title` VARCHAR(200) NULL COMMENT '会话标题（通常取首条问题前50字）',
  `msg_count` INT DEFAULT 0 COMMENT '消息数量（冗余，提升查询性能）',
  `last_msg_time` DATETIME NULL COMMENT '最后消息时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_last_msg_time` (`last_msg_time`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`kb_id`) REFERENCES `doc_knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表（一个会话包含多条问答）';

-- 4.2 问答消息表（核心表）
CREATE TABLE IF NOT EXISTS `qa_message` (
  `id` BIGINT NOT NULL COMMENT '消息ID',
  `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
  `user_id` BIGINT NOT NULL COMMENT '提问用户ID',
  `question` TEXT NOT NULL COMMENT '用户提问内容',
  `answer` LONGTEXT NULL COMMENT 'AI回答内容',
  `source_chunks` JSON NULL COMMENT '引用的chunk_id列表（用于溯源），格式：[{chunk_id, doc_id, doc_title, score}]',
  `tokens_used` INT NULL COMMENT '此次问答消耗的LLM token数',
  `response_ms` INT NULL COMMENT '问答响应耗时（毫秒）',
  `quality_score` TINYINT NULL COMMENT '用户评分：1=赞同 -1=不满意 0=未评分 NULL=未评',
  `is_transferred` TINYINT DEFAULT 0 COMMENT '是否已转人工工单：0=否 1=是',
  `llm_model` VARCHAR(50) NULL COMMENT '使用的LLM模型名（如qwen-long等）',
  `search_type` VARCHAR(20) NULL COMMENT '检索类型：vector/bm25/hybrid',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_quality_score` (`quality_score`),
  KEY `idx_create_time` (`create_time`),
  FOREIGN KEY (`conversation_id`) REFERENCES `qa_conversation` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答消息表（记录每一条问答）';

-- 4.3 答案反馈表（用户点赞/踩，用于评估RAG效果）
CREATE TABLE IF NOT EXISTS `qa_feedback` (
  `id` BIGINT NOT NULL COMMENT '反馈ID',
  `message_id` BIGINT NOT NULL COMMENT '关联的消息ID',
  `user_id` BIGINT NOT NULL COMMENT '反馈用户ID',
  `score` TINYINT NOT NULL COMMENT '评分：1=赞同/满意 -1=不满意/有问题',
  `reason` VARCHAR(500) NULL COMMENT '不满意原因（可选）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_score` (`score`),
  FOREIGN KEY (`message_id`) REFERENCES `qa_message` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答案反馈表（评估RAG系统质量）';

-- ============================================================================
-- 第四层：工单管理体系（1张表）
-- ============================================================================

-- 5.1 咨询工单表
CREATE TABLE IF NOT EXISTS `ticket_order` (
  `id` BIGINT NOT NULL COMMENT '工单ID',
  `message_id` BIGINT NULL COMMENT '来源的QA消息ID（可能为空）',
  `user_id` BIGINT NOT NULL COMMENT '提单用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '工单标题',
  `description` TEXT NULL COMMENT '问题详细描述',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待处理 1=处理中 2=已解决 3=关闭',
  `assignee_id` BIGINT NULL COMMENT '处理人/负责人ID',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1=低 2=中 3=高',
  `kb_id` BIGINT NULL COMMENT '所属知识库ID（可选）',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `resolved_time` DATETIME NULL COMMENT '解决时间',
  `reply` LONGTEXT NULL COMMENT '人工回复内容',
  `reply_time` DATETIME NULL COMMENT '回复时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_created_time` (`created_time`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  FOREIGN KEY (`assignee_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
  FOREIGN KEY (`message_id`) REFERENCES `qa_message` (`id`) ON DELETE SET NULL,
  FOREIGN KEY (`kb_id`) REFERENCES `doc_knowledge_base` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询工单表（AI答不了自动转为人工工单）';

-- ============================================================================
-- 第五层：数据统计体系（2张表）
-- ============================================================================

-- 6.1 每日统计汇总表
CREATE TABLE IF NOT EXISTS `stat_daily` (
  `id` BIGINT NOT NULL COMMENT '统计ID',
  `stat_date` DATE NOT NULL UNIQUE COMMENT '统计日期（唯一约束）',
  `total_qa` INT DEFAULT 0 COMMENT '当日总问答数',
  `total_users` INT DEFAULT 0 COMMENT '当日活跃用户数',
  `positive_rate` DECIMAL(5, 2) DEFAULT 0 COMMENT '好评率（百分比，0-100）',
  `transfer_count` INT DEFAULT 0 COMMENT '转人工工单数量',
  `avg_response_ms` INT DEFAULT 0 COMMENT '平均响应耗时（毫秒）',
  `new_docs` INT DEFAULT 0 COMMENT '新增文档数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计汇总表（定时任务每天凌晨计算）';

-- 6.2 热门问题统计表
CREATE TABLE IF NOT EXISTS `stat_hot_question` (
  `id` BIGINT NOT NULL COMMENT '记录ID',
  `question_hash` VARCHAR(64) NOT NULL COMMENT '问题MD5哈希（用于去重）',
  `question` TEXT NOT NULL COMMENT '完整问题文本',
  `kb_id` BIGINT NULL COMMENT '所属知识库ID（可选）',
  `ask_count` INT NOT NULL DEFAULT 1 COMMENT '被提问次数',
  `last_ask_time` DATETIME NULL COMMENT '最后提问时间',
  `avg_quality` DECIMAL(3, 2) DEFAULT 0 COMMENT '平均满意度（0-1）',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_hash` (`question_hash`, `kb_id`),
  KEY `idx_ask_count` (`ask_count`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_last_ask_time` (`last_ask_time`),
  FOREIGN KEY (`kb_id`) REFERENCES `doc_knowledge_base` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门问题统计表（用于推荐常见问题）';

-- ============================================================================
-- 索引优化补充
-- ============================================================================

-- 对于高频查询的组合索引
ALTER TABLE `qa_message` ADD INDEX `idx_user_kb_time` (`user_id`, `conversation_id`, `create_time`);
ALTER TABLE `doc_chunk` ADD INDEX `idx_kb_status_id` (`kb_id`, `embed_status`, `id`);
ALTER TABLE `stat_hot_question` ADD INDEX `idx_kb_count_time` (`kb_id`, `ask_count` DESC, `last_ask_time` DESC);

-- ============================================================================
-- 初始化基础数据
-- ============================================================================

-- 1. 插入默认部门
INSERT INTO `sys_dept` (`id`, `name`, `parent_id`, `sort`, `remark`) VALUES
(1, '管理部', NULL, 1, '系统管理部门'),
(2, '技术部', 1, 2, '技术团队'),
(3, '市场部', 1, 3, '市场团队'),
(4, '运营部', 1, 4, '运营团队');

-- 2. 插入默认管理员用户（密码：admin123，需要用BCrypt加密）
-- BCrypt格式示例：$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-- 实际部署时应该用真实的BCrypt密码
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `role`, `dept_id`, `status`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DRj2De', '系统管理员', 'admin@company.com', 0, 1, 1),
(2, 'kb_admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DRj2De', '知识库管理员', 'kb_admin@company.com', 1, 2, 1),
(3, 'user1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DRj2De', '普通员工1', 'user1@company.com', 2, 2, 1),
(4, 'user2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36DRj2De', '普通员工2', 'user2@company.com', 2, 3, 1);

-- 3. 插入默认知识库
INSERT INTO `doc_knowledge_base` (`id`, `name`, `description`, `owner_id`, `visibility`, `dept_id`, `doc_count`, `embed_model`) VALUES
(1, '企业规章制度', '公司各类规章制度、流程指南、行为准则等', 1, 2, NULL, 0, 'text-embedding-v3'),
(2, '产品文档库', '产品功能文档、用户指南、API文档', 2, 2, NULL, 0, 'text-embedding-v3'),
(3, '技术部内部知识库', '技术部门的技术文档、最佳实践、踩坑记录', 2, 1, 2, 0, 'text-embedding-v3');

-- ============================================================================
-- 数据库初始化完成
-- ============================================================================
-- 注意事项：
-- 1. 密码使用BCrypt加密，$2a$10$开头的是加密后的密码
-- 2. 所有表都支持逻辑删除（is_deleted），生产环境勿物理删除
-- 3. LONGTEXT字段用于存储大量文本（QA、文档内容等）
-- 4. JSON字段用于灵活存储结构化数据（source_chunks）
-- 5. 全文索引（FULLTEXT）用于BM25关键词检索
-- 6. 定时任务需要定期计算stat_daily表的数据
-- 7. Qdrant Point ID和MySQL doc_chunk表通过vector_id字段关联
-- ============================================================================
