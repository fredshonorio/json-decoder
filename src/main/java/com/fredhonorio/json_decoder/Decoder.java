package com.fredhonorio.json_decoder;

import com.fredhonorio.json_decoder.schema.Schema;
import javaslang.*;
import javaslang.collection.List;
import javaslang.control.Either;
import javaslang.control.Try;
import net.hamnaberg.json.Json;

import java.util.function.Function;
import java.util.function.Predicate;

import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@SuppressWarnings({"WeakerAccess", "unused"})
/**
 * A {@link Decoder<T>} is a function that takes a {@link net.hamnaberg.json.Json.JValue} and returns a {@code Either<String, T>}
 * That is, it either gives a successfully decoded value or an error message.
 */
public interface Decoder<T> {

    Either<String, T> apply(Json.JValue value);

    // we supply this default so that new implementations of decoders don't need to
    default Schema schema() {
        return new Schema.Unknown();
    }

    default Decoder<T> withSchema(Function<Schema, Schema> f) {
        return withSchema(this, f.apply(this.schema()));
    }

    default Decoder<T> setSchema(Schema schema) {
        return withSchema(this, schema);
    }
    }

    default <U> Decoder<U> transform(Function<Decoder<T>, Decoder<U>> f) {
        return f.apply(this);
    }

    /**
     * Applies a function to the decoded value, if it exists.
     */
    default <U> Decoder<U> map(Function<T, U> f) {
        return withSchema(x -> apply(x).map(f), this.schema());
    }

    /**
     * Applies a function to the error, if it exists
     */
    default Decoder<T> mapError(Function<String, String> f) {
        return withSchema(x -> apply(x).mapLeft(f), this.schema());
    }

    /**
     * Creates a Decoder that depends on the result of this Decoder.
     */
    default <U> Decoder<U> andThen(Function<T, Decoder<U>> f) {
        Decoder<U> d = x -> apply(x).flatMap(t -> f.apply(t).apply(x));
        return d.setSchema(this.schema());
    }

    /**
     * Applies this decoder first and another if this fails.
     */
    @Deprecated // oneOf is preferred because it has better messages
    default Decoder<T> orElse(Decoder<T> other) {
        return x -> apply(x).orElse(() -> other.apply(x));
    }

    /**
     * Causes this decoder to fail if the given predicate is not true.
     */
    default Decoder<T> filter(Predicate<T> predicate, String ifMissing) {
        return filter(predicate, __ -> ifMissing);
    }

    /**
     * Causes this decoder to fail if the given predicate is not true.
     */
    default Decoder<T> filter(Predicate<T> predicate, Function<T, String> ifMissing) {
        Decoder<T> d = x -> apply(x)
            .fold(
                Either::left,
                ok ->
                    predicate.test(ok)
                        ? right(ok)
                        : left(ifMissing.apply(ok)));

        return d.setSchema(this.schema());
    }

    /**
     * Attempts to transform the decoded value, fails with a the message of the thrown exception.
     */
    default <U> Decoder<U> mapTry(Try.CheckedFunction<T, U> f) {
        return mapTry(f, Throwable::getMessage);
    }

    /**
     * Attempts to transform the decoded value, fails with a given message if the transformation fails.
     */
    default <U> Decoder<U> mapTry(Try.CheckedFunction<T, U> f, String ifFailed) {
        return mapTry(f, __ -> ifFailed);
    }

    /**
     * Attempts to transform the decoded value, fails if the transformation fails. Accepts a callback to produce an
     * error depending on the exception.
     */
    default <U> Decoder<U> mapTry(Try.CheckedFunction<T, U> f, Function<Throwable, String> ifFailed) {
        return andThen(t ->
            Decoders.fromResult(
                Try.of(() -> f.apply(t)).toEither().mapLeft(ifFailed)));
    }

    /**
     * Widen a decoder to looser type.
     * @param dec The decoder
     * @param <T> The narrow type
     * @return
     */
    static <T> Decoder<T> widen(Decoder<? extends T> dec) {
        return (Decoder<T>) dec;
    }

    // generated
    // @formatter:off
     static <A, B, TT> Decoder<TT> map2(Decoder<A> dA, Decoder<B> dB, Function2<A, B, TT> f) {

        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).map(_dB ->
                f.apply(_dA, _dB)
            ));

        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, TT> Decoder<TT> map3(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Function3<A, B, C, TT> f) {
        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).map(_dC ->
                f.apply(_dA, _dB, _dC)
            )));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, D, TT> Decoder<TT> map4(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Function4<A, B, C, D, TT> f) {
        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).map(_dD ->
                f.apply(_dA, _dB, _dC, _dD)
            ))));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema(), dD.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, D, E, TT> Decoder<TT> map5(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Function5<A, B, C, D, E, TT> f) {
        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).map(_dE ->
                f.apply(_dA, _dB, _dC, _dD, _dE)
            )))));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema(), dD.schema(), dE.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, D, E, F, TT> Decoder<TT> map6(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Function6<A, B, C, D, E, F, TT> f) {
        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).flatMap(_dE ->
            dF.apply(root).map(_dF ->
                f.apply(_dA, _dB, _dC, _dD, _dE, _dF)
            ))))));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema(), dD.schema(), dE.schema(), dF.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, D, E, F, G, TT> Decoder<TT> map7(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Decoder<G> dG, Function7<A, B, C, D, E, F, G, TT> f) {
        Decoder<TT> d = root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).flatMap(_dE ->
            dF.apply(root).flatMap(_dF ->
            dG.apply(root).map(_dG ->
                f.apply(_dA, _dB, _dC, _dD, _dE, _dF, _dG)
            )))))));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema(), dD.schema(), dE.schema(), dF.schema(), dG.schema()));
        return d.setSchema(schema);
    }

    static <A, B, C, D, E, F, G, H, TT> Decoder<TT> map8(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Decoder<G> dG, Decoder<H> dH, Function8<A, B, C, D, E, F, G, H, TT> f) {
        Decoder<TT> d =root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).flatMap(_dE ->
            dF.apply(root).flatMap(_dF ->
            dG.apply(root).flatMap(_dG ->
            dH.apply(root).map(_dH ->
                f.apply(_dA, _dB, _dC, _dD, _dE, _dF, _dG, _dH)
            ))))))));
        Schema schema = new Schema.All(List.of(dA.schema(), dB.schema(), dC.schema(), dD.schema(), dE.schema(), dF.schema(), dG.schema(), dH.schema()));
        return d.setSchema(schema);
    }
    // @formatter:on

    static <T> Decoder<T> withSchema(Decoder<T> d, Schema schema) {
        return new Decoder<T>() {
            @Override
            public Either<String, T> apply(Json.JValue value) {
                return d.apply(value);
            }

            @Override
            public Schema schema() {
                return schema;
            }
        };
    }
}
