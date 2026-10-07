package web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Small helpers for the web UI: HTML escaping, page layout, form parsing and
 * capturing the messages the existing services print to the console.
 */
final class Html {

    private Html() {
    }

    /** Escapes text so user data can never break the page or inject HTML. */
    static String esc(Object value) {
        if (value == null) return "";
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Parses an application/x-www-form-urlencoded body or query string. */
    static Map<String, String> parseForm(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String val = eq >= 0 ? pair.substring(eq + 1) : "";
            out.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(val, StandardCharsets.UTF_8));
        }
        return out;
    }

    static String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    static int toInt(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static double toDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Runs a service call and returns what it printed. The existing services report success and
     * errors on System.out; capturing them lets the web pages show exactly the same messages.
     * The web server handles one request at a time, so swapping System.out here is safe.
     */
    static String capture(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        String text = buffer.toString(StandardCharsets.UTF_8).trim();
        if (!text.isEmpty()) original.println(text); // keep the server console log
        return text;
    }

    /** Last non-empty line of captured output, which is the service's result message. */
    static String lastLine(String text) {
        String[] lines = text.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank() && !lines[i].trim().startsWith("|") && !lines[i].trim().startsWith("=")) {
                return lines[i].trim();
            }
        }
        return text.trim();
    }

    static String page(String title, String userLine, String flash, boolean flashOk, String body) {
        String flashHtml = (flash == null || flash.isBlank()) ? ""
                : "<div class=\"flash " + (flashOk ? "ok" : "err") + "\">" + esc(flash) + "</div>";
        String user = (userLine == null) ? ""
                : "<div class=\"user\">" + esc(userLine) + " &nbsp;<a href=\"/logout\">Log out</a></div>";
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + esc(title) + " · TalentFlow</title><style>" + CSS + "</style></head><body>"
                + "<header><div class=\"brand\">TalentFlow <span>Recruitment &amp; Applicant Tracking</span></div>" + user + "</header>"
                + "<main>" + flashHtml + body + "</main>"
                + "<footer>TalentFlow · Core Java + JDBC + MySQL · running on localhost</footer></body></html>";
    }

    static final String CSS = """
            :root{--navy:#0b2e59;--navy2:#123f73;--bg:#f3f7fb;--card:#fff;--line:#d5e3f0;--teal:#0f8fa1;--text:#1d2733;--muted:#5a6878;--ok:#1e8a4c;--err:#b3261e}
            *{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Arial,sans-serif;background:var(--bg);color:var(--text);font-size:15px}
            header{background:var(--navy);color:#fff;padding:14px 24px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px}
            .brand{font-weight:700;font-size:20px}.brand span{font-weight:400;font-size:13px;opacity:.8;margin-left:8px}
            .user a{color:#9fd8ff}main{max-width:1100px;margin:22px auto;padding:0 16px}
            h1{font-size:22px;color:var(--navy);margin:4px 0 14px}h2{font-size:17px;color:var(--navy2);margin:0 0 10px}
            .card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:16px 18px;margin-bottom:16px}
            .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px}
            table{width:100%;border-collapse:collapse;font-size:14px}th{background:#e6eff8;color:var(--navy);text-align:left;padding:7px 8px}
            td{padding:7px 8px;border-top:1px solid var(--line);vertical-align:middle}
            .pct{font-weight:700}.hi{color:var(--ok)}.mid{color:#b26b00}.lo{color:var(--err)}
            .bar{height:8px;background:#e1ebf4;border-radius:4px;overflow:hidden;min-width:70px}.bar i{display:block;height:100%;background:var(--teal)}
            input,select{padding:7px 9px;border:1px solid #b9cbdc;border-radius:6px;font-size:14px;font-family:inherit}
            label{display:block;font-size:13px;color:var(--muted);margin:8px 0 3px}
            button,.btn{background:var(--navy2);color:#fff;border:0;border-radius:6px;padding:8px 14px;font-size:14px;cursor:pointer;text-decoration:none;display:inline-block}
            button.secondary,.btn.secondary{background:#e6eff8;color:var(--navy)}button.danger{background:var(--err)}
            .inline{display:inline-flex;gap:6px;align-items:center;margin:0}.row{display:flex;gap:10px;flex-wrap:wrap;align-items:end}
            .flash{padding:10px 14px;border-radius:8px;margin-bottom:14px;white-space:pre-line}.flash.ok{background:#e3f4ea;color:var(--ok)}.flash.err{background:#fbe7e6;color:var(--err)}
            .muted{color:var(--muted);font-size:13px}.tag{background:#e6eff8;color:var(--navy);border-radius:10px;padding:2px 8px;font-size:12px}
            .login{max-width:380px;margin:40px auto}.login input,.login select{width:100%}
            footer{text-align:center;color:var(--muted);font-size:12px;padding:20px}
            """;
}
