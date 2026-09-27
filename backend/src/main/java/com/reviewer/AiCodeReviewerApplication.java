package com.reviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class AiCodeReviewerApplication {

    private static final Logger log = LoggerFactory.getLogger(AiCodeReviewerApplication.class);

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(AiCodeReviewerApplication.class, args);
        log.info("AI Code Reviewer Application with Persistent Team Memory started successfully!");
    }

    private static void loadDotEnvIfPresent() {
        // Look in current directory and parent directory for .env
        File[] candidates = new File[]{
                new File(".env"),
                new File("../.env")
        };

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                log.info("Loading environment variables from: {}", file.getAbsolutePath());
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, eqIdx).trim();
                        String value = trimmed.substring(eqIdx + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not read .env file: {}", e.getMessage());
                }
                break;
            }
        }
    }
}
