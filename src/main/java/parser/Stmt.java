package parser;

import lexer.token.Token;
import java.util.List;

public abstract class Stmt {

    public interface Visitor<R> {
        R visitVarDecl(VarDecl s);
        R visitAssign(Assign s);
        R visitPrint(Print s);
        R visitScan(Scan s);
        R visitIf(If s);
        R visitWhile(While s);
        R visitFor(For s);
        R visitExprStmt(ExprStmt s);
    }

    public abstract <R> R accept(Visitor<R> v);

    // gold x, y;  / diamond[2][3] a;
    public static final class VarDecl extends Stmt {
        public final Token typeKeyword;   // to su nam 'gold', 'diamond', 'redstone'...
        public final int arrayRank;       // broj [] (0 ako nije niz)
        public final List<Token> names;   // lista identifikatora

        public VarDecl(Token typeKeyword, int arrayRank, List<Token> names) {
            this.typeKeyword = typeKeyword;
            this.arrayRank = arrayRank;
            this.names = names;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
    }

    // target # value;
    // target može biti IDENT ili IDENT[expr]...
    public static final class Assign extends Stmt {
        public final Expr target;   // npr. ident ili index
        public final Token op;      // ASSIGN ('#')
        public final Expr value;

        public Assign(Expr target, Token op, Expr value) {
            this.target = target;
            this.op = op;
            this.value = value;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
    }

    // collect(expr);
    public static final class Print extends Stmt {
        public final Token keyword; // u ovom slucaju PRINT
        public final Expr expr;

        public Print(Token keyword, Expr expr) {
            this.keyword = keyword;
            this.expr = expr;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitPrint(this); }
    }

    // drop(x);  (učitavanje)
    public static final class Scan extends Stmt {
        public final Token keyword; // SCAN
        public final Expr target;   // često će ovo biti ident

        public Scan(Token keyword, Expr target) {
            this.keyword = keyword;
            this.target = target;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitScan(this); }
    }

    // dig (cond) { thenBranch } deeper(cond2) { ... } bedrock { elseBranch }
    public static final class If extends Stmt {
        public static final class Arm {
            public final Expr condition;
            public final List<Stmt> body;
            public Arm(Expr condition, List<Stmt> body) {
                this.condition = condition; this.body = body;
            }
        }

        public final Arm ifArm;              // dig
        public final List<Arm> elseIfArms;   // deeper
        public final List<Stmt> elseBranch;  // bedrock (može biti null)

        public If(Arm ifArm, List<Arm> elseIfArms, List<Stmt> elseBranch) {
            this.ifArm = ifArm;
            this.elseIfArms = elseIfArms;
            this.elseBranch = elseBranch;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitIf(this); }
    }

    // build (cond) { body }
    public static final class While extends Stmt {
        public final Token keyword; // WHILE (build)
        public final Expr condition;
        public final List<Stmt> body;

        public While(Token keyword, Expr condition, List<Stmt> body) {
            this.keyword = keyword;
            this.condition = condition;
            this.body = body;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitWhile(this); }
    }

    // craft (i goes from a to b) { body }
    // OVO PRILAGODITI JER NISAM SIGURNA DA SAM DOBRO ODRADILA
    public static final class For extends Stmt {
        public final Token keyword; // FOR (craft)
        public final Token var;     // IDENT
        public final Expr from;
        public final Expr to;
        public final List<Stmt> body;

        public For(Token keyword, Token var, Expr from, Expr to, List<Stmt> body) {
            this.keyword = keyword;
            this.var = var;
            this.from = from;
            this.to = to;
            this.body = body;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitFor(this); }
    }

    // gola naredba izraza, npr: a + b * c;
    public static final class ExprStmt extends Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitExprStmt(this); }
    }
}
