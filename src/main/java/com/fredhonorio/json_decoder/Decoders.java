package com.fredhonorio.json_decoder;

import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.control.Either;
import javaslang.control.Option;
import net.hamnaberg.json.Json;
import net.hamnaberg.json.io.JacksonStreamingParser;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;

import static javaslang.control.Either.left;
import static javaslang.control.Either.right;
import static com.fredhonorio.json_decoder.EitherExtra.*;

@SuppressWarnings({"WeakerAccess", "unused"})
/**
 * Contains basic decoders and combinators.
 */
public final class Decoders {
    private Decoders() {
    }

    public static final Decoder<Json.JValue> Value = Either::right;
    public static final Decoder<Json.JObject> JObject = v -> is(v, Json.JValue::isObject, Json.JValue::asJsonObject, "JObject");
    public static final Decoder<Json.JArray> JArray = v -> is(v, Json.JValue::isArray, Json.JValue::asJsonArray, "JArray");
    public static final Decoder<Json.JNull> JNull = v -> is(v, Json.JValue::isNull, Json.JValue::asJsonNull, "JNull");
    public static final Decoder<String> String = v -> is(v, Json.JValue::isString, Json.JValue::asString, "String");
    public static final Decoder<BigDecimal> BigDecimal = v -> is(v, Json.JValue::isNumber, Json.JValue::asBigDecimal, "BigDecimal");
    public static final Decoder<Boolean> Boolean = v -> is(v, Json.JValue::isBoolean, Json.JValue::asBoolean, "Boolean");

