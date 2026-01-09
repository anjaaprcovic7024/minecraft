package intermidiate;

import parser.Ast;
import parser.Expr;
import parser.Stmt;
import lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public final class CodeGenerator implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final List<String> instructions = new ArrayList<>();
    private int labelCounter = 0;

    public List<String> generate(Ast.Program program) {
        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.TopStmt topStmt) {
                topStmt.stmt.accept(this);
            } else if (item instanceof Ast.TopVarDecl topVar) {
                topVar.decl.accept(this);
            } else if (item instanceof Ast.FuncDef func) {
                // funkcije trenutno samo dodeljujemo labelu
                instructions.add("label " + func.name.lexeme);
                for (Stmt stmt : func.body) {
                    stmt.accept(this);
                }
            }
        }
        return instructions;
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    @Override
    public Void visitVarDecl(Stmt.VarDecl s) {
        for (int i = 0; i < s.names.size(); i++) {
            Expr value = s.values.size() > i ? s.values.get(i) : null;

            if (value != null) {
                value.accept(this);

                if (!(value instanceof Expr.Cast)) {
                    if (value.inferredType != null && !Ast.sameType(s.type, value.inferredType)) {
                        String targetType = switch (s.type.kind) {
                            case INT -> "int";
                            case LONG -> "long";
                            case DOUBLE -> "double";
                            case CHAR -> "char";
                            case BOOLEAN -> "bool";
                            case STRING -> "string";
                            default -> throw new IllegalStateException("Unsupported var type: " + s.type.kind);
                        };
                        instructions.add("cast " + targetType);
                    }
                }
            } else {
                instructions.add("push 0");
            }

            instructions.add("pop " + s.names.get(i).lexeme);
        }
        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return s) {
        if (s.expr != null) {
            s.expr.accept(this);
        } else {
            instructions.add("push 0");
        }
        instructions.add("ret"); // kraj funkcije
        return null;
    }

    @Override
    public Void visitAssign(Stmt.Assign s) {
        s.left.accept(this);
        s.lvalue.indices.forEach(idx -> idx.accept(this));
        instructions.add("pop " + s.lvalue.name.lexeme);
        return null;
    }

    @Override
    public Void visitCallStmt(Stmt.CallStmt s) {
        // push arg values
        for (Expr arg : s.call.args) {
            arg.accept(this);
        }
        instructions.add("call " + s.call.callee.lexeme);
        return null;
    }

    @Override
    public Void visitBeginIf(Stmt.BeginIf s) {
        String endLabel = newLabel();

        // prvi ifArm
        String nextLabel = s.orIfArms.isEmpty() && s.elseBlock == null ? endLabel : newLabel();
        s.ifArm.cond.accept(this);
        instructions.add("jmp_if_false " + nextLabel);

        for (Stmt stmt : s.ifArm.block) stmt.accept(this);
        if (!nextLabel.equals(endLabel)) {
            instructions.add("jmp " + endLabel);
            instructions.add("label " + nextLabel);
        }

        for (Stmt.BeginIf.Arm arm : s.orIfArms) {
            String armNext = (s.elseBlock == null && arm == s.orIfArms.get(s.orIfArms.size() - 1)) ? endLabel : newLabel();
            arm.cond.accept(this);
            instructions.add("jmp_if_false " + armNext);

            for (Stmt stmt : arm.block) stmt.accept(this);
            if (!armNext.equals(endLabel)) {
                instructions.add("jmp " + endLabel);
                instructions.add("label " + armNext);
            }
        }

        if (s.elseBlock != null) {
            for (Stmt stmt : s.elseBlock) stmt.accept(this);
        }

        instructions.add("label " + endLabel);
        return null;
    }

    @Override
    public Void visitBeginFor(Stmt.BeginFor s) {
        s.init.accept(this);

        String startLabel = newLabel();
        String endLabel = newLabel();

        instructions.add("label " + startLabel);

        // uslov petlje
        s.cond.accept(this);
        instructions.add("jmp_if_false " + endLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        if (s.update != null) s.update.accept(this);

        instructions.add("jmp " + startLabel);
        instructions.add("label " + endLabel);

        return null;
    }

    @Override
    public Void visitIncDec(Stmt.IncDec s) {
        instructions.add("push " + s.target.name.lexeme);
        instructions.add(s.op.type.name().toLowerCase());
        instructions.add("pop " + s.target.name.lexeme);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.WhileStmt s) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        instructions.add("label " + startLabel);

        s.cond.accept(this);
        instructions.add("jmp_if_false " + endLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        instructions.add("jmp " + startLabel);
        instructions.add("label " + endLabel);

        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhileStmt s) {
        String startLabel = newLabel();
        instructions.add("label " + startLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        s.cond.accept(this);
        instructions.add("jmp_if_true " + startLabel);

        return null;
    }

    @Override
    public Void visitExprStmt(Stmt.ExprStmt s) {
        s.expr.accept(this);
        return null;
    }

    @Override
    public Void visitArrayAssign(Stmt.ArrayAssign s) {
        s.target.indices.forEach(idx -> idx.accept(this));
        s.value.accept(this);
        instructions.add("store_array " + s.target.name.lexeme);
        return null;
    }

    // ================== EXPR VISITOR ==================

    @Override
    public Void visitArrayLiteral(Expr.ArrayLiteral e) {
        for (Expr el : e.elements) el.accept(this);
        instructions.add("make_array " + e.elements.size());
        return null;
    }

    @Override
    public Void visitIntLiteral(Expr.IntLiteral e) {
        instructions.add("push " + e.value);
        return null;
    }

    @Override
    public Void visitDoubleLiteral(Expr.DoubleLiteral e) {
        instructions.add("push " + e.value);
        return null;
    }

    @Override
    public Void visitLongLiteral(Expr.LongLiteral e) {
        instructions.add("push " + e.value);
        return null;
    }

    @Override
    public Void visitCharLiteral(Expr.CharLiteral e) {
        instructions.add("push '" + e.value + "'");
        return null;
    }

    @Override
    public Void visitStringLiteral(Expr.StringLiteral e) {
        instructions.add("push \"" + e.value + "\"");
        return null;
    }

    @Override
    public Void visitBooleanLiteral(Expr.BooleanLiteral e) {
        instructions.add("push " + e.value);
        return null;
    }

    public Void visitIdent(Expr.Ident e) {
        instructions.add("push " + e.name.lexeme);
        return null;
    }

    @Override
    public Void visitIndex(Expr.Index e) {
        e.indices.forEach(idx -> idx.accept(this));
        instructions.add("load_array " + e.name.lexeme);
        return null;
    }

    @Override
    public Void visitGrouping(Expr.Grouping e) {
        e.inner.accept(this);
        return null;
    }

    @Override
    public Void visitCall(Expr.Call e) {
        for (Expr arg : e.args) arg.accept(this);
        instructions.add("call " + e.callee.lexeme);
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary e) {
        e.left.accept(this);
        e.right.accept(this);
        instructions.add(switch (e.op.type) {
            case ADD -> "add";
            case SUBTRACT -> "sub";
            case MULTIPLY-> "mul";
            case DIVIDE -> "div";
            case PERCENT -> "mod";
            case CARET -> "pow";
            case LT -> "lt";
            case LE -> "le";
            case GT -> "gt";
            case GE -> "ge";
            case EQ -> "eq";
            case NEQ -> "neq";
            case AND -> "and";
            case OR -> "or";
            default -> throw new IllegalStateException("Unknown binary op: " + e.op.type);
        });
        return null;
    }

    @Override
    public Void visitUnary(Expr.Unary e) {
        e.right.accept(this);
        instructions.add(switch (e.op.type) {
            case SUBTRACT -> "neg";
            case NOT -> "not";
            default -> throw new IllegalStateException("Unknown unary op: " + e.op.type);
        });
        return null;
    }

    @Override
    public Void visitTernary(Expr.Ternary e) {
        String elseLabel = newLabel();
        String endLabel = newLabel();
        e.cond.accept(this);
        instructions.add("jmp_if_false " + elseLabel);
        e.thenExpr.accept(this);
        instructions.add("jmp " + endLabel);
        instructions.add("label " + elseLabel);
        e.elseExpr.accept(this);
        instructions.add("label " + endLabel);
        return null;
    }

    @Override
    public Void visitCast(Expr.Cast e) {
        e.expr.accept(this);

        String targetType = switch (e.type.kind) {
            case INT -> "int";
            case LONG -> "long";
            case DOUBLE -> "double";
            case CHAR -> "char";
            case BOOLEAN -> "bool";
            case STRING -> "string";
            default -> throw new IllegalStateException("Unsupported cast type: " + e.type.kind);
        };

        instructions.add("cast " + targetType); // nova instrukcija koja konvertuje vrednost na steku
        return null;
    }

}
