package cn.bugstack.ai.infrastructure.adapter.repository;

import cn.bugstack.ai.domain.practice.adapter.ISessionRepository;
import cn.bugstack.ai.domain.practice.model.entity.ConversationRound;
import cn.bugstack.ai.domain.practice.model.entity.PracticeSession;
import cn.bugstack.ai.domain.practice.model.valobj.EvaluationResult;
import cn.bugstack.ai.domain.practice.model.valobj.Scenario;
import cn.bugstack.ai.infrastructure.dao.po.PracticeSessionPO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 练习会话持久化 — 文件 JSON 存储
 *
 * 会话数据以 JSON 格式存储在 data/practice/sessions/{sessionId}.json，
 * 无需数据库即可运行。
 */
@Slf4j
@Repository
public class PracticeSessionRepository implements ISessionRepository {

    private static final String DATA_DIR = "data/practice/sessions";

    public PracticeSessionRepository() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            log.warn("Failed to create data directory: {}", DATA_DIR);
        }
    }

    @Override
    public void save(PracticeSession session) {
        writeFile(session);
    }

    @Override
    public PracticeSession findById(String sessionId) {
        Path path = Paths.get(DATA_DIR, sessionId + ".json");
        if (!Files.exists(path)) return null;
        try {
            String json = Files.readString(path);
            PracticeSessionPO po = JSON.parseObject(json, PracticeSessionPO.class);
            return toEntity(po);
        } catch (Exception e) {
            log.warn("Failed to load session: {}", sessionId, e);
            return null;
        }
    }

    @Override
    public void update(PracticeSession session) {
        writeFile(session);
    }

    @Override
    public void delete(String sessionId) {
        try {
            Files.deleteIfExists(Paths.get(DATA_DIR, sessionId + ".json"));
        } catch (IOException e) {
            log.warn("Failed to delete session: {}", sessionId, e);
        }
    }

    private void writeFile(PracticeSession session) {
        try {
            PracticeSessionPO po = toPO(session);
            String json = JSON.toJSONString(po, true);
            Files.writeString(Paths.get(DATA_DIR, session.getSessionId() + ".json"), json);
        } catch (IOException e) {
            log.warn("Failed to save session: {}", session.getSessionId(), e);
        }
    }

    private PracticeSessionPO toPO(PracticeSession session) {
        PracticeSessionPO po = new PracticeSessionPO();
        po.setSessionId(session.getSessionId());
        po.setScenarioCode(session.getScenario().getCode());
        po.setCreatedTime(session.getCreatedTime());
        po.setActive(session.isActive());

        if (session.getRounds() != null) {
            po.setRounds(session.getRounds().stream().map(r -> {
                PracticeSessionPO.ConversationRoundPO rpo = new PracticeSessionPO.ConversationRoundPO();
                rpo.setRoundNumber(r.getRoundNumber());
                rpo.setUserInput(r.getUserInput());
                rpo.setAsrText(r.getAsrText());
                rpo.setAiReply(r.getAiReply());
                rpo.setTimestamp(r.getTimestamp());
                if (r.getEvaluation() != null) {
                    rpo.setScore(r.getEvaluation().getScore());
                    rpo.setCorrectedText(r.getEvaluation().getCorrectedText());
                    rpo.setGrammarIssues(r.getEvaluation().getGrammarIssues());
                    rpo.setSuggestions(r.getEvaluation().getSuggestions());
                }
                return rpo;
            }).collect(Collectors.toList()));
        }
        return po;
    }

    private PracticeSession toEntity(PracticeSessionPO po) {
        PracticeSession session = new PracticeSession(Scenario.fromCode(po.getScenarioCode()));
        // Use reflection to set sessionId since it's generated in constructor
        setField(session, "sessionId", po.getSessionId());
        setField(session, "createdTime", po.getCreatedTime());
        setField(session, "active", po.isActive());

        if (po.getRounds() != null) {
            for (PracticeSessionPO.ConversationRoundPO rpo : po.getRounds()) {
                ConversationRound round = new ConversationRound(rpo.getRoundNumber(), rpo.getUserInput());
                round.setAsrText(rpo.getAsrText());
                round.setAiReply(rpo.getAiReply());
                round.setTimestamp(rpo.getTimestamp());

                EvaluationResult eval = EvaluationResult.builder()
                        .originalText(rpo.getUserInput())
                        .correctedText(rpo.getCorrectedText())
                        .grammarIssues(rpo.getGrammarIssues())
                        .suggestions(rpo.getSuggestions())
                        .score(rpo.getScore())
                        .build();
                round.setEvaluation(eval);
                session.getRounds().add(round);
            }
        }
        return session;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            log.warn("Failed to set field: {}", fieldName, e);
        }
    }
}
