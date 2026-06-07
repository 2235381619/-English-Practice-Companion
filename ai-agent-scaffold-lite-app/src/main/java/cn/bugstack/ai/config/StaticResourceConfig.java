package cn.bugstack.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置 — 使 /audio/** 映射到音频输出目录
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${audio.output.dir:./audio}")
    private String audioOutputDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String audioPath = audioOutputDir.endsWith("/") ? audioOutputDir : audioOutputDir + "/";
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file:" + audioPath);
    }
}
