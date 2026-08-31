package com.chaekchaek.book.client;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AladinContributorParser {

    private static final Pattern ROLE_GROUP_END = Pattern.compile(
            "(?:\\s+\\(([^()]*)\\)|\\s*\\((지은이|옮긴이)\\))\\s*(?:,\\s*|$)"
    );

    private AladinContributorParser() {
    }

    public static Contributors parse(String source) {
        if (source == null || source.isBlank()) {
            return new Contributors(List.of(), List.of());
        }

        List<String> authors = new ArrayList<>();
        List<String> translators = new ArrayList<>();
        Matcher matcher = ROLE_GROUP_END.matcher(source);
        int groupStart = 0;
        boolean hasRole = false;

        while (matcher.find()) {
            hasRole = true;
            String role = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String names = source.substring(groupStart, matcher.start()).trim();
            switch (role) {
                case "지은이" -> addNames(authors, names);
                case "옮긴이" -> addNames(translators, names);
                default -> {
                }
            }
            groupStart = matcher.end();
        }
        if (!hasRole) {
            addNames(authors, source);
        }
        return new Contributors(authors, translators);
    }

    private static void addNames(List<String> target, String names) {
        int nameStart = 0;
        int parenthesesDepth = 0;

        for (int index = 0; index < names.length(); index++) {
            char character = names.charAt(index);
            if (character == '(') {
                parenthesesDepth++;
            }
            if (character == ')' && parenthesesDepth > 0) {
                parenthesesDepth--;
            }
            if (character == ',' && parenthesesDepth == 0) {
                addName(target, names.substring(nameStart, index));
                nameStart = index + 1;
            }
        }
        addName(target, names.substring(nameStart));
    }

    private static void addName(List<String> target, String name) {
        String trimmed = name.trim();
        if (!trimmed.isBlank()) {
            target.add(trimmed);
        }
    }

    public record Contributors(
            List<String> authors,
            List<String> translators
    ) {
        public Contributors {
            authors = List.copyOf(authors);
            translators = List.copyOf(translators);
        }
    }
}
