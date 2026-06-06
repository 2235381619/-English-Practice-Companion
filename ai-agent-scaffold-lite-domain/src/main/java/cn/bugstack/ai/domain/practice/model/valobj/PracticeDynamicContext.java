package cn.bugstack.ai.domain.practice.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则树上下文 — 节点间传递的数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeDynamicContext {

    /** 会话 ID */
    private String sessionId;

    /** ASR 识别结果（ASRNode 产出） */
    private String asrText;

    /** LLM 回复（LLMNode 产出） */
    private String replyText;

    /** TTS 音频 URL（TTSNode 产出，预留） */
    private String audioUrl;

    /** 执行成功 */
    private boolean success;

    /** 错误信息 */
    private String errorMsg;
}
