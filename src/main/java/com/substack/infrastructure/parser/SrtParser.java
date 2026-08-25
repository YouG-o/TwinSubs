package com.substack.infrastructure.parser;

import com.substack.domain.model.SubtitleEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for standard SRT subtitle streams and files.
 */
public final class SrtParser {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
        "(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})"
    );

    /**
     * Parses raw SRT text content into a list of domain SubtitleEntry models.
     */
    public List<SubtitleEntry> parse(String srtContent) {
        Objects.requireNonNull(srtContent, "SRT content cannot be null");

        // Clean UTF-8 BOM and normalize line endings
        String cleanContent = srtContent.replace("\uFEFF", "").replace("\r\n", "\n");
        String[] blocks = cleanContent.split("\n\\s*\n");

        List<SubtitleEntry> entries = new ArrayList<>();

        for (String block : blocks) {
            parseBlock(block).ifPresent(entries::add);
        }

        return entries;
    }

    private java.util.Optional<SubtitleEntry> parseBlock(String block) {
        String[] lines = block.trim().split("\n");
        if (lines.length < 2) {
            return java.util.Optional.empty();
        }

        // Search line matching timestamp pattern (usually line 1 or 2)
        Matcher matcher = null;
        int textStartIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            Matcher m = TIMESTAMP_PATTERN.matcher(lines[i]);
            if (m.find()) {
                matcher = m;
                textStartIndex = i + 1;
                break;
            }
        }

        if (matcher == null || textStartIndex >= lines.length) {
            return java.util.Optional.empty();
        }

        long startMs = parseTimestampToMs(
            matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)
        );
        long endMs = parseTimestampToMs(
            matcher.group(5), matcher.group(6), matcher.group(7), matcher.group(8)
        );

        StringBuilder textBuilder = new StringBuilder();
        for (int i = textStartIndex; i < lines.length; i++) {
            if (textBuilder.length() > 0) {
                textBuilder.append("\n");
            }
            textBuilder.append(lines[i].trim());
        }

        String text = textBuilder.toString();
        if (text.isBlank()) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new SubtitleEntry(startMs, endMs, text));
    }

    private long parseTimestampToMs(String hours, String minutes, String seconds, String millis) {
        long h = Long.parseLong(hours);
        long m = Long.parseLong(minutes);
        long s = Long.parseLong(seconds);
        long ms = Long.parseLong(millis);

        return (h * 3600000L) + (m * 60000L) + (s * 1000L) + ms;
    }
}