package intermidiate;

import parser.Ast;
import parser.Expr;
import parser.Stmt;
import lexer.token.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CodeGenerator implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final List<String> instructions = new ArrayList<>();
    private int labelCounter = 0;
    private final Map<String, Integer> labelPositions = new HashMap<>();
    private final List<String> unresolvedLabels = new ArrayList<>();

    public List<String> generate(Ast.Program program) {
        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.TopStmt topStmt) {
                topStmt.stmt.accept(this);
            } else if (item instanceof Ast.TopVarDecl topVar) {
                topVar.decl.accept(this);
            } else if (item instanceof Ast.FuncDef func) {
                addInstruction("label " + func.name.lexeme);
                for (Stmt stmt : func.body) {
                    stmt.accept(this);
                }
            }
        }
        return resolveLabels();
    }

    private void addInstruction(String instr) {
        if (instr.startsWith("label ")) {
            String labelName = instr.substring(6);
            labelPositions.put(labelName, instructions.size());
        }
        instructions.add(instr);
    }

    private List<String> resolveLabels() {
        List<String> resolved = new ArrayList<>();

        for (String instr : instructions) {
            if (instr.startsWith("label ")) {
                String labelName = instr.substring(6);
                resolved.add("label " + labelName);
            } else if (instr.startsWith("jmp ")) {
                String target = instr.substring(4);
                if (labelPositions.containsKey(target)) {
                    resolved.add("jmp " + labelPositions.get(target));
                } else {
                    resolved.add(instr);
                }
            } else if (instr.startsWith("jmp_if_false ")) {
                String target = instr.substring(13);
                if (labelPositions.containsKey(target)) {
                    resolved.add("jmp_if_false " + labelPositions.get(target));
                } else {
                    resolved.add(instr);
                }
            } else if (instr.startsWith("jmp_if_true ")) {
                String target = instr.substring(12);
                if (labelPositions.containsKey(target)) {
                    resolved.add("jmp_if_true " + labelPositions.get(target));
                } else {
                    resolved.add(instr);
                }
            } else if (instr.startsWith("call ")) {
                String target = instr.substring(5);
                if (labelPositions.containsKey(target)) {
                    resolved.add("call " + labelPositions.get(target));
                } else {
                    resolved.add(instr);
                }
            } else {
                resolved.add(instr);
            }
        }

        return resolved;
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
                        addInstruction("cast " + targetType);
                    }
                }
            } else {
                addInstruction("push 0");
            }

            addInstruction("pop " + s.names.get(i).lexeme);
        }
        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return s) {
        if (s.expr != null) {
            s.expr.accept(this);
        } else {
            addInstruction("push 0");
        }
        addInstruction("ret");
        return null;
    }

    @Override
    public Void visitAssign(Stmt.Assign s) {
        s.left.accept(this);
        s.lvalue.indices.forEach(idx -> idx.accept(this));
        addInstruction("pop " + s.lvalue.name.lexeme);
        return null;
    }

    @Override
    public Void visitCallStmt(Stmt.CallStmt s) {
        for (Expr arg : s.call.args) {
            arg.accept(this);
        }
        addInstruction("call " + s.call.callee.lexeme);
        return null;
    }

    @Override
    public Void visitBeginIf(Stmt.BeginIf s) {
        String endLabel = newLabel();

        // prvi ifArm
        String nextLabel = s.orIfArms.isEmpty() && s.elseBlock == null ? endLabel : newLabel();
        s.ifArm.cond.accept(this);
        addInstruction("jmp_if_false " + nextLabel);

        for (Stmt stmt : s.ifArm.block) stmt.accept(this);
        if (!nextLabel.equals(endLabel)) {
            addInstruction("jmp " + endLabel);
        }
        if (!nextLabel.equals(endLabel)) {
            addInstruction("label " + nextLabel);
        }

        for (Stmt.BeginIf.Arm arm : s.orIfArms) {
            String armNext = (s.elseBlock == null && arm == s.orIfArms.get(s.orIfArms.size() - 1)) ? endLabel : newLabel();
            arm.cond.accept(this);
            addInstruction("jmp_if_false " + armNext);

            for (Stmt stmt : arm.block) stmt.accept(this);
            if (!armNext.equals(endLabel)) {
                addInstruction("jmp " + endLabel);
            }
            if (!armNext.equals(endLabel)) {
                addInstruction("label " + armNext);
            }
        }

        if (s.elseBlock != null) {
            for (Stmt stmt : s.elseBlock) stmt.accept(this);
        }

        addInstruction("label " + endLabel);
        return null;
    }

    @Override
    public Void visitBeginFor(Stmt.BeginFor s) {
        s.init.accept(this);

        String startLabel = newLabel();
        String endLabel = newLabel();

        addInstruction("label " + startLabel);

        s.cond.accept(this);
        addInstruction("jmp_if_false " + endLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        if (s.update != null) s.update.accept(this);

        addInstruction("jmp " + startLabel);
        addInstruction("label " + endLabel);

        return null;
    }

    @Override
    public Void visitIncDec(Stmt.IncDec s) {
        addInstruction("push " + s.target.name.lexeme);
        addInstruction(s.op.type.name().toLowerCase());
        addInstruction("pop " + s.target.name.lexeme);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.WhileStmt s) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        addInstruction("label " + startLabel);

        s.cond.accept(this);
        addInstruction("jmp_if_false " + endLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        addInstruction("jmp " + startLabel);
        addInstruction("label " + endLabel);

        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhileStmt s) {
        String startLabel = newLabel();
        addInstruction("label " + startLabel);

        for (Stmt stmt : s.body) stmt.accept(this);

        s.cond.accept(this);
        addInstruction("jmp_if_true " + startLabel);

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
        addInstruction("store_array " + s.target.name.lexeme);
        return null;
    }

    // ================== EXPR VISITOR ==================

    @Override
    public Void visitArrayLiteral(Expr.ArrayLiteral e) {
        for (Expr el : e.elements) el.accept(this);
        addInstruction("make_array " + e.elements.size());
        return null;
    }

    @Override
    public Void visitIntLiteral(Expr.IntLiteral e) {
        addInstruction("push " + e.value);
        return null;
    }

    @Override
    public Void visitDoubleLiteral(Expr.DoubleLiteral e) {
        addInstruction("push " + e.value);
        return null;
    }

    @Override
    public Void visitLongLiteral(Expr.LongLiteral e) {
        addInstruction("push " + e.value);
        return null;
    }

    @Override
    public Void visitCharLiteral(Expr.CharLiteral e) {
        addInstruction("push '" + e.value + "'");
        return null;
    }

    @Override
    public Void visitStringLiteral(Expr.StringLiteral e) {
        addInstruction("push \"" + e.value + "\"");
        return null;
    }

    @Override
    public Void visitBooleanLiteral(Expr.BooleanLiteral e) {
        addInstruction("push " + e.value);
        return null;
    }

    public Void visitIdent(Expr.Ident e) {
        addInstruction("push " + e.name.lexeme);
        return null;
    }

    @Override
    public Void visitIndex(Expr.Index e) {
        e.indices.forEach(idx -> idx.accept(this));
        addInstruction("load_array " + e.name.lexeme);
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
        addInstruction("call " + e.callee.lexeme);
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary e) {
        e.left.accept(this);
        e.right.accept(this);
        addInstruction(switch (e.op.type) {
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
        addInstruction(switch (e.op.type) {
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
        addInstruction("jmp_if_false " + elseLabel);
        e.thenExpr.accept(this);
        addInstruction("jmp " + endLabel);
        addInstruction("label " + elseLabel);
        e.elseExpr.accept(this);
        addInstruction("label " + endLabel);
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

        addInstruction("cast " + targetType);
        return null;
    }
}