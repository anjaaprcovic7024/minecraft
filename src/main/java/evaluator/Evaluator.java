package evaluator;

import parser.Expr;

public final class Evaluator implements Expr.Visitor<Double> {

    public double eval(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Double visitLiteral(Expr.Literal e) {
        Object v = e.value;

        if (v == null) {
            throw new RuntimeException("Cannot evaluate null literal.");
        }

        if (v instanceof Number n) {
            return n.doubleValue();
        }
        throw new RuntimeException("Cannot evaluate non-numeric literal: " + v + " (" + v.getClass().getSimpleName() + ")");
    }

    @Override
    public Double visitGrouping(Expr.Grouping e) {
        return e.expr.accept(this);
    }

    @Override
    public Double visitUnary(Expr.Unary e) {
        double r = e.right.accept(this);
        String op = e.op.lexeme;
        switch (op) {
            case "+": return +r;
            case "-": return -r;
            default: throw new RuntimeException("Unknown unary op: " + op);
        }
    }

    @Override
    public Double visitBinary(Expr.Binary e) {
        double l = e.left.accept(this);
        double r = e.right.accept(this);
        String op = e.op.lexeme;

        return switch (op) {
            case "+" -> l + r;
            case "-" -> l - r;
            case "*" -> l * r;
            case "/" -> {
                if (r == 0.0) throw new ArithmeticException("Division by zero");
                yield l / r;
            }
            case "^" ->
                    Math.pow(l, r);
            default -> throw new RuntimeException("Unknown binary op: " + op);
        };
    }
}
