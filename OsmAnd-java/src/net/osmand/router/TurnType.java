package net.osmand.router;

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
	public static final String OFFR = "OFFR"; // Off route //$NON-NLS-1$

	// If the lane is usable for the current turn
	public static final int BIT_LANE_ALLOWED = 1;

	// If an action on a lane is allowed
	public static final int BIT_LANE_STRAIGHT_ALLOWED = 1 << 3;
	public static final int BIT_LANE_SLIGHT_RIGHT_ALLOWED = 1 << 4;
	public static final int BIT_LANE_SLIGHT_LEFT_ALLOWED = 1 << 5;
	public static final int BIT_LANE_RIGHT_ALLOWED = 1 << 6;
	public static final int BIT_LANE_LEFT_ALLOWED = 1 << 7;
	public static final int BIT_LANE_SHARP_RIGHT_ALLOWED = 1 << 8;
	public static final int BIT_LANE_SHARP_LEFT_ALLOWED = 1 << 9;
	public static final int BIT_LANE_UTURN_ALLOWED = 1 << 10;

	// Which action is needed for the current turn
	public static final int BIT_LANE_USE_STRAIGHT = 0;
	public static final int BIT_LANE_USE_SLIGHT_RIGHT = 1 << 11;
	public static final int BIT_LANE_USE_SLIGHT_LEFT = 1 << 12;
	public static final int BIT_LANE_USE_RIGHT = 1 << 12 | 1 << 11;
	public static final int BIT_LANE_USE_LEFT = 1 << 13;
	public static final int BIT_LANE_USE_SHARP_RIGHT = 1 << 13 | 1 << 11;
	public static final int BIT_LANE_USE_SHARP_LEFT = 1 << 13 | 1 << 12;
	public static final int BIT_LANE_USE_UTURN = 1 << 13 | 1 << 12 | 1 << 11;


	public static String[] predefinedTypes = new String[] { C, KL, KR, TL, TSLL, TSHL, TR, TSLR, TSHR, TU, TRU, OFFR };

	public static TurnType sraight() {
		return valueOf(C, false);
	}

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
	// calculated clockwise head rotation if previous direction to NORTH
	private float turnAngle;
	private boolean skipToSpeak;
	private int[] lanes;

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

	// calculated Clockwise head rotation if previous direction to NORTH
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
	
	// lanes encoded as array of int 
	// Use the BIT_LANE_* constants to get the properties of each lane
	public void setLanes(int[] lanes) {
		this.lanes = lanes;
	}
	
	public int[] getLanes() {
		return lanes;
	}
	
	public boolean keepLeft() {
		return value.equals(KL); 
	}
	
	public boolean keepRight() {
		return value.equals(KR); 
	}
	
	public boolean goAhead() {
		return value.equals(C); 
	}
	
	public boolean isSkipToSpeak() {
		return skipToSpeak;
	}
	public void setSkipToSpeak(boolean skipToSpeak) {
		this.skipToSpeak = skipToSpeak;
	}
	
	@Override
	public String toString() {
		if(isRoundAbout()){
			return "Take " + getExitOut() + " exit";
		} else if(value.equals(C)) {
			return "Go ahead";
		} else if(value.equals(TSLL)) {
			return "Turn slightly left";
		} else if(value.equals(TL)) {
			return "Turn left";
		} else if(value.equals(TSHL)) {
			return "Turn sharply left";
		} else if(value.equals(TSLR)) {
			return "Turn slightly right";
		} else if(value.equals(TR)) {
			return "Turn right";
		} else if(value.equals(TSHR)) {
			return "Turn sharply right";
		} else if(value.equals(TU)) {
			return "Make uturn";
		} else if(value.equals(TRU)) {
			return "Make uturn";
		} else if(value.equals(KL)) {
			return "Keep left";
		} else if(value.equals(KR)) {
			return "Keep right";
		} else if(value.equals(OFFR)) {
			return "Off route";
		}
		return super.toString();
	}
}
