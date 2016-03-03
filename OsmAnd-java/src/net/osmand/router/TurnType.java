package net.osmand.router;

public class TurnType {
	public static final int C = 1;//"C"; // continue (go straight) //$NON-NLS-1$
	public static final int TL = 2; // turn left //$NON-NLS-1$
	public static final int TSLL = 3; // turn slightly left //$NON-NLS-1$
	public static final int TSHL = 4; // turn sharply left //$NON-NLS-1$
	public static final int TR = 5; // turn right //$NON-NLS-1$
	public static final int TSLR = 6; // turn slightly right //$NON-NLS-1$
	public static final int TSHR = 7; // turn sharply right //$NON-NLS-1$
	public static final int KL = 8; // keep left //$NON-NLS-1$
	public static final int KR = 9; // keep right//$NON-NLS-1$
	public static final int TU = 10; // U-turn //$NON-NLS-1$
	public static final int TRU = 11; // Right U-turn //$NON-NLS-1$
	public static final int OFFR = 12; // Off route //$NON-NLS-1$
	public static final int RNDB = 13; // Roundabout
	public static final int RNLB = 14; // Roundabout left
	
	public static TurnType straight() {
		return valueOf(C, false);
	}
	
	public String toXmlString() {
		switch (value) {
		case C:
			return "C";
		case TL:
			return "TL";
		case TSLL:
			return "TSLL";
		case TSHL:
			return "TSHL";
		case TR:
			return "TR";
		case TSLR:
			return "TSLR";
		case TSHR:
			return "TSHR";
		case KL:
			return "KL";
		case KR:
			return "KR";
		case TU:
			return "TU";
		case TRU:
			return "TRU";
		case OFFR:
			return "OFFR";
		case RNDB:
			return "RNDB"+exitOut;
		case RNLB:
			return "RNLB"+exitOut;
		}
		return "C";
	}
	
	public static TurnType fromString(String s, boolean leftSide) {
		TurnType t = null;
		if ("C".equals(s)) {
			t = TurnType.valueOf(C, leftSide);
		} else if ("TL".equals(s)) {
			t = TurnType.valueOf(TL, leftSide);
		} else if ("TSLL".equals(s)) {
			t = TurnType.valueOf(TSLL, leftSide);
		} else if ("TSHL".equals(s)) {
			t = TurnType.valueOf(TSHL, leftSide);
		} else if ("TR".equals(s)) {
			t = TurnType.valueOf(TR, leftSide);
		} else if ("TSLR".equals(s)) {
			t = TurnType.valueOf(TSLR, leftSide);
		} else if ("TSHR".equals(s)) {
			t = TurnType.valueOf(TSHR, leftSide);
		} else if ("KL".equals(s)) {
			t = TurnType.valueOf(KL, leftSide);
		} else if ("KR".equals(s)) {
			t = TurnType.valueOf(KR, leftSide);
		} else if ("TU".equals(s)) {
			t = TurnType.valueOf(TU, leftSide);
		} else if ("TRU".equals(s)) {
			t = TurnType.valueOf(TRU, leftSide);
		} else if ("OFFR".equals(s)) {
			t = TurnType.valueOf(OFFR, leftSide);
		} else if (s != null && (s.startsWith("EXIT") ||
				s.startsWith("RNDB") || s.startsWith("RNLB"))) {
			try {
				t = TurnType.getExitTurn(Integer.parseInt(s.substring(4)), 0, leftSide);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if(t == null) {
			t = TurnType.straight();
		}
		return t;
	}
	

	public static TurnType valueOf(int vs, boolean leftSide) {
		if(vs == TU && leftSide) {
			vs = TRU;
		} else if(vs == RNDB && leftSide) {
			vs = RNLB;
		}
	
		return new TurnType(vs);
//		if (s != null && s.startsWith("EXIT")) { //$NON-NLS-1$
//			return getExitTurn(Integer.parseInt(s.substring(4)), 0, leftSide);
//		}
//		return null;
	}

	private final int value;
	private int exitOut;
	// calculated clockwise head rotation if previous direction to NORTH
	private float turnAngle;
	private boolean skipToSpeak;
	private int[] lanes;

	public static TurnType getExitTurn(int out, float angle, boolean leftSide) {
		TurnType r = valueOf(RNDB, leftSide); //$NON-NLS-1$
		r.exitOut = out;
		r.setTurnAngle(angle);
		return r;
	}
	
	
	private TurnType(int vl) {
		this.value = vl;
	}


	// calculated Clockwise head rotation if previous direction to NORTH
	public float getTurnAngle() {
		return turnAngle;
	}

	public boolean isLeftSide() {
		return value == RNLB || value == TRU;
	}

	public void setTurnAngle(float turnAngle) {
		this.turnAngle = turnAngle;
	}

	public int getValue() {
		return value;
	}

	public int getExitOut() {
		return exitOut;
	}

	public boolean isRoundAbout() {
		return value == RNDB || value == RNLB; //$NON-NLS-1$
	}
	
	// lanes encoded as array of int
	// 0 bit - 0/1 - to use or not
	// 1-5 bits - additional turn info 
	// 6-10 bits - secondary turn
	public void setLanes(int[] lanes) {
		this.lanes = lanes;
	}
	
	// Note that the primary turn will be the one displayed on the map.
	public static void setPrimaryTurnAndReset(int[] lanes, int lane, int turnType) {
		lanes[lane] = (turnType << 1);
	}
	
	
	public static int getPrimaryTurn(int laneValue) {
		// Get the primary turn modifier for the lane
		return (laneValue >> 1) & ((1 << 4) - 1);
	}

	public static void setSecondaryTurn(int[] lanes, int lane, int turnType) {
		lanes[lane] &= ~(15 << 5);
		lanes[lane] |= (turnType << 5);
	}

	public static int getSecondaryTurn(int laneValue) {
		// Get the primary turn modifier for the lane
		return (laneValue >> 5);
	}

	
	public int[] getLanes() {
		return lanes;
	}
	
	public boolean keepLeft() {
		return value == KL;
	}
	
	public boolean keepRight() {
		return value == KR;
	}
	
	public boolean goAhead() {
		return value == C;
	}
	
	public boolean isSkipToSpeak() {
		return skipToSpeak;
	}
	
	public void setSkipToSpeak(boolean skipToSpeak) {
		this.skipToSpeak = skipToSpeak;
	}
	
	@Override
	public String toString() {
		if (isRoundAbout()) {
			return "Take " + getExitOut() + " exit";
		} else if (value == C) {
			return "Go ahead";
		} else if (value == TSLL) {
			return "Turn slightly left";
		} else if (value == TL) {
			return "Turn left";
		} else if (value == TSHL) {
			return "Turn sharply left";
		} else if (value == TSLR) {
			return "Turn slightly right";
		} else if (value == TR) {
			return "Turn right";
		} else if (value == TSHR) {
			return "Turn sharply right";
		} else if (value == TU) {
			return "Make uturn";
		} else if (value == TRU) {
			return "Make uturn";
		} else if (value == KL) {
			return "Keep left";
		} else if (value == KR) {
			return "Keep right";
		} else if (value == OFFR) {
			return "Off route";
		}
		return super.toString();
	}

	public static boolean isLeftTurn(int type) {
		return type == TL || type == TSHL || type == TSLL || type == TRU;
	}
	
	public static boolean isRightTurn(int type) {
		return type == TR || type == TSHR || type == TSLR || type == TU;
	}

	public static boolean isSlightTurn(int type) {
		return type == TSLL || type == TSLR || type == C || type == KL || type == KR;
	}
}
