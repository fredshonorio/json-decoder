# json-decoder
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Bintray](https://img.shields.io/bintray/v/fredhonorio-com/maven/json-decoder.svg)](https://bintray.com/fredhonorio-com/maven/json-decoder/)
[![Travis](https://img.shields.io/travis/rust-lang/rust.svg)](https://travis-ci.org/fredshonorio/json-decoder)
[![Codecov](https://img.shields.io/codecov/c/github/fredshonorio/json-decoder.svg)](https://codecov.io/gh/fredshonorio/json-decoder)

__json-decoder__ is a Java 8 library for type-safe JSON decoding, almost a direct
port of [Elm](http://elm-lang.org)'s [Json.Decode](http://package.elm-lang.org/packages/elm-lang/core/5.0.0/Json-Decode).

# Usage
[This page](https://guide.elm-lang.org/interop/json.html) shows the concepts
behind the `Json.Decode` elm package, `json-decoder` attempts to mimic that API.

The `Decoders` class contains simple decoders and facilities to build complex ones.
Once a decoder is built, JSON can be decoded by calling `Decoders.decodeString`
or `Decoders.decodeValue`. These functions will return an `Either<String, T>`
which will have either an error message on the left, or a successfully decoded
value on the right.

## Simple values
We'll statically import `Decoders.*` for brevity. `Integer`, `String`, etc. are members of that class.
``` java
decodeString("1", Integer); // right(1)
decodeString("\"string\"", String); // right("string")
```
## Arrays
`list` decodes a JSON array and decodes every element with a given decoder.
`index` decodes an array and picks the element at a given index.

``` java
decodeString("[1, 2, 3]", list(Integer)); // right(list(1, 2, 3))
decodeString("[1, 2, \"a\"]", index(2, String)); // right("a")
```

## Dictionaries
`dict` decodes a `Map<String, T>` given a `Decoder<T>`.
``` java
decodeString("{\"a\": 1, \"b\": 2, \"c\": 3}", dict(Integer)); // right(HashMap.of("a", 1, "b", 2, "c", 3))
```

## Object fields
`field` decodes an object and accesses a field, fails if the field is missing.
`at` traverses an object tree.
``` java
decodeString("{\"a\": \"b\"}", field("a", String)); // right("b")
decodeString("{\"a\": \"b\"}", field("b", String)); // left("field 'b': missing")
decodeString("{\"a\": {\"b\": \"c\"} }", at(List.of("a", "b"), String)); // right("c")

```
Decoders for complex structures can be build by composed other decoders using `map<N>`:
``` java
// with the following class:
public class Person {
    final String name;
    final int age;
	// constructor
}

Decoder<Person> personDecoder = Decoder.map2(
    field("name", String),
    field("age", Integer),
    Person::new
);

decodeString("{\"name\":\"jack\",\"age\":18}", personDecoder); // right(Person("jack", 18))
```

## Optional values
`option` wraps the value of the given `Decoder<T>` in an `Option<T>`, which is `none` if said decoder fails. Mapping map over a decoder can be useful to modify the decoded `Option<T>`.
`optionalField` attempts to decode a field but will fail if the field exists but it's decoder fails.
``` java
decodeString("1", option(String)); // right(Option.none())
decodeString("1", option(String).map(optString -> optString.getOrElse(""))); // right("")
```
In the following case, both `optionalField` and `option` return `none`:
``` java
decodeString("{\"b\": 1}", optionalField("a", String)); // right(Option.none())
decodeString("{\"b\": 1}", option(field("a", String))); // right(Option.none())
```
However, in this case `option` will silently ignore an unexpected type, while `optionalField` will fail
``` java
decodeString("{\"a\": 1}", optionalField("a", String)); // left("field 'a': expected String, got JNumber{value=1}")
decodeString("{\"a\": 1}", option(field("a", String))); // right(Option.none())
```
In summation, `option` always succeeds event if the inner decoder fails while `optionalField` only succeeds if the field is missing or the field exists and the inner decoder succeeds as well.

## Loosely typed values
`oneOf` attempts multiple decoders, `nullValue` returns a given value is null is found.
``` java
decodeString(
    "[1, \"hello\", null]",
    list(oneOf(
        Integer.map(i -> i.toString()),
        String,
        nullValue("<missing>"))));
// right(List.of("1", "hello", "<missing>"))
```
`nullable` allows null values, returns an `Option<T>`.
``` java
decodeString("[1, 2, null]", list(nullable(Integer))); // right(List.of(some(1), some(2), none()))
```

## Enums
Enums can be parsed by attempting to match a string exactly.
``` java
decodeString("\"ERA\"", enumByName(ChronoField.class)); // right(ChronoField.ERA)
```
## Composing decoders with `andThen`

`andThen` can be used to apply a decoder after another (to the same value), here are some examples:

Deciding on a parser based on a result:
``` java
Decoder<String> versionedDecoder = field("ver", Integer)
    .andThen(version ->
        version == 0 ? field("name", String) :
        version == 1 ? field("fullName", String) :
        fail("unknown version " + version));

decodeString("{\"ver\":0,\"name\":\"john\"}", versionedDecoder); // right("john")
decodeString("{\"ver\":2,\"name\":\"john\"}", versionedDecoder); // left("unknown version 2");
```
Extending a decoder to validate decoded values:
``` java
Decoder<String> nonEmptyString = String.andThen(str -> str.isEmpty() ? fail("empty string") : succeed(str));

decodeString("\"ok\"", nonEmptyString); // right("ok")
decodeString("\"\"", nonEmptyString); // left("empty string")
```
[Here](src/test/java/com/fredhonorio/json_decoder/DecodersTest.java#L290) is an example of using `andThen` to build a `Decoder<T>` when `T` is abstract.

## Recursive structures
`recursive` can be used to build a decoder that references itself. This is necessary because Java lambdas can't reference `this`.
``` java
// given this Tree:
public class Tree<T> {
    final T v;
    final Seq<Tree<T>> children;
	// constructor
}

Decoder<Tree<Integer>> intTreeDecoder =
    recursive(self ->
        Decoder.map2(
            field("v", Integer),
            optionalField("children", list(self)).map(optSeq -> optSeq.getOrElse(List.empty())),
            Tree::new));

String json = "{ \"v\": 1" +
              ", \"children\": [ { \"v\": 2 }" +
                              ", { \"v\": 3, \"children\": [ { \"v\": 4 } ] }" +
                              "]" +
              "}";

decodeString(json, intTreeDecoder); // right(tree(1, tree(2), tree(3, tree(4))))
```

More examples can be found in the [tests](src/test/java/com/fredhonorio/json_decoder/).

# Get it
From [jcenter](https://bintray.com/bintray/jcenter):
``` groovy
repositories {
	jcenter()
}

compile 'com.fredhonorio:json-decoder:0.0.2'
```

# Dependencies

__json-decoder__ uses [immutable-json-ast](https://github.com/hamnis/immutable-json/) for the JSON AST,
[jackson](https://github.com/FasterXML/jackson) for parsing JSON and [javaslang](http://www.javaslang.io/) for utility.

# License
This project is licensed under the Apache License v2.0.
