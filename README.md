# json-decoder
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Download](https://api.bintray.com/packages/fredhonorio-com/maven/json-decoder/images/download.svg)](https://bintray.com/fredhonorio-com/maven/json-decoder/_latestVersion)
[![Travis](https://travis-ci.org/fredshonorio/json-decoder.svg?branch=master)](https://travis-ci.org/fredshonorio/json-decoder)
[![Ccodecov](https://codecov.io/gh/fredshonorio/json-decoder/branch/master/graph/badge.svg)](https://codecov.io/gh/fredshonorio/json-decoder)

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
decodeString("1", String); // left("expected String, got JNumber{value=1}")
decodeString("\"string\"", String); // right("string")
```
## Arrays
`list` decodes a JSON array and decodes every element with a given decoder. Returns a javaslang `List<T>`.
`index` decodes an array and picks the element at a given index.

``` java
decodeString("[1, 2, 3]", list(Integer)); // right(List.of(1, 2, 3))
decodeString("[1, 2, \"a\"]", index(2, String)); // right("a")
```

## Dictionaries
`dict` decodes a `Map<String, T>` given a `Decoder<T>`.
``` java
decodeString("{\"a\": 1, \"b\": 2, \"c\": 3}", dict(Integer)); // right(HashMap.of("a", 1, "b", 2, "c", 3))
```

## Enums
Enums can be parsed by attempting to match a string exactly.
``` java
decodeString("\"ERA\"", enumByName(ChronoField.class)); // right(ChronoField.ERA)
```

## Transforming values with `map`
`map` can be used on a decoder to transform a decoded value. Like changing the container:
``` java
Decoder<LinkedList<Integer>> linkedList = list(Integer)
    .map(ints -> ints.toJavaCollection(LinkedList::new));

decodeString("[1, 2, 3]", linkedList); // right(LinkedList(1, 2, 3))
```

Or computing some other value, like the sum of an array:
``` java
Decoder<Integer> sum = list(Integer).map(ints -> ints.fold(0, (z, x) -> z + x));
decodeString("[1, 2, 3]", sum); // right(6)
```

## Object fields
`field` decodes an object and accesses a field within it, fails if the field is missing.
`at` traverses an object tree.
``` java
decodeString("{\"a\": \"b\"}", field("a", String)); // right("b")
decodeString("{\"a\": \"b\"}", field("b", String)); // left("field 'b': missing")
decodeString("{\"a\": {\"b\": \"c\"} }", at(List.of("a", "b"), String)); // right("c")

```
Decoders for complex structures can be built by composing other decoders with `map<N>`,
where `N` is the number of decoders:
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
decodeString("{\"name\":\"jack\"}", personDecoder); // left("field 'age': missing")
```

## Optional values
`option` will try to use a given encoder and return the result inside an `Option`,
if the decoder fails it just returns `Option.none()`. Therefore, it will never fail.
`optionalField` only succeeds if the field is missing or the field exists and
the inner decoder succeeds as well.

``` java
decodeString("1", option(Integer)); // right(Option.of(1))
decodeString("1", option(String)); // right(Option.none()) -- notice that decoding did not fail
```
In the following case, both `optionalField` and `option` return `none`:
``` java
decodeString("{\"b\": 1}", optionalField("a", String)); // right(Option.none())
decodeString("{\"b\": 1}", option(field("a", String))); // right(Option.none())
```
However, in this case `option` will silently ignore the unexpected number, while `optionalField` will fail
``` java
decodeString("{\"a\": 1}", optionalField("a", String)); // left("field 'a': expected String, got JNumber{value=1}")
decodeString("{\"a\": 1}", option(field("a", String))); // right(Option.none())
```

## Loosely typed values
`oneOf` attempts multiple decoders, `nullValue` returns a given value if `null` is found.
``` java
decodeString(
    "[1, \"hello\", null]",
    list(oneOf(
        Integer.map(i -> i.toString()), // we use `map` to turn `Integer` into a String decoder
        String,
        nullValue("<missing>"))));
// right(List.of("1", "hello", "<missing>"))
```
`nullable` allows null values, returns an `Option<T>`.
``` java
decodeString("[1, 2, null]", list(nullable(Integer))); // right(List.of(some(1), some(2), none()))
```

## Composing decoders with `andThen`

`andThen` can be used to apply a decoder after another (to the same JSON value), here are some examples:

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
First we decode the object and test if it has a `ver`, then we pick a decoder based on `ver` and apply it to the same object.

Extending a decoder to validate decoded values:
``` java
Decoder<String> nonEmptyString = String
    .andThen(str -> str.isEmpty()
        ? fail("empty string")
        : succeed(str));

decodeString("\"ok\"", nonEmptyString); // right("ok")
decodeString("\"\"", nonEmptyString); // left("empty string")
```
We first attempt to decode a string, and then return a failing decoder with a message if it's empty,
or a successful decoder otherwise.

[Here](src/test/java/com/fredhonorio/json_decoder/DecodersTest.java#L291) is an example of using `andThen` to build a `Decoder<T>` when `T` is abstract.

## Recursive structures
`recursive` can be used to build a decoder that references itself. This is necessary because Java lambdas can't reference `this`.
``` java
// given this Tree:
public class Tree<T> {
    final T v;
    final List<Tree<T>> children;
	// constructor
}

Decoder<Tree<Integer>> intTreeDecoder =
    recursive(self ->
        Decoder.map2(
            field("v", Integer),
            optionalField("children", list(self)).map(optList -> optList.getOrElse(List.empty())),
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

compile 'com.fredhonorio:json-decoder:1.1.0'
```

# Dependencies

__json-decoder__ uses [immutable-json-ast](https://github.com/hamnis/immutable-json/) for the JSON AST,
[jackson](https://github.com/FasterXML/jackson) for parsing JSON and [javaslang](http://www.javaslang.io/) for utility.

# License
This project is licensed under the Apache License v2.0.
