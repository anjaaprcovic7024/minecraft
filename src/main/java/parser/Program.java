package parser;

import java.util.List;

public final class Program {
    public final java.util.List<Stmt> statements;

    public Program(List<Stmt> statements) {
        this.statements = statements;
    }
}