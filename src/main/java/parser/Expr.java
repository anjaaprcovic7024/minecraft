package parser;

import lexer.token.Token;

public abstract class Expr {
    public interface Visitor<R> {
        R visitBinary(Binary e);
        R visitUnary(Unary e);
        R visitGrouping(Grouping e);
        R visitLiteral(Literal e);
    }

    public static final class Binary extends Expr {
        public final Expr left; public final Token op; public final Expr right;
        public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
    }

    public static final class Unary extends Expr {
        public final Token op; public final Expr right;
        public Unary(Token op, Expr right) { this.op = op; this.right = right; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitUnary(this); }
    }

    public static final class Grouping extends Expr {
        public final Expr expr;
        public Grouping(Expr expr) { this.expr = expr; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
    }

    public static final class Literal extends Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLiteral(this); }
    }

    public abstract <R> R accept(Visitor<R> v);
}
