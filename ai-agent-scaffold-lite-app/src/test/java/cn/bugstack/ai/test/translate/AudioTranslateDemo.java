package cn.bugstack.ai.test.translate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.FileSystemResource;
import org.junit.Test;
import java.io.File;

@Slf4j
public class AudioTranslateDemo {

    // ========== CONFIG ==========
    // TODO: Replace with your own valid API key
    static final String BASE_URL     = "https://apis.itedus.cn";
    static final String API_KEY      = "sk-BPQbnWSOzxEjOxg9627aFfD87aC44b238eC99c0c992dDd94";
    static final String WHISPER_MODEL = "whisper-1";
    static final String CHAT_MODEL   = "gpt-4o-mini";

    static final String FFMPEG = "C:\\Users\\kqj\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg.Essentials_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-8.1.1-essentials_build\\bin\\ffmpeg.exe";
    static final String INPUT_AUDIO = "src/test/resources/audio/speech_sample.wav";
    // ============================

    @Test
    public void runDemo() throws Exception {
        demoPipeline();
    }

    public static void main(String[] args) throws Exception {
        demoPipeline();
    }

    static void demoPipeline() throws Exception {
        log.info("");
        log.info("==============================================");
        log.info(" AI 同声传译 - 音频转录 + 翻译管线 Demo");
        log.info("==============================================");
        log.info("Pipeline: Audio -> ffmpeg -> Whisper ASR -> GPT Translation -> Output");
        log.info("");

        // ── Step 0: Input check ──
        File inFile = new File(INPUT_AUDIO);
        if (!inFile.exists()) {
            log.error("Audio file not found at: {}", inFile.getAbsolutePath());
            log.error("Please place an English speech audio file at: {}", INPUT_AUDIO);
            return;
        }
        log.info("[0/4] Input: {} ({} bytes)", INPUT_AUDIO, inFile.length());

        // ── Step 1: ffmpeg conversion ──
        log.info("[1/4] ffmpeg format conversion...");
        String audioFile;
        try {
            audioFile = convertTo16kMono(INPUT_AUDIO);
            log.info("  -> {}", audioFile);
        } catch (Exception e) {
            log.error("  ffmpeg failed: {}", e.getMessage());
            audioFile = INPUT_AUDIO;
        }

        // ── Step 2: ASR Transcription ──
        log.info("[2/4] ASR Transcription (Whisper)...");
        String transcribed = null;
        try {
            OpenAiAudioApi audioApi = OpenAiAudioApi.builder()
                    .baseUrl(BASE_URL)
                    .apiKey(API_KEY)
                    .build();
            OpenAiAudioTranscriptionModel model = new OpenAiAudioTranscriptionModel(audioApi);

            OpenAiAudioTranscriptionOptions opts = OpenAiAudioTranscriptionOptions.builder()
                    .model(WHISPER_MODEL).language("en")
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                    .temperature(0.0f).build();

            long t0 = System.currentTimeMillis();
            AudioTranscriptionResponse resp = model.call(
                    new AudioTranscriptionPrompt(new FileSystemResource(audioFile), opts));
            long t1 = System.currentTimeMillis();
            transcribed = resp.getResult().getOutput();
            log.info("  ASR took: {} ms", t1 - t0);
            log.info("  Result: {}", transcribed);
        } catch (Exception e) {
            log.warn("  ASR failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.warn("  (This is expected if API key is expired. See TODOs in the config.)");
        }

        if (transcribed == null || transcribed.isBlank()) {
            log.info("  -> Using fallback text for translation demo...");
            transcribed = "This is a demo of real-time audio translation. The system transcribes speech using Whisper and translates it into Chinese.";
        }

        // ── Step 3: Translation ──
        log.info("[3/4] Translation (GPT)...");
        String translated = null;
        try {
            OpenAiApi chatApi = OpenAiApi.builder()
                    .baseUrl(BASE_URL + "/")
                    .apiKey(API_KEY)
                    .completionsPath("v1/chat/completions")
                    .build();

            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(chatApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(CHAT_MODEL).temperature(0.3d).build())
                    .build();

            long t0 = System.currentTimeMillis();
            translated = chatModel.call(String.format(
                    "You are a professional simultaneous interpreter.\n" +
                    "Translate the following English text into Chinese.\n" +
                    "Rules:\n" +
                    "1. Maintain the speaker's tone and style\n" +
                    "2. Keep technical terms accurate\n" +
                    "3. Output ONLY the translation, no explanations\n\n" +
                    "Text: %s", transcribed));
            long t1 = System.currentTimeMillis();
            log.info("  Translation took: {} ms", t1 - t0);
            log.info("  Result: {}", translated);
        } catch (Exception e) {
            log.warn("  Translation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.warn("  (This is expected if API key is expired. See TODOs in the config.)");
            translated = "[Translation unavailable - API key needs to be updated]";
        }

        // ── Step 4: Summary ──
        log.info("[4/4] Pipeline complete!");
        log.info("");
        log.info("================================================");
        log.info(" RESULT");
        log.info("================================================");
        log.info(" Source (EN): {}", transcribed);
        log.info("----------------------------------------------");
        log.info(" Target (ZH): {}", translated != null ? translated : "N/A");
        log.info("================================================");
        log.info("");
        log.info("=== Pipeline Architecture ===");
        log.info("Browser Audio -> WebSocket -> AudioProcessor (VAD) -> TranscriberAgent (Whisper) -> TranslationAgent (GPT) -> CorrectorAgent (async) -> OutputFormatter -> SSE -> SubtitleOverlay");
        log.info("");
        log.info("NOTE: Replace API_KEY with a valid key in AudioTranslateDemo.java");
    }

    static String convertTo16kMono(String input) throws Exception {
        if (input.contains("_16k_mono")) return input;
        String output = input.replaceAll("(\\.\\w+)$", "_16k_mono.wav");
        if (new File(output).exists()) return output;

        ProcessBuilder pb = new ProcessBuilder(FFMPEG, "-y", "-i", input,
                "-ar", "16000", "-ac", "1", "-sample_fmt", "s16", output);
        pb.inheritIO();
        int code = pb.start().waitFor();
        if (code != 0) {
            log.warn("ffmpeg exit={}, using original", code);
            return input;
        }
        return output;
    }
}