package parser;

import lexer.token.Token;

import java.util.List;

public final class Ast {

    public static final class Program {
        public final boolean explicitProgram; // BEGIN PROGRAM ... END PROGRAM
        public final List<TopItem> items;
        public Program(boolean explicitProgram, List<TopItem> items) {
            this.explicitProgram = explicitProgram;
            this.items = items;
        }
    }

    public interface TopItem {}

    public static final class TopVarDecl implements TopItem {
        public final Stmt.VarDecl decl;
        public TopVarDecl(Stmt.VarDecl decl) { this.decl = decl; }
    }

    public static final class TopStmt implements TopItem {
        public final Stmt stmt;
        public TopStmt(Stmt stmt) { this.stmt = stmt; }
    }

    public static final class FuncDef implements TopItem {
        public final Token name;
        public final List<Param> params;
        public final Type returnType;
        public final List<Stmt> body;
        public FuncDef(Token name, List<Param> params, Type returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }
    }

    public static final class ClassDef implements TopItem {
        public final Token name;
        public final Token extendsName; // moze biti null
        public final List<TopItem> body;
        public ClassDef(Token name, Token extendsName, List<TopItem> body) {
            this.name = name;
            this.extendsName = extendsName;
            this.body = body;
        }
    }

    public static final class Param {
        public final Token name;
        public final Type type;
        public Param(Token name, Type type) { this.name = name; this.type = type; }
    }

    public static final class Type {
        public enum Kind { BOOLEAN, INT, DOUBLE, LONG, CHAR, STRING, ARRAY, VOID, ANY }

        public final Kind kind;
        public final Token baseType;
        public final int rank;
        public Type inner;

        public Type(Kind kind, Token baseType, int rank) {
            this.kind = kind;
            this.baseType = baseType;
            this.rank = rank;
            this.inner = null;
        }

        public boolean isArray() {
            return kind == Kind.ARRAY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type other)) return false;

            if (kind != other.kind) return false;
            if (rank != other.rank) return false;

            if (kind == Kind.ARRAY) {
                return inner != null && inner.equals(other.inner);
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = kind.hashCode();
            result = 31 * result + rank;
            if (kind == Kind.ARRAY && inner != null) {
                result = 31 * result + inner.hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            if (kind == Kind.ARRAY && inner != null) {
                return "ARRAY[" + inner.toString() + "]";
            }
            return kind.name();
        }

    }


    public static boolean isNumeric(Ast.Type t) {
        return t.kind == Ast.Type.Kind.INT
                || t.kind == Ast.Type.Kind.DOUBLE
                || t.kind == Ast.Type.Kind.LONG;
    }

    public static boolean isBoolean(Ast.Type t) {
        return t.kind == Ast.Type.Kind.BOOLEAN;
    }

    public static boolean sameType(Ast.Type a, Ast.Type b) {
        if (a.kind != b.kind) return false;
        if (a.rank != b.rank) return false;
        if (a.kind == Type.Kind.ARRAY) {
            if (a.inner == null || b.inner == null) return false;
            return sameType(a.inner, b.inner);
        }
        return true;
    }





}

