package cn.bugstack.ai.domain.translate.agent;

import reactor.core.publisher.Flux;

/**
 * Agent for translating between languages using LLM models.
 */
public interface TranslatorAgent {

    /**
     * Translate text from source language to target language.
     * Returns a Flux of String chunks for streaming output.
     *
     * @param text       source text to translate
     * @param sourceLang source language code (e.g., "en")
     * @param targetLang target language code (e.g., "zh")
     * @param sessionId  current translation session ID for context continuity
     * @return streaming Flux of translated text chunks
     */
    Flux<String> translate(String text, String sourceLang, String targetLang, String sessionId);
}
