package cn.bugstack.ai.domain.practice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.*;

/**
 * 音频服务 — VAD + ASR 转写
 *
 * 纯功能实现，属于 domain 层。
 * VAD 使用能量检测（RMS threshold 800），ASR 使用 OpenAI Whisper-1。
 */
@Slf4j
@Service
public class AudioService {

    public static final double VAD_THRESHOLD = 800.0;
    public static final int SILENCE_LIMIT = 40;
    public static final int MIN_AUDIO_BYTES = 2000;
    public static final long RECORD_TIMEOUT_MS = 30000;

    private final OpenAiAudioTranscriptionModel asr;

    public AudioService(OpenAiAudioApi audioApi) {
        this.asr = new OpenAiAudioTranscriptionModel(audioApi);
    }

    public String transcribe(File wavFile) {
        try {
            var opts = OpenAiAudioTranscriptionOptions.builder()
                    .model("whisper-1").language("en")
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                    .temperature(0.0f).build();
            var resp = asr.call(new AudioTranscriptionPrompt(new FileSystemResource(wavFile), opts));
            String text = resp.getResult().getOutput();
            return (text == null || text.isBlank()) ? null : text.trim();
        } catch (Exception e) {
            log.warn("ASR failed: {}", e.getMessage());
            return null;
        }
    }

    public String transcribeFromPcm(byte[] pcmData, int sampleRate) {
        try {
            File wav = pcmToWav(pcmData, sampleRate);
            String text = transcribe(wav);
            if (!wav.delete()) log.warn("Temp wav delete failed");
            return text;
        } catch (Exception e) {
            log.warn("ASR from PCM failed: {}", e.getMessage());
            return null;
        }
    }

    public File recordWithVad() throws Exception {
        int micIdx = findBestMicIndex();
        if (micIdx < 0) { log.error("No mic found"); return null; }
        return recordWithVad(micIdx);
    }

    public File recordWithVad(int mixerIdx) throws Exception {
        AudioFormat fmt = new AudioFormat(16000, 16, 1, true, false);
        TargetDataLine line = AudioSystem.getTargetDataLine(fmt, AudioSystem.getMixerInfo()[mixerIdx]);
        line.open(fmt); line.start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] frame = new byte[640];
        long deadline = System.currentTimeMillis() + RECORD_TIMEOUT_MS;
        int sf = 0; boolean speaking = false; int total = 0;

        while (System.currentTimeMillis() < deadline) {
            int n = line.read(frame, 0, frame.length);
            if (n <= 0) continue;
            double rms = computeRms(frame, n);
            if (rms > VAD_THRESHOLD) {
                buf.write(frame, 0, n); total += n;
                if (!speaking) { speaking = true; log.info("(speaking)"); }
                sf = 0;
            } else if (speaking) {
                buf.write(frame, 0, n); total += n;
                if (++sf >= SILENCE_LIMIT) break;
            }
        }
        line.stop(); line.close();
        if (!speaking || total < MIN_AUDIO_BYTES) return null;

        byte[] pcm = buf.toByteArray();
        int dataLen = Math.min(pcm.length - sf * frame.length, pcm.length);
        if (dataLen < MIN_AUDIO_BYTES) dataLen = pcm.length;

        File wav = pcmToWav(pcm, dataLen, 16000);
        log.info("Recorded {}ms", dataLen / 32);
        return wav;
    }

    public static int findBestMicIndex() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        int fallback = -1;
        for (int i = 0; i < mixers.length; i++) {
            Mixer m = AudioSystem.getMixer(mixers[i]);
            for (Line.Info li : m.getTargetLineInfo()) {
                if (li.getLineClass() == TargetDataLine.class) {
                    if (mixers[i].getName().contains("PRO X")) return i;
                    if (fallback < 0) fallback = i;
                }
            }
        }
        return fallback;
    }

    public static File pcmToWav(byte[] pcmData, int sampleRate) throws IOException {
        return pcmToWav(pcmData, pcmData.length, sampleRate);
    }

    private static File pcmToWav(byte[] pcm, int dataLen, int sampleRate) throws IOException {
        File wav = File.createTempFile("practice_", ".wav");
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(wav))) {
            writeWavHeader(dos, dataLen, sampleRate);
            dos.write(pcm, 0, dataLen);
        }
        return wav;
    }

    private static void writeWavHeader(DataOutputStream dos, int dataLen, int sampleRate) throws IOException {
        int byteRate = sampleRate * 2;
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataLen));
        dos.writeBytes("WAVEfmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(byteRate));
        dos.writeShort(Short.reverseBytes((short) 2));
        dos.writeShort(Short.reverseBytes((short) 16));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataLen));
    }

    public static double computeRms(byte[] frame, int n) {
        double sum = 0;
        int samples = n / 2;
        for (int i = 0; i < samples; i++) {
            short s = (short) ((frame[i * 2] & 0xFF) | (frame[i * 2 + 1] << 8));
            sum += s * s;
        }
        return Math.sqrt(sum / samples);
    }
}
