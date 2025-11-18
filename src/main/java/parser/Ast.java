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
        public enum Kind { BOOLEAN, INT, DOUBLE, LONG, CHAR, STRING, ARRAY, VOID }
        public final Kind kind;
        public final Token baseType; // token koji predstavlja tip
        public final int rank; // broj [] zagrada
        public Type(Kind kind, Token baseType, int rank) {
            this.kind = kind;
            this.baseType = baseType;
            this.rank = rank;
        }
    }



}

