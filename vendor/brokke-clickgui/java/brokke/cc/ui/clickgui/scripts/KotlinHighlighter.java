/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.ui.clickgui.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class KotlinHighlighter {
    public static final int KEYWORD = -3174803;
    public static final int CONSTANT = -6785366;
    public static final int NUMBER = -9922629;
    public static final int STRING = -9786509;
    public static final int COMMENT = -8617844;
    public static final int ANNOTATION = -4475542;
    public static final int TYPE = -11613776;
    public static final int FUNCTION = -2302806;
    public static final int OPERATOR = -5656387;
    public static final int DEFAULT = -1972496;
    private static final Set<String> KEYWORDS = Set.of("package", "import", "class", "object", "interface", "fun", "val", "var", "typealias", "constructor", "init", "companion", "if", "else", "when", "for", "while", "do", "return", "break", "continue", "throw", "try", "catch", "finally", "public", "private", "protected", "internal", "override", "open", "abstract", "final", "data", "sealed", "enum", "suspend", "inline", "const", "lateinit", "by", "operator", "infix", "vararg", "is", "as", "in", "typeof", "set", "get");
    private static final Set<String> CONSTANTS = Set.of("true", "false", "null", "this", "super", "it");

    private KotlinHighlighter() {
    }

    public static List<Span> tokenize(String line) {
        ArrayList<Span> out = new ArrayList<Span>();
        if (line == null || line.isEmpty()) {
            return out;
        }
        int len = line.length();
        int i = 0;
        while (i < len) {
            char c = line.charAt(i);
            int start = i;
            if (Character.isWhitespace(c)) {
                while (i < len && Character.isWhitespace(line.charAt(i))) {
                    ++i;
                }
                continue;
            }
            if (c == '/' && i + 1 < len && line.charAt(i + 1) == '/') {
                out.add(new Span(i, len, -8617844));
                break;
            }
            if (c == '/' && i + 1 < len && line.charAt(i + 1) == '*') {
                int end = line.indexOf("*/", i + 2);
                i = end == -1 ? len : end + 2;
                out.add(new Span(start, i, -8617844));
                continue;
            }
            if (c == '\"' || c == '\'') {
                ++i;
                boolean escaped = false;
                while (i < len) {
                    char ch = line.charAt(i);
                    if (escaped) {
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == c) {
                        ++i;
                        break;
                    }
                    ++i;
                }
                out.add(new Span(start, i, -9786509));
                continue;
            }
            if (c == '@') {
                ++i;
                while (i < len && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_' || line.charAt(i) == '.')) {
                    ++i;
                }
                out.add(new Span(start, i, -4475542));
                continue;
            }
            if (Character.isDigit(c)) {
                if (c == '0' && i + 1 < len && (line.charAt(i + 1) == 'x' || line.charAt(i + 1) == 'X')) {
                    i += 2;
                    while (i < len && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                        ++i;
                    }
                } else {
                    char ch;
                    while (!(i >= len || (ch = line.charAt(i)) == '.' && i + 1 < len && line.charAt(i + 1) == '.' || !Character.isDigit(ch) && ch != '.' && ch != '_' && "eEfFdDlL".indexOf(ch) < 0)) {
                        ++i;
                    }
                }
                out.add(new Span(start, i, -9922629));
                continue;
            }
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int color;
                while (i < len && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_' || line.charAt(i) == '$')) {
                    ++i;
                }
                String word = line.substring(start, i);
                if (KEYWORDS.contains(word)) {
                    color = -3174803;
                } else if (CONSTANTS.contains(word) || KotlinHighlighter.isAllUpper(word)) {
                    color = -6785366;
                } else {
                    int peek;
                    for (peek = i; peek < len && line.charAt(peek) == ' '; ++peek) {
                    }
                    color = peek < len && (line.charAt(peek) == '(' || line.charAt(peek) == '{') ? -2302806 : (Character.isUpperCase(word.charAt(0)) ? -11613776 : -1972496);
                }
                out.add(new Span(start, i, color));
                continue;
            }
            out.add(new Span(start, ++i, -5656387));
        }
        return out;
    }

    private static boolean isAllUpper(String s) {
        if (s.length() <= 1) {
            return false;
        }
        boolean letter = false;
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (Character.isLetter(ch)) {
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
                letter = true;
                continue;
            }
            if (ch == '_' || Character.isDigit(ch)) continue;
            return false;
        }
        return letter;
    }

    public record Span(int start, int end, int color) {
    }
}

