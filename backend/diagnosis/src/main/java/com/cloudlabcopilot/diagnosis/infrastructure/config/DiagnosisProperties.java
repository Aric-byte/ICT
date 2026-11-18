package com.cloudlabcopilot.diagnosis.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cloudlab.diagnosis")
public class DiagnosisProperties {

    private Retention retention = new Retention();
    private Stream stream = new Stream();
    private Provider provider = new Provider();
    private Vector vector = new Vector();

    @Data
    public static class Retention {
        private int days = 90;
    }

    @Data
    public static class Stream {
        private String version = "1.0";
        private int heartbeatMs = 15000;
        private int chunkMaxChars = 200;
        private int connectTimeoutMs = 2000;
    }

    @Data
    public static class Provider {
        private String type = "NONE";
        private String endpointUrl = "https://<待核实>";
        private String modelName = "<待核实>";
        private String authMode = "<待核实>";
        private String apiKey = "";
        private int syncTimeoutMs = 8000;
        private int streamTimeoutMs = 20000;
        private int maxRetries = 1;
        private String promptVersion = "diag-p1";
    }

    @Data
    public static class Vector {
        private boolean enabled = false;
        private int timeoutMs = 500;
    }
}