package net.agkn.hll.util;

/*
 * Copyright 2013 Aggregate Knowledge, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.agkn.hll.HLL;

/**
 * Static functions for computing constants and parameters used in the HLL
 * algorithm.
 *
 * @author timon
 */
public final class HLLUtil {
    /**
     * Precomputed <code>pwMaxMask</code> values indexed by <code>registerSizeInBits</code>.
     * Calculated with this formula:
     * <pre>
     *     int maxRegisterValue = (1 << registerSizeInBits) - 1;
     *     // Mask with all bits set except for (maxRegisterValue - 1) least significant bits (see #addRaw())
     *     return ~((1L << (maxRegisterValue - 1)) - 1);
     * </pre>
     *
     * @see #pwMaxMask(int)
     */
    private static final long[] PW_MASK = {
            ~((1L << (((1 << 0) - 1) - 1)) - 1),
            ~((1L << (((1 << 1) - 1) - 1)) - 1),
            ~((1L << (((1 << 2) - 1) - 1)) - 1),
            ~((1L << (((1 << 3) - 1) - 1)) - 1),
            ~((1L << (((1 << 4) - 1) - 1)) - 1),
            ~((1L << (((1 << 5) - 1) - 1)) - 1),
            ~((1L << (((1 << 6) - 1) - 1)) - 1),
            ~((1L << (((1 << 7) - 1) - 1)) - 1),
            ~((1L << (((1 << 8) - 1) - 1)) - 1)
    };

    /**
     * Precomputed <code>twoToL</code> values indexed by a linear combination of
     * <code>regWidth</code> and <code>log2m</code>. Calculated with this formula:
     *
     * <pre>
     *     int maxRegisterValue = (1 << registerSizeInBits) - 1;
     *     // since 1 is added to p(w) only maxRegisterValue - 1 bits are inspected
     *     final int pwBits = (maxRegisterValue - 1);
     *     final int totalBits = (pwBits + log2m);
     *     final long twoToL = (1L << totalBits);
     * </pre>
     *
     * The array is one-dimensional and can be accessed by using index
     * <code>(REG_WIDTH_INDEX_MULTIPLIER * regWidth) + log2m</code>
     * for <code>regWidth</code> and <code>log2m</code> between the specified
     * <code>HLL.{MINIMUM,MAXIMUM}_{REGWIDTH,LOG2M}_PARAM</code> constants.
     *
     * @see #largeEstimatorCutoff(int, int), #largeEstimator(int, int, double),
     */
    private static final long[] TWO_TO_L = new long[(HLL.MAXIMUM_REGWIDTH_PARAM + 1) * (HLL.MAXIMUM_LOG2M_PARAM + 1)];

    /**
     * Spacing constant used to compute offsets into {@link TWO_TO_L}.
     */
    private static final int REG_WIDTH_INDEX_MULTIPLIER = HLL.MAXIMUM_LOG2M_PARAM + 1;

    static {
        for(int regWidth = HLL.MINIMUM_REGWIDTH_PARAM; regWidth <= HLL.MAXIMUM_REGWIDTH_PARAM; regWidth++) {
            for(int log2m = HLL.MINIMUM_LOG2M_PARAM ; log2m <= HLL.MAXIMUM_LOG2M_PARAM; log2m++) {
                TWO_TO_L[(REG_WIDTH_INDEX_MULTIPLIER * regWidth) + log2m] = (1L << (((1 << regWidth) - 1 - 1) + log2m));
            }
        }
    }

    // ************************************************************************
    /**
     * Computes the bit-width of HLL registers necessary to estimate a set of
     * the specified cardinality.
     *
     * @param  expectedUniqueElements an upper bound on the number of unique
     *         elements that are expected.  This must be greater than zero.
     * @return a register size in bits (i.e. <code>log2(log2(n))</code>)
     */
    public static int registerBitSize(final long expectedUniqueElements) {
        return Math.max(4/*min size defined in paper*/,
                        (int)Math.ceil(NumberUtil.log2(NumberUtil.log2(expectedUniqueElements))));
    }

