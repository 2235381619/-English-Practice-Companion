package cn.bugstack.ai.test.ise;

import cn.bugstack.ai.domain.practice.service.IIseService;
import cn.bugstack.ai.domain.practice.model.valobj.IseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;

/**
 * ISE 发音评测测试
 *
 * 音频文件：src/test/resources/audio/ise-test-input.pcm（16kHz 16bit 单声道 PCM）
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class IseServiceTest {

    @Resource
    private IIseService iseService;

    @Test
    public void test_evaluate_pronunciation() throws Exception {
        File audioFile = new File("src/test/resources/audio/tts-custom.mp3");
        assert audioFile.exists() : "音频文件不存在";

        byte[] audioData = Files.readAllBytes(audioFile.toPath());
        assert audioData.length > 64 : "音频数据太短";

        String referenceText = "hello everyone welcome to speech sentence test";
        log.info("ISE 开始：audio={}B, text=\"{}\"", audioData.length, referenceText);

        IseResult result = iseService.evaluate(audioData, referenceText);
        assert result != null : "ISE 结果不应为 null";

        log.info("ISE 完成：success={}, total={}, accuracy={}, fluency={}, integrity={}",
                result.isSuccess(), result.getTotalScore(),
                result.getAccuracyScore(), result.getFluencyScore(),
                result.getIntegrityScore());

        if (result.isSuccess()) {
            assert result.getTotalScore() >= 0 : "总分不应为负";
        }
    }

    @Test
    public void test_evaluate_invalid_params() {
        assert !iseService.evaluate(null, "hello").isSuccess() : "空音频应失败";
        assert !iseService.evaluate(new byte[]{1, 2, 3}, "").isSuccess() : "空文本应失败";
        log.info("参数校验通过");
    }
}
