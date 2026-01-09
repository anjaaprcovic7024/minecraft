package semantic;

import parser.Ast;
import lexer.token.Token;

public final class SemanticError extends RuntimeException {
    public final String message;     // opis greske
    public final Ast.TopItem node;   // AST cvor gde se greska desila (moze biti null)
    public final Token location;     // token koji daje liniju/kolonu (moze biti null)

    public SemanticError(String message, Ast.TopItem node, Token location) {
        super(message);  // poziva RuntimeException
        this.message = message;
        this.node = node;
        this.location = location;
    }

    public SemanticError(String message, Token location) {
        this(message, null, location);
    }

    public SemanticError(String message) {
        this(message, null, null);
    }

    @Override
    public String toString() {
        String locStr = "";
        if (location != null) {
            locStr = " at line " + location.line + ", col " + location.colStart;
        }
        return "Semantic error" + locStr + ": " + message;
    }
}
