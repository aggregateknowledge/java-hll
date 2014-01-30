package net.agkn.hll.serialization;

import net.agkn.hll.HLL;
import net.agkn.hll.HLLType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * Simple test, should not fail under any circumstances.
 *
 * Created by yerenkow
 */
public class HllSerializationTest {

    @Test
    public void runHllSerializeTest () {

        Boolean[] booleans  = new Boolean[] {Boolean.FALSE, Boolean.TRUE};

        for(int log2m =  4; log2m <= 30; log2m ++) {
            for(int regw =  1; regw <=8; regw ++) {
                for(int expthr = -1; expthr <= 18; expthr ++ ) {
                    for (int i = 0; i < HLLType.values().length; i++) {
                        for (int j = 0; j < booleans.length; j++) {
                            Boolean aBoolean = booleans[j];

                            HLLType hllType = HLLType.values()[i];

                            HLL hll = new HLL(log2m, regw, expthr, aBoolean, hllType);
                            HLL hllCopy = HLL.fromBytes(hll.toBytes());

                            Assert.assertNotNull(hllCopy);

                        }
                    }
                }
            }
        }
    }

}
