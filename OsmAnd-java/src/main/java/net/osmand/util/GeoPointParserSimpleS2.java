package net.osmand.util;

/**
 * LLM: ChatGPT 5.2
 * GeoPointParserSimpleS2
 * Minimal S2 CellId -> lat/lon (cell center) implementation.
 */

public final class GeoPointParserSimpleS2 {

	// ---- public API ----

	public static final class CellId {
		private final long id;

		public CellId(long id) { this.id = id; }

		/** "0x3f8d..." -> CellId (unsigned 64-bit stored in signed long). */
		public static CellId fromHex(String hex) {
			String h = (hex.startsWith("0x") || hex.startsWith("0X")) ? hex.substring(2) : hex;
			return new CellId(Long.parseUnsignedLong(h, 16));
		}

		/**
		 * "0xAAA:0xBBB" or "AAA:BBB" -> uses the FIRST part as S2 CellId.
		 */
		public static CellId fromFtid(String ftid) {
			String first = ftid;
			int colon = ftid.indexOf(':');
			if (colon >= 0) first = ftid.substring(0, colon);
			return fromHex(first.trim());
		}

		/** Basic structural validation (same idea as S2CellId::is_valid). */
		public boolean isValid() {
			return face() < NUM_FACES && ((lowestOnBit() & 0x1555_5555_5555_5555L) != 0);
		}

		/**
		 * Returns cell center as [latDeg, lonDeg].
		 * This is the center of the S2 cell, NOT necessarily the POI coordinate.
		 */
		public double[] toLatLon() {
			Vec3 p = toPointRaw().normalize();
			double lat = Math.atan2(p.z, Math.sqrt(p.x * p.x + p.y * p.y));
			double lon = Math.atan2(p.y, p.x);
			return new double[]{ lat * RAD_TO_DEG, lon * RAD_TO_DEG };
		}

		// ---- internals ----

		private int face() { return (int) (id >>> POS_BITS); }

		private boolean isLeaf() { return (((int) id) & 1) != 0; }

		private long lowestOnBit() { return id & -id; }

		private Vec3 toPointRaw() {
			MutableInt i = new MutableInt(0);
			MutableInt j = new MutableInt(0);
			int face = toFaceIJ(i, j);

			// This mirrors the "cell center" handling used by S2 for non-leaf cells.
			int delta = isLeaf() ? 1 : ((((i.v ^ (((int) id) >>> 2)) & 1) != 0) ? 2 : 0);

			int si = (i.v << 1) + delta - MAX_SIZE;
			int ti = (j.v << 1) + delta - MAX_SIZE;

			return faceSiTiToXyz(face, si, ti);
		}

		private int toFaceIJ(MutableInt pi, MutableInt pj) {
			int face = face();
			int bits = (face & SWAP_MASK);
			for (int k = 7; k >= 0; --k) bits = getBits1(pi, pj, k, bits);
			return face;
		}

		private int getBits1(MutableInt i, MutableInt j, int k, int bits) {
			final int nbits = (k == 7) ? (MAX_LEVEL - 7 * LOOKUP_BITS) : LOOKUP_BITS;

			bits += (((int) (id >>> (k * 2 * LOOKUP_BITS + 1)) & ((1 << (2 * nbits)) - 1))) << 2;
			bits = LOOKUP_IJ[bits];

			i.v += ((bits >> (LOOKUP_BITS + 2)) << (k * LOOKUP_BITS));
			j.v += (((bits >> 2) & ((1 << LOOKUP_BITS) - 1)) << (k * LOOKUP_BITS));

			return bits & (SWAP_MASK | INVERT_MASK);
		}
	}

	// ---- minimal vector math ----

	private record Vec3(double x, double y, double z) {
		Vec3 normalize() {
				double n = Math.sqrt(x * x + y * y + z * z);
				return new Vec3(x / n, y / n, z / n);
			}
		}

	private static final class MutableInt { int v; MutableInt(int v) { this.v = v; } }

	// ---- S2 projection helpers ----

	// Quadratic projection used by S2.
	private static double stToUV(double s) {
		if (s >= 0) return (1.0 / 3.0) * ((1 + s) * (1 + s) - 1);
		return (1.0 / 3.0) * (1 - (1 - s) * (1 - s));
	}

	private static Vec3 faceUvToXyz(int face, double u, double v) {
		return switch (face) {
			case 0 -> new Vec3(1, u, v);
			case 1 -> new Vec3(-u, 1, v);
			case 2 -> new Vec3(-u, -v, 1);
			case 3 -> new Vec3(-1, -v, -u);
			case 4 -> new Vec3(v, -1, -u);
			default -> new Vec3(v, u, -1);
		};
	}

	private static Vec3 faceSiTiToXyz(int face, int si, int ti) {
		final double kScale = 1.0 / MAX_SIZE;
		double u = stToUV(kScale * si);
		double v = stToUV(kScale * ti);
		return faceUvToXyz(face, u, v);
	}

	// ---- Hilbert lookup tables (same concept as S2) ----

	private static final int MAX_LEVEL = 30;
	private static final int POS_BITS  = 2 * MAX_LEVEL + 1; // 61
	private static final int NUM_FACES = 6;
	private static final int MAX_SIZE  = 1 << MAX_LEVEL;

	private static final int LOOKUP_BITS = 4;
	private static final int SWAP_MASK   = 0x01;
	private static final int INVERT_MASK = 0x02;

	private static final int[] LOOKUP_IJ  = new int[1 << (2 * LOOKUP_BITS + 2)];

	private static final int[] POS_TO_ORIENTATION = { SWAP_MASK, 0, 0, INVERT_MASK | SWAP_MASK };

	private static final int[][] POS_TO_IJ = {
			{0, 1, 3, 2},
			{0, 2, 3, 1},
			{3, 2, 0, 1},
			{3, 1, 0, 2}
	};

	private static int posToOrientation(int position) { return POS_TO_ORIENTATION[position & 3]; }
	private static int posToIJ(int orientation, int position) { return POS_TO_IJ[orientation & 3][position & 3]; }

	static {
		initLookupCell(0, 0, 0, 0, 0, 0);
		initLookupCell(0, 0, 0, SWAP_MASK, 0, SWAP_MASK);
		initLookupCell(0, 0, 0, INVERT_MASK, 0, INVERT_MASK);
		initLookupCell(0, 0, 0, SWAP_MASK | INVERT_MASK, 0, SWAP_MASK | INVERT_MASK);
	}

	private static void initLookupCell(int level, int i, int j,
	                                   int origOrientation, int pos, int orientation) {
		if (level == LOOKUP_BITS) {
			int ij = (i << LOOKUP_BITS) + j;
			LOOKUP_IJ[(pos << 2) + origOrientation] = (ij << 2) + orientation;
		} else {
			level++;
			i <<= 1; j <<= 1; pos <<= 2;
			for (int subPos = 0; subPos < 4; subPos++) {
				int ij = posToIJ(orientation, subPos);
				int orientationMask = posToOrientation(subPos);
				initLookupCell(level, i + (ij >>> 1), j + (ij & 1),
						origOrientation, pos + subPos, orientation ^ orientationMask);
			}
		}
	}

	private static final double RAD_TO_DEG = 180.0 / Math.PI;

	private GeoPointParserSimpleS2() {}
}
