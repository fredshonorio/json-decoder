package com.fredhonorio.json_decoder.schema;

import com.fredhonorio.json_decoder.Decoder;
import com.fredhonorio.json_decoder.Decoders;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.control.Option;
import net.hamnaberg.json.Json;

public class JsonSchema {

    public static final class Array extends JsonSchema {
        public final JsonSchema inner;

        public Array(JsonSchema inner) {
            this.inner = inner;
        }
    }

    public static final class None extends JsonSchema {
        public None() {}
    }

    public static final class Field extends JsonSchema {
        public final String name;
        public final JsonSchema value;

        public Field(String name, JsonSchema value) {
            this.name = name;
            this.value = value;
        }
    }

    public static final class All extends JsonSchema {
        public final List<JsonSchema> all;

        public All(List<JsonSchema> all) {
            this.all = all;
        }
    }

    public static final class Lit extends JsonSchema {
        public static enum Type {
            NULL, INT, NUMBER, BOOL, STRING
        } // TODO: add match

        public final Type type;

        public Lit(Type type) {
            this.type = type;
        }
    }


    public static Json.JObject interpret(JsonSchema f) {
        if (f instanceof Array) {
            return array((Array) f);
        } else if (f instanceof Lit) {
            return lit((Lit) f);
        } else if (f instanceof All) {
            return all((All) f);
        } else {
            throw new RuntimeException("!");
        }
    }

    private static Json.JObject all(All all) {
        // other implementations make sense, but probably the flattened children of an All should be of the same type, eg:
        // all(int(params1), int(params2), all(int(param3))?

        List<Field> fields = all.all.filter(e -> e instanceof Field)
            .map(e -> (Field) e);

        List<Tuple2<String, Json.JValue>> props = fields
            .map(f -> Tuple.of(f.name, interpret(f.value)));

        return type("object")
            .put("properties", Json.jObject(props));
    }

    private static Json.JObject array(Array arr) {
        return type("array")
            .put("items", interpret(arr.inner));
    }

    private static Json.JObject lit(Lit lit) {
        switch (lit.type) {
            case INT: return type("integer");
            case BOOL: return type("boolean");
            case NULL: return type("null");
            case NUMBER: return type("number");
            case STRING: return type("string");
            default:
                throw new RuntimeException("!");
        }
    }

    private static Json.JObject type(String type) {
        return Json.jObject("type", type);
    }

    public static void main(String[] args) {

        JsonSchema sch = Decoders.list(Decoders.list(Decoders.String)).schema();

        System.out.println(interpret(sch).spaces2());

        JsonSchema s = Decoder.map2(
            Decoders.field("a", Decoders.list(Decoders.String)),
            Decoders.field("b", Decoders.String),
            Tuple::of
        ).schema();

        System.out.println(interpret(s).spaces2());
    }
}
