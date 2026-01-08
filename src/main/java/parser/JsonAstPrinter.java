package parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public final class JsonAstPrinter implements
        Expr.Visitor<JsonNode>,
        Stmt.Visitor<JsonNode> {

    private static final ObjectMapper M = new ObjectMapper();

    @Override
    public JsonNode visitUnary(Expr.Unary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "unary");
        o.put("op", e.op.lexeme);       // npr. "!"
        o.set("right", e.right.accept(this));
        return o;
    }

    @Override
    public JsonNode visitTernary(Expr.Ternary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "ternary");
        o.set("cond", e.cond.accept(this));
        o.set("thenExpr", e.thenExpr.accept(this));
        o.set("elseExpr", e.elseExpr.accept(this));
        return o;
    }

    public String print(Ast.Program p) {
        try {
            ObjectNode root = M.createObjectNode();
            root.put("type", "program");
            root.put("explicitProgram", p.explicitProgram);
            ArrayNode items = M.createArrayNode();
            for (Ast.TopItem it : p.items) items.add(printTopItem(it));
            root.set("items", items);
            return M.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode printTopItem(Ast.TopItem it) {
        if (it instanceof Ast.TopVarDecl v) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topVarDecl");
            o.set("decl", v.decl.accept(this));
            return o;
        }
        if (it instanceof Ast.FuncDef f) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "funcDef");
            o.put("name", f.name.lexeme);
            ObjectNode rt = M.createObjectNode();
            rt.put("base", f.returnType.kind == Ast.Type.Kind.INT ? f.returnType.baseType.lexeme : "implicit void");
            rt.put("rank", f.returnType.rank);
            o.set("returnType", rt);
            ArrayNode params = M.createArrayNode();
            for (Ast.Param p : f.params) {
                ObjectNode po = M.createObjectNode();
                po.put("name", p.name.lexeme);
                ObjectNode t = M.createObjectNode();
                t.put("base", p.type.baseType.lexeme);
                t.put("rank", p.type.rank);
                po.set("type", t);
                params.add(po);
            }
            o.set("params", params);
            ArrayNode body = M.createArrayNode();
            for (Stmt s : f.body) body.add(s.accept(this));
            o.set("body", body);
            return o;
        }
        if (it instanceof Ast.TopStmt ts) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topStmt");
            o.set("stmt", ts.stmt.accept(this));
            return o;
        }
        ObjectNode u = M.createObjectNode();
        u.put("kind", "unknownTopItem");
        return u;
    }

    // ===== Expr.Visitor =====

    @Override
    public JsonNode visitArrayLiteral(Expr.ArrayLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "array");
        ArrayNode elems = M.createArrayNode();
        for (Expr ex : e.elements) elems.add(ex.accept(this));
        o.set("elements", elems);
        return o;
    }


    @Override public JsonNode visitIdent(Expr.Ident e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "ident");
        o.put("name", e.name.lexeme);
        return o;
    }

    @Override public JsonNode visitIndex(Expr.Index e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "index");
        o.put("name", e.name.lexeme);
        ArrayNode idx = M.createArrayNode();
        for (Expr ex : e.indices) idx.add(ex.accept(this));
        o.set("indices", idx);
        return o;
    }

    @Override public JsonNode visitGrouping(Expr.Grouping e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "group");
        o.set("expr", e.inner.accept(this));
        return o;
    }

    @Override public JsonNode visitCall(Expr.Call e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "call");
        o.put("name", e.callee.lexeme);
        ArrayNode args = M.createArrayNode();
        for (Expr a : e.args) args.add(a.accept(this));
        o.set("args", args);
        return o;
    }

    @Override public JsonNode visitBinary(Expr.Binary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "binary");
        o.put("op", e.op.lexeme);
        o.set("left", e.left.accept(this));
        o.set("right", e.right.accept(this));
        return o;
    }

    // ===== Stmt.Visitor =====
    @Override public JsonNode visitVarDecl(Stmt.VarDecl s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "varDecl");
        ArrayNode dims = M.createArrayNode();
        for (Expr d : s.dims) dims.add(d.accept(this));
        o.set("dims", dims);

        ArrayNode names = M.createArrayNode();
        for (var t : s.names) names.add(t.lexeme);
        o.set("names", names);

        ArrayNode values = M.createArrayNode();
        for(Expr v: s.values) {
            if (v != null) values.add(v.accept(this));
            else values.add(M.nullNode()); // ili ArrayNode/NullNode po potrebi
        }

        o.set("values", values);

        return o;
    }

    @Override public JsonNode visitReturn(Stmt.Return s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "return");
        o.set("expr", s.expr.accept(this));
        return o;
    }

    @Override public JsonNode visitAssign(Stmt.Assign s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "assign");
        o.set("left", s.left.accept(this));
        ObjectNode lv = M.createObjectNode();
        lv.put("name", s.lvalue.name.lexeme);
        ArrayNode idx = M.createArrayNode();
        for (Expr e : s.lvalue.indices) idx.add(e.accept(this));
        lv.set("indices", idx);
        o.set("lvalue", lv);
        return o;
    }

    @Override public JsonNode visitCallStmt(Stmt.CallStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "call");
        o.set("call", s.call.accept(this));
        return o;
    }

    @Override public JsonNode visitBeginIf(Stmt.BeginIf s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "begin_if");
        ObjectNode first = M.createObjectNode();
        first.set("cond", s.ifArm.cond.accept(this));
        ArrayNode fbody = M.createArrayNode();
        for (Stmt st : s.ifArm.block) fbody.add(st.accept(this));
        first.set("block", fbody);
        o.set("if", first);

        ArrayNode orifs = M.createArrayNode();
        for (Stmt.BeginIf.Arm a : s.orIfArms) {
            ObjectNode ar = M.createObjectNode();
            ar.set("cond", a.cond.accept(this));
            ArrayNode bb = M.createArrayNode();
            for (Stmt st : a.block) bb.add(st.accept(this));
            ar.set("block", bb);
            orifs.add(ar);
        }
        o.set("or_if", orifs);

        if (s.elseBlock != null) {
            ArrayNode eb = M.createArrayNode();
            for (Stmt st : s.elseBlock) eb.add(st.accept(this));
            o.set("else", eb);
        }
        return o;
    }

    @Override public JsonNode visitBeginFor(Stmt.BeginFor s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "for");
        o.set("init", s.init.accept(this));
        o.set("cond", s.cond.accept(this));
        o.set("update", s.update.accept(this));
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) body.add(st.accept(this));
        o.set("body", body);
        return o;
    }




    @Override
    public JsonNode visitIncDec(Stmt.IncDec s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "incDec");
        ObjectNode target = M.createObjectNode();
        target.put("name", s.target.name.lexeme);
        ArrayNode idx = M.createArrayNode();
        for (Expr e : s.target.indices) idx.add(e.accept(this));
        target.set("indices", idx);
        o.set("target", target);
        o.put("op", s.op.lexeme);
        return o;
    }

    @Override
    public JsonNode visitWhileStmt(Stmt.WhileStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "while");
        o.set("cond", s.cond.accept(this));
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) body.add(st.accept(this));
        o.set("body", body);
        return o;
    }

    @Override
    public JsonNode visitDoWhileStmt(Stmt.DoWhileStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "doWhile");
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) body.add(st.accept(this));
        o.set("body", body);
        o.set("cond", s.cond.accept(this));
        return o;
    }

    @Override
    public JsonNode visitExprStmt(Stmt.ExprStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "expr");
        o.set("expr", s.expr.accept(this));
        return o;
    }



    @Override
    public JsonNode visitIntLiteral(Expr.IntLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "int");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitDoubleLiteral(Expr.DoubleLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "double");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitLongLiteral(Expr.LongLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "long");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitCharLiteral(Expr.CharLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "char");
        o.put("value", String.valueOf(e.value));
        return o;
    }

    @Override
    public JsonNode visitStringLiteral(Expr.StringLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "string");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitBooleanLiteral(Expr.BooleanLiteral e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "boolean");
        o.put("value", e.value);
        return o;
    }


}
