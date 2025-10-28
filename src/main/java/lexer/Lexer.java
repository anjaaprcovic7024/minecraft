package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("redstone", TokenType.REDSTONE),
            Map.entry("gold", TokenType.GOLD),
            Map.entry("diamond", TokenType.DIAMOND),
            Map.entry("emerald", TokenType.EMERALD),
            Map.entry("iron", TokenType.IRON),
            Map.entry("chain", TokenType.CHAIN),
            Map.entry("chest", TokenType.CHEST),

            // kontrola / struktura
            Map.entry("dig", TokenType.DIG),
            Map.entry("loot", TokenType.LOOT),
            Map.entry("mine", TokenType.MINE),
            Map.entry("deeper", TokenType.DEEPER),
            Map.entry("bedrock", TokenType.BEDROCK),
            Map.entry("craft", TokenType.CRAFT),
            Map.entry("build", TokenType.BUILD),
            Map.entry("do", TokenType.DO),
            Map.entry("fortress", TokenType.FORTRESS),
            Map.entry("extends", TokenType.EXTENDS),
            Map.entry("drop", TokenType.DROP),
            Map.entry("collect", TokenType.COLLECT),

            // logičke konstante i reč-op.:
            Map.entry("powered", TokenType.POWERED),
            Map.entry("unpowered", TokenType.UNPOWERED),
            Map.entry("and", TokenType.AND),
            Map.entry("or", TokenType.OR)
    );

    public Lexer(String source) {
        this.source = source;
        this.sc = new ScannerCore(source);
    }

    public List<Token> scanTokens() {
        while (!sc.isAtEnd()) {
            sc.beginToken();
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "\0", null, sc.getLine(), sc.getCol(), sc.getCol()));
        return tokens;
    }

    private void scanToken() {
        char c = sc.advance();

        switch (c) {
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case ',' -> add(TokenType.SEPARATOR_COMMA);
            case '#' -> add(TokenType.ASSIGN);
            case ':' -> add(TokenType.COLON);
            case '+' -> add(TokenType.ADD);
            case '-' -> add(TokenType.SUBTRACT);
            case '*' -> add(TokenType.MULTIPLY);
            case '/' -> add(TokenType.DIVIDE);
            case '%' -> add(TokenType.PERCENT);
            case '=' -> add(TokenType.EQ);
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case '?' -> add(TokenType.QUESTION);
            case ';' -> add(TokenType.SEMICOLON);
            case '!' -> {
                if (sc.match('=')) add(TokenType.NEQ);
                else add(TokenType.NOT);
            }
            case '<' -> {
                if (sc.match('<')) add(TokenType.LSHIFT);
                else add(sc.match('=') ? TokenType.LE : TokenType.LT);
            }
            case '>' -> {
                if (sc.match('>')) add(TokenType.RSHIFT);
                else add(sc.match('=') ? TokenType.GE : TokenType.GT);
            }
            case '&' -> add(TokenType.BIT_AND);
            case '|' -> add(TokenType.BIT_OR);
            case '\n' -> tokens.add(new Token(
                    TokenType.NEWLINE, "\n", null, sc.getStartLine(), sc.getStartCol(), sc.getStartCol()
            ));
            case ' ', '\r', '\t' -> {}
            default -> {
                if (Character.isDigit(c)) number();
                else if (isIdentStart(c)) identifier();
                else throw error("Unexpected character");
            }
        }
    }

    private void number() {
        while (Character.isDigit(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("Error: Character in int literal");
        }
        addLiteralInt(text);
    }

    private void identifier() {
        while (isIdentPart(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        add(type, text);
    }

    private boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isIdentPart(char c)  { return isIdentStart(c) || Character.isDigit(c); }

    private void add(TokenType type) {
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        tokens.add(new Token(type, lex, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralInt(String literal) {
        tokens.add(new Token(TokenType.INT_LIT, literal, Integer.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg) {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}
