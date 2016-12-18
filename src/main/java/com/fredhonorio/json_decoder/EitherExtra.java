package com.fredhonorio.json_decoder;

import javaslang.collection.List;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;

import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

class EitherExtra {

    static <T> Either<String, T> ofOption(Option<T> s, String ifMissing) {
        return s.map(Either::<String, T>right)
            .getOrElse(left(ifMissing));
    }

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

    static <U> Either<String, U> tryEither(Try.CheckedSupplier<U> f) {
        return Try.of(f).toEither().mapLeft(Throwable::getMessage);
    }
}
