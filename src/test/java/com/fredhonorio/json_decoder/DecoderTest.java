package com.fredhonorio.json_decoder;

import javaslang.*;
import javaslang.collection.List;
import org.junit.*;
import org.junit.Test;

import static com.fredhonorio.json_decoder.Decoders.*;
import static com.fredhonorio.json_decoder.Decoders.Integer;
import static com.fredhonorio.json_decoder.Decoders.String;
import static com.fredhonorio.json_decoder.Test.assertError;
import static com.fredhonorio.json_decoder.Test.assertValue;
import static org.junit.Assert.*;

/**
 * Created by fred on 25/12/16.
 */
public class DecoderTest {

    @Test
    public void testMapN() {

        Decoder<Tuple4<String, String, Integer, Boolean>> dec4 = Decoder.map4(
            index(0, String),
            index(1, String),
            index(2, Integer),
            index(3, Boolean),
            Tuple::of);

        assertValue("[\"0\", \"1\", 2, true]", dec4, Tuple.of("0", "1", 2, true));

        Decoder<Tuple5<java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer>> dec5 = Decoder.map5(
            index(0, Integer),
            index(1, Integer),
            index(2, Integer),
            index(3, Integer),
            index(4, Integer),
            Tuple::of);

        assertValue(List.range(0, 5).mkString("[", ",", "]"), dec5, Tuple.of(0, 1, 2, 3, 4));

        Decoder<Tuple6<java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer>> dec6 = Decoder.map6(
            index(0, Integer),
            index(1, Integer),
            index(2, Integer),
            index(3, Integer),
            index(4, Integer),
            index(5, Integer),
            Tuple::of);

        assertValue(List.range(0, 6).mkString("[", ",", "]"), dec6, Tuple.of(0, 1, 2, 3, 4, 5));

        Decoder<Tuple7<java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer>> dec7 = Decoder.map7(
            index(0, Integer),
            index(1, Integer),
            index(2, Integer),
            index(3, Integer),
            index(4, Integer),
            index(5, Integer),
            index(6, Integer),
            Tuple::of
        );

        assertValue(List.range(0, 7).mkString("[", ",", "]"), dec7, Tuple.of(0, 1, 2, 3, 4, 5, 6));

        Decoder<Tuple8<java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer>> dec8 = Decoder.map8(
            index(0, Integer),
            index(1, Integer),
            index(2, Integer),
            index(3, Integer),
            index(4, Integer),
            index(5, Integer),
            index(6, Integer),
            index(7, Integer),
            Tuple::of);

        assertValue(List.range(0, 8).mkString("[", ",", "]"), dec8, Tuple.of(0, 1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    public void testOrElse() {
        assertValue("1", Integer.orElse(String.map(java.lang.Integer::parseInt)), 1);
        assertValue("1", String.map(java.lang.Integer::parseInt).orElse(Integer), 1);
    }

    @Test
    public void filter() throws Exception {
        Decoder<Integer> evenDecoder = Integer.filter(i -> i % 2 == 0, "don't even");

        assertValue("0", evenDecoder, 0);
        assertError("1", evenDecoder, "don't even");
    }

    @Test
    public void mapTry() throws Exception {

        Decoder<Integer> divFiveBy = Integer.mapTry(i -> 5 / i, "can't divide");
        assertValue("1", divFiveBy, 5);
        assertError("0", divFiveBy, "can't divide");

        Decoder<String> fail = String
            .mapTry(s -> { throw new RuntimeException("can't work"); }, Throwable::getMessage);

        assertError("\"\"", fail, "can't work");

        Decoder<String> fail2 = String
            .mapTry(s -> { throw new RuntimeException("can't work"); });

        assertError("\"\"", fail, "can't work");
    }

    @Test
    public void widen() {
        Decoder<Number> num = Decoder.widen(Integer);
        assertValue("1", num, 1);
    }

}