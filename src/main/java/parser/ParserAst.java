package parser;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;

import static lexer.token.TokenType.*;
import static lexer.token.TokenType.ADD;
import static lexer.token.TokenType.ASSIGN;
import static lexer.token.TokenType.DIVIDE;
import static lexer.token.TokenType.ELSE;
import static lexer.token.TokenType.EOF;
import static lexer.token.TokenType.EQ;
import static lexer.token.TokenType.FOR;
import static lexer.token.TokenType.FUNCTION;
import static lexer.token.TokenType.GE;
import static lexer.token.TokenType.GT;
import static lexer.token.TokenType.IF;
import static lexer.token.TokenType.INT;
import static lexer.token.TokenType.INT_LIT;
import static lexer.token.TokenType.LBRACKET;
import static lexer.token.TokenType.LE;
import static lexer.token.TokenType.LPAREN;
import static lexer.token.TokenType.LT;
import static lexer.token.TokenType.MULTIPLY;
import static lexer.token.TokenType.NEQ;
import static lexer.token.TokenType.OR;
import static lexer.token.TokenType.RBRACKET;
import static lexer.token.TokenType.RETURN;
import static lexer.token.TokenType.RPAREN;
import static lexer.token.TokenType.SUBTRACT;

public final class ParserAst {
    private final List<Token> tokens;
    private int current = 0;

    public ParserAst(List<Token> tokens) { this.tokens = tokens; }

    public Ast.Program parseProgram() {
        // parsira ceo program i vraca AST programa
        List<Ast.TopItem> items = new ArrayList<>();
        while (!check(EOF)) {
            Ast.TopItem item = parseTopItem();

            /*if (item instanceof Ast.TopVarDecl) {
                consume(SEPARATOR, "Expected ':' after top-level variable declaration");
            }*/

            items.add(item);
        }
        return new Ast.Program(true, items);
    }

    private Ast.TopItem parseTopItem() {
        // parsira jedan top-level element (funkcija, klasa, statement ili varijabla)
        if (check(FUNCTION)) return parseFuncDef();
        if (check(CLASS)) return parseClassDef();
        if (check(FOR) || check(IF) || check(WHILE) || check(DO) || check(RETURN) || check(IDENTIFICATOR)) {
            return new Ast.TopStmt(parseStmt());
        }
        if (isTypeStart()) return new Ast.TopVarDecl(parseVarDecl());
        throw error(peek(), "Unexpected top-level item");
    }

    private Ast.FuncDef parseFuncDef() {
        // parsira definiciju funkcije i njeno telo
        consume(FUNCTION, "expected FUNCTION");
        Token name = consume(IDENTIFICATOR, "expected function name");
        consume(LPAREN, "expected '('");
        List<Ast.Param> params = new ArrayList<>();
        if (!check(RPAREN)) params = parseParams();
        consume(RPAREN, "expected ')'");
        Ast.Type returnType = new Ast.Type(Ast.Type.Kind.VOID, null, 0);
        List<Stmt> body = parseBlock();
        return new Ast.FuncDef(name, params, returnType, body);
    }

    private Ast.ClassDef parseClassDef() {
        // parsira definiciju klase i njeno telo
        consume(CLASS, "Expected 'class'");
        Token name = consume(IDENTIFICATOR, "Expected class name");
        Token extendsName = null;
        if (match(EXTENDS)) {
            extendsName = consume(IDENTIFICATOR, "Expected base class name");
        }
        List<Ast.TopItem> body = parseClassBody();
        return new Ast.ClassDef(name, extendsName, body);
    }

    private List<Ast.TopItem> parseClassBody() {
        List<Ast.TopItem> items = new ArrayList<>();
        consume(LBRACE, "Expected '{' at class body start");
        while (!check(RBRACE) && !check(EOF)) {
            if (isTypeStart()) {
                Ast.TopVarDecl var = new Ast.TopVarDecl(parseVarDecl(false));
                consume(SEPARATOR, "Expected ':' after class field");
                items.add(var);
            } else if (check(FUNCTION)) {
                items.add(parseFuncDef());
            } else {
                throw error(peek(), "Unexpected item in class body");
            }
        }
        consume(RBRACE, "Expected '}' at class body end");
        return items;
    }




    private List<Ast.Param> parseParams() {
        // parsira listu parametara funkcije
        List<Ast.Param> params = new ArrayList<>();
        params.add(parseParam());
        while (match(COMMA)) params.add(parseParam());
        return params;
    }

