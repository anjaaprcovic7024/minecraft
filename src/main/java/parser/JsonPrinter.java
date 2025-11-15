package parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;


public final class JsonPrinter implements Expr.Visitor<JsonNode> {
    private static final ObjectMapper M = new ObjectMapper();

    public String print(Expr e) {
        try {
            JsonNode node = e.accept(this);
            return M.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public JsonNode visitLiteral(Expr.Literal e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literal"); ///ovo jer kod nas nije nuzno samo number

        Object v = e.value;

        if (v == null) {
            o.putNull("value");
        } else if (v instanceof Integer) {
            o.put("value", (Integer) v);
            o.put("valueType", "int");
        } else if (v instanceof Long) {
            o.put("value", (Long) v);
            o.put("valueType", "long");
        } else if (v instanceof Double) {
            o.put("value", (Double) v);
            o.put("valueType", "double");
        } else if (v instanceof Boolean) {
            o.put("value", (Boolean) v);
            o.put("valueType", "boolean");
        } else if (v instanceof Character) {
            o.put("value", v.toString());
            o.put("valueType", "char");
        } else {
            // String ili bilo šta drugo
            o.put("value", v.toString());
            o.put("valueType", "string");
        }

        return o;
    }

    @Override public JsonNode visitGrouping(Expr.Grouping e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "grouping");
        o.set("expression", e.expr.accept(this));
        return o;
    }

    @Override public JsonNode visitUnary(Expr.Unary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "unary");
        o.put("op", e.op.lexeme);
        o.set("operand", e.right.accept(this));
        return o;
    }

    @Override public JsonNode visitBinary(Expr.Binary e) {
        String op = e.op.lexeme;
        ObjectNode o = M.createObjectNode();
        if ("^".equals(op)) {
            o.put("type", "power");
            o.put("op", op);
            o.set("base",     e.left.accept(this));
            o.set("exponent", e.right.accept(this));
        } else {
            o.put("type", "binary");
            o.put("op", op);
            o.set("left",  e.left.accept(this));
            o.set("right", e.right.accept(this));
        }
        return o;
    }
}
