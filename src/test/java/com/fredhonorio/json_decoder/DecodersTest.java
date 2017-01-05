package com.fredhonorio.json_decoder;

import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.control.Option;
import net.hamnaberg.json.Json;
import org.junit.Test;

import static com.fredhonorio.json_decoder.Decoder.map2;
import static com.fredhonorio.json_decoder.Decoders.*;
import static com.fredhonorio.json_decoder.Decoders.Boolean;
import static com.fredhonorio.json_decoder.Decoders.Double;
import static com.fredhonorio.json_decoder.Decoders.Float;
import static com.fredhonorio.json_decoder.Decoders.Integer;
import static com.fredhonorio.json_decoder.Decoders.Long;
import static com.fredhonorio.json_decoder.Decoders.String;
import static com.fredhonorio.json_decoder.Test.assertError;
import static com.fredhonorio.json_decoder.Test.assertValue;
import static java.util.function.Predicate.isEqual;
import static javaslang.control.Option.none;
import static javaslang.control.Option.some;
import static net.hamnaberg.json.Json.jObject;
import static org.junit.Assert.assertTrue;

public class DecodersTest {

    @Test
    public void testNumbers() {
        assertValue("1", Integer, 1);
        assertValue("2147483647", Integer, java.lang.Integer.MAX_VALUE);
        assertError("2147483648", Integer, "Overflow");
        assertValue("-2147483648", Integer, java.lang.Integer.MIN_VALUE);
        assertError("\"1\"", Integer, "expected BigDecimal, got JString{value='1'}");

        assertValue("1", Long, 1L);
        assertError("\"1\"", Long, "expected BigDecimal, got JString{value='1'}");

        assertValue("1", Float, 1F);
        assertError("\"1\"", Float, "expected BigDecimal, got JString{value='1'}");

        assertValue("1", Double, 1D);
        assertError("\"1\"", Double, "expected BigDecimal, got JString{value='1'}");

        assertValue("1", BigDecimal, java.math.BigDecimal.ONE);
        assertError("\"1\"", BigDecimal, "expected BigDecimal, got JString{value='1'}");
    }

    @Test
    public void testString() {
        assertError("hello", String, err -> err.contains("Unrecognized token 'hello'"));
        assertValue("\"hello\"", String, "hello");
        assertError("1", String, "expected String, got JNumber{value=1}");
    }

    @Test
    public void testNull() {
        assertValue("null", nullValue(1), 1);
        assertError("1", nullValue(1), "expected JNull, got JNumber{value=1}");
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
            "field 'hey': expected String, got JNumber{value=1}"
        );

        assertError(
            "{ \"a\": { \"b\": 1 } }",
            field("a", field("b", String)),
            "field 'a': field 'b': expected String, got JNumber{value=1}"
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
            "field 'a': expected String, got JNumber{value=1}"
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
        assertError("[1, \"2\" ,3]", list(Integer), "array element: expected BigDecimal, got JString{value='2'}");

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
            "array element: expected BigDecimal, got JString{value='1'}"
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
            "dict key 'b': expected BigDecimal, got JString{value='2'}"
        );
    }

    @Test
    public void testNullable() {
        assertValue("1", nullable(Integer), Option.of(1));
        assertValue("null", nullable(Integer), Option.none());
        assertError("true", nullable(Integer), "expected BigDecimal, got JBoolean{value=true}");
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

    static enum X { A, B }

    @Test
    public void testEnum() {
        assertValue("\"A\"", enumByName(X.class), X.A);
        assertError("\"C\"", enumByName(X.class), "cannot parse JString{value='C'} into a value of enum com.fredhonorio.json_decoder.DecodersTest$X");
    }

    public static abstract class Top {
        public static class A extends Top {
            public final int x;

            public A(int x) {
                this.x = x;
            }
        }

        public static class B extends Top {
            public final boolean y;

            public B(boolean y) {
                this.y = y;
            }
        }
    }

    @Test
    public void testAndThenSubclass() {

        Decoder<Top> topDecoder = field("type", String)
            .andThen(type ->
                type.equals("A") ? field("x", Integer).map(Top.A::new) :
                type.equals("B") ? field("y", Boolean).map(Top.B::new) :
                fail("unknown type " + type)
            );

        Top a = topDecoder.apply(
            jObject("type", "A")
                .put("x", 1)).get();

        assertTrue(a instanceof Top.A);

        Top b = topDecoder.apply(
            jObject("type", "B")
                .put("y", true)).get();

        assertTrue(b instanceof Top.B);
    }

    @Test
    public void testEqual() {

        // a Decoder::andThen(Decoder<U>) could be nice, the extraneous `t` would be gone
        Decoder<Integer> sumDecoder = field("type", equal(String, "add"))
            .andThen(t ->
                Decoder.map2(
                    field("a", Integer),
                    field("b", Integer),
                    (a, b) -> a + b
                )
            );

        assertValue(
            "{\"type\":\"add\", \"a\": 3, \"b\": 6}",
            sumDecoder,
            9
        );

        assertError("{\"type\":\"div\", \"a\": 3, \"b\": 6}", sumDecoder,  "field 'type': expected value: 'add'");


    }


}