    private Ast.Param parseParam() {
        // parsira pojedinacan parametar i njegov tip
        Token typeToken = consumeOneOf("expected type",
                INT, BOOLEAN, DOUBLE, LONG, CHAR, STRING, ARRAY);

        Ast.Type type;
        switch (typeToken.type) {
            case INT -> type = new Ast.Type(Ast.Type.Kind.INT, typeToken, 0);
            case BOOLEAN -> type = new Ast.Type(Ast.Type.Kind.BOOLEAN, typeToken, 0);
            case DOUBLE -> type = new Ast.Type(Ast.Type.Kind.DOUBLE, typeToken, 0);
            case LONG -> type = new Ast.Type(Ast.Type.Kind.LONG, typeToken, 0);
            case CHAR -> type = new Ast.Type(Ast.Type.Kind.CHAR, typeToken, 0);
            case STRING -> type = new Ast.Type(Ast.Type.Kind.STRING, typeToken, 0);
            case ARRAY -> type = new Ast.Type(Ast.Type.Kind.ARRAY, typeToken, 0);
            default -> throw error(typeToken, "unknown type");
        }

        Token name = consume(IDENTIFICATOR, "expected parameter name");

        match(SEPARATOR);

        return new Ast.Param(name, type);
    }


    private Ast.Type parseType() {
        Token t = consumeOneOf("expected type", INT, BOOLEAN, DOUBLE, LONG, CHAR, STRING, ARRAY);
        Ast.Type.Kind kind;
        switch (t.type) {
            case INT -> kind = Ast.Type.Kind.INT;
            case BOOLEAN -> kind = Ast.Type.Kind.BOOLEAN;
            case DOUBLE -> kind = Ast.Type.Kind.DOUBLE;
            case LONG -> kind = Ast.Type.Kind.LONG;
            case CHAR -> kind = Ast.Type.Kind.CHAR;
            case STRING -> kind = Ast.Type.Kind.STRING;
            case ARRAY -> kind = Ast.Type.Kind.ARRAY;
            default -> throw error(t, "unknown type");
        }
        return new Ast.Type(kind, t, 0);
    }

    private Stmt.VarDecl parseVarDecl() {
        return parseVarDecl(true);
    }

    private Stmt.VarDecl parseVarDecl(boolean expectSeparator) {
        Ast.Type type;

        // parsiranje tipa niza
        if (match(ARRAY)) {
            Token arrayToken = previous();
            Ast.Type.Kind innerKind = Ast.Type.Kind.INT; // default
            if (match(LBRACKET)) {
                Token innerTypeToken = consumeOneOf(
                        "expected type inside array",
                        INT, BOOLEAN, DOUBLE, LONG, CHAR, STRING
                );
                switch (innerTypeToken.type) {
                    case INT -> innerKind = Ast.Type.Kind.INT;
                    case BOOLEAN -> innerKind = Ast.Type.Kind.BOOLEAN;
                    case DOUBLE -> innerKind = Ast.Type.Kind.DOUBLE;
                    case LONG -> innerKind = Ast.Type.Kind.LONG;
                    case CHAR -> innerKind = Ast.Type.Kind.CHAR;
                    case STRING -> innerKind = Ast.Type.Kind.STRING;
                }
                consume(RBRACKET, "expected ']' after array inner type");
            }
            type = new Ast.Type(Ast.Type.Kind.ARRAY, arrayToken, 1);
            type.inner = innerKind;
        } else {
            type = parseType();
        }

        // parsiranje dimenzija niza (staticke)
        List<Expr> dims = new ArrayList<>();
        while (match(LBRACKET)) {
            dims.add(parseExpr());
            consume(RBRACKET, "expected ']' after dimension");
        }

        Token id = consume(IDENTIFICATOR, "expected variable name");

        Expr value = null;

        // inicijalizator niza
        if (match(ASSIGN)) {
            if (match(LBRACKET)) { // array literal
                if (!dims.isEmpty()) {
                    throw error(peek(), "Cannot use '#' initializer with fixed size array");
                }
                List<Expr> elems = new ArrayList<>();
                if (!check(RBRACKET)) {
                    elems.add(parseExpr());
                    while (match(COMMA)) elems.add(parseExpr());
                }
                consume(RBRACKET, "expected ']' after array literal");
                value = new Expr.ArrayLiteral(elems);
            } else {
                value = parseExpr();
            }
        }

        List<Token> names = new ArrayList<>();
        List<Expr> values = new ArrayList<>();
        names.add(id);
        values.add(value);

        while (match(COMMA)) {
            id = consume(IDENTIFICATOR, "expected variable name after ','");
            value = null;
            if (match(ASSIGN)) {
                if (match(LBRACKET)) {
                    if (!dims.isEmpty()) {
                        throw error(peek(), "Cannot use '#' initializer with fixed size array");
                    }
                    List<Expr> elems = new ArrayList<>();
                    if (!check(RBRACKET)) {
                        elems.add(parseExpr());
                        while (match(COMMA)) elems.add(parseExpr());
                    }
                    consume(RBRACKET, "expected ']' after array literal");
                    value = new Expr.ArrayLiteral(elems);
                } else {
                    value = parseExpr();
                }
            }
            names.add(id);
            values.add(value);
        }

        if (expectSeparator) consume(SEPARATOR, "expected ':' after statement");
        return new Stmt.VarDecl(type, dims, names, values);
    }

