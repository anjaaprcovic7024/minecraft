package parser;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private static class ParseError extends RuntimeException {
        ParseError(String msg) { super(msg); }
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /// NJIHOV DIO KODA KOJI NE ZNAM DA LI SMIJEM IZBRISAT
    /* public Expr parse() {
        Expr expr = expression();
        if (!isAtEnd()) {
            throw error(peek(), "Unexpected input after complete expression.");
        }
        return expr;
    }

    */

    ///JA DODALA:
    public Program parseProgram() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }
        return new Program(statements);
    }

    // ======== STATEMENTS ========
    // JA DODALA:
    private Stmt statement() {
        if (match(TokenType.IF))    return ifStatement();     // dig
        if (match(TokenType.WHILE)) return whileStatement();  // build
        if (match(TokenType.FOR))   return forStatement();    // craft
        if (match(TokenType.PRINT)) return printStatement();  // collect
        if (match(TokenType.SCAN))  return scanStatement();   // drop

        // deklaracije tipova: gold/diamond/redstone/emerald/iron/chain/chest
        if (check(TokenType.INT) || check(TokenType.DOUBLE) ||
                check(TokenType.BOOLEAN) || check(TokenType.LONG) ||
                check(TokenType.CHAR) || check(TokenType.STRING) ||
                check(TokenType.ARRAY)) {
            return varDeclStatement();
        }

        // sve ostalo: dodjela ili goli izrazi
        return exprOrAssignStatement();
    }

    // JA DODALA:
    private Stmt varDeclStatement() {
        Token typeTok = advance(); // gold / diamond / redstone...

        int rank = 0;
        while (match(TokenType.LBRACKET)) {
            consume(TokenType.RBRACKET, "Expect ']' in array type.");
            rank++;
        }

        List<Token> names = new ArrayList<>();
        names.add(consume(TokenType.IDENTIFICATOR, "Expect variable name."));
        while (match(TokenType.COMMA)) {
            names.add(consume(TokenType.IDENTIFICATOR, "Expect variable name."));
        }

        consume(TokenType.SEPARATOR, "Expect ':' after variable declaration.");
        return new Stmt.VarDecl(typeTok, rank, names);
    }

    // JA DODALA:
    // ili: x # expr:
    // ili: a + b * c:
    private Stmt exprOrAssignStatement() {
        Expr left = expression();  // koristi tvoj postojeći expression()

        if (match(TokenType.ASSIGN)) { // '#'
            Token op = previous();
            Expr value = expression();
            // <<< OVDJE JE PROMJENA >>>
            consume(TokenType.SEPARATOR, "Expect ':' after assignment.");
            return new Stmt.Assign(left, op, value);
        }

        // gola naredba izraza
        // <<< OVDJE JE PROMJENA >>>
        consume(TokenType.SEPARATOR, "Expect ':' after expression.");
        return new Stmt.ExprStmt(left);
    }

    // collect(expr): JA DODALA
    private Stmt printStatement() {
        Token kw = previous(); // PRINT (collect)
        consume(TokenType.LPAREN, "Expect '(' after collect.");
        Expr expr = expression();
        consume(TokenType.RPAREN, "Expect ')' after expression.");
        consume(TokenType.SEPARATOR, "Expect ':' after collect(...).");
        return new Stmt.Print(kw, expr);
    }

    // drop(x): JA DODALA
    private Stmt scanStatement() {
        Token kw = previous(); // SCAN (drop)
        consume(TokenType.LPAREN, "Expect '(' after drop.");
        Expr target = expression(); // za sad dozvoljavamo bilo koji izraz
        consume(TokenType.RPAREN, "Expect ')' after argument.");
        consume(TokenType.SEPARATOR, "Expect ':' after drop(...).");
        return new Stmt.Scan(kw, target);
    }

    private Stmt ifStatement() {
        // već si pojela IF (dig)
        consume(TokenType.LPAREN, "Expect '(' after dig.");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition.");

        consume(TokenType.LBRACE, "Expect '{' to start if block.");
        List<Stmt> thenBranch = block();
        Stmt.If.Arm ifArm = new Stmt.If.Arm(cond, thenBranch);

        List<Stmt.If.Arm> elseIfArms = new ArrayList<>();
        while (match(TokenType.ELSEIF)) { // deeper
            consume(TokenType.LPAREN, "Expect '(' after deeper.");
            Expr c = expression();
            consume(TokenType.RPAREN, "Expect ')' after condition.");
            consume(TokenType.LBRACE, "Expect '{' to start elif block.");
            List<Stmt> b = block();
            elseIfArms.add(new Stmt.If.Arm(c, b));
        }

        List<Stmt> elseBranch = null;
        if (match(TokenType.ELSE)) { // bedrock
            consume(TokenType.LBRACE, "Expect '{' to start else block.");
            elseBranch = block();
        }

        return new Stmt.If(ifArm, elseIfArms, elseBranch);
    }

    private Stmt whileStatement() {
        Token kw = previous(); // WHILE (build)
        consume(TokenType.LPAREN, "Expect '(' after build.");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition.");
        consume(TokenType.LBRACE, "Expect '{' to start while block.");
        List<Stmt> body = block();
        return new Stmt.While(kw, cond, body);
    }

    private Stmt forStatement() {
        Token kw = previous(); // FOR (craft)
        consume(TokenType.LPAREN, "Expect '(' after craft.");
        Token var = consume(TokenType.IDENTIFICATOR, "Expect loop variable.");
        // Ovdje dodamo šta tacno hocemo za sintaksu opsega (goes from / to / itd.)
        // Za skeleton: pretpostavimo: var # from; var < to; ++var; je već u tijelu;

        // Za sada: craft(i, from, to) { ... }
        consume(TokenType.COMMA, "Expect ',' after loop variable.");
        Expr from = expression();
        consume(TokenType.COMMA, "Expect ',' after 'from' expr.");
        Expr to = expression();
        consume(TokenType.RPAREN, "Expect ')' after for header.");
        consume(TokenType.LBRACE, "Expect '{' to start for body.");
        List<Stmt> body = block();
        return new Stmt.For(kw, var, from, to, body);
    }

    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(statement());
        }
        consume(TokenType.RBRACE, "Expect '}' after block.");
        return stmts;
    }

    /// ===============NJIHOV DIO I KOMENTARI KOJI SU VEC BILI OVDJE:
    /*
    expression -> add ;
    add -> mul ( ( "+" | "-" ) mul )* ;
    mul -> unary ( ( "*" | "/" ) unary )* ;
    unary -> ( "+" | "-" )? power ;
    power -> primary ( "^" power )? ;
    primary -> NUMBER | "(" expression ")" ;
     */

    // Za vežbu zamenite jezik i program da ne parsira 2*-5, ali parsira 2^-5

    // expression -> add ;
    private Expr expression() { return add(); }

    // add -> mul ( ( "+" | "-" ) mul )* ;
    private Expr add() {
        Expr expr = mul();
        while (match(TokenType.ADD, TokenType.SUBTRACT)) {
            Token op = previous();
            Expr right = mul();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // mul -> unary ( ( "*" | "/" ) unary )* ;
    private Expr mul() {
        Expr expr = unary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // unary -> ( "+" | "-" )? power ;
    private Expr unary() {
        if (match(TokenType.ADD, TokenType.SUBTRACT)) {
            Token op = previous();
            Expr right = power();
            return new Expr.Unary(op, right);
        }
        // return new Expr.Unary(new Token(TokenType.PLUS, "+", null, -1), power());
        return power(); // fake + not needed
        // ex: -2^4 = -16
    }

    // power -> primary ( "^" power )? ;
    private Expr power() {
        Expr left = primary();

        if (match(TokenType.CARET)) {
            Token op = previous();
            Expr right = power(); // right associativity
            return new Expr.Binary(left, op, right);
        }
        return left;
        // ex: (-2)^4 = 16
    }

    // primary -> NUMBER | "(" expression ")" ;
    private Expr primary() {
        if (match(TokenType.INT_LIT, TokenType.DOUBLE_LIT, TokenType.LONG_LIT,
                TokenType.CHAR_LIT, TokenType.STRING_LIT,
                TokenType.TRUE, TokenType.FALSE)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect number or '('.");
        // ex 2, (5*2^4+3)
    }


    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    /// OVO SAM PROMJENILA DA VRACA TOKEN
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        else throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }

    private ParseError error(Token token, String message) {
        String where = token.type == TokenType.EOF ? "end" : "'" + token.lexeme + "'";
        String pos = "line " + token.line + ", col " + token.colStart;
        throw new ParseError("Parse error at " + where + " (" + pos + "): " + message);
    }

}
