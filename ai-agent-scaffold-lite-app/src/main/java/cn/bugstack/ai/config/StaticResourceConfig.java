package cn.bugstack.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置 — 使 /audio/** 映射到音频输出目录 + CORS
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/audio/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}
