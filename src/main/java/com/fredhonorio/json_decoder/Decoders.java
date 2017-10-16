package com.fredhonorio.json_decoder;

import com.fredhonorio.json_decoder.schema.Schema;
import com.fredhonorio.json_decoder.schema.Schema.Lit.Type;
import javaslang.Function2;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;
import net.hamnaberg.json.Json;
import net.hamnaberg.json.jackson.JacksonStreamingParser;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.fredhonorio.json_decoder.EitherExtra.*;
import static com.fredhonorio.json_decoder.schema.Schema.lit;
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
    public static final Decoder<Json.JValue> Value = Decoder.withSchema(v -> Either.right(v), new Schema.Any());

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JObject}.
     */
    public static final Decoder<Json.JObject> JObject = val(Json.JValue::isObject, Json.JValue::asJsonObject, "JObject", Schema.noKnownFields());

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray}.
     */
    public static final Decoder<Json.JArray> JArray = val(Json.JValue::isArray, Json.JValue::asJsonArray, "JArray", new Schema.Array(new Schema.Any()));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNull}.
     */
    public static final Decoder<Json.JNull> JNull = val(Json.JValue::isNull, Json.JValue::asJsonNull, "JNull", lit(Type.NULL));

    /**
     * Decodes a {@link String}. Only succeeds if the {@link net.hamnaberg.json.Json.JValue} is a json string. Performs
     * no coercion.
     */
    public static final Decoder<String> String = val(Json.JValue::isString, Json.JValue::asString, "String", lit(Type.STRING));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link BigDecimal}.
     */
    public static final Decoder<BigDecimal> BigDecimal = val(Json.JValue::isNumber, Json.JValue::asBigDecimal, "BigDecimal", lit(Type.FLOAT));

    /**
     * Decodes a {@link Boolean}.
     */
    public static final Decoder<Boolean> Boolean = val(Json.JValue::isBoolean, Json.JValue::asBoolean, "Boolean", lit(Type.BOOL));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Float}.
     */
    public static final Decoder<Float> Float = BigDecimal.mapTry(java.math.BigDecimal::floatValue, Throwable::getMessage);

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Double}.
     */
    public static final Decoder<Double> Double = BigDecimal.mapTry(java.math.BigDecimal::doubleValue, Throwable::getMessage);

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Integer}.
     */
    public static final Decoder<Integer> Integer = BigDecimal.mapTry(java.math.BigDecimal::intValueExact, Throwable::getMessage).setSchema(lit(Type.INT));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JNumber} as a {@link Long}.
     */
    public static final Decoder<Long> Long = BigDecimal.mapTry(java.math.BigDecimal::longValueExact, Throwable::getMessage).setSchema(lit(Type.INT));

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and applies the given decoder to the members.
     *
     * @param inner The decoder for the members
     * @return
     */
    public static <T> Decoder<List<T>> list(Decoder<T> inner) {
        Decoder<List<T>> l = val -> JArray.apply(val)
            .map(Stream::ofAll)
            .flatMap(s ->
                s.zipWithIndex()
                    .map(t -> t.transform((j, idx) -> inner.mapError(err -> "array element #" + idx + ": " + err).apply(j)))
                    .transform(EitherExtra::sequence)
            );

        return l.setSchema(new Schema.Array(inner.schema()));
    }

    /**
     * Attempts to use the given decoder, but doesn't fail if it does.
     *
     * @param inner
     * @return
     */
    public static <T> Decoder<Option<T>> option(Decoder<T> inner) {
        return oneOf(
            inner.map(Option::of),
            succeed(Option.none())
        );
    }

    /**
     * Attempts to use the given decoder, returns a given value if the decoder fails.
     *
     * @param decoder The decoder to use
     * @param otherwise The default value to use if the decoder fails
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> option(Decoder<T> decoder, T otherwise) {
        return oneOf(decoder, succeed(otherwise));
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
        Decoder<Option<T>> d = root -> JObject.apply(root)
            .flatMap(r ->
                r.get(key)
                    .map(val -> inner.apply(val).map(Option::some).mapLeft(err -> "field '" + key + "': " + err))
                    .getOrElse(right(Option.none())));

        return d.setSchema(Schema.singleField(key, inner.schema(), false));
    }

    /**
     * Attempts to pick a field from an {@link net.hamnaberg.json.Json.JObject} and applies a given decoder.
     * Returns a default value if the field doesn't exist. The decoder will still fail if the field exists but is invalid.
     *
     * @param key The name of the field
     * @param inner The decoder for the value in the field
     * @param otherwise The default value
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> optionalField(String key, Decoder<T> inner, T otherwise) {
        return optionalField(key, inner).map(v -> v.getOrElse(otherwise));
    }

    /**
     * Allows a value to be <code>null</code>.
     *
     * @param inner
     * @return
     */
    public static <T> Decoder<Option<T>> nullable(Decoder<T> inner) {
        return oneOf(
            inner.map(Option::of),
            nullValue(Option.<T>none())
        );
    }

    /**
     * Allows a value to be <code>null</code>. If it is then the given default value is used.
     *
     * @param decoder The decoder to use
     * @param ifNull The default value
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> nullable(Decoder<T> decoder, T ifNull) {
        return oneOf(
            decoder,
            nullValue(ifNull)
        );
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
        Decoder<T> field = root -> JObject.apply(root)
            .flatMap(val -> ofOption(val.get(key), "field '" + key + "': missing"))
            .flatMap(val -> inner.apply(val).mapLeft(err -> "field '" + key + "': " + err));

        return field.setSchema(Schema.singleField(key, inner.schema(), true));
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

        Decoder<T> dec = val -> {
            Stream<Either<java.lang.String, T>> results = decoders
                .toStream()
                .map(d -> d.apply(val));

            if (results.isEmpty())
                return left("no decoders given");

            return results
                .find(Either::isRight)
                .getOrElse(() -> left(
                    results
                        .map(Either::getLeft)
                        .prepend("Attempted multiple decoders, all failed:")
                        .mkString("\n\t - ")));
        };

        return dec.setSchema(new Schema.Union(decoders.map(Decoder::schema)));
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
        Function2<String, Json.JValue, Either<String, Tuple2<String, T>>> decodeEntry = (k, v) -> valueDecoder.apply(v)
            .map(decV -> Tuple.of(k, decV))
            .mapLeft(err -> "dict key '" + k + "': " + err);

        return JObject
            .andThen(j -> fromResult(sequence(j.mapToList(decodeEntry))))
            .<Map<String, T>>map(HashMap::ofEntries)
            .setSchema(Schema.unnamedFields(valueDecoder.schema()));
    }

    /**
     * Decodes a {@link net.hamnaberg.json.Json.JArray} and accesses a position.
     *
     * @param index
     * @return
     */
    public static Decoder<Json.JValue> index(int index) {
        return index(index, Value);
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
        return JArray
            .andThen(arr -> arr.get(index)
                .map(v -> fromResult(inner.apply(v)))
                .getOrElse(fail("missing")))
            .mapError(err -> "at index " + index + ": " + err);
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
     *
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
     *
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
        return enumByName(enumClass, Enum::name);
    }

    public static <T extends Enum<T>> Decoder<T> enumByName(Class<T> enumClass, Function<T, String> mapping) {
        List<T> enumValues = List.of(enumClass.getEnumConstants());
        Decoder<T> en = json -> Decoders.String.apply(json)
            .flatMap(s -> enumValues.find(ev -> mapping.apply(ev).equals(s))
                .map(Either::<String, T>right)
                .getOrElse(Either.left("cannot parse " + json + " into a value of enum " + enumClass.getName())));

        return en.withSchema(__ -> new Schema.Enum(enumValues.map(v -> Json.jString(mapping.apply(v)))));
    }

    /**
     * Decodes a value and applies a given mapping. Fails if the mapping does not have a corresponding value for the decoded value.
     * @param decoder The decoder
     * @param mapping The mapping
     * @param <T>
     * @param <U>
     * @return
     */
    public static <T, U> Decoder<U> mapping(Decoder<T> decoder, Function<T, Option<U>> mapping) {
        return decoder.andThen(t -> mapping.apply(t)
            .map(Decoders::succeed)
            .getOrElse(fail("Cannot find mapping for " + t))
        );
    }

    /**
     * This decoder is successful if the given decoder succeeds and the decoded value equals a given value.
     * @param decoder The decoder
     * @param value The value to match against
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> equal(Decoder<T> decoder, T value) {
        return decoder.filter(x -> x.equals(value), v -> "expected value: '" + value + "', got '" + v + "'");
    }

    /**
     * This decoder is successful if the given decoder succeeds and the decoded value matches a predicate.
     * @param decoder The decoder
     * @param test The predicate to test the decoded value
     * @param <T>
     * @return
     */
    public static <T> Decoder<T> matches(Decoder<T> decoder, Predicate<T> test) {
        return decoder.filter(test, v -> "the value '" + v + "' doesn't match the predicate");
    }

    /**
     * Builds a recursive decoder.
     *
     * @param generate
     * @return
     */
    public static <T> Decoder<T> recursive(Function<Decoder<T>, Decoder<T>> generate) {
        // with Java lambdas we can't use recursive definitions like in Elm, so implementing lazy doesn't make much sense
        // we instead provide a solution for the special case of recursive decoders

        return new Decoder<T>() {
            @Override
            public Either<String, T> apply(Json.JValue value) {
                return generate.apply(this).apply(value);
            }

            @Override
            public Schema schema() {
                /*
                we must set this unknown schema in case the user did not set the schema of `self`
                if we don't, then decoder.schema() is recursively defined, and we get a stack overflow (notice that
                while it looks like a parameter, the value is never defined anywhere, is just a big nested function)
                when we set self as a reference (or any other schema) we also break the infinite recursion
                also, this unknown schema is overwritten by calling ref

                let's describe this behavior by successive function application
                we also ignore the decoding part, so recursive is a function that takes a function from a schema to
                another and returns another schema (technically, because this is OOP we have the current schema implicitly,
                but since it is not used inside `recursive` we ignore it)

                recursive : (Schema -> Schema) -> Schema

                `ref` overwrites the current schema, so if we ignore the decoder its just:
                ref : (String -> Schema -> Schema)
                ref str ignored = Ref(str)

                with an example `generate`:

                g : (Schema -> Schema)
                g = self -> field("a", ref("r", self))

                expanding `recursive`
                --
                g(Unknown())
                (self -> field("a", self.ref("r")))(Unknown())
                field("a", ref("r", Unknown()))
                field("a", Ref("r"))
                --

                when user does not call ref:
                --
                g(Unknown())
                (self -> field("a", self))(Unknown())
                field("a", Unknown())
                --
                notice that we can still get a partial schema.

                if instead we did pass the current schema instead of unknown:

                g(current) -- current is also g
                g(g(current))
                g(g(g(current))) -- etc.
                */

                Schema unknown = new Schema.Unknown("This decoder was defined recursively, but the reference was never set, see: http://example.com");
                return generate.apply(this.setSchema(unknown)).schema();
            }
        };
    }

    private static <T> Decoder<T> val(Predicate<Json.JValue> predicate, Function<Json.JValue, Option<T>> narrow, String type, Schema schema) {
        Decoder<T> d = val -> predicate.test(val)
            ? right(narrow.apply(val).get())
            : left("expected " + type + ", got " + val.toString());
        return d.setSchema(schema);
    }
}
