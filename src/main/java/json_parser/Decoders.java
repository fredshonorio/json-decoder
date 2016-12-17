package json_parser;

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
import static json_parser.EitherExtra.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Decoders {
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

    public static <T> Decoder<Seq<T>> list(Decoder<T> inner) {
        return val -> JArray.apply(val)
            .flatMap(arr -> sequence(arr.mapToList(inner::apply)).mapLeft(err -> "array element: " + err));
    }

    // TODO: explain
    // option(decoder) allows `decoder` to fail
    public static <T> Decoder<Option<T>> option(Decoder<T> inner) {
        return val -> inner.apply(val)
            .map(Option::of)
            .orElse(right(Option.none()));
    }

    // TODO: explain
    // optionalField allows the field to be missing, but the inner decoder cannot fail
    public static <T> Decoder<Option<T>> optionalField(String key, Decoder<T> inner) {
        return root -> JObject.apply(root)
            .flatMap(r ->
                r.get(key)
                    .map(val -> inner.apply(val).map(Option::some).mapLeft(err -> "field '" + key + "': " + err))
                    .getOrElse(right(Option.none())));
    }

    public static <T> Decoder<Option<T>> nullable(Decoder<T> inner) {
        return val -> JNull.apply(val)
            .map(n -> Option.<T>none())
            .orElse(() -> inner.apply(val).map(Option::some));
    }

    public static <T> Decoder<T> nullValue(T defaultValue) {
        return val -> JNull.apply(val)
            .map(n -> defaultValue);
    }

    public static <T> Decoder<T> field(String key, Decoder<T> inner) {
        return root -> JObject.apply(root)
            .flatMap(val -> ofOption(val.get(key), "field '" + key + "': missing"))
            .flatMap(val -> inner.apply(val).mapLeft(err -> "field '" + key + "': " + err));
    }

    @SafeVarargs
    public static <T> Decoder<T> oneOf(Decoder<T>... decoders) {
        return oneOf(List.of(decoders));
    }

    public static <T> Decoder<T> oneOf(List<Decoder<T>> decoders) {
        return obj -> decoders.foldLeft(
            left("no decoders given"),
            (z, x) -> z.orElse(x.apply(obj))
        );
    }

    public static <T> Decoder<T> succeed(T value) {
        return obj -> Either.right(value);
    }

    public static <T> Decoder<T> fail(String error) {
        return obj -> Either.left(error);
    }

    public static <T> Decoder<Map<String, T>> dict(Decoder<T> valueDecoder) {
        return root -> JObject.apply(root)
            .flatMap(r ->
                r.mapToList((k, v) -> valueDecoder.apply(v)
                    .map(decV -> Tuple.of(k, decV))
                    .mapLeft(err -> "dict key '" + k + "': " + err))
                    .transform(EitherExtra::sequence))
            .map(HashMap::ofEntries);
    }

    // TODO: index: http://package.elm-lang.org/packages/elm-lang/core/latest/Json-Decode#index
    // TODO: at: http://package.elm-lang.org/packages/elm-lang/core/latest/Json-Decode#at

    public static <T> Either<String, T> decodeString(String json, Decoder<T> decoder) {
        return tryEither(() -> new JacksonStreamingParser().parse(json))
            .flatMap(decoder::apply);
    }

    public static <T> Either<String, T> decodeValue(Json.JValue json, Decoder<T> decoder) {
        return decoder.apply(json);
    }

    private static <T> Either<String, T> is(Json.JValue val, Predicate<Json.JValue> predicate, Function<Json.JValue, Option<T>> narrow, String type) {
        return predicate.test(val)
            ? right(narrow.apply(val).get())
            : left("not a valid " + type);
    }
}
