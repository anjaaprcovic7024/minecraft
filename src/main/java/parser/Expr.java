package parser;

import lexer.token.Token;

import java.util.List;

public abstract class Expr {

    public interface Visitor<R> {
        R visitArrayLiteral(ArrayLiteral e);
        R visitIntLiteral(IntLiteral e);
        R visitDoubleLiteral(DoubleLiteral e);
        R visitLongLiteral(LongLiteral e);
        R visitCharLiteral(CharLiteral e);
        R visitStringLiteral(StringLiteral e);
        R visitBooleanLiteral(BooleanLiteral e);
        R visitIdent(Ident e);
        R visitIndex(Index e);
        R visitGrouping(Grouping e);
        R visitCall(Call e);
        R visitBinary(Binary e);
    }

    public abstract <R> R accept(Visitor<R> v);

    // ===== LITERALI =====

    public static final class ArrayLiteral extends Expr {
        public final List<Expr> elements;
        public ArrayLiteral(List<Expr> elements) { this.elements = elements; }
        @Override public <R> R accept(Visitor<R> v) {return v.visitArrayLiteral(this);}
    }

    public static final class IntLiteral extends Expr {
        public final Token token;
        public final int value;
        public IntLiteral(Token token, int value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIntLiteral(this); }
    }

    public static final class DoubleLiteral extends Expr {
        public final Token token;
        public final double value;
        public DoubleLiteral(Token token, double value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitDoubleLiteral(this); }
    }

    public static final class LongLiteral extends Expr {
        public final Token token;
        public final long value;
        public LongLiteral(Token token, long value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLongLiteral(this); }
    }

    public static final class CharLiteral extends Expr {
        public final Token token;
        public final char value;
        public CharLiteral(Token token, char value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitCharLiteral(this); }
    }

    public static final class StringLiteral extends Expr {
        public final Token token;
        public final String value;
        public StringLiteral(Token token, String value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitStringLiteral(this); }
    }

    public static final class BooleanLiteral extends Expr {
        public final Token token;
        public final boolean value;
        public BooleanLiteral(Token token, boolean value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBooleanLiteral(this); }
    }

    // ===== IDENT I INDEX =====
    public static final class Ident extends Expr {
        public final Token name;
        public Ident(Token name) { this.name = name; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
    }

    public static final class Index extends Expr {
        public final Token name;
        public final List<Expr> indices;
        public Index(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIndex(this); }
    }

    // ===== GROUPING =====
    public static final class Grouping extends Expr {
        public final Expr inner;
        public Grouping(Expr inner) { this.inner = inner; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
    }

    // ===== CALL =====
    public static final class Call extends Expr {
        public final Token callTok;
        public final Token callee;
        public final List<Expr> args;
        public Call(Token callTok, Token callee, List<Expr> args) {
            this.callTok = callTok; this.callee = callee; this.args = args;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitCall(this); }
    }

    // ===== BINARY =====
    public static final class Binary extends Expr {
        public final Expr left;
        public final Token op; // ADD/SUB/MULT/DIV/PERCENT/CARET ili LT/LE/GT/GE/EQ/NEQ/AND/OR
        public final Expr right;
        public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
    }
}
