package semantic;

import parser.Ast;

public final class Symbol {

    public enum Kind {
        VARIABLE, FUNCTION, PARAMETER, CLASS
    }

    public final String name;
    public final Kind kind;
    public final Ast.Type type;
    public final Ast.FuncDef funcDef; // samo za funkcije

    // za varijable, parametre, klase
    public Symbol(String name, Kind kind, Ast.Type type) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.funcDef = null;
    }

    // za funkcije
    public Symbol(String name, Kind kind, Ast.Type type, Ast.FuncDef funcDef) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.funcDef = funcDef;
    }
}
