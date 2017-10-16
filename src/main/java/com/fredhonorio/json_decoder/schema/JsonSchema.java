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
        Function<Schema.Union, T> oneOf,
        Function<Schema.Object, T> object,
        Function<Schema.Array, T> array,
        Function<Schema.Intersection, T> all,
        Function<Schema.Lit, T> lit,
        Function<Schema.Enum, T> enumeration,
        Function<Schema.Ref, T> ref,
        Function<Schema.Unknown, T> none
    ) {
        return
            s instanceof Schema.Any ? any.apply((Schema.Any) s) :
            s instanceof Schema.Union ? oneOf.apply((Schema.Union) s) :
            s instanceof Schema.Object ? object.apply((Schema.Object) s) :
            s instanceof Schema.Array ? array.apply((Schema.Array) s) :
            s instanceof Schema.Intersection ? all.apply((Schema.Intersection) s) :
            s instanceof Schema.Lit ? lit.apply((Schema.Lit) s) :
            s instanceof Schema.Enum ? enumeration.apply((Schema.Enum) s) :
            s instanceof Schema.Ref ? ref.apply((Schema.Ref) s) :
            s instanceof Schema.Unknown ? none.apply((Schema.Unknown) s) :
            fail("Match error " + s);
    }

    private static <T> T fail(String message) {
        throw new RuntimeException(message);
    }


    // how to handle references?
    // resolving intersection is useful for objects because declaring an object with n fields is modeled by 3 objects with
    // a single field, we don't need to resolve intersections for any other type
    public static Json.JObject jsonSchema(Schema schema) {
        return fold(schema,
            any -> Json.jEmptyObject(),
            union -> union.possibilities.size() == 1
                ? jsonSchema(union.possibilities.head())
                : Json.jObject("anyOf", Json.jArray(union.possibilities.map(JsonSchema::jsonSchema))),
            object -> object(object),
            array -> array(array),
            intersection -> intersection(intersection),
            lit -> lit(lit.type),
            enumeration -> Json.jObject("enum", Json.jArray(enumeration.values)),
            ref -> Json.jObject("$ref", ref.ref),
            unknown -> Json.jObject("json-decoder error", unknown.message)
        );
    }

    private static <T extends Schema> Option<T> as(Schema x, Class<T> clz) {
        return clz.isInstance(x) ? Option.some(clz.cast(x)) : Option.none();
    }

    private static Json.JObject array(Schema.Array array) {
        Json.JObject r = type("array");
        return as(array.inner, Schema.Any.class).isEmpty() // only restrict items if the item schema is not Any
            ? r.put("items", jsonSchema(array.inner))
            : r
        ;
    }


    // TODO: there are also other naive simplifications, like making intersection members distinct (we'd need equals though)
    // or we could go full monoidal and reduce intersections and unions recursively, also taking into account
    // number ranges list sizes, and so on
    private static Schema simplifyIntersection(Schema.Intersection i) {

        if (i.all.isEmpty()) {
            return i;
        }

        if (i.all.size() == 1) {
            return i.all.head();
        }

        List<Option<Schema.Object>> asObjects = flatten(i).map(s -> as(s, Schema.Object.class));

        if (asObjects.forAll(Option::isDefined)) {
            return asObjects.flatMap(Function.identity())
                .reduceLeft((a, b) -> new Schema.Object(a.knownFields.appendAll(b.knownFields), a.unnamedFields.appendAll(b.unnamedFields)));
        }

        return i;
    }

    private static Json.JObject intersection(Schema.Intersection i) {
        Schema simplified = simplifyIntersection(i); // we try to simplify the intersection (in case it's an interesction of 1, or intersection of multiple objects

        return as(simplified, Schema.Intersection.class)
            .map(inter ->
                Json.jObject("allOf", Json.jArray(inter.all.map(JsonSchema::jsonSchema))))
            .getOrElse(() -> jsonSchema(simplified));
    }

    private static List<Schema> flatten(Schema.Intersection xs) {
        List<Schema.Intersection> nested = xs.all.flatMap(x -> as(x, Schema.Intersection.class));

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
            : jsonSchema(new Schema.Union(object.unnamedFields));

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
