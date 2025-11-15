package parser;

public final class AstPrinter implements Expr.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override public String visitBinary(Expr.Binary e) {
        return parenthesize(e.op.lexeme, e.left, e.right);
    }

    @Override public String visitUnary(Expr.Unary e) {
        return parenthesize(e.op.lexeme, e.right);
    }

    @Override public String visitGrouping(Expr.Grouping e) {
        return parenthesize("group", e.expr);
    }

    @Override public String visitLiteral(Expr.Literal e) {
        return e.value == null ? "nil" : e.value.toString();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();
        sb.append('(').append(name);
        for (Expr ex : exprs) {
            sb.append(' ').append(ex.accept(this));
        }
        sb.append(')');
        return sb.toString();
    }
}