    private List<Stmt> parseBlock() {
        List<Stmt> stmts = new ArrayList<>();
        consume(LBRACE, "Expected '{' at start of block");
        while (!check(RBRACE) && !check(EOF)) {
            stmts.add(parseStmt());
        }
        consume(RBRACE, "Expected '}' at end of block");
        return stmts;
    }

    private boolean isTypeStart() {
        TokenType t = peek().type;
        return t == INT
                || t == BOOLEAN
                || t == DOUBLE
                || t == LONG
                || t == CHAR
                || t == STRING
                || t == ARRAY;
    }

    private Stmt parseStmt() {
        switch (peek().type) {
            case FOR:    return parseForStmt();
            case IF:     return parseIfStmt();
            case WHILE:  return parseWhileStmt();
            case DO:     return parseDoWhileStmt();
            case RETURN: return parseReturnStmt();
        }

        if (isTypeStart()) return parseVarDecl();

        if (peek().type == PRINT || peek().type == SCAN) return parseCallStmt();

        if (check(IDENTIFICATOR)) {
            if (checkNext(LPAREN)) return parseCallStmt();
            if (checkNext(ASSIGN)) return parseAssignStmt(); // Poziva gornju metodu
            if (checkNext(INC) || checkNext(DEC)) return parseIncDecStmt();

            Expr e = parseExpr();
            consume(SEPARATOR, "expected ':' after statement");
            return new Stmt.ExprStmt(e);
        }


        throw error(peek(), "Unexpected statement");
    }


    private Stmt parseIncDecStmt() {
        Stmt.LValue target = parseLValue();
        Token op;
        if (check(INC) || check(DEC)) op = advance();
        else throw error(peek(), "Expected '++' or '--'");
        consume(SEPARATOR, "expected ':' after inc/dec");
        return new Stmt.IncDec(target, op);
    }

    private Stmt parseWhileStmt() {
        consume(WHILE, "Expected 'while'");
        consume(LPAREN, "Expected '(' after while");
        Expr cond = parseCond();
        consume(RPAREN, "Expected ')' after while condition");
        List<Stmt> body = parseBlock();
        return new Stmt.WhileStmt(cond, body);
    }

    private Stmt parseDoWhileStmt() {
        consume(DO, "Expected 'do'");
        List<Stmt> body = parseBlock();
        consume(WHILE, "Expected 'while' after do block");
        consume(LPAREN, "Expected '(' after while");
        Expr cond = parseCond();
        consume(RPAREN, "Expected ')' after do-while condition");
        consume(SEPARATOR, "expected ':' after do-while");
        return new Stmt.DoWhileStmt(body, cond);
    }

    private Stmt.BeginIf parseIfStmt() {
        consume(IF, "expected 'dig' (IF)");

        consume(LPAREN, "expected '(' after IF");
        Expr cond = parseCond();
        consume(RPAREN, "expected ')' after IF condition");

        List<Stmt> ifBlock = parseBlock();
        Stmt.BeginIf.Arm ifArm = new Stmt.BeginIf.Arm(cond, ifBlock);

        List<Stmt.BeginIf.Arm> orArms = new ArrayList<>();
        while (match(ELSEIF)) {
            consume(LPAREN, "expected '(' after deeper");
            Expr c = parseCond();
            consume(RPAREN, "expected ')' after deeper condition");
            List<Stmt> b = parseBlock();
            orArms.add(new Stmt.BeginIf.Arm(c, b));
        }

        List<Stmt> elseBlock = null;
        if (match(ELSE)) {
            elseBlock = parseBlock();
        }

        return new Stmt.BeginIf(ifArm, orArms, elseBlock);
    }

