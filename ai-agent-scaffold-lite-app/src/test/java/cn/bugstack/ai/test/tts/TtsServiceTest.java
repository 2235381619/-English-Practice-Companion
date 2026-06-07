package cn.bugstack.ai.test.tts;

import cn.bugstack.ai.domain.practice.model.valobj.VoiceVo;
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
    public void test_synthesize_default() {
        String text = "Hello everyone, welcome to the speech synthesis test.";
        File outputDir = new File("src/test/resources/audio");
        if (!outputDir.exists()) outputDir.mkdirs();

        File result = ttsService.synthesize(text, new File(outputDir, "tts-default.mp3"));
        assert result != null && result.exists() && result.length() > 0
                : "合成失败，请检查配置或网络";
        log.info("默认参数合成成功, {} 字节", result.length());
    }

    @Test
    public void test_synthesize_custom_voice() {
        String text = "Hello everyone, welcome to the speech synthesis test.";
        File outputDir = new File("src/test/resources/audio");
        if (!outputDir.exists()) outputDir.mkdirs();

        VoiceVo voice = new VoiceVo(70, 80, 60);
        File result = ttsService.synthesize(text, new File(outputDir, "tts-custom.mp3"), voice);

        assert result != null && result.exists() && result.length() > 0
                : "合成失败，请检查配置或网络";
        log.info("自定义参数合成成功, {} 字节, voice={}", result.length(), voice);
    }

    @Test
    public void test_synthesize_empty_text() {
        File result = ttsService.synthesize("", new File("src/test/resources/audio/empty.mp3"));
        assert result == null : "空文本应返回 null";
    }
}
