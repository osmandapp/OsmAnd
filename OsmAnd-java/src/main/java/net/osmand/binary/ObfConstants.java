package net.osmand.binary;

public class ObfConstants {

	public static final int SHIFT_MULTIPOLYGON_IDS = 43;
	public static final int SHIFT_NON_SPLIT_EXISTING_IDS = 41;

	public static final int SHIFT_PROPAGATED_NODE_IDS = 50;
	public static final int SHIFT_PROPAGATED_NODES_BITS = 11;
	public static final long MAX_COUNT_PROPAGATED_NODES = (1L << SHIFT_PROPAGATED_NODES_BITS) - 1;//2047
}
