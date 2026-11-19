package com.cloudlabcopilot.retrieval.domain;

import com.cloudlabcopilot.retrieval.domain.model.RetrievalIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("检索意图抽取")
class RetrievalIntentExtractorTest {

    private final RetrievalIntentExtractor extractor = new RetrievalIntentExtractor();

    @Test
    @DisplayName("抽取对象名与技术栈")
    void extractObjectName() {
        RetrievalIntent intent = extractor.extract("relation \"t_user\" does not exist", "OPENGAUSS");
        assertThat(intent.techStack()).isEqualTo("OPENGAUSS");
        assertThat(intent.objectNames()).contains("t_user");
    }

    @Test
    @DisplayName("抽取错误码")
    void extractErrorCode() {
        RetrievalIntent intent = extractor.extract("ERROR 1045 Access denied", "MYSQL");
        assertThat(intent.errorCodes()).isNotEmpty();
    }

    @Test
    @DisplayName("关键词截断不超过 60 字符")
    void keywordTruncated() {
        String longQuestion = "a".repeat(200);
        RetrievalIntent intent = extractor.extract(longQuestion, "MYSQL");
        assertThat(intent.keywords().length()).isLessThanOrEqualTo(60);
    }
}