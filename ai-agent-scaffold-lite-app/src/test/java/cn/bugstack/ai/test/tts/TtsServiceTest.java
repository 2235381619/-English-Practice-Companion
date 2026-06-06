package cn.bugstack.ai.test.tts;

import cn.bugstack.ai.domain.practice.service.ITtsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;

/**
 * 讯飞 TTS 语音合成测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TtsServiceTest {

    @Resource
    private ITtsService ttsService;

    @Test
    public void test_synthesize_english() {
        String text = "Hello everyone, welcome to the speech synthesis test. " +
                      "This is a demonstration of the iflytek text to speech engine.";

        File outputDir = new File("src/test/resources/audio");
        if (!outputDir.exists()) outputDir.mkdirs();

        File outputFile = new File(outputDir, "tts-test-output.mp3");

        log.info("开始 TTS 合成, text={}", text);
        File result = ttsService.synthesize(text, outputFile);

        assert result != null : "合成失败，返回 null";
        assert result.exists() : "文件未生成";
        assert result.length() > 0 : "文件为空，请检查配置或网络";

        log.info("TTS 合成成功, 文件: {}", result.getAbsolutePath());
        log.info("文件大小: {} 字节", result.length());
    }

    @Test
    public void test_synthesize_empty_text() {
        File result = ttsService.synthesize("", new File("src/test/resources/audio/empty.mp3"));
        assert result == null : "空文本应返回 null";
    }

    @Test
    public void test_synthesize_null_text() {
        File result = ttsService.synthesize(null, new File("src/test/resources/audio/null.mp3"));
        assert result == null : "null 文本应返回 null";
    }
}
