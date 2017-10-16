package com.fredhonorio.json_decoder.schema;

import com.fredhonorio.json_decoder.Decoder;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import net.hamnaberg.json.Json;
import org.junit.Assert;
import org.junit.Test;

import static com.fredhonorio.json_decoder.Decoders.*;
import static com.fredhonorio.json_decoder.Decoders.Boolean;
import static com.fredhonorio.json_decoder.Decoders.Double;
import static com.fredhonorio.json_decoder.Decoders.Float;
import static com.fredhonorio.json_decoder.Decoders.Integer;
import static com.fredhonorio.json_decoder.Decoders.JArray;
import static com.fredhonorio.json_decoder.Decoders.JNull;
import static com.fredhonorio.json_decoder.Decoders.JObject;
import static com.fredhonorio.json_decoder.Decoders.Long;
import static com.fredhonorio.json_decoder.Decoders.String;
import static net.hamnaberg.json.Json.*;

import java.lang.String;

public class JsonSchemaTest {


    private static void schemaEquals(Decoder<?> d, Json.JValue expected) {
        Assert.assertEquals(
            expected,
            JsonSchema.jsonSchema(d.schema())
        );
    }

    private static Json.JObject t(String type) {
        return jObject("type", type);
    }

    private static Json.JObject S_STRING = t("string");
    private static Json.JObject S_OBJECT = t("object");
    private static Json.JObject S_INT = t("integer");
    private static Json.JObject S_NULL = t("null");
    private static Json.JObject S_ARRAY = t("array");

    @Test
    public void basics() throws Exception {
        schemaEquals(Value, jEmptyObject());
        schemaEquals(JObject, t("object").put("additionalProperties", true));
        schemaEquals(JArray, t("array"));
        schemaEquals(JNull, t("null"));
        schemaEquals(String, S_STRING);
        schemaEquals(BigDecimal, t("number"));
        schemaEquals(Boolean, t("boolean"));
        schemaEquals(Float, t("number"));
        schemaEquals(Double, t("number"));
        schemaEquals(Integer, t("integer"));
        schemaEquals(Long, t("integer"));
    }

    @Test
    public void listTest() throws Exception {
        schemaEquals(list(Integer), t("array").put("items", t("integer")));
    }

    @Test
    public void fieldTest() throws Exception {

        schemaEquals(field("a", String),
            S_OBJECT
                .put("properties",
                    jObject("a", S_STRING))
                .put("additionalProperties", true)
                .put("required", jArray(jString("a"))));

        schemaEquals(optionalField("a", String),
            S_OBJECT
                .put("properties",
                    jObject("a", S_STRING))
                .put("additionalProperties", true));

        // TODO: might be useful to fill in the "default" field in the schema
        // https://spacetelescope.github.io/understanding-json-schema/reference/generic.html
        schemaEquals(optionalField("a", String, "DEFAULT"),
            S_OBJECT
                .put("properties",
                    jObject("a", S_STRING))
                .put("additionalProperties", true));
    }

    // TODO: somehow use allOf?

    @Test
    public void nullableTest() {
        schemaEquals(nullable(Integer),
            jObject("anyOf", jArray(S_INT, S_NULL)));

        // TODO: again, default values
        schemaEquals(nullable(Integer, 1),
            jObject("anyOf", jArray(S_INT, S_NULL)));
    }

    @Test
    public void nullValueTest() {
        schemaEquals(nullValue(1), S_NULL);
    }

    @Test
    public void oneOfTest() {
        Decoder<String> d = oneOf(Integer.map(i -> i.toString()), String);

        schemaEquals(d,
            jObject("anyOf", jArray(S_INT, S_STRING)));
    }

    @Test
    public void dictTest() {
        schemaEquals(dict(Integer),
            S_OBJECT
                .put("additionalProperties", S_INT));

        Decoder<String> d = oneOf(Integer.map(i -> i.toString()), String);
        schemaEquals(dict(d),
            S_OBJECT
                .put("additionalProperties", jObject("anyOf", jArray(S_INT, S_STRING))));
    }

    @Test
    public void indexTest() {
        // min items
    }


    @Test
    public void atTest() {
        schemaEquals(at(List.of("a", "b"), Integer),
            S_OBJECT
                .put("properties", jObject(
                    "a", S_OBJECT
                        .put("properties", jObject("b", S_INT))
                        .put("additionalProperties", true)
                        .put("required", jArray(jString("b")))))
                .put("additionalProperties", true)
                .put("required", jArray(jString("a"))));
    }

    static enum X {a, b}

    @Test
    public void enumByNameTest() {
        schemaEquals(enumByName(X.class),
            jObject("enum", jArray(jString("a"), jString("b"))));

        schemaEquals(enumByName(X.class, x -> x.name().toUpperCase()),
            jObject("enum", jArray(jString("A"), jString("B"))));
    }


    static class Tree {
        final int value;
        final List<Tree> children;

        Tree(int value, List<Tree> children) {
            this.value = value;
            this.children = children;
        }
    }

    @Test
    public void recursiveTest() {
        Decoder<Tree> d = recursive(self ->
            Decoder.map2(
                field("value", Integer),
                field("children", list(self.ref("hello"))),
                Tree::new));

        Json.JObject expected = S_OBJECT
            .put("properties",
                jObject("value", S_INT)
                    .put("children",
                        S_ARRAY
                            .put("items", jObject("$ref", "hello"))))
            .put("required", jArray(jString("value"), jString("children")))
            .put("additionalProperties", true);

        schemaEquals(d, expected);

        Decoder<Tree> d2 = recursive(self ->
            Decoder.map2(
                field("value", Integer),
                field("children", list(self)),
                Tree::new));

        Json.JObject expected2 = S_OBJECT
            .put("properties",
                jObject("value", S_INT)
                    .put("children",
                        S_ARRAY
                            .put("items", jObject("json-decoder error", "This decoder was defined recursively, but the reference was never set, see: http://example.com"))))
            .put("required", jArray(jString("value"), jString("children")))
            .put("additionalProperties", true);

        schemaEquals(d2, expected2);
    }

    @Test
    public void andThenTest() {
        schemaEquals(Integer.andThen(i -> succeed("pants")), S_INT);
    }

    @Test
    public void mapNTest() {
        Decoder<?> d = Decoder.map2(
            field("a", String),
            field("b", String),
            Tuple::of);

        schemaEquals(d,
            S_OBJECT
                .put("properties",
                    jObject("a", S_STRING)
                        .put("b", S_STRING))
                .put("additionalProperties", true)
                .put("required", jArray(jString("a"), jString("b"))));

        Decoder<?> impossible = Decoder.map2(
            field("a", String),
            String,
            Tuple::of);

        schemaEquals(impossible,
            jObject("allOf", jArray(
                S_OBJECT
                    .put("properties",
                        jObject("a", S_STRING))
                    .put("additionalProperties", true)
                    .put("required", jArray(jString("a"))),
                S_STRING
            )));


        // TODO: all variants from 2 to 8
    }

}