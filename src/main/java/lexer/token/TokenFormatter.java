package lexer.token;

import java.util.List;

public final class TokenFormatter {
    // vrsi formatiranje tokena, kreiranje objekata i predstavljanje izlazne tabele u konzoli

    private static String escape(String s) {
        return s.replace("\n", "\\n").replace("\0", "\\0");
    }

    private static String center(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int pad = width - s.length();
        int left = pad / 2;
        int right = pad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    public static String format(Token t) {
        String typeStr = center(String.valueOf(t.type), 20);
        String lexStr = center("'" + escape(t.lexeme) + "'", 30);
        String litStr = center(t.literal == null ? " " : t.literal.toString(), 30);
        String lineStr = center("line " + t.line, 9);
        String colStr = center("col " + t.colStart + "-" + t.colEnd, 12);

        return String.format("|%s|%s|%s|%s|%s|", typeStr, lexStr, litStr, lineStr, colStr);
    }

    public static String formatList(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();

        String header = String.format(
            "|%s|%s|%s|%s|%s|",
            center("TYPE", 20),
            center("LEXEME", 30),
            center("LITERAL", 30),
            center("LINE", 9),
            center("COLUMNS", 12)
        );

        String separator = "-".repeat(header.length());

        sb.append(separator).append("\n");
        sb.append(header).append("\n");
        sb.append(separator).append("\n");

        for (Token t : tokens) {
            sb.append(format(t)).append("\n");
        }

        sb.append(separator);
        return sb.toString();
    }
}
