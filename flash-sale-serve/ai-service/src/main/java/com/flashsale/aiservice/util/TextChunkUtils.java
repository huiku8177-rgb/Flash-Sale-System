package com.flashsale.aiservice.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunkUtils {

    private static final int DEFAULT_CHUNK_SIZE = 220;
    private static final int DEFAULT_OVERLAP = 40;

    private TextChunkUtils() {
    }

    public static List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (text.length() <= chunkSize) {
            return List.of(text.trim());
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }
}
