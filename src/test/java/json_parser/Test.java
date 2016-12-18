package json_parser;

import javaslang.control.Either;

import java.util.function.Predicate;

import static json_parser.Decoders.decodeString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class Test {

    static <T> void assertValue(String json, Decoder<T> decoder, T expected) {
        Either<String, T> v = decodeString(json, decoder);
        assertTrue("Expected successful value, got error: " + v.left().getOrElse(""), v.isRight());
        assertEquals(expected, v.get());
    }

    static <T> void assertValue(String json, Decoder<T> decoder, Predicate<T> test) {
        Either<String, T> v = decodeString(json, decoder);
        assertTrue("Expected successful value, got error: " + v.left().getOrElse(""), v.isRight());
        assertTrue("Predicate failed for value " + v, test.test(v.get()));
    }

    static <T> void assertError(String json, Decoder<T> decoder, String expectedError) {
        Either<String, T> v = decodeString(json, decoder);
        assertTrue("Expected error, got value: " + v.right().getOrElse((T) null), v.isLeft());
        assertEquals(expectedError, v.getLeft());
    }

    static <T> void assertError(String json, Decoder<T> decoder, Predicate<String> test) {
        Either<String, T> v = decodeString(json, decoder);
        assertTrue("Expected error, got value: " + v.right().getOrElse((T) null), v.isLeft());
        assertTrue("Predicate failed for error " + v, test.test(v.getLeft()));
    }
}