    private Stmt parseForStmt() {
        consume(FOR, "expected 'craft'");
        consume(LPAREN, "expected '(' after craft");

        // for-init
        Stmt.VarDecl forInit = parseVarDecl(false);
        consume(SEPARATOR, "expected ':' after for-init");

        // for-cond
        Expr cond = parseCond();
        consume(SEPARATOR, "expected ':' after for-cond");

        // for-update
        Stmt update = parseForUpdate();

        consume(RPAREN, "expected ')' after for header");

        List<Stmt> body = parseBlock();
        return new Stmt.BeginFor(forInit, cond, update, body);
    }


    private Stmt parseForUpdate() {
        if (check(IDENTIFICATOR) && checkNext(ASSIGN)) {
            return parseAssignStmt();
        } else if (check(IDENTIFICATOR) && (checkNext(INC) || checkNext(DEC))) {
            return parseIncDecStmt();
        } else {
            throw error(peek(), "expected for-update statement");
        }
    }



    private Stmt parseReturnStmt() {
        consume(RETURN, "expected RETURN");
        Expr e = parseExpr();
        consume(SEPARATOR, "expected ':' after return");
        return new Stmt.Return(e);
    }

    private Stmt parseCallStmt() {
        Expr.Call callExpr;

        if (match(PRINT)) {
            Token callee = previous();
            consume(LPAREN, "expected '(' after PRINT");
            List<Expr> args = new ArrayList<>();
            if (!check(RPAREN)) {
                args.add(parseExpr());
                while (match(COMMA)) {
                    args.add(parseExpr());
                }
            }
            consume(RPAREN, "expected ')' after PRINT");
            callExpr = new Expr.Call(null, callee, args);
        } else if (match(SCAN)) {
            Token callee = previous();
            consume(LPAREN, "expected '(' after SCAN");
            List<Expr> args = new ArrayList<>();
            if (!check(RPAREN)) {
                args.add(parseExpr());
                while (match(COMMA)) {
                    args.add(parseExpr());
                }
            }
            consume(RPAREN, "expected ')' after SCAN");
            callExpr = new Expr.Call(null, callee, args);
        } else {
            Expr e = parseAtom();
            if (!(e instanceof Expr.Call)) throw error(peek(), "expected function call");
            callExpr = (Expr.Call) e;
        }

        consume(SEPARATOR, "expected ':' after call statement");
        return new Stmt.CallStmt(callExpr);
    }


    private Stmt parseAssignStmt() {
        Stmt.LValue lv = parseLValue();
        consume(ASSIGN, "expected '#'");
        Expr value = parseExpr(); // Ovo sada poziva ceo lanac (Ternary -> Or -> And -> Bitwise...)
        consume(SEPARATOR, "expected ':' after assignment"); // DODAJ OVO
        return new Stmt.Assign(value, lv);
    }


    private Stmt.LValue parseLValue() {
        Token id = consume(IDENTIFICATOR, "expected identifier");
        List<Expr> idx = new ArrayList<>();
        while (match(LBRACKET)) {
            idx.add(parseExpr());
            consume(RBRACKET, "expected ']'");
        }
        return new Stmt.LValue(id, idx);
    }

    // ===== expressions =====
    private Expr parseExpr() {
        return parseTernary(); // umesto parseAExpr()
    }


