package net.osmand.router;

import net.osmand.util.Algorithms;

import java.util.HashSet;
import java.util.LinkedHashSet;


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
	private static final int[] TURNS_ORDER = {TU, TSHL, TL, TSLL, C, TSLR, TR, TSHR, TRU};

	public static TurnType straight() {
		return valueOf(C, false);
	}
	
	public int getActiveCommonLaneTurn() {
		if (lanes == null || lanes.length == 0) {
			return -1;
		}
		for (int i = 0; i < lanes.length; i++) {
			if (lanes[i] % 2 == 1) {
				return TurnType.getPrimaryTurn(lanes[i]);
			}
		}
		return -1;
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
				int type = s.contains("RNLB") ? RNLB : RNDB;
				t = TurnType.getExitTurn(type, Integer.parseInt(s.substring(4)), 0, leftSide);
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
	private boolean possiblyLeftTurn;
	private boolean possiblyRightTurn;

	public TurnType(int value, int exitOut, float turnAngle, boolean skipToSpeak, int[] lanes,
	                boolean possiblyLeftTurn, boolean possiblyRightTurn) {
		this.value = value;
		this.exitOut = exitOut;
		this.turnAngle = turnAngle;
		this.skipToSpeak = skipToSpeak;
		this.lanes = lanes;
		this.possiblyLeftTurn = possiblyLeftTurn;
		this.possiblyRightTurn = possiblyRightTurn;
	}

	public static TurnType getExitTurn(int out, float angle, boolean leftSide) {
		TurnType r = valueOf(RNDB, leftSide); //$NON-NLS-1$
		r.exitOut = out;
		r.setTurnAngle(angle);
		return r;
	}

	private static TurnType getExitTurn(int type, int out, float angle, boolean leftSide) {
		if (type != RNDB && type != RNLB) {
			return getExitTurn(out, angle, leftSide);
		}
		TurnType r = valueOf(type, leftSide); //$NON-NLS-1$
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

	public void setExitOut(int exitOut) {
		this.exitOut = exitOut;
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
	// 11-15 bits - tertiary turn
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
	
	public static void setPrimaryTurn(int[] lanes, int lane, int turnType) {
		lanes[lane] &= ~(15 << 1);
		lanes[lane] |= (turnType << 1);
	}

	public static int getSecondaryTurn(int laneValue) {
		// Get the secondary turn modifier for the lane
		return (laneValue >> 5) & ((1 << 5) - 1);
	}
	
	public static void setPrimaryTurnShiftOthers(int[] lanes, int lane, int turnType) {
		int pt = getPrimaryTurn(lanes[lane]);
		int st = getSecondaryTurn(lanes[lane]);
		//int tt = getTertiaryTurn(lanes[lane]); is lost here
		setPrimaryTurnAndReset(lanes, lane, turnType);
		setSecondaryTurn(lanes, lane, pt);
		setTertiaryTurn(lanes, lane, st);
	}
	
	public static void setSecondaryToPrimary(int[] lanes, int lane) {
		int st = getSecondaryTurn(lanes[lane]);
		int pt = getPrimaryTurn(lanes[lane]);
		setPrimaryTurn(lanes, lane, st);
		setSecondaryTurn(lanes, lane, pt);
	}
	
	public static void setTertiaryToPrimary(int[] lanes, int lane) {
		int st = getSecondaryTurn(lanes[lane]);
		int pt = getPrimaryTurn(lanes[lane]);
		int tt = getTertiaryTurn(lanes[lane]);
		setPrimaryTurn(lanes, lane, tt);
		setSecondaryTurn(lanes, lane, pt);
		setTertiaryTurn(lanes, lane, st);
	}

	public static void setTertiaryTurn(int[] lanes, int lane, int turnType) {
		lanes[lane] &= ~(15 << 10);
		lanes[lane] |= (turnType << 10);
	}

	public static int getTertiaryTurn(int laneValue) {
		// Get the tertiary turn modifier for the lane
		return (laneValue >> 10);
	}

	public static String lanesToString(int[] lns) {
        StringBuilder s = new StringBuilder();
        for (int h = 0; h < lns.length; h++) {
            if (h > 0) {
                s.append("|");
            }
            if (lns[h] % 2 == 1) {
                s.append("+");
            }
            int pt = TurnType.getPrimaryTurn(lns[h]);
            if (pt == 0) {
                pt = 1;
            }
            s.append(TurnType.valueOf(pt, false).toXmlString());
            int st = TurnType.getSecondaryTurn(lns[h]);
            if (st != 0) {
                s.append(",").append(TurnType.valueOf(st, false).toXmlString());
            }
            int tt = TurnType.getTertiaryTurn(lns[h]);
            if (tt != 0) {
                s.append(",").append(TurnType.valueOf(tt, false).toXmlString());
            }

        }
        return s.toString();
	}

	public int countTurnTypeDirections(int type, boolean onlyActive) {
		if (lanes == null) {
			return 0;
		}
		int cnt = 0;
		for (int h = 0; h < lanes.length; h++) {
			boolean active = lanes[h] % 2 == 1;
			if (onlyActive && !active) {
				continue;
			}
			int primary = TurnType.getPrimaryTurn(lanes[h]);
			if (primary == 0) {
				primary = TurnType.C;
			}
			if (primary == type) {
				cnt++;
			}
			if (onlyActive) {
				continue;
			}
			int secondary = TurnType.getSecondaryTurn(lanes[h]);
			if (secondary == type) {
				cnt++;
			}
			int tertiary = TurnType.getTertiaryTurn(lanes[h]);
			if (tertiary == type) {
				cnt++;
			}
		}
		return cnt;
	}

	public int countDirections() {
		HashSet<Integer> directions = new HashSet<>();
		for (int h = 0; h < lanes.length; h++) {
			int primary = TurnType.getPrimaryTurn(lanes[h]);
			if (primary == 0) {
				primary = TurnType.C;
			}
			directions.add(primary);
			int secondary = TurnType.getSecondaryTurn(lanes[h]);
			if (secondary > 0) {
				directions.add(secondary);
			}
			int tertiary = TurnType.getTertiaryTurn(lanes[h]);
			if (tertiary > 0) {
				directions.add(tertiary);
			}
		}
		return directions.size();
	}

	public static int[] lanesFromString(String lanesString) {
		if (Algorithms.isEmpty(lanesString)) {
			return null;
		}
		String[] lanesArr = lanesString.split("\\|");
		int[] lanes = new int[lanesArr.length];
		for (int l = 0; l < lanesArr.length; l++) {
			String lane = lanesArr[l];
			String[] turns = lane.split(",");
			TurnType primaryTurn = null;
			TurnType secondaryTurn = null;
			TurnType tertiaryTurn = null;
			boolean plus = false;
			for (int i = 0; i < turns.length; i++) {
				String turn = turns[i];
				if (i == 0) {
					plus = turn.length() > 0 && turn.charAt(0) == '+';
					if (plus) {
						turn = turn.substring(1);
					}
					primaryTurn = TurnType.fromString(turn, false);
				} else if (i == 1) {
					secondaryTurn =TurnType.fromString(turn, false);
				} else if (i == 2) {
					tertiaryTurn =TurnType.fromString(turn, false);
				}
			}
			setPrimaryTurnAndReset(lanes, l, primaryTurn.value);
			if (secondaryTurn != null) {
				setSecondaryTurn(lanes, l, secondaryTurn.value);
			}
			if (tertiaryTurn != null) {
				setTertiaryTurn(lanes, l, tertiaryTurn.value);
			}
			if (plus) {
				lanes[l] |= 1;
			}
		}
		return lanes;
	}

	public int[] getLanes() {
		return lanes;
	}
	
	public void setPossibleLeftTurn(boolean possiblyLeftTurn) {
		this.possiblyLeftTurn = possiblyLeftTurn;
	}
	
	public void setPossibleRightTurn(boolean possiblyRightTurn) {
		this.possiblyRightTurn = possiblyRightTurn;
	}
	
	public boolean isPossibleLeftTurn() {
		return possiblyLeftTurn;
	}
	
	public boolean isPossibleRightTurn() {
		return possiblyRightTurn;
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
		String vl = null;
		if (isRoundAbout()) {
			vl = "Take " + getExitOut() + " exit";
		} else if (value == C) {
			vl = "Go ahead";
		} else if (value == TSLL) {
			vl = "Turn slightly left";
		} else if (value == TL) {
			vl = "Turn left";
		} else if (value == TSHL) {
			vl = "Turn sharply left";
		} else if (value == TSLR) {
			vl = "Turn slightly right";
		} else if (value == TR) {
			vl = "Turn right";
		} else if (value == TSHR) {
			vl = "Turn sharply right";
		} else if (value == TU) {
			vl = "Make uturn";
		} else if (value == TRU) {
			vl = "Make uturn";
		} else if (value == KL) {
			vl = "Keep left";
		} else if (value == KR) {
			vl = "Keep right";
		} else if (value == OFFR) {
			vl = "Off route";
		}
		if(vl != null) {
			if (lanes != null && lanes.length > 0) {
				vl += " (" + lanesToString(lanes) +")";
			}
			return vl;
		}
		return super.toString();
	}

	public static boolean isLeftTurn(int type) {
		return type == TL || type == TSHL || type == TSLL || type == TU || type == KL;
	}
	
	public static boolean isLeftTurnNoUTurn(int type) {
		return type == TL || type == TSHL || type == TSLL || type == KL;
	}
	
	public static boolean isRightTurn(int type) {
		return type == TR || type == TSHR || type == TSLR || type == TRU || type == KR;
	}
	
	public static boolean isRightTurnNoUTurn(int type) {
		return type == TR || type == TSHR || type == TSLR || type == KR;
	}

	public static boolean isSlightTurn(int type) {
		return type == TSLL || type == TSLR || type == C || type == KL || type == KR;
	}
	
	public static boolean isKeepDirectionTurn(int type) {
		return type == C || type == KL || type == KR;
	}

	public static boolean isSharpOrReverse(int type) {
		return type == TurnType.TSHL || type == TurnType.TSHR || type == TurnType.TU || type == TurnType.TRU;
	}

	public static boolean isSharpLeftOrUTurn(int type) {
		return type == TurnType.TSHL || type == TurnType.TU;
	}

	public static boolean isSharpRightOrUTurn(int type) {
		// turn:lanes=reverse is transform to TU only
		return type == TurnType.TSHR || type == TurnType.TRU || type == TurnType.TU;
	}
	
	public static boolean hasAnySlightTurnLane(int type) {
		return TurnType.isSlightTurn(TurnType.getPrimaryTurn(type))
				|| TurnType.isSlightTurn(TurnType.getSecondaryTurn(type))
				|| TurnType.isSlightTurn(TurnType.getTertiaryTurn(type));
	}
	
	public static boolean hasAnyTurnLane(int type, int turn) {
		return TurnType.getPrimaryTurn(type) == turn
				|| TurnType.getSecondaryTurn(type) == turn
				|| TurnType.getTertiaryTurn(type) == turn;
	}

	public static void collectTurnTypes(int lane, LinkedHashSet<Integer> set) {
		int pt = TurnType.getPrimaryTurn(lane);
		if(pt != 0) {
			set.add(pt);
		}
		pt = TurnType.getSecondaryTurn(lane);
		if(pt != 0) {
			set.add(pt);
		}		
		pt = TurnType.getTertiaryTurn(lane);
		if(pt != 0) {
			set.add(pt);
		}		
	}
	
	public static int orderFromLeftToRight(int type) {
		switch(type) {
		case TU:
			return -5;
		case TSHL:
			return -4;
		case TL:
			return -3;
		case TSLL:
			return -2;
		case KL:
			return -1;
		
		case TRU:
			return 5;
		case TSHR:
			return 4;
		case TR:
			return 3;
		case TSLR:
			return 2;
		case KR:
			return 1;
		default:
			return 0;
		}
	}

	public static int convertType(String lane) {
		int turn;
		// merge should be recognized as continue route (but it could displayed differently)
		if(lane.equals("merge_to_left")) {
			turn = TurnType.C;
		} else if(lane.equals("merge_to_right")) {
			turn = TurnType.C;
		} else if (lane.equals("none") || lane.equals("through")) {
			turn = TurnType.C;
		} else if (lane.equals("slight_right")) {
			turn = TurnType.TSLR;
		} else if (lane.equals("slight_left") ) {
			turn = TurnType.TSLL;
		} else if (lane.equals("right")) {
			turn = TurnType.TR;
		} else if (lane.equals("left")) {
			turn = TurnType.TL;
		} else if (lane.equals("sharp_right")) {
			turn = TurnType.TSHR;
		} else if (lane.equals("sharp_left")) {
			turn = TurnType.TSHL;
		} else if (lane.equals("reverse")) {
			turn = TurnType.TU;
		} else {
			// Unknown string
			turn = TurnType.C;
//			continue;
		}
		return turn;
	}

	public static int getPrev(int turn) {
		for (int i = TURNS_ORDER.length - 1; i >= 0; i--) {
			int t = TURNS_ORDER[i];
			if (t == turn && i > 0) {
				return TURNS_ORDER[i - 1];
			}
		}
		return turn;
	}

	public static int getNext(int turn) {
		for (int i = 0; i < TURNS_ORDER.length; i++) {
			int t = TURNS_ORDER[i];
			if (t == turn && i + 1 < TURNS_ORDER.length) {
				return TURNS_ORDER[i + 1];
			}
		}
		return turn;
	}
}
