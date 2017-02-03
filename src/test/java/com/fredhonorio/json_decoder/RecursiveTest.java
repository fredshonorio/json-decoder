package com.fredhonorio.json_decoder;

import javaslang.collection.List;
import org.junit.Test;

import static com.fredhonorio.json_decoder.Decoders.String;
import static com.fredhonorio.json_decoder.Decoders.*;
import static net.hamnaberg.json.Json.JObject;
import static net.hamnaberg.json.Json.*;
import static org.junit.Assert.assertEquals;

public class RecursiveTest {

    @SuppressWarnings("EqualsHashCode") // hashCode() is never called
    public static class Person {
        public final String name;
        public final List<Person> children;

        public Person(String name, List<Person> children) {
            this.name = name;
            this.children = children;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            if (name != null ? !name.equals(person.name) : person.name != null) return false;
            return children != null ? children.equals(person.children) : person.children == null;
        }
    }

    @Test
    public void testRecursive() {

        JObject family = jObject(
            entry("name", "grandpa"),
            entry("children",
                jArray(
                    jObject(
                        entry("name", "father"),
                        entry("children",
                            jArray(
                                jObject(entry("name", "son"), entry("children", jEmptyArray())),
                                jObject(entry("name", "daughter"), entry("children", jEmptyArray()))
                            ))))));

        Decoder<Person> personDecoder = Decoders.recursive(self ->
            Decoder.map2(
                field("name", String),
                field("children", list(self)),
                Person::new));

        assertEquals(
            new Person("grandpa", List.of(
                new Person("father", List.of(
                    new Person("son", List.empty()),
                    new Person("daughter", List.empty())
                )))),
            decodeValue(family, personDecoder).get());
    }
}
