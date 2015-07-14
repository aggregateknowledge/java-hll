package net.agkn.hll.serialization;

import net.agkn.hll.HLL;
import net.agkn.hll.HLLType;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static net.agkn.hll.HLL.MAXIMUM_EXPTHRESH_PARAM;
import static net.agkn.hll.HLL.MAXIMUM_REGWIDTH_PARAM;
import static net.agkn.hll.HLL.MINIMUM_EXPTHRESH_PARAM;
import static net.agkn.hll.HLL.MINIMUM_LOG2M_PARAM;
import static net.agkn.hll.HLL.MINIMUM_REGWIDTH_PARAM;

/**
 * Serialization smoke-tests.
 *
 * @author yerenkow
 * @author benl
 */
public class HLLSerializationTest {
    // A fixed random seed so that this test is reproducible.
    private static final long RANDOM_SEED = 1L;

    /**
     * A smoke-test that covers serialization/deserialization of an HLL
     * under all possible parameters.
     */
    @Test
    public void serializationSmokeTest() throws Exception {
        final Random random = new Random(RANDOM_SEED);
        final int randomCount = 250;
        final List<Long> randoms = new ArrayList<Long>(randomCount);
        for (int i=0; i<randomCount; i++) {
          randoms.add(random.nextLong());
      }

        assertCardinality(HLLType.EMPTY, randoms);
        assertCardinality(HLLType.EXPLICIT, randoms);
        assertCardinality(HLLType.SPARSE, randoms);
        assertCardinality(HLLType.FULL, randoms);
    }

    // NOTE: log2m<=16 was chosen as the max log2m parameter so that the test
    //       completes in a reasonable amount of time. Not much is gained by
    //       testing larger values - there are no more known serialization
    //       related edge cases that appear as log2m gets even larger.
    // NOTE: This test completed successfully with log2m<=MAXIMUM_LOG2M_PARAM
    //       on 2014-01-30.
    private static void assertCardinality(final HLLType hllType, final Collection<Long> items)
           throws CloneNotSupportedException {
        for(int log2m=MINIMUM_LOG2M_PARAM; log2m<=16; log2m++) {
            for(int regw=MINIMUM_REGWIDTH_PARAM; regw<=MAXIMUM_REGWIDTH_PARAM; regw++) {
                for(int expthr=MINIMUM_EXPTHRESH_PARAM; expthr<=MAXIMUM_EXPTHRESH_PARAM; expthr++ ) {
                    for(final boolean sparse: new boolean[]{true, false}) {
                        HLL hll = new HLL(log2m, regw, expthr, sparse, hllType);
                        for(final Long item: items) {
                            hll.addRaw(item);
                        }
                        HLL copy = HLL.fromBytes(hll.toBytes());
                        assertEquals(copy.cardinality(), hll.cardinality());
                        assertEquals(copy.getType(), hll.getType());
                        assertEquals(copy.toBytes(), hll.toBytes());

                        HLL clone = hll.clone();
                        assertEquals(clone.cardinality(), hll.cardinality());
                        assertEquals(clone.getType(), hll.getType());
                        assertEquals(clone.toBytes(), hll.toBytes());
                    }
                }
            }
        }
    }
}
