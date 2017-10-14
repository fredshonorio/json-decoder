package com.fredhonorio.json_decoder.schema;

import com.fredhonorio.json_decoder.Decoder;
import com.fredhonorio.json_decoder.Decoders;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import net.hamnaberg.json.Json;

import static com.fredhonorio.json_decoder.schema.JsonSchema.jsonSchema;

public abstract class Schema {

    public static class Field {
        public final String name;
        public final Schema value;
        public final boolean required;

        public Field(String name, Schema value, boolean required) {
            this.name = name;
            this.value = value;
            this.required = required;
        }
    }

    public static Schema singleField(String name, Schema schema, boolean required) {
     return new Object(List.of(new Field(name, schema, required)), List.empty());
    }

    public static Schema noKnownFields() {
     return new Object(List.empty(), List.empty());
    }

    public static Schema unnamedFields(Schema schema) {
        return new Object(List.empty(), List.of(schema));
    }

    public static final class Any extends Schema {
        public Any() {}
    }

    public static final class OneOf extends Schema {
        public final List<Schema> possibilities;

        public OneOf(List<Schema> possibilities) {
            this.possibilities = possibilities;
        }
    }

    public static final class Object extends Schema {

        public final List<Field> knownFields;
        public final List<Schema> unnamedFields;

        public Object(List<Field> knownFields, List<Schema> unnamedFields) {
            this.knownFields = knownFields;
            this.unnamedFields = unnamedFields;
        }
    }

    public static final class Array extends Schema {
        public final Schema inner;

        public Array(Schema inner) {
            this.inner = inner;
        }
    }

    public static final class Unknown extends Schema {
        public Unknown() { }
    }

    public static final class All extends Schema {
        public final List<Schema> all;

        public All(List<Schema> all) {
            this.all = all;
        }
    }

    public static final class Lit extends Schema {
        public static enum Type {
            NULL, INT, FLOAT, BOOL, STRING
        } // TODO: add match

        public final Type type;

        public Lit(Type type) {
            this.type = type;
        }
    }

    public static final class Enum extends Schema {
        public final List<Json.JValue> values;

        public Enum(List<Json.JValue> values) {
            this.values = values;
        }
    }

    public static Lit lit(Lit.Type type) {
        return new Lit(type);
    }

    public static void main(String[] args) {

        Schema sch = Decoders.list(Decoders.list(Decoders.String)).schema();

        System.out.println(jsonSchema(sch).spaces2());

        Schema s = Decoder.map2(
            Decoders.field("a", Decoders.list(Decoders.String)),
            Decoders.field("b", Decoders.String),
            Tuple::of
        ).schema();

        System.out.println(jsonSchema(s).spaces2());

        Schema s1 = Decoder.map2(
            Decoders.field("a", Decoders.field("b", Decoders.list(Decoders.String))),
            Decoder.map2(
                Decoders.field("b", Decoders.String),
                Decoders.field("c", Decoders.String),
                Tuple::of
            ),
            Tuple::of
        ).schema();

        System.out.println(jsonSchema(s1).spaces2());

        System.out.println(jsonSchema(Decoders.enumByName(Lit.Type.class).schema()).spaces2());

        System.out.println(jsonSchema(Decoders.enumByName(Lit.Type.class, x -> x.name().toLowerCase()).schema()).spaces2());

        System.out.println(jsonSchema(Decoders.at(List.of("a", "b", "c"), Decoders.Integer).schema()).spaces2());
    }
}
