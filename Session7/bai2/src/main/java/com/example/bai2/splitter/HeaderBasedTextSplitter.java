package com.example.bai2.splitter;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderBasedTextSplitter extends TextSplitter {

    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");

    private final Set<Integer> splitHeaderLevels;
    private final int minChunkSizeChars;

    public HeaderBasedTextSplitter(
            Set<Integer> splitHeaderLevels,
            int minChunkSizeChars) {

        if (splitHeaderLevels == null
                || splitHeaderLevels.isEmpty()) {

            throw new IllegalArgumentException(
                    "splitHeaderLevels must not be empty"
            );
        }

        if (minChunkSizeChars < 1) {

            throw new IllegalArgumentException(
                    "minChunkSizeChars must be greater than zero"
            );
        }

        this.splitHeaderLevels =
                Set.copyOf(splitHeaderLevels);

        this.minChunkSizeChars =
                minChunkSizeChars;
    }

    @Override
    protected List<String> splitText(String text) {

        List<String> chunks = new ArrayList<>();

        StringBuilder body =
                new StringBuilder();

        StringBuilder headingPath =
                new StringBuilder();

        String[] lines =
                text.split("\\R", -1);

        for (String line : lines) {

            Matcher matcher =
                    HEADER_PATTERN.matcher(line);

            if (matcher.matches()) {

                int level =
                        matcher.group(1).length();

                String title =
                        matcher.group(2).trim();

                /*
                 * Nếu gặp heading thuộc level
                 * được cấu hình thì kiểm tra
                 * chunk hiện tại.
                 */
                if (splitHeaderLevels.contains(level)
                        && body.toString()
                        .trim()
                        .length() >= minChunkSizeChars) {

                    chunks.add(
                            buildChunk(
                                    headingPath,
                                    body
                            )
                    );

                    body.setLength(0);
                }

                updateHeadingPath(
                        headingPath,
                        level,
                        title
                );

                body.append(headingPath)
                        .append("\n");

            } else {

                body.append(line)
                        .append("\n");
            }
        }

        /*
         * Chunk cuối cùng.
         */
        if (!body.toString().trim().isEmpty()) {

            chunks.add(
                    buildChunk(
                            headingPath,
                            body
                    )
            );
        }

        return chunks;
    }

    private String buildChunk(
            StringBuilder headingPath,
            StringBuilder body) {

        String content =
                body.toString().trim();

        if (!headingPath.isEmpty()
                && !content.startsWith(
                headingPath.toString().trim()
        )) {

            content =
                    headingPath
                            + "\n"
                            + content;
        }

        return content;
    }

    private void updateHeadingPath(
            StringBuilder path,
            int level,
            String title) {

        String[] existing =
                path.toString().split("\\R");

        List<String> headers =
                new ArrayList<>();

        for (String header : existing) {

            if (!header.isBlank()) {
                headers.add(header);
            }
        }

        /*
         * Xóa các heading cùng hoặc thấp hơn
         * cấp hiện tại.
         */
        while (headers.size() >= level) {

            headers.remove(
                    headers.size() - 1
            );
        }

        headers.add(
                "#".repeat(level)
                        + " "
                        + title
        );

        path.setLength(0);

        for (String header : headers) {

            path.append(header)
                    .append("\n");
        }
    }
}
