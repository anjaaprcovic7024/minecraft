package parser;

import lexer.token.Token;

import java.util.List;

public abstract class Stmt {

    public interface Visitor<R> {
        R visitVarDecl(VarDecl s);
        R visitReturn(Return s);
        R visitAssign(Assign s);
        R visitCallStmt(CallStmt s);
        R visitBeginIf(BeginIf s);
        R visitBeginFor(BeginFor s);
        R visitIncDec(IncDec s);
        R visitWhileStmt(WhileStmt s);
        R visitDoWhileStmt(DoWhileStmt s);
    }

    public abstract <R> R accept(Visitor<R> v);

    // int[...] a, b, c
    public static final class VarDecl extends Stmt {
        public final Ast.Type type;
        public final List<Expr> dims;
        public final List<Token> names;
        public final List<Expr> values;

        public VarDecl(Ast.Type type, List<Expr> dims, List<Token> names, List<Expr> values) {
            this.type = type;
            this.dims = dims;
            this.names = names;
            this.values = values;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
    }




    public static final class Return extends Stmt {
        public final Expr expr;
        public Return(Expr expr) { this.expr = expr; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
    }

    public static final class Assign extends Stmt {
        public final Expr left;
        public final LValue lvalue;
        public Assign(Expr left, LValue lvalue) { this.left = left; this.lvalue = lvalue; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
    }

    public static final class CallStmt extends Stmt {
        public final Expr.Call call;
        public CallStmt(Expr.Call call) { this.call = call; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitCallStmt(this); }
    }

    public static final class BeginIf extends Stmt {
        public static final class Arm {
            public final Expr cond;                 // aexpr rel_op aexpr
            public final List<Stmt> block;         // block
            public Arm(Expr cond, List<Stmt> block) { this.cond = cond; this.block = block; }
        }
        public final Arm ifArm;
        public final List<Arm> orIfArms;
        public final List<Stmt> elseBlock; // null ako ne postoji
        public BeginIf(Arm ifArm, List<Arm> orIfArms, List<Stmt> elseBlock) {
            this.ifArm = ifArm; this.orIfArms = orIfArms; this.elseBlock = elseBlock;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBeginIf(this); }
    }

    public static final class BeginFor extends Stmt {
        public final Stmt.VarDecl init;
        public final Expr cond;
        public final Stmt update;
        public final List<Stmt> body;

        public BeginFor(Stmt.VarDecl init, Expr cond, Stmt update, List<Stmt> body) {
            this.init = init;
            this.cond = cond;
            this.update = update;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitBeginFor(this); }
    }




    public static final class IncDec extends Stmt {
        public final LValue target;
        public final Token op; // INC ili DEC
        public IncDec(LValue target, Token op) {
            this.target = target;
            this.op = op;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIncDec(this); }
    }

    public static final class WhileStmt extends Stmt {
        public final Expr cond;
        public final List<Stmt> body;
        public WhileStmt(Expr cond, List<Stmt> body) {
            this.cond = cond;
            this.body = body;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitWhileStmt(this); }
    }

    public static final class DoWhileStmt extends Stmt {
        public final List<Stmt> body;
        public final Expr cond;
        public DoWhileStmt(List<Stmt> body, Expr cond) {
            this.body = body;
            this.cond = cond;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitDoWhileStmt(this); }
    }

    public static final class LValue {
        public final Token name;
        public final List<Expr> indices;
        public LValue(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
    }
}
