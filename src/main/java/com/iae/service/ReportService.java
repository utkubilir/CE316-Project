package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the evaluation outcome for a project to a self-contained HTML file
 * so the lecturer has a portable, shareable artifact (Requirement #9, PDF
 * scenario: "These reports must be saved within the project itself along
 * with any errors that the IAE encounters").
 */
public class ReportService {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void exportReport(Project project, String expectedOutput, File destination) throws IOException {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination cannot be null.");
        }
        String html = buildHtml(project, expectedOutput);
        Files.writeString(destination.toPath(), html);
    }

    String buildHtml(Project project, String expectedOutput) {
        List<StudentResult> results = project.getResults();
        Map<TestStatus, Integer> counts = countByStatus(results);
        Configuration cfg = project.getConfiguration();

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\"><head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>").append(escape(project.getName())).append(" — IAE Report</title>\n")
                .append("<style>\n")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; ")
                .append("margin: 24px; color: #1f2937; }\n")
                .append("h1 { margin-bottom: 4px; }\n")
                .append(".meta { color: #6b7280; font-size: 0.9em; margin-bottom: 24px; }\n")
                .append("section { margin-bottom: 28px; }\n")
                .append("h2 { border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; }\n")
                .append("table { border-collapse: collapse; width: 100%; }\n")
                .append("th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }\n")
                .append("th { background: #f9fafb; }\n")
                .append("pre { background: #f3f4f6; padding: 8px; margin: 0; border-radius: 4px; ")
                .append("white-space: pre-wrap; word-break: break-word; font-size: 0.85em; }\n")
                .append(".pill { display: inline-block; padding: 2px 10px; border-radius: 999px; ")
                .append("font-size: 0.85em; font-weight: 600; }\n")
                .append(".passed { background: #dcfce7; color: #14532d; }\n")
                .append(".failed, .mismatch { background: #fee2e2; color: #7f1d1d; }\n")
                .append(".compile { background: #fef3c7; color: #78350f; }\n")
                .append(".runtime { background: #fee2e2; color: #7f1d1d; }\n")
                .append(".timeout { background: #ede9fe; color: #4c1d95; }\n")
                .append(".missing, .extraction { background: #e5e7eb; color: #1f2937; }\n")
                .append(".pending { background: #e0f2fe; color: #075985; }\n")
                .append(".summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; }\n")
                .append(".summary-card { background: #f9fafb; padding: 12px; border-radius: 6px; }\n")
                .append(".summary-card .num { font-size: 1.6em; font-weight: 700; }\n")
                .append("</style>\n")
                .append("</head><body>\n");

        sb.append("<h1>").append(escape(project.getName())).append("</h1>\n");
        sb.append("<div class=\"meta\">Generated ").append(LocalDateTime.now().format(TIMESTAMP)).append("</div>\n");

        sb.append("<section><h2>Project</h2>\n<table>\n");
        appendRow(sb, "Project Name", project.getName());
        appendRow(sb, "Submission Folder", project.getSubmissionFolder());
        appendRow(sb, "Expected Output Path", project.getExpectedOutputPath());
        sb.append("</table></section>\n");

        if (cfg != null) {
            sb.append("<section><h2>Configuration</h2>\n<table>\n");
            appendRow(sb, "Name", cfg.getName());
            appendRow(sb, "Language", cfg.getLanguage());
            appendRow(sb, "Source File", cfg.getSourceFileName());
            appendRow(sb, "Compile Command", cfg.getCompileCommand());
            appendRow(sb, "Run Command", cfg.getRunCommand());
            appendRow(sb, "Run Args", formatRunArgs(cfg.getRunArgs()));
            sb.append("</table></section>\n");
        }

        sb.append("<section><h2>Summary</h2>\n<div class=\"summary-grid\">\n");
        int total = results == null ? 0 : results.size();
        appendSummaryCard(sb, "Total", total);
        for (TestStatus status : TestStatus.values()) {
            int n = counts.getOrDefault(status, 0);
            if (n > 0) {
                appendSummaryCard(sb, status.display(), n);
            }
        }
        sb.append("</div></section>\n");

        sb.append("<section><h2>Per-Student Results</h2>\n");
        if (total == 0) {
            sb.append("<p>No student submissions evaluated.</p>\n");
        } else {
            sb.append("<table>\n")
                    .append("<thead><tr><th>Student ID</th><th>Status</th><th>Details</th></tr></thead>\n<tbody>\n");
            for (StudentResult r : results) {
                sb.append("<tr><td>").append(escape(r.getStudentId())).append("</td>")
                        .append("<td><span class=\"pill ").append(pillClass(r.getStatus())).append("\">")
                        .append(escape(r.getStatus().display())).append("</span></td>")
                        .append("<td><pre>").append(escape(emptyToDash(r.getDetails()))).append("</pre></td></tr>\n");
            }
            sb.append("</tbody></table>\n");
        }
        sb.append("</section>\n");

        if (expectedOutput != null && !expectedOutput.isBlank()) {
            sb.append("<section><h2>Expected Output</h2>\n<pre>")
                    .append(escape(expectedOutput))
                    .append("</pre></section>\n");
        }

        sb.append("</body></html>\n");
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String key, String value) {
        sb.append("<tr><th>").append(escape(key)).append("</th><td>")
                .append(escape(value == null || value.isBlank() ? "—" : value))
                .append("</td></tr>\n");
    }

    private static void appendSummaryCard(StringBuilder sb, String label, int count) {
        sb.append("<div class=\"summary-card\"><div class=\"num\">")
                .append(count).append("</div><div>").append(escape(label)).append("</div></div>\n");
    }

    private static Map<TestStatus, Integer> countByStatus(List<StudentResult> results) {
        Map<TestStatus, Integer> counts = new EnumMap<>(TestStatus.class);
        if (results != null) {
            for (StudentResult r : results) {
                if (r.getStatus() != null) {
                    counts.merge(r.getStatus(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static String pillClass(TestStatus status) {
        if (status == null) return "pending";
        return switch (status) {
            case PASSED -> "passed";
            case FAILED -> "failed";
            case COMPILATION_ERROR -> "compile";
            case RUNTIME_ERROR -> "runtime";
            case TIMEOUT -> "timeout";
            case OUTPUT_MISMATCH -> "mismatch";
            case MISSING_SOURCE -> "missing";
            case EXTRACTION_ERROR -> "extraction";
            case PENDING -> "pending";
        };
    }

    private static String formatRunArgs(List<String> args) {
        if (args == null || args.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(' ');
            String arg = args.get(i);
            if (arg == null) continue;
            if (arg.contains(" ")) {
                sb.append('"').append(arg.replace("\"", "\\\"")).append('"');
            } else {
                sb.append(arg);
            }
        }
        return sb.toString();
    }

    private static String emptyToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
