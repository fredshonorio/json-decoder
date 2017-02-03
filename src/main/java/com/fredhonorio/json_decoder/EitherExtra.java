package com.fredhonorio.json_decoder;

import javaslang.collection.List;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;

import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

final class EitherExtra {
    private EitherExtra() {
    }

    /**
     * Returns a {@link javaslang.control.Either.Right} if the value exists, a {@link javaslang.control.Either.Left}
     * with a given value otherwise.
     * @param s
     * @param ifMissing
     * @param <L>
     * @param <R>
     * @return
     */
    static <L, R> Either<L, R> ofOption(Option<R> s, L ifMissing) {
        return s.isDefined()
            ? right(s.get())
            : left(ifMissing);
    }

    /**
     * Returns the first {@link javaslang.control.Either.Left} or a list of all values on {@link javaslang.control.Either.Right}.
     *
     * @param res
     * @param <T>
     * @return
     */
    // @formatter:off
    static <T> Either<String, List<T>> sequence(List<Either<String, T>> res) {
        return res.foldLeft(
            right(List.<T>empty()),
            (z, x) ->
                z.isLeft()
                    ? z
                    : x.isLeft()
                        ? left(x.getLeft())
                        : right(z.get().append(x.get())));
    }
    // @formatter:on

    /**
     * Attempts a computation that can fail, returns either the value on the right or the contents of
     * {@link Throwable#getMessage()} on the left.
     * @param f
     * @param <U>
     * @return
     */
    static <U> Either<String, U> tryEither(Try.CheckedSupplier<U> f) {
        return Try.of(f).toEither().mapLeft(Throwable::getMessage);
    }
}
