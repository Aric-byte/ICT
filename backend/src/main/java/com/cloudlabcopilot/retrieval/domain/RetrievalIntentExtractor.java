package com.cloudlabcopilot.retrieval.domain;

import com.cloudlabcopilot.retrieval.domain.model.RetrievalIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RetrievalIntentExtractor {

    private static final Pattern OBJECT_NAME = Pattern.compile(
            "(?i)(?:table|relation|column)\\s+['\"`]?([A-Za-z_][A-Za-z0-9_]*)['\"`]?");
    private static final Pattern ERROR_CODE = Pattern.compile(
            "(?i)(ERROR\\s+\\d{4,5}|ORA-\\d{5}|errno\\s+\\d+)");

    public RetrievalIntent extract(String question, String techStack) {
        String text = question == null ? "" : question;
        List<String> objects = new ArrayList<>();
        Matcher objMatcher = OBJECT_NAME.matcher(text);
        while (objMatcher.find()) {
            objects.add(objMatcher.group(1));
        }
        List<String> codes = new ArrayList<>();
        Matcher codeMatcher = ERROR_CODE.matcher(text);
        while (codeMatcher.find()) {
            codes.add(codeMatcher.group(1));
        }
        String keywords = text.length() > 60 ? text.substring(0, 60) : text;
        return new RetrievalIntent(techStack, codes, objects, keywords);
    }
}