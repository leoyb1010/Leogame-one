package com.shatteredpixel.shatteredpixeldungeon.bukov;

/**
 * Numeric helpers implemented only with the Java subset present in RoboVM
 * 2.3.24. Keep gameplay code off newer java.lang convenience methods.
 */
public final class BukovNumbers {

	private BukovNumbers() {
	}

	public static boolean isFinite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	public static boolean isFinite(double value) {
		return !Double.isNaN(value) && !Double.isInfinite(value);
	}

	public static int addExact(int first, int second) {
		long result = (long)first + second;
		if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
			throw new ArithmeticException("integer overflow");
		}
		return (int)result;
	}

	public static long addExact(long first, long second) {
		long result = first + second;
		if (((first ^ result) & (second ^ result)) < 0L) {
			throw new ArithmeticException("long overflow");
		}
		return result;
	}

	public static int floorMod(int value, int modulus) {
		if (modulus <= 0) {
			throw new IllegalArgumentException("modulus must be positive");
		}
		int result = value % modulus;
		return result < 0 ? result + modulus : result;
	}

	public static long floorMod(long value, long modulus) {
		if (modulus <= 0L) {
			throw new IllegalArgumentException("modulus must be positive");
		}
		long result = value % modulus;
		return result < 0L ? result + modulus : result;
	}

	public static long remainderUnsigned(long dividend, long divisor) {
		if (divisor <= 0L) {
			throw new IllegalArgumentException("divisor must be positive");
		}
		if (dividend >= 0L) {
			return dividend % divisor;
		}
		long quotient = ((dividend >>> 1) / divisor) << 1;
		long remainder = dividend - quotient * divisor;
		return remainder >= divisor ? remainder - divisor : remainder;
	}

	public static long toUnsignedLong(int value) {
		return value & 0xffffffffL;
	}

	public static String toUnsignedString(long value) {
		if (value >= 0L) {
			return Long.toString(value);
		}
		long quotient = (value >>> 1) / 5L;
		long remainder = value - quotient * 10L;
		return Long.toString(quotient) + remainder;
	}
}
