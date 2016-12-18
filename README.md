# json-decoder

__json-decoder__ is a Java 8 library for type-safe JSON decoding, almost a direct
port of [Elm](http://elm-lang.org)'s [Json.Decode](http://package.elm-lang.org/packages/elm-lang/core/latest/Json-Decode]).

## Dependencies

`json-decoder` uses [immutable-json-ast](https://github.com/hamnis/immutable-json/) for the JSON AST,
[jackson](https://github.com/FasterXML/jackson) for parsing json and [javaslang](http://www.javaslang.io/) for utility.

## Usage

The `Decoders` class contains simple decoders and facilities to build complex ones.
Once a decoder is built, JSON can be decoded by calling `Decoders.decodeString`
or `Decoders.decodeValue`. These functions will return an `Either<String, T>`
which will have either an error message on the left, or a decoded value on the right.

Simple values:

``` java
// we'll statically import Decoders.*
decodeString("1", Integer); // right(1)
decodeString("\"string\"", String); // right("string")
```
Arrays:
``` java
// `list` decodes a JSON array and decodes every element with a given decoder
decodeString("[1, 2, 3]", list(Integer)); // right(list(1, 2, 3))

// `index` decodes an array and picks the element at a given index
decodeString("[1, 2, \"a\"]", index(2, String)); // right("a")
```

Dictionary:
``` java
decodeString("{\"a\": 1, \"b\": 2, \"c\": 3}", dict(Integer)); // right(HashMap.of("a", 1, "b", 2, "c", 3))
```

Fields:
``` java
decodeString("{\"a\": \"b\"}", field("a", String)); // right("b")

// `field` fails if the field is missing
decodeString("{\"a\": \"b\"}", field("b", String)); // left("field 'b': missing")

// `at` traverses an object tree
decodeString("{\"a\": {\"b\": \"c\"} }", at(List.of("a", "b"), String)); // right("c")

```
Decoders for complex structures can be composed using `map<N>`:
``` java
// with the following class:
public class Person {
    final String name;
    final int age;
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

Decoder<Person> personDecoder = Decoder.map2(
    field("name", String),
    field("age", Integer),
    Person::new
);

decodeString("{\"name\":\"jack\",\"age\":18}", personDecoder); // right(Person("jack", 18))
```

Optional values:
``` java
// `option` wraps the value of the given decoder in a Option, returns `none` if said decoder fails
decodeString("1", option(String)); // right(Option.none())
// you can map over the decoder to modify the decoded value
decodeString("1", option(String).map(optString -> optString.getOrElse(""))); // right("")

// `optionalField` attempts to decode a field but will fail if the field exists but is of a different type
// in the following case, both `optionalField` and `option` return `none`:
decodeString("{\"b\": 1}", optionalField("a", String)); // right(Option.none())
decodeString("{\"b\": 1}", option(field("a", String))); // right(Option.none())

// however, in this case `option` will silently ignore an unexpected type, while `optionalField` will fail
decodeString("{\"a\": 1}", optionalField("a", String)); // left("field 'a': not a valid String")
decodeString("{\"a\": 1}", option(field("a", String))); // right(Option.none())

// in summation, `option` always succeeds event if the inner decoder fails
// while `optionalField` only succeeds if the field is missing or the field exists and the inner decoder succeeds as well.
```

Poorly typed values:
``` java
// `oneOf` attempts multiple decoders, `nullValue` returns a given value is null is found
r = decodeString(
    "[1, \"hello\", null]",
    list(oneOf(
        Integer.map(i -> i.toString()),
        String,
        nullValue("<missing>"))
    ));
// right(List.of("1", "hello", "<missing>"))

// nullable allows null values, returns an `Option`
decodeString("[1, 2, null]", list(nullable(Integer))); // right(List.of(some(1), some(2), none()))
```

More examples can be found in [tests](src/test/java/json_decoder/).

TODO: big complex example

## How to get
TODO

## License
TODO
