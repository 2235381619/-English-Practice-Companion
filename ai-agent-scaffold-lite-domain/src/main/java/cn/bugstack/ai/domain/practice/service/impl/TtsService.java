package cn.bugstack.ai.domain.practice.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TTS 服务 — 常驻 edge-tts 引擎
 *
 * 纯功能实现，属于 domain 层。
 * 维护长生命周期 Python edge-tts 子进程，通过 stdin/stdout 通信。
 */
@Slf4j
public class TtsService {

    private static final String DEFAULT_PYTHON = "C:\\Users\\kqj\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";
    private static final String DEFAULT_FFPLAY = "C:\\ffmpge\\bin\\ffplay.exe";
    private static final String DEFAULT_VOICE = "en-US-JennyNeural";

    private final String pythonPath;
    private final String ffplayPath;
    private final String voice;
    private final ExecutorService bg;

    private Process proc;
    private OutputStream stdin;
    private volatile boolean running;

    public TtsService() {
        this(DEFAULT_PYTHON, DEFAULT_FFPLAY, DEFAULT_VOICE);
    }

    public TtsService(String pythonPath, String ffplayPath, String voice) {
        this.pythonPath = pythonPath;
        this.ffplayPath = ffplayPath;
        this.voice = voice;
        this.bg = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (running) return;
        String script = buildScript(voice);
        proc = new ProcessBuilder(pythonPath, "-u", "-c", script).start();
        stdin = proc.getOutputStream();
        running = true;
        log.info("TTS engine started (voice={})", voice);
    }

    public void speak(String text) {
        if (!running) { log.warn("TTS engine not started"); return; }
        bg.submit(() -> {
            try {
                stdin.write((text + "\n").getBytes("UTF-8"));
                stdin.flush();
                byte[] sizeBuf = new byte[4];
                int read = proc.getInputStream().readNBytes(sizeBuf, 0, 4);
                if (read < 4) return;
                int size = ByteBuffer.wrap(sizeBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if (size <= 0 || size > 10_000_000) return;
                byte[] mp3 = proc.getInputStream().readNBytes(size);
                File tmp = File.createTempFile("tts_", ".mp3");
                try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(mp3); }
                new ProcessBuilder(ffplayPath, "-nodisp", "-autoexit", "-loglevel", "quiet", tmp.getAbsolutePath())
                        .start().waitFor();
                if (!tmp.delete()) log.warn("Temp file delete failed");
            } catch (Exception e) {
                log.warn("TTS: {}", e.getMessage());
            }
        });
    }

    public byte[] synthesize(String text) throws IOException {
        if (!running) throw new IOException("TTS engine not started");
        stdin.write((text + "\n").getBytes("UTF-8"));
        stdin.flush();
        byte[] sizeBuf = new byte[4];
        int read = proc.getInputStream().readNBytes(sizeBuf, 0, 4);
        if (read < 4) throw new IOException("TTS: insufficient header");
        int size = ByteBuffer.wrap(sizeBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (size <= 0 || size > 10_000_000) throw new IOException("TTS: invalid size " + size);
        return proc.getInputStream().readNBytes(size);
    }

    public void stop() {
        running = false;
        if (proc != null) { proc.destroy(); proc = null; }
        bg.shutdownNow();
        log.info("TTS engine stopped");
    }

    public boolean isRunning() { return running; }

    private static String buildScript(String voice) {
        return String.join("\n",
                "import asyncio,sys,struct",
                "from edge_tts import Communicate",
                "async def run():",
                "  while True:",
                "    line=sys.stdin.readline()",
                "    if not line: break",
                "    text=line.rstrip()",
                "    if not text: continue",
                "    data=b''",
                "    async for c in Communicate(text,\"" + voice + "\").stream():",
                "      if c['type']=='audio': data+=c['data']",
                "    sys.stdout.buffer.write(struct.pack('<I',len(data)))",
                "    sys.stdout.buffer.write(data)",
                "    sys.stdout.buffer.flush()",
                "asyncio.run(run())");
    }
}
