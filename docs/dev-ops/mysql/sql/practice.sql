-- =============================================
-- 口语陪练 — 练习会话表
-- =============================================
CREATE DATABASE IF NOT EXISTS ai_practice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_practice;

CREATE TABLE IF NOT EXISTS practice_session (
    session_id   VARCHAR(32)  NOT NULL COMMENT '会话ID',
    scenario     VARCHAR(20)  NOT NULL COMMENT '场景(interview/restaurant/meeting)',
    created_time BIGINT       NOT NULL COMMENT '创建时间戳',
    active       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否活跃',
    round_count  INT          NOT NULL DEFAULT 0 COMMENT '对话轮数',
    session_data JSON         COMMENT '会话完整数据(JSON)',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习会话表';

-- =============================================
-- 口语陪练 — 对话轮次表
-- =============================================
CREATE TABLE IF NOT EXISTS conversation_round (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id      VARCHAR(32)  NOT NULL COMMENT '会话ID',
    round_number    INT          NOT NULL COMMENT '轮次',
    user_input      TEXT         COMMENT '用户原始输入',
    asr_text        TEXT         COMMENT 'ASR转写文本',
    ai_reply        TEXT         COMMENT 'AI回复',
    score           INT          DEFAULT 0 COMMENT '评分(1-10)',
    corrected_text  TEXT         COMMENT '语法修正文本',
    grammar_issues  JSON         COMMENT '语法问题列表',
    suggestions     JSON         COMMENT '表达建议列表',
    timestamp       BIGINT       NOT NULL COMMENT '轮次时间戳',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话轮次表';
