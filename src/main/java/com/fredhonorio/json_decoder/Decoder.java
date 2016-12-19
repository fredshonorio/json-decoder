package com.fredhonorio.json_decoder;

import javaslang.*;
import javaslang.control.Either;
import net.hamnaberg.json.Json;

import java.util.function.Function;

@SuppressWarnings({"WeakerAccess", "unused"})
public interface Decoder<T> {

    Either<String, T> apply(Json.JValue value);

    default <U> Decoder<U> map(Function<T, U> f) {
        return x -> apply(x).map(f);
    }

    default Decoder<T> mapError(Function<String, String> f) {
        return x -> apply(x).mapLeft(f);
    }

    default <U> Decoder<U> andThen(Function<T, Decoder<U>> f) {
        return x -> apply(x).flatMap(t -> f.apply(t).apply(x));
    }

    // generated
    // @formatter:off
     static <A, B, TT> Decoder<TT> map2(Decoder<A> dA, Decoder<B> dB, Function2<A, B, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).map(_dB ->
                f.apply(_dA, _dB)
            ));
    }

    static <A, B, C, TT> Decoder<TT> map3(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Function3<A, B, C, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).map(_dC ->
                f.apply(_dA, _dB, _dC)
            )));
    }

    static <A, B, C, D, TT> Decoder<TT> map4(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Function4<A, B, C, D, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).map(_dD ->
                f.apply(_dA, _dB, _dC, _dD)
            ))));
    }

    static <A, B, C, D, E, TT> Decoder<TT> map5(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Function5<A, B, C, D, E, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).map(_dE ->
                f.apply(_dA, _dB, _dC, _dD, _dE)
            )))));
    }

    static <A, B, C, D, E, F, TT> Decoder<TT> map6(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Function6<A, B, C, D, E, F, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).flatMap(_dE ->
            dF.apply(root).map(_dF ->
                f.apply(_dA, _dB, _dC, _dD, _dE, _dF)
            ))))));
    }

    static <A, B, C, D, E, F, G, TT> Decoder<TT> map7(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Decoder<G> dG, Function7<A, B, C, D, E, F, G, TT> f) {
        return root ->
            dA.apply(root).flatMap(_dA ->
            dB.apply(root).flatMap(_dB ->
            dC.apply(root).flatMap(_dC ->
            dD.apply(root).flatMap(_dD ->
            dE.apply(root).flatMap(_dE ->
            dF.apply(root).flatMap(_dF ->
            dG.apply(root).map(_dG ->
                f.apply(_dA, _dB, _dC, _dD, _dE, _dF, _dG)
            )))))));
    }

    static <A, B, C, D, E, F, G, H, TT> Decoder<TT> map8(Decoder<A> dA, Decoder<B> dB, Decoder<C> dC, Decoder<D> dD, Decoder<E> dE, Decoder<F> dF, Decoder<G> dG, Decoder<H> dH, Function8<A, B, C, D, E, F, G, H, TT> f) {
        return root ->
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
    }
    // @formatter:on
}