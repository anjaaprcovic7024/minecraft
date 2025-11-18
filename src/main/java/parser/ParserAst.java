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
        List<Ast.TopItem> items = new ArrayList<>();
        while (!check(EOF)) {
            Ast.TopItem item = parseTopItem();

            if (item instanceof Ast.TopVarDecl) {
                consume(SEPARATOR, "Expected ':' after top-level variable declaration");
            }

            items.add(item);
        }
        return new Ast.Program(true, items);
    }


    private Ast.TopItem parseTopItem() {
        if (check(FUNCTION)) {
            Ast.TopItem func = parseFuncDef();
            return func;
        }
        if (check(CLASS)) {
            Ast.TopItem klasa  = parseClassDef();
            return klasa;
        }
        Ast.TopItem varDecl = new Ast.TopVarDecl(parseVarDecl());
        consume(SEPARATOR, "Expected ':' after top-level item"); // samo promenljive
        return varDecl;
    }


    private Ast.FuncDef parseFuncDef() {
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
            items.add(parseTopItem());
            consume(SEPARATOR, "Expected ';' after class body item");
        }
        consume(RBRACE, "Expected '}' at class body end");
        return items;
    }

    private List<Ast.Param> parseParams() {
        List<Ast.Param> params = new ArrayList<>();
        params.add(parseParam());
        while (match(COMMA)) params.add(parseParam());
        return params;
    }

    private Ast.Param parseParam() {
        // prvo tip
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

        // zatim identifikator
        Token name = consume(IDENTIFICATOR, "expected parameter name");

        // preskoci eventualni separator ':'
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
        Ast.Type type = parseType();

        List<Expr> dims = new ArrayList<>();
        if (match(LBRACKET)) {
            dims.add(parseExpr());
            consume(RBRACKET, "expected ']'");
            while (match(LBRACKET)) {
                dims.add(parseExpr());
                consume(RBRACKET, "expected ']'");
            }
        }

        List<Token> names = new ArrayList<>();
        List<Expr> values = new ArrayList<>();
        do {
            Token id = consume(IDENTIFICATOR, "expected variable name");
            Expr value = null;
            if (match(ASSIGN)) { // obradi #
                value = parseExpr();
            }
            names.add(id);
            values.add(value);
            consume(SEPARATOR, "expected ':' after variable declaration");
        } while (match(COMMA));

        return new Stmt.VarDecl(dims, names, values);
    }





    private List<Token> parseIdentList() {
        List<Token> ids = new ArrayList<>();
        ids.add(consume(IDENTIFICATOR, "expected identifier"));
        while (match(COMMA)) ids.add(consume(IDENTIFICATOR, "expected identifier"));
        return ids;
    }

    private List<Stmt> parseBlock() {
        List<Stmt> stmts = new ArrayList<>();

        if (match(LBRACE)) { // blok sa {}
            while (!check(RBRACE) && !check(EOF)) {
                stmts.add(parseStmt());
                match(SEPARATOR); // ':'
            }
            consume(RBRACE, "Expected '}' at end of block");
        } else { // inline blok sa :
            while (!check(EOF) && !check(RBRACE) && !isNextBlockEnd()) {
                stmts.add(parseStmt());
                consume(SEPARATOR, "expected ':' after statement");
            }
        }


        return stmts;
    }

    private Stmt parseStmt() {
        if (check(INT)) return parseVarDecl();
        if (check(IDENTIFICATOR)) {
            if (checkNext(ASSIGN)) return parseAssignStmt();
            if (checkNext(INC)  || checkNext(DEC)) return parseIncDecStmt();
            return parseCallAndMaybeAssign();
        }
        switch (peek().type) {
            case RETURN: return parseReturnStmt();
            case IF: return parseIfTailAfterIF();
            case FOR: return parseForTailAfterFOR();
            case WHILE: return parseWhileStmt();
            case DO: return parseDoWhileStmt();
            default: throw error(peek(), "Unexpected statement");
        }
    }

    private Stmt parseIncDecStmt() {
        Stmt.LValue target = parseLValue();
        Token op;
        if (check(INC) || check(DEC)) op = advance();
        else throw error(peek(), "Expected '++' or '--'");
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
        return new Stmt.DoWhileStmt(body, cond);
    }

    private Stmt.BeginIf parseIfTailAfterIF() {
        Expr cond = null;
        if (!check(SEPARATOR) && !check(LBRACE)) {
            cond = parseCond(); // postoji uslov
        }

        List<Stmt> ifBlock = parseBlock(); // podržava {} ili :

        Stmt.BeginIf.Arm ifArm = new Stmt.BeginIf.Arm(cond, ifBlock);

        List<Stmt.BeginIf.Arm> orArms = new ArrayList<>();
        while (match(OR)) { // OR IF
            consume(ELSEIF, "expected 'deeper' for ELSEIF");
            Expr c = null;
            if (!check(SEPARATOR) && !check(LBRACE)) {
                c = parseCond();
            }
            List<Stmt> b = parseBlock();
            orArms.add(new Stmt.BeginIf.Arm(c, b));
        }

        List<Stmt> elseBlock = null;
        if (match(ELSE)) // else
            elseBlock = parseBlock();

        return new Stmt.BeginIf(ifArm, orArms, elseBlock);
    }



    private Stmt.BeginFor parseForTailAfterFOR() {
        consume(LPAREN, "expected '(' after FOR");
        Token var = consume(IDENTIFICATOR, "expected loop variable");
        consume(LPAREN, "expected '(' before for range");
        Expr from = parseExpr(); // samo jedan izraz za granicu
        consume(RPAREN, "expected ')' after for range");
        List<Stmt> body = parseBlock();
        return new Stmt.BeginFor(var, from, body);
    }

    private Stmt parseReturnStmt() {
        consume(RETURN, "expected RETURN");
        Expr e = parseExpr();
        return new Stmt.Return(e);
    }

    private Stmt parseCallAndMaybeAssign() {
        Expr.Call callExpr;
        if (match(PRINT)) {
            Token callee = previous();
            consume(LPAREN, "expected '(' after PRINT");
            List<Expr> args = new ArrayList<>();
            args.add(parseExpr());
            consume(RPAREN, "expected ')' after PRINT");
            callExpr = new Expr.Call(null, callee, args);
        } else if (match(SCAN)) {
            Token callee = previous();
            consume(LPAREN, "expected '(' after SCAN");
            Token id = consume(IDENTIFICATOR, "expected identifier in SCAN");
            consume(RPAREN, "expected ')' after SCAN");
            List<Expr> args = new ArrayList<>();
            args.add(new Expr.Ident(id));
            callExpr = new Expr.Call(null, callee, args);
        } else {
            callExpr = (Expr.Call) parseCallExpr();
        }

        if (match(ASSIGN)) {
            Stmt.LValue lv = parseLValue();
            return new Stmt.Assign(callExpr, lv);
        }
        return new Stmt.CallStmt(callExpr);
    }



    private Stmt parseAssignStmt() {
        Expr left = parseExprNoCall();
        consume(ASSIGN, "expected '->'");
        Stmt.LValue lv = parseLValue();
        return new Stmt.Assign(left, lv);
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
    // expr = aexpr ;
    private Expr parseExpr() { return parseAExpr(); }

    private Expr parseAExpr() {
        Expr left = parseAtom();
        while (match(ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT, CARET)) {
            Token op = previous();
            Expr right = parseAtom();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr parseExprNoCall() {
        return parseAExpr();
    }

    private Expr parseAtom() {
        if (match(INT_LIT)) {
            Token t = previous();
            return new Expr.IntLiteral(t, (Integer) t.literal);
        }
        if (match(DOUBLE_LIT)) {
            Token t = previous();
            return new Expr.DoubleLiteral(t, (Double) t.literal);
        }
        if (match(LONG_LIT)) {
            Token t = previous();
            return new Expr.LongLiteral(t, (Long) t.literal);
        }
        if (match(CHAR_LIT)) {
            Token t = previous();
            return new Expr.CharLiteral(t, (Character) t.literal);
        }
        if (match(STRING_LIT)) {
            Token t = previous();
            return new Expr.StringLiteral(t, (String) t.literal);
        }
        if (match(TRUE)) {
            Token t = previous();
            return new Expr.BooleanLiteral(t, true);
        }
        if (match(FALSE)) {
            Token t = previous();
            return new Expr.BooleanLiteral(t, false);
        }

        if (match(IDENTIFICATOR)) {
            Token id = previous();
            if (match(LBRACKET)) {
                List<Expr> idx = new ArrayList<>();
                idx.add(parseExpr());
                consume(RBRACKET, "expected ']'");
                while (match(LBRACKET)) {
                    idx.add(parseExpr());
                    consume(RBRACKET, "expected ']'");
                }
                return new Expr.Index(id, idx);
            }
            return new Expr.Ident(id);
        }

        if (match(LPAREN)) {
            Expr inner = parseExpr();
            consume(RPAREN, "expected ')'");
            return new Expr.Grouping(inner);
        }

        throw error(peek(), "expected expression");
    }

    private Expr parseCallExpr() {
        Token callee = consume(IDENTIFICATOR, "expected function name");
        consume(LPAREN, "expected '('");
        List<Expr> args = new ArrayList<>();
        if (!check(RPAREN)) args = parseArgs();
        consume(RPAREN, "expected ')'");
        return new Expr.Call(null, callee, args);
    }

    private List<Expr> parseArgs() {
        List<Expr> args = new ArrayList<>();
        args.add(parseExpr());
        while (match(SEPARATOR)) args.add(parseExpr());
        return args;
    }

    private Expr parseCond() {
        Expr left = parseAExpr();
        if (match(LT, LE, GT, GE, EQ, NEQ)) {
            Token op = previous();
            Expr right = parseAExpr();
            left = new Expr.Binary(left, op, right);
        }
        while (match(AND, OR)) {
            Token op = previous();
            Expr right = parseCond();
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
