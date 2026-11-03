package com.humolix.volur.competition.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtil {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvExportUtil() {
    }

    public record CsvSection(String title, List<String> headers, List<List<String>> rows) {}

    public static void writeCsv(OutputStream os, List<String> headers, List<List<String>> rows) throws IOException {
        writeSections(os, List.of(new CsvSection(null, headers, rows)));
    }

    public static void writeMultiSectionCsv(OutputStream os,
                                           String title1, List<String> headers1, List<List<String>> rows1,
                                           String title2, List<String> headers2, List<List<String>> rows2) throws IOException {
        if (headers2 != null) {
            writeSections(os, List.of(
                    new CsvSection(title1, headers1, rows1),
                    new CsvSection(title2, headers2, rows2)
            ));
        } else {
            writeSections(os, List.of(new CsvSection(title1, headers1, rows1)));
        }
    }

    public static void writeSections(OutputStream os, List<CsvSection> sections) throws IOException {
        os.write(UTF8_BOM);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            boolean first = true;
            if (sections != null) {
                for (CsvSection sec : sections) {
                    if (sec == null || sec.headers() == null) continue;
                    if (!first) {
                        writer.newLine();
                    }
                    first = false;
                    if (sec.title() != null && !sec.title().isBlank()) {
                        writer.write(escapeCell(sec.title()));
                        writer.newLine();
                    }
                    writeRow(writer, sec.headers());
                    if (sec.rows() != null) {
                        for (List<String> row : sec.rows()) {
                            writeRow(writer, row);
                        }
                    }
                }
            }
            writer.flush();
        }
    }

    private static void writeRow(BufferedWriter writer, List<String> row) throws IOException {
        if (row == null) return;
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCell(row.get(i)));
        }
        writer.newLine();
    }

    public static String escapeCell(String raw) {
        if (raw == null) {
            return "";
        }
        String val = raw;
        if (!val.isEmpty()) {
            char first = val.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                val = "'" + val;
            }
        }
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            val = "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

}