    // ========================================================================
    /**
     * Computes the 'alpha-m-squared' constant used by the HyperLogLog algorithm.
     *
     * @param  m this must be a power of two, cannot be less than
     *         16 (2<sup>4</sup>), and cannot be greater than 65536 (2<sup>16</sup>).
     * @return gamma times <code>registerCount</code> squared where gamma is
     *         based on the value of <code>registerCount</code>
     * @throws IllegalArgumentException if <code>registerCount</code> is less
     *         than 16
     */
    public static double alphaMSquared(final int m) {
        switch(m) {
            case 1/*2^0*/:
            case 2/*2^1*/:
            case 4/*2^2*/:
            case 8/*2^3*/:
                throw new IllegalArgumentException("'m' cannot be less than 16 (" + m + " < 16).");

            case 16/*2^4*/:
                return 0.673 * m * m;

            case 32/*2^5*/:
                return 0.697 * m * m;

            case 64/*2^6*/:
                return 0.709 * m * m;

            default/*>2^6*/:
                return (0.7213 / (1.0 + 1.079 / m)) * m * m;
        }
    }

    // ========================================================================
    /**
     * Computes a mask that prevents overflow of HyperLogLog registers.
     *
     * @param  registerSizeInBits the size of the HLL registers, in bits.
     * @return mask a <code>long</code> mask to prevent overflow of the registers
     * @see #registerBitSize(long)
     */
    public static long pwMaxMask(final int registerSizeInBits) {
        return PW_MASK[registerSizeInBits];
    }

    // ========================================================================
    /**
     * The cutoff for using the "small range correction" formula, in the
     * HyperLogLog algorithm.
     *
     * @param  m the number of registers in the HLL. <em>m<em> in the paper.
     * @return the cutoff for the small range correction.
     * @see #smallEstimator(int, int)
     */
    public static double smallEstimatorCutoff(final int m) {
        return ((double)m * 5) / 2;
    }

    /**
     * The "small range correction" formula from the HyperLogLog algorithm. Only
     * appropriate if both the estimator is smaller than <pre>(5/2) * m</pre> and
     * there are still registers that have the zero value.
     *
     * @param  m the number of registers in the HLL. <em>m<em> in the paper.
     * @param  numberOfZeroes the number of registers with value zero. <em>V</em>
     *         in the paper.
     * @return a corrected cardinality estimate.
     */
    public static double smallEstimator(final int m, final int numberOfZeroes) {
        return m * Math.log((double)m / numberOfZeroes);
    }

    /**
     * The cutoff for using the "large range correction" formula, from the
     * HyperLogLog algorithm.
     *
     * @param  log2m log-base-2 of the number of registers in the HLL. <em>b<em> in the paper.
     * @param  registerSizeInBits the size of the HLL registers, in bits.
     * @return the cutoff for the large range correction.
     * @see #largeEstimator(int, int, double)
     */
    public static double largeEstimatorCutoff(final int log2m, final int registerSizeInBits) {
        return (TWO_TO_L[(REG_WIDTH_INDEX_MULTIPLIER * registerSizeInBits) + log2m]) / 30.0;
    }

    /**
     * The "large range correction" formula from the HyperLogLog algorithm. Only
     * appropriate for estimators whose value exceeds the return of
     * {@link #largeEstimatorCutoff(int, int)}.
     *
     * @param  log2m log-base-2 of the number of registers in the HLL. <em>b<em> in the paper.
     * @param  registerSizeInBits the size of the HLL registers, in bits.
     * @param  estimator the original estimator ("E" in the paper).
     * @return a corrected cardinality estimate.
     */
    public static double largeEstimator(final int log2m, final int registerSizeInBits, final double estimator) {
        return (-1 * TWO_TO_L[(REG_WIDTH_INDEX_MULTIPLIER * registerSizeInBits) + log2m])
                * Math.log(1.0 - (estimator/TWO_TO_L[(REG_WIDTH_INDEX_MULTIPLIER * registerSizeInBits) + log2m]));
    }
}