    private Expr parseAExpr() {
        return parseAdditive(); // ili parseCond() ako je to tvoja logika
    }

    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (match(ADD, SUBTRACT)) {
            Token op = previous();
            Expr right = parseMultiplicative();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseMultiplicative() {
        Expr left = parseUnary(); // **VAŽNO: unarni operatori ovde**
        while (match(MULTIPLY, DIVIDE, PERCENT)) {
            Token op = previous();
            Expr right = parseUnary();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseUnary() {
        if (match(NOT, SUBTRACT)) {
            Token op = previous();
            Expr right = parseUnary();
            return new Expr.Unary(op, right);
        }
        return parseAtom();
    }
    private Expr parseTernary() {
        Expr cond = parseOr();
        if (match(TQUESTION)) {
            Expr thenBranch = parseExpr();
            consume(TSEMICOLON, "expected ';' after ternary true branch");
            Expr elseBranch = parseTernary();
            return new Expr.Ternary(cond, thenBranch, elseBranch);
        }
        return cond;
    }



    private Expr parseExprNoCall() {
        return parseAExpr();
    }
    private Expr parseAtom() {
        // unary NOT operator
        if (match(NOT)) {
            Token op = previous();
            Expr right = parseAtom();  // NOT je unarni, parsiramo odmah operand
            return new Expr.Unary(op, right);
        }

        // array literal
        if (match(LBRACKET)) {
            List<Expr> elems = new ArrayList<>();
            if (!check(RBRACKET)) {
                elems.add(parseExpr());
                while (match(COMMA)) {
                    elems.add(parseExpr());
                }
            }
            consume(RBRACKET, "expected ']' after array literal");
            return new Expr.ArrayLiteral(elems);
        }

        // identifikatori i pozivi funkcija
        if (match(IDENTIFICATOR)) {
            Token id = previous();
            if (check(LPAREN)) {
                consume(LPAREN, "expected '(' after function name");
                List<Expr> args = new ArrayList<>();
                if (!check(RPAREN)) {
                    args.add(parseExpr());
                    while (match(COMMA)) args.add(parseExpr());
                }
                consume(RPAREN, "expected ')' after function call");
                return new Expr.Call(null, id, args);
            }
            List<Expr> idx = new ArrayList<>();
            while (match(LBRACKET)) {
                idx.add(parseExpr());
                consume(RBRACKET, "expected ']'");
            }
            if (!idx.isEmpty()) return new Expr.Index(id, idx);
            return new Expr.Ident(id);
        }

        // literali
        if (match(INT_LIT)){
            Token t = previous();
            return new Expr.IntLiteral(t, (Integer) t.literal);
        }
        if (match(DOUBLE_LIT)) return new Expr.DoubleLiteral(previous(), (Double) previous().literal);
        if (match(TRUE)) return new Expr.BooleanLiteral(previous(), true);
        if (match(FALSE)) return new Expr.BooleanLiteral(previous(), false);
        if (match(LONG_LIT)) return new Expr.LongLiteral(previous(), (Long) previous().literal);
        if (match(CHAR_LIT)) return new Expr.CharLiteral(previous(), (Character) previous().literal);
        if (match(STRING_LIT)) return new Expr.StringLiteral(previous(), (String) previous().literal);


        if (match(LPAREN)) {
            Expr inner = parseExpr();
            consume(RPAREN, "expected ')'");
            return new Expr.Grouping(inner);
        }

        throw error(peek(), "expected expression");
    }

    private List<Expr> parseArgs() {
        List<Expr> args = new ArrayList<>();
        args.add(parseExpr());
        while (match(COMMA)) {
            args.add(parseExpr());
        }
        return args;
    }

    private Expr parseCond() {
        return parseOr(); // start od najnižeg prioritetnog logičkog operatora
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(OR)) {
            Token op = previous();
            Expr right = parseAnd();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseBitOr();
        while (match(AND)) {
            Token op = previous();
            Expr right = parseBitOr();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseBitOr() {
        Expr left = parseBitAnd();
        while (match(BIT_OR)) {
            Token op = previous();
            Expr right = parseBitAnd();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseBitAnd() {
        Expr left = parseEquality(); // Bitwise AND (&) ima manji prioritet od ==
        while (match(BIT_AND)) {
            Token op = previous();
            Expr right = parseEquality();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseShift() {
        Expr left = parseAdditive();
        while (match(BIT_LSHIFT, BIT_RSHIFT)) { // '<<' i '>>'
            Token op = previous();
            Expr right = parseAdditive();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseEquality() {
        Expr left = parseRelational();
        while (match(EQ, NEQ)) {
            Token op = previous();
            Expr right = parseRelational();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseRelational() {
        Expr left = parseShift(); 
        while (match(LT, LE, GT, GE)) {
            Token op = previous();
            Expr right = parseShift();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }


    // ===== utilities =====

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private Token consumeOneOf(String message, TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) return advance();
        }
        throw error(peek(), message);
    }


    private boolean check(TokenType type) {
        if (isAtEnd()) return type == EOF;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == EOF; }

    private Token peek() { return tokens.get(current); }

    private Token previous() { return tokens.get(current - 1); }

    private ParseError error(Token token, String message) {
        String where = token.type == EOF ? " at end" : " at '" + token.lexeme + "'";
        return new ParseError("Parse error" + where + ": " + message +
                " (line: " + token.line + ", col: " + token.colStart + ")");
    }

    private boolean isNextBlockEnd() {
        TokenType t = peek().type;
        return t == ELSE || t == ELSEIF || t == EOF || t == RETURN || t == IF || t == FOR || t == WHILE;
    }

    private static final class ParseError extends RuntimeException {
        ParseError(String s) { super(s); }
        ParseError() { super("parser error"); }
    }


}
