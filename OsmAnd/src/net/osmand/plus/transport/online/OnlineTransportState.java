package net.osmand.plus.transport.online;

public class OnlineTransportState {

	private static volatile long timeMillis = 0;
	private static volatile boolean arriveBy = false;

	private OnlineTransportState() {
	}

	public static long getTimeMillis() {
		return timeMillis;
	}

	public static void setTimeMillis(long value) {
		timeMillis = value;
	}

	public static boolean isArriveBy() {
		return arriveBy;
	}

	public static void setArriveBy(boolean value) {
		arriveBy = value;
	}

	public static boolean isNow() {
		return timeMillis <= 0;
	}

	public static void reset() {
		timeMillis = 0;
		arriveBy = false;
	}
}
