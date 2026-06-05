package cn.bugstack.ai.test.translate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.FileSystemResource;
import org.junit.Test;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class EnglishTutorDemo {

    static final String BASE_URL = "https://apis.itedus.cn";
    static final String API_KEY = "sk-BPQbnWSOzxEjOxg9627aFfD87aC44b238eC99c0c992dDd94";
    static final String PYTHON = "C:\\Users\\kqj\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";
    static final String FFPLAY = "C:\\ffmpge\\bin\\ffplay.exe";

    static TtsEngine tts; // 后台常驻 TTS 引擎
    static final ExecutorService bg = Executors.newCachedThreadPool();

    @Test
    public void run() throws Exception { main(new String[0]); }

    public static void main(String[] args) throws Exception {
        var sc = new java.util.Scanner(System.in);
        System.out.println("\n=== AI English Tutor ===");
        System.out.println("Scene: 1-Interview  2-Restaurant  3-Meeting");
        System.out.print("Choose: ");
        String prompt = switch (sc.nextLine().trim()) {
            case "2" -> "You are a waiter. Greet customer, take order, correct grammar.";
            case "3" -> "You are a colleague in a business meeting. Lead discussion, correct grammar.";
            default -> "You are a senior tech interviewer. Ask questions, correct grammar, respond naturally.";
        };

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        int micIdx = -1;
        for (int i = 0; i < mixers.length; i++) {
            Mixer m = AudioSystem.getMixer(mixers[i]);
            for (Line.Info li : m.getTargetLineInfo()) {
                if (li.getLineClass() == TargetDataLine.class && (mixers[i].getName().contains("PRO X") || micIdx < 0))
                    micIdx = i;
            }
        }
        System.out.println("Mic: " + mixers[micIdx].getName());

        log.info("Loading models & TTS engine...");
        var audioApi = OpenAiAudioApi.builder().baseUrl(BASE_URL).apiKey(API_KEY).build();
        var asr = new OpenAiAudioTranscriptionModel(audioApi);
        var chatApi = OpenAiApi.builder().baseUrl(BASE_URL + "/").apiKey(API_KEY)
                .completionsPath("v1/chat/completions").build();
        var chat = OpenAiChatModel.builder().openAiApi(chatApi)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.7d).build())
                .build();

        // 启动常驻 TTS 引擎
        tts = new TtsEngine();
        tts.start();

        System.out.println("\nSpeak! AI responds with voice. (q=quit)");
        int round = 1;
        while (true) {
            System.out.print("\n--- Round " + round + " ---\n");
            System.out.flush();

            File wav = recordWithVad(micIdx);
            if (wav == null) break;
            if (wav.length() < 2000) { System.out.println("(too short)"); continue; }

            System.out.print("ASR... ");
            String text = transcribe(asr, wav);
            if (text == null || text.isBlank()) { System.out.println("(no speech)"); continue; }
            System.out.println(text);

            System.out.print("GPT... ");
            String reply = chat.call(prompt + "\n\nUser said: " + text +
                    "\n\nCorrect grammar, suggest better expressions, respond naturally.");
            System.out.println(reply);

            // TTS 后台播放，不阻塞下一轮
            tts.speak(reply);
            round++;
        }
        tts.stop();
        System.out.println("\nDone!");
    }

    // ===== 常驻 TTS 引擎 =====
    static class TtsEngine {
        Process proc;
        OutputStream stdin;

        void start() throws Exception {
            // Python 守护脚本：读一行 → TTS → 写 [4字节大小][音频数据] 到 stdout
            String script = "import asyncio,sys,struct; from edge_tts import Communicate\n" +
                    "async def run():\n" +
                    "  while True:\n" +
                    "    line=sys.stdin.readline()\n" +
                    "    if not line: break\n" +
                    "    text=line.rstrip()\n" +
                    "    if not text: continue\n" +
                    "    data=b''\n" +
                    "    async for c in Communicate(text,'en-US-JennyNeural').stream():\n" +
                    "      if c['type']=='audio': data+=c['data']\n" +
                    "    sys.stdout.buffer.write(struct.pack('<I',len(data)))\n" +
                    "    sys.stdout.buffer.write(data)\n" +
                    "    sys.stdout.buffer.flush()\n" +
                    "asyncio.run(run())";
            proc = new ProcessBuilder(PYTHON, "-u", "-c", script).start();
            stdin = proc.getOutputStream();
        }

        void speak(String text) {
            bg.submit(() -> {
                try {
                    stdin.write((text + "\n").getBytes("UTF-8"));
                    stdin.flush();
                    byte[] sizeBuf = new byte[4];
                    proc.getInputStream().readNBytes(sizeBuf, 0, 4);
                    int size = ByteBuffer.wrap(sizeBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    byte[] mp3 = proc.getInputStream().readNBytes(size);
                    File f = File.createTempFile("tts_", ".mp3");
                    try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(mp3); }
                    new ProcessBuilder(FFPLAY, "-nodisp", "-autoexit", "-loglevel", "quiet",
                            f.getAbsolutePath()).start().waitFor();
                    f.delete();
                } catch (Exception e) { log.warn("TTS: {}", e.getMessage()); }
            });
        }

        void stop() { if (proc != null) proc.destroy(); }
    }

    // ===== VAD 录音 =====
    static File recordWithVad(int mixerIdx) throws Exception {
        AudioFormat fmt = new AudioFormat(16000, 16, 1, true, false);
        TargetDataLine line = AudioSystem.getTargetDataLine(fmt, AudioSystem.getMixerInfo()[mixerIdx]);
        line.open(fmt); line.start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] frame = new byte[640];
        int silenceLimit = 20;
        long deadline = System.currentTimeMillis() + 30000;
        int sf = 0;
        boolean speaking = false;
        int total = 0;

        while (System.currentTimeMillis() < deadline) {
            if (System.in.available() > 0 && (System.in.read() == 'q' || System.in.read() == 'Q')) {
                line.stop(); line.close(); return null;
            }
            int n = line.read(frame, 0, frame.length);
            if (n <= 0) continue;
            double e = 0;
            for (int i = 0; i < n / 2; i++) {
                short s = (short)((frame[i*2] & 0xFF) | (frame[i*2+1] << 8));
                e += s * s;
            }
            e = Math.sqrt(e / (n / 2));
            if (e > 800) {
                buf.write(frame, 0, n); total += n;
                if (!speaking) { speaking = true; System.out.print("(speaking) "); }
                sf = 0;
            } else if (speaking) {
                buf.write(frame, 0, n); total += n;
                if (++sf >= silenceLimit) break;
            }
        }
        line.stop(); line.close();
        if (!speaking || total < 2000) return new File(".");

        byte[] pcm = buf.toByteArray();
        int dataLen = pcm.length - Math.min(sf * frame.length, pcm.length);
        if (dataLen < 2000) dataLen = pcm.length;

        File wav = File.createTempFile("tutor_", ".wav");
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(wav))) {
            dos.writeBytes("RIFF"); dos.writeInt(Integer.reverseBytes(36 + dataLen));
            dos.writeBytes("WAVEfmt "); dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short)1)); dos.writeShort(Short.reverseBytes((short)1));
            dos.writeInt(Integer.reverseBytes(16000)); dos.writeInt(Integer.reverseBytes(32000));
            dos.writeShort(Short.reverseBytes((short)2)); dos.writeShort(Short.reverseBytes((short)16));
            dos.writeBytes("data"); dos.writeInt(Integer.reverseBytes(dataLen));
            dos.write(pcm, 0, dataLen);
        }
        System.out.println("(" + (dataLen / 32) + "ms)");
        return wav;
    }

    static String transcribe(OpenAiAudioTranscriptionModel asr, File wav) {
        try {
            var opts = OpenAiAudioTranscriptionOptions.builder()
                    .model("whisper-1").language("en")
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                    .temperature(0.0f).build();
            var resp = asr.call(new AudioTranscriptionPrompt(new FileSystemResource(wav), opts));
            String t = resp.getResult().getOutput();
            return (t == null || t.isBlank()) ? null : t.trim();
        } catch (Exception e) { log.warn("ASR: {}", e.getMessage()); return null; }
    }
}