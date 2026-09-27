package com.reviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Groq groq = new Groq();
    private Hindsight hindsight = new Hindsight();
    private Cors cors = new Cors();

    public static class Groq {
        private String apiKey = "";
        private String model = "openai/gpt-oss-120b";
        private String fallbackModel = "qwen/qwen3-32b";
        private String baseUrl = "https://api.groq.com/openai/v1";
        private int maxRetries = 2;
        private int timeoutMs = 30000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getFallbackModel() { return fallbackModel; }
        public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Hindsight {
        private String apiKey = "";
        private String baseUrl = "https://api.hindsight.vectorize.io";
        private int timeoutMs = 15000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173";

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public Groq getGroq() { return groq; }
    public void setGroq(Groq groq) { this.groq = groq; }

    public Hindsight getHindsight() { return hindsight; }
    public void setHindsight(Hindsight hindsight) { this.hindsight = hindsight; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
}
