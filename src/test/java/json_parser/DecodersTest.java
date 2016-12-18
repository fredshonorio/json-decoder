package json_parser;

import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.control.Option;
import net.hamnaberg.json.Json;
import org.junit.Test;

import static java.util.function.Predicate.isEqual;
import static javaslang.control.Option.none;
import static javaslang.control.Option.some;
import static json_parser.Decoder.map2;
import static json_parser.Decoders.*;
import static json_parser.Decoders.Double;
import static json_parser.Decoders.Float;
import static json_parser.Decoders.Integer;
import static json_parser.Decoders.Long;
import static json_parser.Decoders.String;
import static json_parser.Test.assertError;
import static json_parser.Test.assertValue;

public class DecodersTest {

    @Test
    public void testNumbers() {
        assertValue("1", Integer, 1);
        assertValue("2147483647", Integer, java.lang.Integer.MAX_VALUE);
        assertValue("-2147483648", Integer, java.lang.Integer.MIN_VALUE);
        assertError("\"1\"", Integer, "not a valid BigDecimal");

        assertValue("1", Long, 1L);
        assertError("\"1\"", Long, "not a valid BigDecimal");

        assertValue("1", Float, 1F);
        assertError("\"1\"", Float, "not a valid BigDecimal");

        assertValue("1", Double, 1D);
        assertError("\"1\"", Double, "not a valid BigDecimal");

        assertValue("1", BigDecimal, java.math.BigDecimal.ONE);
        assertError("\"1\"", BigDecimal, "not a valid BigDecimal");
    }

    @Test
    public void testString() {
        assertError("hello", String, err -> err.contains("Unrecognized token 'hello'"));
        assertValue("\"hello\"", String, "hello");
        assertError("1", String, "not a valid String");
    }

    @Test
    public void testNull() {
        assertValue("null", nullValue(1), 1);
        assertError("1", nullValue(1), "not a valid JNull");
    }

    @Test
    public void testObj() {
        assertValue(
            "{ \"hey\": 1}",
            field("hey", Integer),
            1
        );

        assertValue(
            "{ \"hey\": 1 " +
            ", \"hi\" : \"hello\"" +
            "}",
            map2(
                field("hey", Integer),
                field("hi", String),
                Tuple::of),
            Tuple.of(1, "hello")
        );

        assertError(
            "{}",
            field("hey", Integer),
            "field 'hey': missing"
        );

        assertError(
            "{ \"hey\": 1 }",
            field("hey", String),
            "field 'hey': not a valid String"
        );

        assertError(
            "{ \"a\": { \"b\": 1 } }",
            field("a", field("b", String)),
            "field 'a': field 'b': not a valid String"
        );
    }

    @Test
    public void testOption() {

        assertValue(
            "{ \"a\": 1 }",
            option(field("a", Integer)),
            Option.of(1)
        );

        // `String` fails, therefore `field` fails, therefore `option` decodes to none
        assertValue(
            "{ \"a\": 1 }",
            option(field("a", String)),
            Option.none()
        );

        // `field` fails, therefore `option` decodes to none
        assertValue(
            "{ \"a\": 1 }",
            option(field("b", String)),
            Option.none()
        );

        // `String` fails, therefore `option` decodes to none
        assertValue(
            "{ \"a\": 1 }",
            field("a", option(String)),
            Option.none()
        );

        // `field` fails
        assertError(
            "{ \"a\": 1 }",
            field("b", option(String)),
            "field 'b': missing"
        );

        // the field exists and `String` fails, therefore `optionalField` fails
        assertError(
            "{ \"a\": 1 }",
            optionalField("a", String),
            "field 'a': not a valid String"
        );

        // the field doesn't exist, therefore `optionalField` decodes to none
        assertValue(
            "{ \"a\": 1 }",
            optionalField("b", String),
            Option.none()
        );
    }

    @Test
    public void testList() {
        assertValue("[1, 2 ,3]", list(Integer), List.of(1, 2, 3));
        assertError("[1, \"2\" ,3]", list(Integer), "array element: not a valid BigDecimal");

        assertValue(
            "[ {\"a\": 1}" +
            ", {\"a\": 2}" +
            ", {\"a\": 3}" +
            "]",
            list(field("a", Integer).map(Tuple::of)),
            isEqual(List.of(
                Tuple.of(1),
                Tuple.of(2),
                Tuple.of(3))));

        assertValue(
            "[ {\"a\": 1}" +
            ", {\"b\": 2}" +
            ", {\"a\": 3}" +
            "]",
            list(option(field("a", Integer))),
            isEqual(List.of(
                some(1),
                none(),
                some(3))));

        assertValue(
            "[ {\"a\": 1}" +
            ", {\"a\": \"a\"}" +
            ", {\"a\": 3}" +
            "]",
            list(field("a", option(Integer)).map(Tuple::of)),
            isEqual(List.of(
                Tuple.of(some(1)),
                Tuple.of(none()),
                Tuple.of(some(3)))));

        assertValue(
            "[ 1, \"1\", null ]",
            list(oneOf(String, Integer.map(Object::toString), nullValue("1"))),
            List.of("1", "1", "1")
        );

        assertError(
            "[ 1, \"1\" ]",
            list(Integer.map(Object::toString)),
            "array element: not a valid BigDecimal"
        );

    }

    @Test
    public void testDict() {
        assertValue(
            "{ \"a\": 1" +
            ", \"b\": 2" +
            "}",
            dict(Integer),
            HashMap.of("a", 1).put("b", 2)
        );

        assertError(
            "{ \"a\": 1" +
            ", \"b\": \"2\"" +
            "}",
            dict(Integer),
            "dict key 'b': not a valid BigDecimal"
        );
    }

    @Test
    public void testNullable() {
        assertValue("1", nullable(Integer), Option.of(1));
        assertValue("null", nullable(Integer), Option.none());
        assertError("true", nullable(Integer), "not a valid BigDecimal");
    }

    @Test
    public void testAt() {
        assertValue(
            "{\"a\": { \"b\": { \"c\": 1 } } }",
            at(List.of("a", "b", "c"), Integer),
            1
        );

        assertValue("1", at(List.empty(), Integer), 1);
    }

    @Test
    public void testIndex() {
        assertValue(
            "[0, 1]",
            index(1),
            Json.jNumber(1)
        );

        assertValue(
            "[0, 1]",
            index(1, Integer),
            1
        );
    }

    @Test
    public void testMapError() {
        Decoder<Integer> myDec = Integer.mapError(err -> "NO");
        assertValue("1", myDec, 1);
        assertError("null", myDec, "NO");
    }
}