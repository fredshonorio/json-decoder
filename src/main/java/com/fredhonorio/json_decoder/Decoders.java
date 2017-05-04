package com.fredhonorio.json_decoder;

import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;
import net.hamnaberg.json.Json;
import net.hamnaberg.json.io.JacksonStreamingParser;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.fredhonorio.json_decoder.EitherExtra.*;
import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@SuppressWarnings({"WeakerAccess", "unused"})
/**
 * Contains basic decoders and combinators. A  @{code Decoder<T>} takes a {@link net.hamnaberg.json.Json.JValue} and
 * attempts to return a <code>T</code>. If it succeeds it returns a <code>right(T)</code> otherwise a <code>left(String)</code>
 * with an error message.
 */
public final class Decoders {
    private Decoders() {
    }

    /**
     * Simply returns the {@link net.hamnaberg.json.Json.JValue}. Always succeeds.
     */
    public static final Decoder<Json.JValue> Value = Either::right;

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JObject}.
     */
    public static final Decoder<Json.JObject> JObject = v -> is(v, Json.JValue::isObject, Json.JValue::asJsonObject, "JObject");

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray}.
     */
    public static final Decoder<Json.JArray> JArray = v -> is(v, Json.JValue::isArray, Json.JValue::asJsonArray, "JArray");

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNull}.
     */
    public static final Decoder<Json.JNull> JNull = v -> is(v, Json.JValue::isNull, Json.JValue::asJsonNull, "JNull");

    /**
     * Decodes a {@link String}. Only succeeds if the {@link net.hamnaberg.json.Json.JValue} is a json string. Performs
     * no coercion.
     */
    public static final Decoder<String> String = v -> is(v, Json.JValue::isString, Json.JValue::asString, "String");

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link BigDecimal}.
     */
    public static final Decoder<BigDecimal> BigDecimal = v -> is(v, Json.JValue::isNumber, Json.JValue::asBigDecimal, "BigDecimal");

    /**
     * Decodes a {@link Boolean}.
     */
    public static final Decoder<Boolean> Boolean = v -> is(v, Json.JValue::isBoolean, Json.JValue::asBoolean, "Boolean");

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Float}.
     */
    public static final Decoder<Float> Float = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::floatValue));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Double}.
     */
    public static final Decoder<Double> Double = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::doubleValue));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Integer}.
     */
    public static final Decoder<Integer> Integer = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::intValueExact));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Long}.
     */
    public static final Decoder<Long> Long = v -> BigDecimal.apply(v).flatMap(big -> tryEither(big::longValueExact));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and applies the given decoder to the members.
     *
     * @param inner The decoder for the members
     * @return
     */
    public static <T> Decoder<List<T>> list(Decoder<T> inner) {
        return val -> JArray.apply(val)
            .map(Stream::ofAll)
            .flatMap(arr -> sequence(arr.map(inner::apply)).mapLeft(err -> "array element: " + err));
    }

    /**
     * Attempts to use the given decoder, but doesn't fail if it does.
     *
     * @param inner
     * @return
     */
    public static <T> Decoder<Option<T>> option(Decoder<T> inner) {
        return inner
            .map(Option::of)
            .orElse(succeed(Option.none()));
    }

    /**
     * Attempts to pick a field from an {@link net.hamnaberg.json.Json.JObject} and applies a given decoder.
     * Succeeds if the field does not exist but still fails if the inner decoder fails.
     *
     * @param key   The name of the field
     * @param inner
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
     * Allows a value to be <code>null</code>.
     *
     * @param inner
     * @return
     */
    public static <T> Decoder<Option<T>> nullable(Decoder<T> inner) {
        return nullValue(Option.<T>none())
            .orElse(inner.map(Option::of));
    }

    /**
     * Returns a given value if the {@link net.hamnaberg.json.Json.JValue} is null.
     *
     * @param defaultValue
     * @return
     */
    public static <T> Decoder<T> nullValue(T defaultValue) {
        return JNull.map(n -> defaultValue);
    }

    /**
     * Picks a field from a {@link net.hamnaberg.json.Json.JObject }and applies a given decoder. Fails if the
     * field doesn't exist.
     *
     * @param key   The name of the field
     * @param inner
     * @return
     */
    public static <T> Decoder<T> field(String key, Decoder<T> inner) {
        return root -> JObject.apply(root)
            .flatMap(val -> ofOption(val.get(key), "field '" + key + "': missing"))
            .flatMap(val -> inner.apply(val).mapLeft(err -> "field '" + key + "': " + err));
    }

    /**
     * @param decoders
     * @return
     * @see #oneOf(List)
     */
    @SafeVarargs
    public static <T> Decoder<T> oneOf(Decoder<T>... decoders) {
        return oneOf(List.of(decoders));
    }

    /**
     * Attempts the given decoders until one succeeds.
     *
     * @param decoders
     * @return
     */
    public static <T> Decoder<T> oneOf(List<Decoder<T>> decoders) {
        return obj -> decoders.foldLeft(
            left("no decoders given"),
            (z, x) -> z.orElse(x.apply(obj))
        );
    }

    /**
     * Succeeds with a given value.
     *
     * @param value
     * @return
     */
    public static <T> Decoder<T> succeed(T value) {
        return obj -> Either.right(value);
    }

    /**
     * Fails with a given error.
     *
     * @param error
     * @return
     */
    public static <T> Decoder<T> fail(String error) {
        return obj -> Either.left(error);
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JObject} and applies a given decoder to each of the values, resulting in
     * a dictionary.
     *
     * @param valueDecoder
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
     *
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
     *
     * @param index
     * @param inner
     * @return
     */
    public static <T> Decoder<T> index(int index, Decoder<T> inner) {
        return index(index).andThen(item -> fromResult(inner.apply(item)));
    }

    /**
     * Traverses an object tree and applies a decoder at the leaf.
     *
     * @param fields
     * @param inner
     * @return
     */
    public static <T> Decoder<T> at(List<String> fields, Decoder<T> inner) {
        return fields.foldRight(inner, Decoders::field);
    }

    /**
     * Always returns a given {@code Either<String, T>}.
     *
     * @param result
     * @return
     */
    public static <T> Decoder<T> fromResult(Either<String, T> result) {
        return t -> result;
    }

    /**
     * Decodes a json string with a given decoder, uses Jackson.
     *
     * @param json
     * @param decoder
     * @return
     */
    public static <T> Either<String, T> decodeString(String json, Decoder<T> decoder) {
        return tryEither(() -> new JacksonStreamingParser().parse(json))
            .flatMap(decoder::apply);
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JValue}
     *
     * @param json
     * @param decoder
     * @return
     */
    public static <T> Either<String, T> decodeValue(Json.JValue json, Decoder<T> decoder) {
        return decoder.apply(json);
    }

    /**
     * Decodes a json string with a given decoder, uses Jackson. Returns the result in a {@link Try}
     * @param json
     * @param decoder
     * @return
     */
    public static <T> Try<T> tryDecodeString(String json, Decoder<T> decoder) {
        return Try.of(() -> new JacksonStreamingParser().parse(json))
            .flatMap(j -> tryDecodeValue(j, decoder));
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JValue} with a given decoder. Returns the result in a {@link Try}
     * @param json
     * @param decoder
     * @param <T>
     * @return
     */
    public static <T> Try<T> tryDecodeValue(Json.JValue json, Decoder<T> decoder) {
        return EitherExtra.toTry(decoder.apply(json));
    }

    /**
     * Decodes an enum by matching a string with the exact value name.
     *
     * @param enumClass
     * @return
     */
    public static <T extends Enum<T>> Decoder<T> enumByName(Class<T> enumClass) {
        List<T> enumValues = List.of(enumClass.getEnumConstants());
        return json -> Decoders.String.apply(json)
            .flatMap(s -> enumValues.find(ev -> ev.name().equals(s))
                .map(Either::<String, T>right)
                .getOrElse(Either.left("cannot parse " + json + " into a value of enum " + enumClass.getName())));
    }

    public static <T> Decoder<T> equal(Decoder<T> decoder, T value) {
        return decoder.filter(x -> x.equals(value), "expected value: '" + value + "'");
    }

    /**
     * Builds a recursive decoder.
     *
     * @param recursive
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
