package json_decoder;

import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.control.Either;
import org.junit.Test;

import java.time.temporal.ChronoField;

import static javaslang.control.Either.left;
import static javaslang.control.Either.right;
import static javaslang.control.Option.none;
import static javaslang.control.Option.some;
import static json_decoder.Decoders.Integer;
import static json_decoder.Decoders.String;
import static json_decoder.Decoders.*;
import static net.hamnaberg.json.Json.jObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadmeTest {

    public static class Person {
        final String name;
        final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public java.lang.String toString() {
            return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            if (age != person.age) return false;
            return name.equals(person.name);
        }
    }

    @Test
    public void test() {
        Object r;

        r = decodeString("1", Integer);
        assertEquals(right(1), r);

        r = decodeString("\"string\"", String);
        assertEquals(right("string"), r);

        r = decodeString("[1, 2, 3]", list(Integer));
        assertEquals(right(List.of(1, 2, 3)), r);

        r = decodeString("[1, 2, \"a\"]", index(2, String));
        assertEquals(right("a"), r);

        r = decodeString("{\"a\": 1, \"b\": 2, \"c\": 3}", dict(Integer));
        assertEquals(right(HashMap.of("a", 1, "b", 2, "c", 3)), r);

        r = decodeString("{\"a\": \"b\"}", field("a", String));
        assertEquals(right("b"), r);

        r = decodeString("{\"a\": \"b\"}", field("b", String));
        assertEquals(left("field 'b': missing"), r);

        r = decodeString("{\"a\": {\"b\": \"c\"} }", at(List.of("a", "b"), String));
        assertEquals(right("c"), r);

        Decoder<Person> personDecoder = Decoder.map2(
            field("name", String),
            field("age", Integer),
            Person::new
        );

        r = decodeString("{\"name\":\"jack\",\"age\":18}", personDecoder);
        assertEquals(right(new Person("jack", 18)), r);

        r = decodeString("1", option(String));
        assertEquals(right(none()), r);

        r = decodeString("1", option(String).map(optString -> optString.getOrElse("")));
        assertEquals(right(""), r);

        r = decodeString("{\"b\": 1}", optionalField("a", String));
        assertEquals(right(none()), r);
        r = decodeString("{\"b\": 1}", option(field("a", String)));
        assertEquals(right(none()), r);

        r = decodeString("{\"a\": 1}", optionalField("a", String));
        assertEquals(left("field 'a': expected String, got JNumber{value=1}"), r);
        r = decodeString("{\"a\": 1}", option(field("a", String)));
        assertEquals(right(none()), r);

        r = decodeString(
            "[1, \"hello\", null]",
            list(oneOf(
                Integer.map(i -> i.toString()),
                String,
                nullValue("<missing>"))
            ));

        assertEquals(right(List.of("1", "hello", "<missing>")), r);

        r = decodeString("[1, 2, null]", list(nullable(Integer)));
        assertEquals(right(List.of(some(1), some(2), none())), r);

        r = decodeString("\"ERA\"", enumByName(ChronoField.class));
        assertEquals(right(ChronoField.ERA), r);

        Decoder<String> versionedDecoder = field("ver", Integer)
            .andThen(version ->
                version == 0 ? field("name", String) :
                version == 1 ? field("fullName", String) :
                fail("unknown version " + version));

        r = decodeString("{\"ver\":0,\"name\":\"john\"}", versionedDecoder);
        assertEquals(right("john"), r);

        r = decodeString("{\"ver\":2,\"name\":\"john\"}", versionedDecoder);
        assertEquals(left("unknown version 2"), r);

        Decoder<String> nonEmptyString = String
            .andThen(str ->
                str.isEmpty()
                    ? fail("empty string")
                    : succeed(str));

        r = decodeString("\"ok\"", nonEmptyString);
        assertEquals(right("ok"), r);

        r = decodeString("\"\"", nonEmptyString);
        assertEquals(left("empty string"), r);
    }
}
