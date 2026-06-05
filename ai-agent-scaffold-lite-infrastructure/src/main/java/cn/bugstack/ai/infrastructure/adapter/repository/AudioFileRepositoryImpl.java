package cn.bugstack.ai.infrastructure.adapter.repository;

import cn.bugstack.ai.domain.practice.adapter.IAudioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 音频文件存储 — 文件系统实现
 *
 * 音频文件存储在 data/practice/audio/{sessionId}/{roundNumber}.wav
 */
@Slf4j
@Repository
public class AudioFileRepositoryImpl implements IAudioRepository {

    private static final String AUDIO_DIR = "data/practice/audio";

    public AudioFileRepositoryImpl() {
        try {
            Files.createDirectories(Paths.get(AUDIO_DIR));
        } catch (IOException e) {
            log.warn("Failed to create audio directory: {}", AUDIO_DIR);
        }
    }

    @Override
    public String save(String sessionId, int roundNumber, File audioFile) {
        try {
            Path sessionDir = Paths.get(AUDIO_DIR, sessionId);
            Files.createDirectories(sessionDir);

            String fileName = roundNumber + ".wav";
            Path target = sessionDir.resolve(fileName);
            Files.copy(audioFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Audio saved: sessionId={}, round={}, path={}", sessionId, roundNumber, target);
            return target.toString();
        } catch (IOException e) {
            log.warn("Failed to save audio: sessionId={}, round={}", sessionId, roundNumber, e);
            return null;
        }
    }

    @Override
    public File load(String path) {
        if (path == null) return null;
        File file = new File(path);
        return file.exists() ? file : null;
    }
}
