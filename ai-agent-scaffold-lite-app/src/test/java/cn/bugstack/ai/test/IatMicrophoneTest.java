package cn.bugstack.ai.test;

import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 讯飞实时语音听写 - 麦克风输入测试
 *
 * 直接注入 IflytekConfiguration 中创建的 IatClient Bean，
 * 不需要手动拼 APPID/APIKey。
 *
 * 运行：右键 run this test，按回车开始录音。
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class IatMicrophoneTest {

    @Resource
    private IatClient iatClient;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static  class MicrophoneAudioSender {

        private static final Logger logger = LoggerFactory.getLogger(MicrophoneAudioSender.class);

        public interface AudioDataCallback {
            void onAudioData(byte[] audioData, int length);
        }

        private final float sampleRate;
        private final int sampleSizeInBits;
        private final int channels;
        private final boolean signed;
        private final boolean bigEndian;
        private final int bufferSize;
        private final AudioDataCallback callback;

        private TargetDataLine microphoneLine;
        private AtomicBoolean isRunning = new AtomicBoolean(false);
        private Thread captureThread;

        public MicrophoneAudioSender(AudioDataCallback callback) {
            // 默认参数 16kHz 16bit 单声道
            this(16000, 16, 1, true, false, 4096, callback);
        }

        public MicrophoneAudioSender(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian,
                                     int bufferSize, AudioDataCallback callback) {
            this.sampleRate = sampleRate;
            this.sampleSizeInBits = sampleSizeInBits;
            this.channels = channels;
            this.signed = signed;
            this.bigEndian = bigEndian;
            this.bufferSize = bufferSize;
            this.callback = callback;
        }

        /**
         * 启动麦克风采集
         */
        public void start() {
            if (isRunning.get()) return;
            isRunning.set(true);

            captureThread = new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    microphoneLine = (TargetDataLine) AudioSystem.getLine(info);
                    microphoneLine.open(format);
                    microphoneLine.start();

                    byte[] buffer = new byte[bufferSize];

                    while (isRunning.get()) {
                        int count = microphoneLine.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            callback.onAudioData(buffer, count); // 回调实时数据
                        }
                        // TimeUnit.MILLISECONDS.sleep(20);
                    }

                    microphoneLine.stop();
                    microphoneLine.close();
                    logger.info("麦克风采集线程结束");

                } catch (Exception e) {
                    logger.error("麦克风采集出错", e);
                }
            });

            captureThread.setName("Microphone-Capture-Thread");
            captureThread.start();
        }

        /**
         * 停止采集
         */
        public void stop() {
            isRunning.set(false);
            try {
                if (captureThread != null) {
                    captureThread.join();
                }
            } catch (InterruptedException e) {
                logger.error("麦克风关闭出错", e);
            }
        }
    }

    @Test
    public void test_microphone() throws Exception {
        MicrophoneAudioSender sender = new MicrophoneAudioSender((audioData, length) -> {
            iatClient.sendMessage(audioData, 1);
        });

        List<Text> resultSegments = Collections.synchronizedList(new ArrayList<>());
        Date[] dateBegin = new Date[1];

        try (Scanner scanner = new Scanner(System.in)) {
            log.info("========================================");
            log.info("  讯飞实时语音听写 - 麦克风测试");
            log.info("  按回车开始录音，对着麦克风说话...");
            log.info("  停顿2秒自动结束");
            log.info("========================================");
            scanner.nextLine();

            dateBegin[0] = new Date();

            iatClient.start(new AbstractIatWebSocketListener() {

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info(">>> WebSocket 已连接，开始聆听...");
                    iatClient.sendMessage(null, 0);
                }

                @Override
                public void onSuccess(WebSocket webSocket, IatResponse resp) {
                    if (resp.getCode() != 0) {
                        log.warn("code: {}, error: {}", resp.getCode(), resp.getMessage());
                        return;
                    }
                    if (resp.getData() != null && resp.getData().getResult() != null) {
                        IatResult result = resp.getData().getResult();
                        Text text = result.getText();
                        if ("rpl".equals(text.getPgs()) && text.getRg() != null && text.getRg().length == 2) {
                            int s = text.getRg()[0] - 1, e = text.getRg()[1] - 1;
                            for (int i = s; i <= e && i < resultSegments.size(); i++) {
                                resultSegments.get(i).setDeleted(true);
                            }
                        }
                        resultSegments.add(text);
                        StringBuilder sb = new StringBuilder();
                        for (Text t : resultSegments) {
                            if (t != null && !t.isDeleted()) sb.append(t.getText());
                        }
                        String current = sb.toString().trim();
                        if (!current.isEmpty()) System.out.print("\r识别中: " + current);
                    }
                    if (resp.getData() != null && resp.getData().getStatus() == 2) {
                        Date dateEnd = new Date();
                        StringBuilder sb = new StringBuilder();
                        for (Text t : resultSegments) {
                            if (t != null && !t.isDeleted()) sb.append(t.getText());
                        }
                        log.info("");
                        log.info("========================================");
                        log.info("  识别完成! 耗时: {}ms",
                                dateEnd.getTime() - dateBegin[0].getTime());
                        log.info("  最终结果: [{}]", sb.toString().trim());
                        log.info("========================================");
                        iatClient.closeWebsocket();
                    }
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable t, Response response) {
                    log.error(">>> WebSocket 连接失败", t);
                }
            });

            sender.start();
            log.info(">>> 录音中，按回车手动结束...");
            scanner.nextLine();

        } finally {
            sender.stop();
            log.info("--- 测试结束 ---");
        }
    }
}
