package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    // pravi odgovarajuce tokene na osnovu koda iz fajla

    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("redstone", TokenType.BOOLEAN),
            Map.entry("gold", TokenType.INT),
            Map.entry("diamond", TokenType.DOUBLE),
            Map.entry("emerald", TokenType.LONG),
            Map.entry("iron", TokenType.CHAR),
            Map.entry("chain", TokenType.STRING),
            Map.entry("chest", TokenType.ARRAY),

            // kontrola / struktura
            Map.entry("dig", TokenType.IF),
            Map.entry("loot", TokenType.RETURN),
            Map.entry("mine", TokenType.FUNCTION),
            Map.entry("deeper", TokenType.ELSEIF),
            Map.entry("bedrock", TokenType.ELSE),
            Map.entry("craft", TokenType.FOR),
            Map.entry("build", TokenType.WHILE),
            Map.entry("do", TokenType.DO),

            // strukture
            Map.entry("fortress", TokenType.CLASS),
            Map.entry("extends", TokenType.EXTENDS),
            Map.entry("drop", TokenType.SCAN),
            Map.entry("collect", TokenType.PRINT),

            // logičke konstante i reč-op.:
            Map.entry("powered", TokenType.TRUE),
            Map.entry("unpowered", TokenType.FALSE),
            Map.entry("and", TokenType.AND),
            Map.entry("or", TokenType.OR)
    );

    public Lexer(String source) {
        this.source = source;
        this.sc = new ScannerCore(source);
    }

    public List<Token> scanTokens() {
        // obradjuje sve tokene i na kraju dodaje EOF
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
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case ',' -> add(TokenType.COMMA);
            case '#' -> add(TokenType.ASSIGN);
            case ':' -> add(TokenType.SEPARATOR);
            case '*' -> add(TokenType.MULTIPLY);
            case '/' -> add(TokenType.DIVIDE);
            case '%' -> add(TokenType.PERCENT);
            case '=' -> add(TokenType.EQ);
            case '?' -> add(TokenType.TQUESTION);
            case ';' -> add(TokenType.TSEMICOLON);
            case '^' -> add(TokenType.CARET);

            case '+' -> {
                if (sc.match('+')) {
                    add(TokenType.INC);
                } else {
                    add(TokenType.ADD);
                }
            }
            case '-' -> {
                if (sc.match('-')) {
                    add(TokenType.DEC);
                } else {
                    add(TokenType.SUBTRACT);
                }
            }
            case '!' -> {
                if (sc.match('=')) add(TokenType.NEQ);
                else add(TokenType.NOT);
            }
            case '<' -> {
                if (sc.match('<')) add(TokenType.BIT_LSHIFT);
                else add(sc.match('=') ? TokenType.LE : TokenType.LT);
            }
            case '>' -> {
                if (sc.match('>')) add(TokenType.BIT_RSHIFT);
                else add(sc.match('=') ? TokenType.GE : TokenType.GT);
            }
            case '&' -> add(TokenType.BIT_AND);
            case '|' -> add(TokenType.BIT_OR);
            case '\'' -> { // char literal
                char value = sc.advance(); // uzmi sledeci karakter
                if (!sc.match('\'')) throw error("Quotes for char literal not closed");
                addLiteralChar(value);
            }
            case '\"' -> { // string literal
                while (sc.peek() != '\"' && !sc.isAtEnd()) {
                    if (sc.peek() == '\n') throw error("Quotes for string literal not closed");
                    sc.advance();
                }
                if (sc.isAtEnd()) throw error("Quotes for string literal not closed");
                sc.advance();
                String text = source.substring(sc.getStartIdx() + 1, sc.getCur() - 1); // cuva bez navodnika
                addLiteralString(text);
            }

            case ' ', '\r', '\t', '\n' -> {}
            default -> {
                // ako karakter nije nista od ovoga, onda gledamo da li je:
                if (Character.isDigit(c)) number(); // broj, ako jeste zovemo funkciju number
                else if (isIdentStart(c)) identifier(); // ako ne onda zovemo identifier
                else throw error("Unexpected character"); // greska
            }
        }
    }

    private void number() {
        // dodat support za double

        while (Character.isDigit(sc.peek())) sc.advance();

        // double
        if (sc.peek() == '.' && Character.isDigit(sc.peekNext())) {
            sc.advance(); // preskace tacku
            while (Character.isDigit(sc.peek())) sc.advance();
            String text = source.substring(sc.getStartIdx(), sc.getCur());
            addLiteralDouble(text);
            return;
        }

        // int
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("error: character in int literal");
        }
        addLiteralInt(text);
    }


    private void identifier() {
        // naziv promenljive krece ili slovom ili _, ne sme brojem!!!
        while (isIdentPart(sc.peek())){
            // trazimo najduzu mogucu leksemu
            sc.advance();
        }
        String text = source.substring(sc.getStartIdx(), sc.getCur()); // uzmemo taj podstring
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFICATOR); // vraca da li je identifier tip tokena
        add(type, text); // dodavanje lekseme
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentPart(char c)  {
        return isIdentStart(c) || Character.isDigit(c);
    }

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

    private void addLiteralDouble(String literal) {
        tokens.add(new Token(TokenType.DOUBLE_LIT, literal, Double.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralLong(String literal) {
        tokens.add(new Token(TokenType.LONG_LIT, literal, Long.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralChar(char c) {
        tokens.add(new Token(TokenType.CHAR_LIT, "'" + c + "'", c,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralString(String text) {
        tokens.add(new Token(TokenType.STRING_LIT, "\"" + text + "\"", text,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg) {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}
