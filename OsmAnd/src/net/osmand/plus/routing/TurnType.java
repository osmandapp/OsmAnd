package net.osmand.plus.routing;

public class TurnType {
	public static final String C = "C"; // continue (go straight) //$NON-NLS-1$
	public static final String TL = "TL"; // turn left //$NON-NLS-1$
	public static final String TSLL = "TSLL"; // turn slightly left //$NON-NLS-1$
	public static final String TSHL = "TSHL"; // turn sharply left //$NON-NLS-1$
	public static final String TR = "TR"; // turn right //$NON-NLS-1$
	public static final String TSLR = "TSLR"; // turn slightly right //$NON-NLS-1$
	public static final String TSHR = "TSHR"; // turn sharply right //$NON-NLS-1$
	public static final String KL = "KL"; // keep left //$NON-NLS-1$
	public static final String KR = "KR"; // keep right//$NON-NLS-1$
	public static final String TU = "TU"; // U-turn //$NON-NLS-1$
	public static final String TRU = "TRU"; // Right U-turn //$NON-NLS-1$
	public static String[] predefinedTypes = new String[] { C, KL, KR, TL, TSLL, TSHL, TR, TSLR, TSHR, TU, TRU };

	public static TurnType valueOf(String s, boolean leftSide) {
		for (String v : predefinedTypes) {
			if (v.equals(s)) {
				if (leftSide && TU.equals(v)) {
					v = TRU;
				}
				return new TurnType(v);
			}
		}
		if (s != null && s.startsWith("EXIT")) { //$NON-NLS-1$
			return getExitTurn(Integer.parseInt(s.substring(4)), 0, leftSide);
		}
		return null;
	}

	private final String value;
	private int exitOut;
	private boolean isLeftSide;
	// calculated CW head rotation if previous direction to NORTH
	private float turnAngle;

	private static TurnType getExitTurn(int out, float angle, boolean leftSide) {
		TurnType r = new TurnType("EXIT", out, leftSide); //$NON-NLS-1$
		r.setTurnAngle(angle);
		return r;
	}

	private TurnType(String value, int exitOut, boolean leftSide) {
		this.value = value;
		this.exitOut = exitOut;
		this.isLeftSide = leftSide;
	}

	// calculated CW head rotation if previous direction to NORTH
	public float getTurnAngle() {
		return turnAngle;
	}

	public boolean isLeftSide() {
		return isLeftSide;
	}

	public void setTurnAngle(float turnAngle) {
		this.turnAngle = turnAngle;
	}

	private TurnType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public int getExitOut() {
		return exitOut;
	}

	public boolean isRoundAbout() {
		return value.equals("EXIT"); //$NON-NLS-1$
	}
}