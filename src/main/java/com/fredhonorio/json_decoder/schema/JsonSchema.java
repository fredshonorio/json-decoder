package com.fredhonorio.json_decoder.schema;

import com.fredhonorio.json_decoder.Decoder;
import com.fredhonorio.json_decoder.Decoders;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import net.hamnaberg.json.Json;

// we're using show() temporarily, this will be an interpreted ADT later
public abstract class JsonSchema {

    private static Json.JObject type(String type) {
        return Json.jObject("type", type);
    }

    private static Json.JObject object(List<Field> fields) {
        List<Tuple2<String, Json.JValue>> props = fields
            .map(f -> Tuple.of(f.name, f.value.show()));

        return type("object")
            .put("properties", Json.jObject(props));
    }

    public abstract Json.JObject show();

    public static final class Any extends JsonSchema {

        public Any() {}

        @Override
        public Json.JObject show() {
            return Json.jEmptyObject();
        }
    }

    public static final class Object extends JsonSchema {

        @Override
        public Json.JObject show() {
            return JsonSchema.type("object");
        }
    }

    public static final class Array extends JsonSchema {
        public final JsonSchema inner;

        public Array(JsonSchema inner) {
            this.inner = inner;
        }

        @Override
        public Json.JObject show() {
            return type("array")
                .put("items", inner.show());
        }
    }

    public static final class None extends JsonSchema {
        public None() {
        }

        @Override
        public Json.JObject show() {
            return Json.jEmptyObject();
        }
    }

    public static final class Field extends JsonSchema {
        public final String name;
        public final JsonSchema value;
        public final boolean required;

        @Deprecated
        public Field(String name, JsonSchema value) {
            this.name = name;
            this.value = value;
            this.required = true;
        }

        public Field(String name, JsonSchema value, boolean required) {
            this.name = name;
            this.value = value;
            this.required = required;
        }

        @Override
        public Json.JObject show() {
            return object(List.of(this));
        }
    }

    public static final class All extends JsonSchema {
        public final List<JsonSchema> all;

        public All(List<JsonSchema> all) {
            this.all = all;
        }

                        /*
        Describes that this value matches all of these conditions
        Inside a flattened All, all items must be of the same type (a value can't be both an array and an int)
        We must then unify all the items of the same type do provide a single description
     */

        @Override
        public Json.JObject show() {
            // other implementations make sense, but probably the flattened children of an All should be of the same type, eg:
            // all(int(params1), int(params2), all(int(param3))?

            List<JsonSchema> items = flatten(this);

            // TODO: ensure non-empty

            Class<? extends JsonSchema> first = items.head().getClass();

            if (first.equals(Field.class)) {
                return object(items.map(e -> (Field) e));
            } else {
                throw new RuntimeException("!");
            }
        }

        private static List<JsonSchema> flatten(All all) {
            return all.all
                .flatMap(one -> one instanceof All ? flatten((All) one) : List.of(one));
        }

    }

    public static final class Lit extends JsonSchema {
        @Override
        public Json.JObject show() {
            switch (type) {
                case INT:
                    return JsonSchema.type("integer");
                case BOOL:
                    return type("boolean");
                case NULL:
                    return type("null");
                case NUMBER:
                    return type("number");
                case STRING:
                    return type("string");
                default:
                    throw new RuntimeException("!");
            }
        }

        public static enum Type {
            NULL, INT, NUMBER, BOOL, STRING
        } // TODO: add match

        public final Type type;

        public Lit(Type type) {
            this.type = type;
        }
    }

    public static final class Enum extends JsonSchema {
        public final JsonSchema type;
        public final List<Json.JValue> values;

        public Enum(JsonSchema type, List<Json.JValue> values) {
            this.type = type;
            this.values = values;
        }

        @Override
        public Json.JObject show() {
            return type.show()
                .put("enum", Json.jArray(values));
        }

    }

    public static Lit lit(Lit.Type type) {
        return new Lit(type);
    }

    public static void main(String[] args) {

        JsonSchema sch = Decoders.list(Decoders.list(Decoders.String)).schema();

        System.out.println(sch.show().spaces2());

        JsonSchema s = Decoder.map2(
            Decoders.field("a", Decoders.list(Decoders.String)),
            Decoders.field("b", Decoders.String),
            Tuple::of
        ).schema();

        System.out.println(s.show().spaces2());

        JsonSchema s1 = Decoder.map2(
            Decoders.field("a", Decoders.field("b", Decoders.list(Decoders.String))),
            Decoder.map2(
                Decoders.field("b", Decoders.String),
                Decoders.field("c", Decoders.String),
                Tuple::of
            ),
            Tuple::of
        ).schema();

        System.out.println(s1.show().spaces2());


        System.out.println(Decoders.enumByName(Lit.Type.class).schema().show().spaces2());

        System.out.println(Decoders.enumByName(Lit.Type.class, x -> x.name().toLowerCase()).schema().show().spaces2());

        System.out.println(Decoders.equal("hey").schema().show().spaces2());


        System.out.println(Decoders.at(List.of("a", "b", "c"), Decoders.Integer).schema().show().spaces2());




    }
}