    public static final Decoder<Float> Float = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::floatValue));
    public static final Decoder<Double> Double = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::doubleValue));
    public static final Decoder<Integer> Integer = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::intValueExact));
    public static final Decoder<Long> Long = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::longValueExact));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and applies the given decoder to the members.
     *
     * @param inner The decoder for the members
     * @param <T>
     * @return
     */
    public static <T> Decoder<Seq<T>> list(Decoder<T> inner) {
        return val -> JArray.apply(val)
            .flatMap(arr -> sequence(arr.mapToList(inner::apply)).mapLeft(err -> "array element: " + err));
    }

    /**
     * Attempts to use the given decoder, but doesn't fail if it does.
     *
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<Option<T>> option(Decoder<T> inner) {
        return val -> inner.apply(val)
            .map(Option::of)
            .orElse(right(Option.none()));
    }

    /**
     * Attempts to pick a field from an {@link net.hamnaberg.json.Json.JObject} and applies a given decoder.
     * Succeeds if the field does not exist but still fails if the inner decoder fails.
     *
     * @param key   The name of the field
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<Option<T>> optionalField(String key, Decoder<T> inner) {
        return root -> JObject.apply(root)
            .flatMap(r ->
                r.get(key)
                    .map(val -> inner.apply(val).map(Option::some).mapLeft(err -> "field '" + key + "': " + err))
                    .getOrElse(right(Option.none())));
    }

    /**
     * Allows a value to be `null`.
     *
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<Option<T>> nullable(Decoder<T> inner) {
        return val -> JNull.apply(val)
            .map(n -> Option.<T>none())
            .orElse(() -> inner.apply(val).map(Option::some));
    }

    /**
     * Returns a given value if the {@link net.hamnaberg.json.Json.JValue} is null.
     *
     * @param defaultValue
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> nullValue(T defaultValue) {
        return val -> JNull.apply(val)
            .map(n -> defaultValue);
    }

    /**
     * Picks a field from a {@link net.hamnaberg.json.Json.JObject }and applies a given decoder. Fails if the
     * field doesn't exist.
     *
     * @param key The name of the field
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> field(String key, Decoder<T> inner) {
        return root -> JObject.apply(root)
            .flatMap(val -> ofOption(val.get(key), "field '" + key + "': missing"))
            .flatMap(val -> inner.apply(val).mapLeft(err -> "field '" + key + "': " + err));
    }

    /**
     * @see #oneOf(Seq)
     * @param decoders
     * @param <T>
     * @return
     */
    @SafeVarargs
    public static <T> Decoder<T> oneOf(Decoder<T>... decoders) {
        return oneOf(List.of(decoders));
    }

    /**
     * Attempts the given decoders until one succeeds.
     *
     * @param decoders
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> oneOf(Seq<Decoder<T>> decoders) {
        return obj -> decoders.foldLeft(
            left("no decoders given"),
            (z, x) -> z.orElse(x.apply(obj))
        );
    }

    /**
     * Succeeds with a given value.
     * @param value
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> succeed(T value) {
        return obj -> Either.right(value);
    }

    /**
     * Fails with a given error.
     * @param error
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> fail(String error) {
        return obj -> Either.left(error);
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JObject} and applies a given decoder to each of the values, resulting in
     * a dictionary.
     * @param valueDecoder
     * @param <T>
     * @return
     */
    public static <T> Decoder<Map<String, T>> dict(Decoder<T> valueDecoder) {
        return root -> JObject.apply(root)
            .flatMap(r ->
                r.mapToList((k, v) -> valueDecoder.apply(v)
                    .map(decV -> Tuple.of(k, decV))
                    .mapLeft(err -> "dict key '" + k + "': " + err))
                    .transform(EitherExtra::sequence))
            .map(HashMap::ofEntries);
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and accesses a position.
     * @param index
     * @return
     */
    public static Decoder<Json.JValue> index(int index) {
        return JArray
            .andThen(arr -> arr.get(index)
                .map(Decoders::succeed)
                .getOrElse(fail("no element at index " + index)));
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and accesses a position and applies a given decoder to the
     * array element.
     * @param index
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> index(int index, Decoder<T> inner) {
        return index(index).andThen(item -> fromResult(inner.apply(item)));
    }

    /**
     * Traverses an object tree and applies a decoder at the leaf.
     * @param fields
     * @param inner
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> at(Seq<String> fields, Decoder<T> inner) {
        return fields.foldRight(inner, Decoders::field);
    }

    /**
     * Always returns a given {@code Either<String, T>}.
     * @param result
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> fromResult(Either<String, T> result) {
        return t -> result;
    }

    /**
     * Decodes a json string with a given decoder, uses Jackson.
     * @param json
     * @param decoder
     * @param <T>
     * @return
     */
    public static <T> Either<String, T> decodeString(String json, Decoder<T> decoder) {
        return tryEither(() -> new JacksonStreamingParser().parse(json))
            .flatMap(decoder::apply);
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JValue}
     * @param json
     * @param decoder
     * @param <T>
     * @return
     */
    public static <T> Either<String, T> decodeValue(Json.JValue json, Decoder<T> decoder) {
        return decoder.apply(json);
    }

    /**
     * Decodes an enum by matching a string with the exact value name.
     * @param enumClass
     * @param <T>
     * @return
     */
    public static <T extends Enum<T>> Decoder<T> enumByName(Class<T> enumClass) {
        List<T> enumValues = List.of(enumClass.getEnumConstants());
        return json -> Decoders.String.apply(json)
            .flatMap(s -> enumValues.find(ev -> ev.name().equals(s))
                .map(Either::<String, T>right)
                .getOrElse(Either.left("cannot parse " + json + " into a value of enum " + enumClass.getName())));
    }

    /**
     * Builds a recursive decoder.
     * @param recursive
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> recursive(Function<Decoder<T>, Decoder<T>> recursive) {
        // with Java lambdas we can't use recursive definitions like in Elm, so implementing lazy doesn't make much sense
        // we instead provide a solution for the special case of recursive decoders

        return new Decoder<T>() {
            @Override
            public Either<String, T> apply(Json.JValue value) {
                return recursive.apply(this).apply(value);
            }
        };
    }

    private static <T> Either<String, T> is(Json.JValue val, Predicate<Json.JValue> predicate, Function<Json.JValue, Option<T>> narrow, String type) {
        return predicate.test(val)
            ? right(narrow.apply(val).get())
            : left("expected " + type + ", got " + val.toString());
    }
}
