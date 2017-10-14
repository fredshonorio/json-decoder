package com.fredhonorio.json_decoder.schema;

import javaslang.Function2;
import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.control.Option;
import net.hamnaberg.json.Json;

import java.util.function.Function;

public class JsonSchema {

    private static <T> T fold(
        Schema s,
        Function<Schema.Any, T> any,
        Function<Schema.OneOf, T> oneOf,
        Function<Schema.Object, T> object,
        Function<Schema.Array, T> array,
        Function<Schema.All, T> all,
        Function<Schema.Lit, T> lit,
        Function<Schema.Enum, T> enumeration,
        Function<Schema.Unknown, T> none) {
        return
            s instanceof Schema.Any ? any.apply((Schema.Any) s) :
            s instanceof Schema.OneOf ? oneOf.apply((Schema.OneOf) s) :
            s instanceof Schema.Object ? object.apply((Schema.Object) s) :
            s instanceof Schema.Array ? array.apply((Schema.Array) s) :
            s instanceof Schema.All ? all.apply((Schema.All) s) :
            s instanceof Schema.Lit ? lit.apply((Schema.Lit) s) :
            s instanceof Schema.Enum ? enumeration.apply((Schema.Enum) s) :
            s instanceof Schema.Unknown ? none.apply((Schema.Unknown) s) :
            fail("Match error " + s);
    }

    private static <T> T fail(String message) {
        throw new RuntimeException(message);
    }

    // how to handle references?
    public static Json.JObject jsonSchema(Schema schema) {
        return fold(schema,
            any -> Json.jEmptyObject(),
            oneOf -> Json.jObject("anyOf", Json.jArray(oneOf.possibilities.map(JsonSchema::jsonSchema))),
            object -> object(object),
            array -> type("array").put("items", jsonSchema(array.inner)),
            all -> jsonSchema(flatten(all).foldLeft((Schema) new Schema.Any(), JsonSchema::both)),
            lit -> lit(lit.type),
            enumeration -> Json.jObject("enum", Json.jArray(enumeration.values)),
            unknown -> Json.jObject("nooooooooooo", "nooooooooooo")
        );
    }

    private static <T extends Schema> Option<T> as(Schema x, Class<T> clz) {
        return clz.isInstance(x) ? Option.some(clz.cast(x)) : Option.none();
    }

    // how to
    private static Schema both(Schema a, Schema b) {
        return fold(a,
            any -> b,
            oneOf -> as(b, Schema.OneOf.class).<Schema>map(other -> new Schema.OneOf(oneOf.possibilities.appendAll(other.possibilities))).getOrElse(b),
            object -> as(b, Schema.Object.class).<Schema>map(other -> new Schema.Object(object.knownFields.appendAll(other.knownFields), object.unnamedFields.appendAll(other.unnamedFields))).getOrElse(b),
            array -> b, // TODO
            all -> as(b, Schema.All.class).<Schema>map(other -> new Schema.All(all.all.appendAll(other.all))).getOrElse(b),
            lit -> b, // TODO
            enumerable -> b, // TODO
            unknown -> new Schema.OneOf(List.of(unknown, b))
        );
    }

    private static List<Schema> flatten(Schema.All xs) {
        System.out.println(xs + ", " + xs.all);

        List<Schema.All> nested = xs.all.flatMap(x -> as(x, Schema.All.class));

        return xs.all.removeAll(nested)
            .appendAll(nested.flatMap(JsonSchema::flatten));
    }

    private static Json.JObject lit(Schema.Lit.Type type) {
            switch (type) {
                case INT:    return type("integer");
                case BOOL:   return type("boolean");
                case NULL:   return type("null");
                case FLOAT:  return type("number");
                case STRING: return type("string");
                default:     throw new RuntimeException("!");
            }
    }

    private static Json.JObject object(Schema.Object object) {

        Json.JObject properties = object.knownFields
            .toMap(f -> Tuple.<String, Json.JValue>of(f.name, jsonSchema(f.value)))
            .transform(Json::jObject);

        Json.JArray requiredFields = object.knownFields
            .filter(f -> f.required)
            .map(f -> f.name)
            .<Json.JValue>map(Json::jString)
            .transform(Json::jArray);

        Json.JValue additionalProps = object.unnamedFields.isEmpty()
            ? Json.jBoolean(true) // additional props by default
            : jsonSchema(new Schema.OneOf(object.unnamedFields));

        return With.of(type("object"))
            .doWhen(!properties.isEmpty(),
                Json.JObject::concat,
                Json.jObject("properties", properties))
            .doWhen(requiredFields.size() > 0,
                Json.JObject::concat,
                Json.jObject("required", requiredFields))
            .get()
            .put("additionalProperties", additionalProps);
    }

    private static class With<T> {
        private final T v;

        public With(T v) {
            this.v = v;
        }

        public <U> With<T> doWhen(Option<U> o, Function2<T, U, T> apply) {
            return new With<>(o.map(apply.apply(v)).getOrElse(v));
        }

        public <U> With<T> doWhen(boolean check, Function2<T, U, T> apply, U u) {
            return doWhen(Option.when(check, u), apply);
        }

        public T get() {
            return v;
        }

        public static <T> With<T> of(T v) {
            return new With<>(v);
        }
    }

    private static Json.JObject type(String type) {
        return Json.jObject("type", type);
    }
}
