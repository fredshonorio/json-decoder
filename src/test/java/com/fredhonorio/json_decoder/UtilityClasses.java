package com.fredhonorio.json_decoder;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.*;
import org.junit.Test;

public class UtilityClasses {

    // I guess I really like that 100% coverage
    @Test
    public void testUtilityClasses() throws ReflectiveOperationException {
        // https://trajano.net/2013/04/covering-utility-classes/
        UtilityClassTestUtil.assertUtilityClassWellDefined(Decoders.class);
        UtilityClassTestUtil.assertUtilityClassWellDefined(EitherExtra.class);
    }
}
