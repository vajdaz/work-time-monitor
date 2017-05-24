package vajdaz.util;

public final class HelperFunctions {
	public static long[] splitTimeDiff(long diffInSeconds) {
		long[] diff = new long[5];

		if (diffInSeconds > 0) {
			diff[0] = 1;
		} else if (diffInSeconds < 0) {
			diffInSeconds = -diffInSeconds;
			diff[0] = -1;
		} else {
			diff[0] = 0;
		}

		diff[4] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
		diff[3] = ((diffInSeconds /= 60) >= 60) ? (diffInSeconds % 60) : diffInSeconds;
		diff[2] = ((diffInSeconds /= 60) >= 24) ? (diffInSeconds % 24) : diffInSeconds;
		diff[1] = (diffInSeconds / 24);
		return diff;
	}
}
