package com.fredhonorio.json_decoder;

import javaslang.control.Either;
import javaslang.control.Option;
import net.hamnaberg.json.Json;
import org.junit.Test;

import static com.fredhonorio.json_decoder.Decoders.*;
import static com.fredhonorio.json_decoder.Test.assertValue;
import static net.hamnaberg.json.Json.jObject;
import static org.junit.Assert.assertTrue;

public class ComplexTest {

    public static enum City {
        MADRID
    }

    public static class Address {
        public final String postalCode;
        public final City city;
        public final Option<Location> location;

        public Address(String postalCode, City city, Option<Location> location) {
            this.postalCode = postalCode;
            this.city = city;
            this.location = location;
        }
    }

    public static class Location {
        public final String street;

        public Location(String street) {
            this.street = street;
        }
    }

    private static Decoder<String> postalCodeDecoder = oneOf(String, Integer.map(Object::toString), nullValue("0"));
    private static Decoder<Location> locationDecoder = field("street", String).map(Location::new);
    private static Decoder<Address> addressDecoder = Decoder.map3(
        field("postalCode", postalCodeDecoder),
        field("city", enumByName(City.class)),
        optionalField("location", locationDecoder),
        Address::new);

    @Test
    public void testComplex() {
        Either<?, Address> r;

        r = addressDecoder.apply(
            jObject("postalCode", 123)
                .put("city", "MADRID")
        );

        assertTrue(r.isRight());

        r = addressDecoder.apply(
            jObject("postalCode", Json.jNull())
                .put("city", "MADRID")
                .put("location", jObject("street", "1st Street"))
        );

        assertTrue(r.isRight());

        r = addressDecoder.apply(
            jObject("postalCode", "123-321")
                .put("city", "MADRID")
                .put("location", jObject("street", 1))
        );

        assertTrue(r.isLeft());
    }

}
