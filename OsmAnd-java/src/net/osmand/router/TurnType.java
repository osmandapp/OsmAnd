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

	public enum Turn {
		UNKNOWN (0, ""),
		STRAIGHT (1, C),
		SLIGHT_RIGHT (1 << 1, TSLR),
		SLIGHT_LEFT (1 | 1 << 1, TSLL),
		RIGHT (1 << 2, TR),
		LEFT (1 << 2 | 1, TL),
		SHARP_RIGHT (1 << 2 | 1 << 1, TSHR),
		SHARP_LEFT (1 << 2 | 1 << 1 | 1, TSHL),
		UTURN (1 << 3, TU);

		private final int modifier;
		private final String value;

		Turn(int modifier, String value) {
			this.modifier = modifier;
			this.value = value;
		}

		public int getModifier() {
			return modifier;
		}

		public String getValue() {
			return value;
		}
	}

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

	// Note that there is no "weight" or ordering between the primary and secondary turns.

	public void setPrimaryTurn(int lane, Turn turn) {
		lanes[lane] &= ~((1 << 3 | 1 << 2 | 1 << 1 | 1) << 3);
		lanes[lane] |= turn.getModifier() << 3;
	}

	public Turn getPrimaryTurn(int lane) {
		// Get the primary turn modifier for the lane
		int turnModifier = (lanes[lane] >> 3) & (1 << 3 | 1 << 2 | 1 << 1 | 1);
		Turn[] turns = Turn.values();
		for (int i = 0; i < turns.length; i++) {
			if (turns[i].getModifier() == turnModifier) {
				return turns[i];
			}
		}
		throw new IllegalStateException("Unknown primary turn value");
	}

	public void setSecondaryTurn(int lane, Turn turn) {
		lanes[lane] &= ~((1 << 3 | 1 << 2 | 1 << 1 | 1) << 7);
		lanes[lane] |= turn.getModifier() << 7;
	}

	public Turn getSecondaryTurn(int lane) {
		// Get the secondary turn modifier for the lane
		int turnModifier = (lanes[lane] >> 7) & (1 << 3 | 1 << 2 | 1 << 1 | 1);
		Turn[] turns = Turn.values();
		for (int i = 0; i < turns.length; i++) {
			if (turns[i].getModifier() == turnModifier) {
				return turns[i];
			}
		}
		throw new IllegalStateException("Unknown secondary turn value");
	}

	public boolean isTurnAllowed(int lane, Turn turn) {
		return getPrimaryTurn(lane) == turn || getSecondaryTurn(lane) == turn;
	}

	public boolean hasAttribute(int lane, int turn) {
		return (lanes[lane] & turn) == turn;
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
