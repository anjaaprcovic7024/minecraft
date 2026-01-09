package semantic;

import lexer.token.Token;
import parser.Ast;
import parser.Expr;
import parser.Stmt;
import lexer.token.TokenType;

import java.util.List;

public final class SemanticAnalyzer
        implements Expr.Visitor<Ast.Type>,
        Stmt.Visitor<Void> {

    private SymbolTable symbols = new SymbolTable();
    private Ast.Type currentReturnType = null;
    private boolean mainFound = false;
    private boolean hasReturn = false;
    public Ast.Type inferredType = null;


    /* ================= PROGRAM ================= */

    public void analyze(Ast.Program program) {

        for (Ast.TopItem it : program.items) {

            if (it instanceof Ast.FuncDef f) {
                if (!symbols.defineFunc(f.name.lexeme, f))
                    throw error("Function redeclared: " + f.name.lexeme, f, f.name);

                if (f.name.lexeme.equals("main")) {
                    if (mainFound)
                        throw error("Multiple main functions", f, f.name);
                    mainFound = true;

                    if (f.returnType.kind != Ast.Type.Kind.INT)
                        throw error("Main function must return int", f, f.name);

                    if (!f.params.isEmpty())
                        throw error("Main function must have no parameters", f, f.name);
                }
            }

            if (it instanceof Ast.TopVarDecl v) {
                for (int i = 0; i < v.decl.names.size(); i++) {
                    String name = v.decl.names.get(i).lexeme;

                    if (!symbols.defineVar(name, v.decl.type))
                        throw error("Global variable redeclared: " + name, v, v.decl.names.get(i));
                }
            }
        }

        if (!mainFound)
            throw error("No main function found");

        for (Ast.TopItem it : program.items) {
            if (it instanceof Ast.FuncDef f) {
                visitFunc(f);
            } else if (it instanceof Ast.TopStmt s) {
                s.stmt.accept(this);
            }
        }
    }


    private void visitFunc(Ast.FuncDef f) {

        symbols = symbols.enterScope();

        currentReturnType = f.returnType;
        boolean oldHasReturn = hasReturn;
        hasReturn = false;

        for (Ast.Param p : f.params) {
            if (!symbols.defineParam(p.name.lexeme, p.type))
                throw error("Parameter redeclared: " + p.name.lexeme, f, p.name);
        }

        for (Stmt s : f.body) {
            s.accept(this);
        }

        if (currentReturnType.kind != Ast.Type.Kind.VOID && !hasReturn)
            throw error("Missing return in function: " + f.name.lexeme, f, f.name);

        hasReturn = oldHasReturn;
        symbols = symbols.exitScope();
        currentReturnType = null;
    }

    /* ================= EXPRESSIONS ================= */

    @Override
    public Ast.Type visitIntLiteral(Expr.IntLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.INT, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitDoubleLiteral(Expr.DoubleLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.DOUBLE, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitLongLiteral(Expr.LongLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.LONG, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitCharLiteral(Expr.CharLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.CHAR, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitStringLiteral(Expr.StringLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.STRING, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitBooleanLiteral(Expr.BooleanLiteral e) {
        e.inferredType = new Ast.Type(Ast.Type.Kind.BOOLEAN, null, 0);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitArrayLiteral(Expr.ArrayLiteral e) {
        if (e.elements.isEmpty())
            throw error("Empty array literal not allowed");

        Ast.Type first = e.elements.get(0).accept(this);
        for (Expr ex : e.elements) {
            Ast.Type t = ex.accept(this);
            if (!Ast.sameType(t, first))
                throw error("Array literal elements must have same type");
        }

        Ast.Type arr = new Ast.Type(Ast.Type.Kind.ARRAY, null, 1);
        arr.inner = first;
        e.inferredType = arr;
        return arr;
    }

    @Override
    public Ast.Type visitIdent(Expr.Ident e) {
        Ast.Type t = symbols.lookupVar(e.name.lexeme);
        if (t == null)
            throw error("Undefined variable: " + e.name.lexeme, null, e.name);
        e.inferredType = t;
        return t;
    }

    @Override
    public Ast.Type visitIndex(Expr.Index e) {
        Ast.Type t = symbols.lookupVar(e.name.lexeme);
        if (t == null)
            throw error("Undefined variable: " + e.name.lexeme, null, e.name);

        Ast.Type current = t;
        for (Expr idx : e.indices) {
            if (current.kind != Ast.Type.Kind.ARRAY)
                throw error("Indexing non-array", null, e.name);
            Ast.Type it = idx.accept(this);
            if (it.kind != Ast.Type.Kind.INT)
                throw error("Array index must be INT", null, idx instanceof Expr.Ident ide ? ide.name : null);
            current = current.inner;
        }

        e.inferredType = current;
        return current;
    }

    @Override
    public Ast.Type visitGrouping(Expr.Grouping e) {
        e.inferredType = e.inner.accept(this);
        return e.inferredType;
    }

    @Override
    public Ast.Type visitCall(Expr.Call e) {
        Ast.FuncDef f = symbols.lookupFunc(e.callee.lexeme);
        if (f == null) {
            if (symbols.lookupVar(e.callee.lexeme) != null)
                throw error("Trying to call a variable as function: " + e.callee.lexeme, null, e.callee);
            throw error("Call to undefined function: " + e.callee.lexeme, null, e.callee);
        }

        if (f.params.size() != e.args.size())
            throw error("Argument count mismatch", null, e.callee);

        for (int i = 0; i < f.params.size(); i++) {
            Ast.Type pt = f.params.get(i).type;
            Ast.Type at = e.args.get(i).accept(this);
            if (pt.kind != Ast.Type.Kind.ANY && !Ast.sameType(pt, at))
                throw error("Argument type mismatch", null, e.args.get(i) instanceof Expr.Ident ide ? ide.name : null);
        }

        e.inferredType = f.returnType;
        return e.inferredType;
    }

    @Override
    public Ast.Type visitUnary(Expr.Unary e) {
        Ast.Type t = e.right.accept(this);

        switch (e.op.type) {
            case NOT -> {
                if (t.kind != Ast.Type.Kind.BOOLEAN)
                    throw error("NOT expects boolean", null, e.op);
                e.inferredType = new Ast.Type(Ast.Type.Kind.BOOLEAN, null, 0);
                return e.inferredType;
            }
            case SUBTRACT -> {
                if (!isNumeric(t))
                    throw error("Unary minus expects numeric", null, e.op);
                e.inferredType = t;
                return e.inferredType;
            }
        }

        throw error("Invalid unary operator");
    }

    @Override
    public Ast.Type visitBinary(Expr.Binary e) {
        Ast.Type l = e.left.accept(this);
        Ast.Type r = e.right.accept(this);
        Ast.Type result;

        switch (e.op.type) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT -> {
                if (!isNumeric(l) || !isNumeric(r))
                    throw error("Arithmetic on non-numeric", null, e.op);
                if (!Ast.sameType(l, r))
                    throw error("Mixed numeric types not allowed", null, e.op);
                result = l;
            }
            case LT, LE, GT, GE -> {
                if (!isNumeric(l) || !isNumeric(r))
                    throw error("Relational operators expect numeric");
                result = new Ast.Type(Ast.Type.Kind.BOOLEAN, null, 0);
            }
            case EQ, NEQ -> {
                if (!Ast.sameType(l, r))
                    throw error("Equality operators require same type");
                result = new Ast.Type(Ast.Type.Kind.BOOLEAN, null, 0);
            }
            case AND, OR -> {
                if (l.kind != Ast.Type.Kind.BOOLEAN || r.kind != Ast.Type.Kind.BOOLEAN)
                    throw error("Logical operators expect boolean");
                result = new Ast.Type(Ast.Type.Kind.BOOLEAN, null, 0);
            }
            case BIT_AND, BIT_OR, BIT_LSHIFT, BIT_RSHIFT -> {
                if (l.kind != Ast.Type.Kind.INT || r.kind != Ast.Type.Kind.INT)
                    throw error("Bitwise operators expect int");
                result = new Ast.Type(Ast.Type.Kind.INT, null, 0);
            }
            default -> throw error("Unknown binary operator");
        }

        e.inferredType = result;
        return result;
    }

    @Override
    public Ast.Type visitTernary(Expr.Ternary e) {
        Ast.Type c = e.cond.accept(this);
        if (c.kind != Ast.Type.Kind.BOOLEAN)
            throw error("Ternary condition must be boolean", null, e.cond instanceof Expr.Ident ide ? ide.name : null);

        Ast.Type t1 = e.thenExpr.accept(this);
        Ast.Type t2 = e.elseExpr.accept(this);

        if (!Ast.sameType(t1, t2))
            throw error("Ternary branches must match type", null, e.thenExpr instanceof Expr.Ident ide ? ide.name : null);

        e.inferredType = t1;
        return t1;
    }

    @Override
    public Ast.Type visitCast(Expr.Cast e) {
        Ast.Type from = e.expr.accept(this);
        Ast.Type to = e.type;

        if (!isNumeric(from) || !isNumeric(to))
            throw error("Invalid cast: non-numeric type");

        if (from.kind == Ast.Type.Kind.DOUBLE && (to.kind == Ast.Type.Kind.INT || to.kind == Ast.Type.Kind.LONG)) {
            if (e.expr instanceof Expr.DoubleLiteral dl) {
                if (dl.value % 1 != 0)
                    throw error("Cannot cast fractional double literal " + dl.value + " to gold");
            } else {
                throw error("Cannot cast double variable/expression to gold");
            }
            e.inferredType = to;
            return to;
        }

        if ((from.kind == Ast.Type.Kind.INT || from.kind == Ast.Type.Kind.LONG) && to.kind == Ast.Type.Kind.DOUBLE) {
            e.inferredType = to;
            return to;
        }

        if (Ast.sameType(from, to)) {
            e.inferredType = to;
            return to;
        }

        throw error("Invalid numeric cast from " + from.kind + " to " + to.kind);
    }


    /* ================= STATEMENTS ================= */

    @Override
    public Void visitVarDecl(Stmt.VarDecl s) {
        for (int i = 0; i < s.names.size(); i++) {
            String name = s.names.get(i).lexeme;

            if (symbols.isDefinedLocally(name))
                throw error("Variable redeclared in local scope: " + name);

            Ast.Type t = s.type;

            if (s.values.get(i) != null) {
                Expr rhsExpr = s.values.get(i);
                Ast.Type rhs = rhsExpr.accept(this);

                if (!Ast.sameType(t, rhs)) {
                    if (isNumeric(t) && isNumeric(rhs)) {

                        if ((t.kind == Ast.Type.Kind.INT || t.kind == Ast.Type.Kind.LONG)
                                && rhs.kind == Ast.Type.Kind.DOUBLE) {

                            if (rhsExpr instanceof Expr.DoubleLiteral dl) {
                                if (dl.value % 1 != 0)
                                    throw error("Cannot assign fractional double literal " + dl.value + " to gold");
                            } else if (rhsExpr instanceof Expr.Cast cast) {
                                if (cast.expr instanceof Expr.DoubleLiteral dl2) {
                                    if (dl2.value % 1 != 0)
                                        throw error("Cannot cast fractional double literal " + dl2.value + " to gold");
                                }
                            } else {
                                throw error("Cannot assign double variable/expression to gold without explicit (gold) cast");
                            }

                        }
                        else if (t.kind == Ast.Type.Kind.DOUBLE && rhs.kind == Ast.Type.Kind.INT) {
                        } else {
                            throw error("Invalid numeric cast in declaration");
                        }

                    } else {
                        throw error("Type mismatch in initialization");
                    }
                }
            }

            symbols.defineVar(name, t);
        }
        return null;
    }

    @Override
    public Void visitAssign(Stmt.Assign s) {
        Ast.Type target = resolveLValue(s.lvalue);
        Ast.Type value = s.left.accept(this);

        if (!Ast.sameType(target, value)) {
            if (isNumeric(target) && isNumeric(value)) {

                if ((target.kind == Ast.Type.Kind.INT || target.kind == Ast.Type.Kind.LONG) && value.kind == Ast.Type.Kind.DOUBLE) {
                    if (s.left instanceof Expr.DoubleLiteral dl) {
                        if (dl.value % 1 != 0)
                            throw error("Cannot assign fractional double literal " + dl.value + " to gold");
                    } else if (s.left instanceof Expr.Cast cast) {
                        if (cast.expr instanceof Expr.DoubleLiteral dl2 && dl2.value % 1 != 0)
                            throw error("Cannot cast fractional double literal " + dl2.value + " to gold");
                    } else {
                        throw error("Cannot assign double expression to gold without explicit (gold) cast");
                    }
                }

                else if (target.kind == Ast.Type.Kind.DOUBLE && value.kind == Ast.Type.Kind.INT) {
                    // dozvoljeno
                }

                else {
                    throw error("Invalid numeric cast");
                }

            } else {
                throw error("Assignment type mismatch");
            }
        }

        return null;
    }

    @Override
    public Void visitIncDec(Stmt.IncDec s) {
        Ast.Type t = resolveLValue(s.target);

        if (t.kind != Ast.Type.Kind.INT)
            throw error("Inc/dec expects int");

        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return s) {
        if (currentReturnType == null)
            throw error("Return outside function");

        Ast.Type t = s.expr.accept(this);

        if (!Ast.sameType(t, currentReturnType)) {
            if (isNumeric(t) && isNumeric(currentReturnType)) {
                if (currentReturnType.kind == Ast.Type.Kind.DOUBLE && t.kind == Ast.Type.Kind.INT) {
                } else if (currentReturnType.kind == Ast.Type.Kind.INT && t.kind == Ast.Type.Kind.DOUBLE) {
                    if (s.expr instanceof Expr.DoubleLiteral dl && dl.value % 1 != 0)
                        throw error("Return type mismatch: fractional part lost");
                    else
                        throw error("Return type mismatch: double to int requires literal");
                } else {
                    throw error("Return type mismatch");
                }
            } else {
                throw error("Return type mismatch");
            }
        }

        hasReturn = true;
        return null;
    }

    @Override
    public Void visitCallStmt(Stmt.CallStmt s) {
        s.call.accept(this);
        return null;
    }

    @Override
    public Void visitBeginIf(Stmt.BeginIf s) {
        checkCondBlock(s.ifArm);
        for (Stmt.BeginIf.Arm a : s.orIfArms) checkCondBlock(a);
        if (s.elseBlock != null) visitBlock(s.elseBlock);
        return null;
    }

    private void checkCondBlock(Stmt.BeginIf.Arm arm) {
        Ast.Type c = arm.cond.accept(this);
        if (c.kind != Ast.Type.Kind.BOOLEAN)
            throw error("If condition must be boolean");
        visitBlock(arm.block);
    }

    @Override
    public Void visitBeginFor(Stmt.BeginFor s) {
        symbols = symbols.enterScope();

        s.init.accept(this);

        Ast.Type c = s.cond.accept(this);
        if (c.kind != Ast.Type.Kind.BOOLEAN)
            throw error("For condition must be boolean");

        s.update.accept(this);

        for (Stmt st : s.body) st.accept(this);

        symbols = symbols.exitScope();
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.WhileStmt s) {
        Ast.Type c = s.cond.accept(this);
        if (c.kind != Ast.Type.Kind.BOOLEAN)
            throw error("While condition must be boolean");

        symbols = symbols.enterScope();
        for (Stmt st : s.body) st.accept(this);
        symbols = symbols.exitScope();

        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhileStmt s) {
        symbols = symbols.enterScope();
        for (Stmt st : s.body) st.accept(this);
        symbols = symbols.exitScope();

        Ast.Type c = s.cond.accept(this);
        if (c.kind != Ast.Type.Kind.BOOLEAN)
            throw error("Do-while condition must be boolean");

        return null;
    }

    @Override
    public Void visitExprStmt(Stmt.ExprStmt s) {
        s.expr.accept(this);
        return null;
    }

    @Override
    public Void visitArrayAssign(Stmt.ArrayAssign s) {
        Ast.Type arrType = resolveLValue(s.target);
        Ast.Type valueType = s.value.accept(this);

        if (!Ast.sameType(arrType, valueType)) {
            if (isNumeric(arrType) && isNumeric(valueType)) {
                if (arrType.kind == Ast.Type.Kind.DOUBLE && valueType.kind == Ast.Type.Kind.INT) {
                    // ok, implicit cast int -> double
                } else if (arrType.kind == Ast.Type.Kind.INT && valueType.kind == Ast.Type.Kind.DOUBLE) {
                    if (s.value instanceof Expr.DoubleLiteral dl && dl.value % 1 != 0)
                        throw error("Cannot cast double to int with fractional part");
                    else
                        throw error("Cannot cast double to int");
                } else {
                    throw error("Invalid numeric cast");
                }
            } else {
                throw error("Type mismatch in array assignment");
            }
        }

        return null;
    }

    /* ================= HELPERS ================= */

    private void visitBlock(List<Stmt> stmts) {
        symbols = symbols.enterScope();
        for (Stmt s : stmts) s.accept(this);
        symbols = symbols.exitScope();
    }

    private boolean isNumeric(Ast.Type t) {
        return t.kind == Ast.Type.Kind.INT
                || t.kind == Ast.Type.Kind.DOUBLE
                || t.kind == Ast.Type.Kind.LONG;
    }

    private Ast.Type resolveLValue(Stmt.LValue lv) {

        Ast.Type t = symbols.lookupVar(lv.name.lexeme);
        if (t == null)
            throw error("Assign to undefined variable: " + lv.name.lexeme);

        Ast.Type current = t;

        for (Expr idx : lv.indices) {

            if (current.kind != Ast.Type.Kind.ARRAY)
                throw error("Indexing non-array variable: " + lv.name.lexeme);

            Ast.Type it = idx.accept(this);
            if (it.kind != Ast.Type.Kind.INT)
                throw error("Array index must be INT");

            current = current.inner;
        }

        return current;
    }

    // GRESKE

    private SemanticError error(String msg, Ast.TopItem node, Token location) {
        return new SemanticError(msg, node, location);
    }


    private SemanticError error(String msg) {
        return new SemanticError(msg, null, null);
    }
}
