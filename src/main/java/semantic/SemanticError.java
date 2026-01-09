package semantic;

import lexer.token.Token;
import parser.Ast;

public final class SemanticError extends RuntimeException {

    private final String message;
    private final Ast.TopItem node;
    private final Token token;

    public SemanticError(String message, Ast.TopItem node, Token token) {
        super(buildMessage(message, node, token));
        this.message = message;
        this.node = node;
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    private static String buildMessage(String msg, Ast.TopItem node, Token token) {
        StringBuilder sb = new StringBuilder();
        sb.append("Semantic error: ").append(msg);

        if (node != null) {
            sb.append(" [AST node: ").append(node.getClass().getSimpleName()).append("]");
        }

        if (token != null) {
            sb.append(" [at line ").append(token.line)
                    .append(", column ").append(token.colStart).append("]");
        }

        return sb.toString();
    }
}
