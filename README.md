# json-decoder

__json-decoder__ is a Java 8 library for type-safe JSON decoding, almost a direct
port of [Elm](http://elm-lang.org)'s [Json.Decode](http://package.elm-lang.org/packages/elm-lang/core/5.0.0/Json-Decode).

## Dependencies

`json-decoder` uses [immutable-json-ast](https://github.com/hamnis/immutable-json/) for the JSON AST,
[jackson](https://github.com/FasterXML/jackson) for parsing json and [javaslang](http://www.javaslang.io/) for utility.

## Usage
[This page](https://guide.elm-lang.org/interop/json.html) shows the concepts
behind the `Json.Decode` elm package, `json-decoder` attempts to mimic that API.

The `Decoders` class contains simple decoders and facilities to build complex ones.
Once a decoder is built, JSON can be decoded by calling `Decoders.decodeString`
or `Decoders.decodeValue`. These functions will return an `Either<String, T>`
which will have either an error message on the left, or a successfully decoded
value on the right.

Simple values:

``` java
// we'll statically import Decoders.* for brevity
// Integer, String, etc. are members of that class
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
decodeString("{\"a\": 1}", optionalField("a", String)); // left("field 'a': expected String, got JNumber{value=1}")
decodeString("{\"a\": 1}", option(field("a", String))); // right(Option.none())

// in summation, `option` always succeeds event if the inner decoder fails
// while `optionalField` only succeeds if the field is missing or the field exists and the inner decoder succeeds as well.
```

Loosely typed values:
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

Enums can be parsed by attempting to match a string exactly
``` java
decodeString("\"ERA\"", enumByName(ChronoField.class)); // right(ChronoField.ERA)
```

Using previous results with `andThen`:
``` java
// deciding on a parser based on a result
Decoder<String> versionedDecoder = field("ver", Integer)
    .andThen(version ->
        version == 0 ? field("name", String) :
        version == 1 ? field("fullName", String) :
        fail("unknown version " + version));

decodeString("{\"ver\":0,\"name\":\"john\"}", versionedDecoder); // right("john")
decodeString("{\"ver\":2,\"name\":\"john\"}", versionedDecoder); // left("unknown version 2");

// extending a parser to validate decoded values
Decoder<String> nonEmptyString = String.andThen(str -> str.isEmpty() ? fail("empty string") : succeed(str));

decodeString("\"ok\"", nonEmptyString); // right("ok")
decodeString("\"\"", nonEmptyString); // left("empty string")

```
[Here](src/test/java/json_decoder/DecodersTest.java#L290) is an example of using `andThen` to build a `Decoder<T>` when `T` has subtypes.

Decoding recursive structures:
``` java
// given this Tree:
public class Tree<T> {
    public final T value;
    public final Seq<Tree<T>> children;

    public Tree(T value, Seq<Tree<T>> children) {
        this.value = value;
        this.children = children;
    }
}

// we can use use `recursive` to build a decoder that references itself
// this is necessary because Java lambdas can't reference `this`
Decoder<Tree<Integer>> intTreeDecoder =
    recursive(self ->
        Decoder.map2(
            field("value", Integer),
            optionalField("children", list(self)).map(optSeq -> optSeq.getOrElse(List.empty())),
            Tree::new));

String json = "{ \"value\": 1" +
              ", \"children\": [ { \"value\": 2 }" +
                              ", { \"value\": 3, \"children\": [ { \"value\": 4 } ] }" +
                              "]" +
              "}";

decodeString(json, intTreeDecoder);
// right(
//   tree(1,
//     tree(2),
//     tree(3,
//       tree(4)))
// )
```

More examples can be found in [tests](src/test/java/json_decoder/).

## How to get
TODO

## License
TODO
