package com.fredhonorio.json_decoder;

import io.vavr.CheckedFunction0;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

public final class EitherExtra {
    private EitherExtra() {
    }

    /**
     * Returns a {@link io.vavr.control.Either.Right} if the value exists, a {@link io.vavr.control.Either.Left}
     * with a given value otherwise.
     *
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
     * Returns the first {@link io.vavr.control.Either.Left} or a list of all values on {@link io.vavr.control.Either.Right}.
     *
     * @param res
     * @param <L>
     * @param <T>
     * @return
     */
    // @formatter:off
    public static <L, T> Either<L, List<T>> sequence(Seq<Either<L, T>> res) {
        Option<Either<L,T>> failure = res.find(Either::isLeft);
        if (failure.isDefined()) {
            return left(failure.get().getLeft());
        } else {
            return right(res.map(Either::get).toList());
        }
    }
    // @formatter:on

    /**
     * Attempts a computation that can fail, returns either the value on the right or the contents of
     * {@link Throwable#getMessage()} on the left.
     *
     * @param f
     * @param <U>
     * @return
     */
    static <U> Either<String, U> tryEither(CheckedFunction0<U> f) {
        return Try.of(f).toEither().mapLeft(Throwable::getMessage);
    }

    /**
     * Transforms an Either&lt;String, T&gt; into a {@link Try}. The exception (if applicable) is a {@link IllegalArgumentException}
     * with the right side as the message.
     *
     * @param e
     * @param <T>
     * @return
     */
    static <T> Try<T> toTry(Either<String, T> e) {
        return e.fold(
            fail -> Try.failure(new IllegalArgumentException(fail)),
            Try::success
        );
    }
}
