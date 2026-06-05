package cn.bugstack.ai.domain.practice.adapter;

import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;

/**
 * 会话持久化接口
 */
public interface ISessionRepository {

    void save(PracticeSession session);

    PracticeSession findById(String sessionId);

    void update(PracticeSession session);

    void delete(String sessionId);
}
