package cn.bugstack.ai.domain.translate.agent;

/**
 * Agent for transcribing audio to text using ASR models (e.g., Whisper).
 */
public interface TranscriberAgent {

    /**
     * Transcribe audio data to text.
     *
     * @param sessionId current translation session ID
     * @param audioData raw audio bytes
     * @param mimeType  audio MIME type (e.g., audio/webm, audio/wav)
     * @return transcribed text, or empty string if no speech detected
     */
    String transcribe(String sessionId, byte[] audioData, String mimeType);
}
