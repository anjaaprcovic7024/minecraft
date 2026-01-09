package semantic;

import lexer.token.Token;
import lexer.token.TokenType;
import parser.Ast;
import parser.Stmt;

import java.util.*;

public final class SymbolTable {

    private final Map<String, Symbol> symbols;
    private final SymbolTable parent;

    // === KONSTRUKTORI ===
    public SymbolTable() {
        this.parent = null;
        this.symbols = new HashMap<>(); // globalni scope

        // prazno telo funkcije
        List<Stmt> emptyBody = new ArrayList<>();

        // collect(x: any)
        List<Ast.Param> collectParams = new ArrayList<>();
        collectParams.add(new Ast.Param(
                new Token(TokenType.IDENTIFICATOR, "x", 0, 0, 0, 0),
                new Ast.Type(Ast.Type.Kind.ANY, null, 0) // ANY tip
        ));
        defineFunc("collect", new Ast.FuncDef(
                new Token(TokenType.IDENTIFICATOR, "collect", 0, 0, 0, 0),
                collectParams,
                new Ast.Type(Ast.Type.Kind.VOID, null, 0),
                emptyBody
        ));

        List<Ast.Param> dropParams = new ArrayList<>();
        dropParams.add(new Ast.Param(
                new Token(TokenType.IDENTIFICATOR, "x", 0, 0, 0, 0),
                new Ast.Type(Ast.Type.Kind.ANY, null, 0)
        ));
        defineFunc("drop", new Ast.FuncDef(
                new Token(TokenType.IDENTIFICATOR, "drop", 0, 0, 0, 0),
                dropParams,
                new Ast.Type(Ast.Type.Kind.VOID, null, 0),
                emptyBody
        ));
    }

    private SymbolTable(SymbolTable parent) {
        this.parent = parent;
        this.symbols = new HashMap<>(); // svaki child scope dobija sopstvenu praznu mapu
    }

    /* ================= SCOPES ================= */
    public SymbolTable enterScope() {
        return new SymbolTable(this);  // vraca novi child scope sa praznom mapom
    }

    public SymbolTable exitScope() {
        return this.parent; // vraca parent scope
    }

    /* ================= DEFINICIJE ================= */
    public boolean defineVar(String name, Ast.Type type) {
        return define(new Symbol(name, Symbol.Kind.VARIABLE, type));
    }

    public boolean defineParam(String name, Ast.Type type) {
        return define(new Symbol(name, Symbol.Kind.PARAMETER, type));
    }

    public boolean defineFunc(String name, Ast.FuncDef func) {
        return define(new Symbol(name, Symbol.Kind.FUNCTION, func.returnType, func));
    }

    public boolean defineClass(String name, Ast.Type type) {
        return define(new Symbol(name, Symbol.Kind.CLASS, type));
    }

    private boolean define(Symbol sym) {
        if (symbols.containsKey(sym.name)) return false;
        symbols.put(sym.name, sym);
        return true;
    }

    /* ================= PRETRAGA ================= */
    public Ast.Type lookupVar(String name) {
        Symbol s = resolve(name);
        if (s != null && (s.kind == Symbol.Kind.VARIABLE || s.kind == Symbol.Kind.PARAMETER))
            return s.type;
        return null;
    }

    public Ast.FuncDef lookupFunc(String name) {
        Symbol s = resolve(name);
        if (s != null && s.kind == Symbol.Kind.FUNCTION)
            return s.funcDef;
        return null;
    }

    private Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if (s != null) return s;
        if (parent != null) return parent.resolve(name);
        return null;
    }

    public boolean isDefinedLocally(String name) {
        // proverava samo trenutno mapu (trenutni scope), ne parent
        return symbols.containsKey(name);
    }
}
