package net.osmand.plus.routing;

import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.TurnType;
import android.content.Context;
import android.location.Location;


public class VoiceRouter {
	private final int STATUS_UTWP_TOLD = -1;
	private final int STATUS_UNKNOWN = 0;
	private final int STATUS_LONG_PREPARE = 1;
	private final int STATUS_PREPARE = 2;
	private final int STATUS_TURN_IN = 3;
	private final int STATUS_TURN = 4;
	private final int STATUS_TOLD = 5;
	
	private final RoutingHelper router;
	private boolean mute = false;
	private CommandPlayer player;
	
 
	private int currentStatus = STATUS_UNKNOWN;
	private float playGoAheadDist = 0;

	// private long lastTimeRouteRecalcAnnounced = 0;
	
	// default speed to have comfortable announcements (if actual speed is higher than it would be problem)
	// Speed in m/s 
	protected float DEFAULT_SPEED = 12;
	protected float TURN_DEFAULT_SPEED = 5;
		
	protected int PREPARE_LONG_DISTANCE = 0;
	protected int PREPARE_LONG_DISTANCE_END = 0;
	protected int PREPARE_DISTANCE = 0;
	protected int PREPARE_DISTANCE_END = 0;
	protected int TURN_IN_DISTANCE = 0;
	protected int TURN_IN_DISTANCE_END = 0;
	protected int TURN_DISTANCE = 0;
	
	protected VoiceCommandPending pendingCommand = null;
	private RouteDirectionInfo nextRouteDirection;


	public VoiceRouter(RoutingHelper router, CommandPlayer player) {
		this.router = router;
		this.player = player;
		updateAppMode();
	}
	
	public void setPlayer(CommandPlayer player) {
		this.player = player;
		if(pendingCommand != null && player != null){
			CommandBuilder newCommand = getNewCommandPlayerToPlay();
			if (newCommand != null) {
				pendingCommand.play(newCommand);
			}
			pendingCommand = null;
		}
	}

	public CommandPlayer getPlayer() {
		return player;
	}
	
	public void setMute(boolean mute) {
		this.mute = mute;
	}
	
	public boolean isMute() {
		return mute;
	}
	
	
	protected CommandBuilder getNewCommandPlayerToPlay(){
		if(player == null || mute){
			return null;
		}
		return player.newCommandBuilder();
	}
	
	
	
	public void updateAppMode(){
		// turn prompt starts either at distance, or if actual-lead-time(currentSpeed) < maximum-lead-time  
		// lead time criterion only for TURN_IN and TURN
		PREPARE_LONG_DISTANCE = 3500; // (105 sec) - 120 km/h
		PREPARE_LONG_DISTANCE_END = 3000; // (90 sec) - 120 km/h
		if(router.getAppMode() == ApplicationMode.PEDESTRIAN){
			// prepare distance is not needed for pedestrian
			PREPARE_DISTANCE = 200;     //(100 sec)
			PREPARE_DISTANCE_END = 150 + 100; //( 75 sec) + not play
			TURN_IN_DISTANCE = 100;     //  50 sec
			TURN_IN_DISTANCE_END = 70;  //  35 sec
			TURN_DISTANCE = 25;         //  12 sec
			TURN_DEFAULT_SPEED = DEFAULT_SPEED = 2f;         //   7,2 km/h
		} else if(router.getAppMode() == ApplicationMode.BICYCLE){
			PREPARE_DISTANCE = 500;     //(100 sec)
			PREPARE_DISTANCE_END = 350; //( 70 sec)
			TURN_IN_DISTANCE = 225;     //  45 sec
			TURN_IN_DISTANCE_END = 80;  //  16 sec
			TURN_DISTANCE = 45;         //   9 sec  
			TURN_DEFAULT_SPEED = DEFAULT_SPEED = 5;          //  18 km/h
		} else {
			PREPARE_DISTANCE = 1500;    //(125 sec)
			PREPARE_DISTANCE_END = 1200;//(100 sec)
			TURN_IN_DISTANCE = 390;     //  30 sec
			TURN_IN_DISTANCE_END = 182; //  14 sec
			TURN_DISTANCE = 50;         //  7 sec
			TURN_DEFAULT_SPEED = 7f; 	//  25 km/h
			DEFAULT_SPEED = 13;         //  48 km/h
		}
	}
	
	protected boolean isDistanceLess(float currentSpeed, double dist, double etalon){
		if(dist < etalon || ((dist / currentSpeed) < (etalon / DEFAULT_SPEED))){
			return true;
		}
		return false;
	}
	
	protected boolean isDistanceLess(float currentSpeed, double dist, double etalon, double defSpeed){
		if(dist < etalon || ((dist / currentSpeed) < (etalon / defSpeed))){
			return true;
		}
		return false;
	}
	
	public int calculateImminent(float dist, Location loc){
		float speed = DEFAULT_SPEED;
		if(loc != null && loc.hasSpeed()) {
			speed = loc.getSpeed();
		}
		if (isDistanceLess(speed, dist, TURN_IN_DISTANCE_END)) {
			return 0;
		} else if ( dist <= PREPARE_DISTANCE) {
			return 1;
		} else if (dist <= PREPARE_LONG_DISTANCE) {
			return 2;
		} else {
			return -1;
		}
	}
	
	
	private void nextStatusAfter(int previousStatus){
		//STATUS_UNKNOWN=0 -> STATUS_LONG_PREPARE=1 -> STATUS_PREPARE=2 -> STATUS_TURN_IN=3 -> STATUS_TURN=4 -> STATUS_TOLD=5
		if(previousStatus != STATUS_TOLD){
			this.currentStatus = previousStatus + 1;
		} else {
			this.currentStatus = previousStatus;
		}
	}
	
	private boolean statusNotPassed(int statusToCheck){
		return currentStatus <= statusToCheck;
	}
	
	protected void makeUTStatus() {
		// Mechanism via STATUS_UTWP_TOLD: Until turn in the right direction, or route is re-calculated in forward direction
		if (currentStatus != STATUS_UTWP_TOLD) {
			if (playMakeUTwp()) {
				currentStatus = STATUS_UTWP_TOLD;
				playGoAheadDist = 0;
			}
		}

	}
	
	/**
	 * Updates status of voice guidance 
	 * @param currentLocation 
	 */
	protected void updateStatus(Location currentLocation) {
		// Directly after turn: goAhead (dist), unless:
		// < PREPARE_LONG_DISTANCE (3000m): playPrepareTurn
		// < PREPARE_DISTANCE (1500m): playPrepareTurn
		// < TURN_IN_DISTANCE (300m or 25sec): playMakeTurnIn
		// < TURN_DISTANCE (60m or 5sec): playMakeTurn
		float speed = DEFAULT_SPEED;
		if (currentLocation != null && currentLocation.hasSpeed()) {
			speed = Math.max(currentLocation.getSpeed(), speed);
		}

		
		NextDirectionInfo nextInfo = router.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		// after last turn say:
		if (nextInfo == null || nextInfo.directionInfo == null || nextInfo.directionInfo.distance == 0) {
			// if(currentStatus <= STATUS_UNKNOWN && currentDirection > 0){ This caused this prompt to be suppressed when coming back from a
			if (currentStatus <= STATUS_UNKNOWN) {
				if (playGoAheadToDestination()) {
					currentStatus = STATUS_TOLD;
					playGoAheadDist = 0;
				}
			}
			return;
		}
		if(nextInfo.intermediatePoint){
			if (currentStatus <= STATUS_UNKNOWN) {
				if (playGoAheadToIntermediate()) {
					currentStatus = STATUS_TOLD;
					playGoAheadDist = 0;
				}
			}
			return;
		}
		int dist = nextInfo.distanceTo;
		RouteDirectionInfo next = nextInfo.directionInfo;

		// if routing is changed update status to unknown
		if (next != nextRouteDirection) {
			nextRouteDirection = next;
			currentStatus = STATUS_UNKNOWN;
			playGoAheadDist = 0;
		}

		if (dist == 0 || currentStatus == STATUS_TOLD) {
			// nothing said possibly that's wrong case we should say before that
			// however it should be checked manually ?
			return;
		}
		// say how much to go if there is next turn is a bit far
		if (currentStatus == STATUS_UNKNOWN) {
			if (!isDistanceLess(speed, dist, TURN_IN_DISTANCE * 1.3)) {
				playGoAheadDist = dist - 80;
			}
			// say long distance message only for long distances > 10 km
			// if (dist >= PREPARE_LONG_DISTANCE && !isDistanceLess(speed, dist, PREPARE_LONG_DISTANCE)) {
			if (dist > PREPARE_LONG_DISTANCE + 300) {
				nextStatusAfter(STATUS_UNKNOWN);
			} else if (dist > PREPARE_DISTANCE + 300) {
				// say prepare message if it is far enough and don't say preare long distance
				nextStatusAfter(STATUS_LONG_PREPARE);
			} else {
				// don't say even prepare message
				nextStatusAfter(STATUS_PREPARE);
			}
		}

		NextDirectionInfo nextNextInfo = router.getNextRouteDirectionInfoAfter(nextInfo, new NextDirectionInfo(), false);
		if (statusNotPassed(STATUS_TURN) && isDistanceLess(speed, dist, TURN_DISTANCE, TURN_DEFAULT_SPEED)) {
			if (next.distance < TURN_IN_DISTANCE_END && nextNextInfo != null) {
				playMakeTurn(next, nextNextInfo.directionInfo);
			} else {
				playMakeTurn(next, null);
			}
			nextStatusAfter(STATUS_TURN);
		} else if (statusNotPassed(STATUS_TURN_IN) && isDistanceLess(speed, dist, TURN_IN_DISTANCE)) {
			if (dist >= TURN_IN_DISTANCE_END) {
				if ((isDistanceLess(speed, next.distance, TURN_DISTANCE) || next.distance < TURN_IN_DISTANCE_END) &&
						nextNextInfo != null) {
					playMakeTurnIn(next, dist, nextNextInfo.directionInfo);
				} else {
					playMakeTurnIn(next, dist, null);
				}
			}
			nextStatusAfter(STATUS_TURN_IN);
			// } else if (statusNotPassed(STATUS_PREPARE) && isDistanceLess(speed, dist, PREPARE_DISTANCE)) {
		} else if (statusNotPassed(STATUS_PREPARE) && (dist <= PREPARE_DISTANCE)) {
			if (dist >= PREPARE_DISTANCE_END) {
				if(next.getTurnType().keepLeft() || next.getTurnType().keepRight()){
					// do not play prepare for keep left/right
				} else {
					playPrepareTurn(next, dist);
				}
			}
			nextStatusAfter(STATUS_PREPARE);
			// } else if (statusNotPassed(STATUS_LONG_PREPARE) && isDistanceLess(speed, dist, PREPARE_LONG_DISTANCE)){
		} else if (statusNotPassed(STATUS_LONG_PREPARE) && (dist <= PREPARE_LONG_DISTANCE)) {
			if (dist >= PREPARE_LONG_DISTANCE_END) {
				playPrepareTurn(next, dist);
			}
			nextStatusAfter(STATUS_LONG_PREPARE);
		} else if (statusNotPassed(STATUS_UNKNOWN)) {
			// strange how we get here but
			nextStatusAfter(STATUS_UNKNOWN);
		} else if (statusNotPassed(STATUS_TURN_IN) && dist < playGoAheadDist) {
			playGoAheadDist = 0;
			playGoAhead(dist);
		}
	}

	public void announceCurrentDirection(Location currentLocation) {
		NextDirectionInfo nextInfo = router.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		if(nextInfo == null) {
			playGoAheadToDestination();
			return;
		}
		NextDirectionInfo nextNextInfo = router.getNextRouteDirectionInfoAfter(nextInfo, new NextDirectionInfo(), false);
		float speed = DEFAULT_SPEED;
		RouteDirectionInfo next = nextInfo.directionInfo;
		int dist = nextInfo.distanceTo;

		if(currentLocation != null && currentLocation.hasSpeed()){
			speed = Math.max(currentLocation.getSpeed(), speed);
		}

		switch (currentStatus) {
		case STATUS_UTWP_TOLD:
			playMakeUTwp();
			break;
		case STATUS_UNKNOWN:
			if (nextRouteDirection != null && ((next == null) || (next.distance == 0))) {
				playGoAheadToDestination();
			} else {
				playGoAhead(dist);
			}
			break;
		case STATUS_TOLD:
			if (nextRouteDirection != null) {
				playGoAheadToDestination();
			}
			break;
		case STATUS_TURN:
			if(next.distance < TURN_IN_DISTANCE_END && nextNextInfo != null) {
				playMakeTurn(next, nextNextInfo.directionInfo);
			} else {
				playMakeTurn(next, null);
			}
			break;
		case STATUS_TURN_IN:
			if((isDistanceLess(speed, next.distance, TURN_DISTANCE) || next.distance < TURN_IN_DISTANCE_END) && 
					nextNextInfo != null) {
				playMakeTurnIn(next, dist, nextNextInfo.directionInfo);
			} else {
				playMakeTurnIn(next, dist, null);
			}
			break;
		case STATUS_PREPARE:
			playPrepareTurn(next, dist);
			break;
		case STATUS_LONG_PREPARE:
			playPrepareTurn(next, dist);
			break;
		default:
			break;
		}
	}


	private boolean playGoAheadToDestination() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.goAhead(router.getLeftDistance()).andArriveAtDestination().play();
			return true;
		}
		return false;
	}
	
	private boolean playGoAheadToIntermediate() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.goAhead(router.getLeftDistanceNextIntermediate()).andArriveAtIntermediatePoint().play();
			return true;
		}
		return false;
	}
	
	private boolean playMakeUTwp() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.makeUTwp().play();
			return true;
		}
		return false;
	}

	private void playGoAhead(int dist) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.goAhead(dist).play();
		}
	}

	private void playPrepareTurn(RouteDirectionInfo next, int dist) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			String tParam = getTurnType(next.getTurnType());
			if(tParam != null){
				play.prepareTurn(tParam, dist).play();
			} else if(next.getTurnType().isRoundAbout()){
				play.prepareRoundAbout(dist).play();
			} else if(next.getTurnType().getValue().equals(TurnType.TU) || next.getTurnType().getValue().equals(TurnType.TRU)){
				play.prepareMakeUT(dist).play();
			} 
		}
	}

	private void playMakeTurnIn(RouteDirectionInfo next, int dist, RouteDirectionInfo pronounceNextNext) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			String tParam = getTurnType(next.getTurnType());
			boolean isPlay = true;
			if (tParam != null) {
				play.turn(tParam, dist);
			} else if (next.getTurnType().isRoundAbout()) {
				play.roundAbout(dist, next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut());
			} else if (next.getTurnType().getValue().equals(TurnType.TU) || next.getTurnType().getValue().equals(TurnType.TRU)) {
				play.makeUT(dist);
			} else {
				isPlay = false;
			}
			// small preparation to next after next
			if (pronounceNextNext != null) {
				TurnType t = pronounceNextNext.getTurnType();
				isPlay = true;
				if (next.getTurnType().getValue().equals(TurnType.C) && 
						!TurnType.C.equals(t.getValue())) {
					play.goAhead(dist);
				}
				if (TurnType.TL.equals(t.getValue()) || TurnType.TSHL.equals(t.getValue()) || TurnType.TSLL.equals(t.getValue())
						|| TurnType.TU.equals(t.getValue()) || TurnType.KL.equals(t.getValue())) {
					play.then().bearLeft();
				} else if (TurnType.TR.equals(t.getValue()) || TurnType.TSHR.equals(t.getValue()) || TurnType.TSLR.equals(t.getValue())
						|| TurnType.KR.equals(t.getValue())) {
					play.then().bearRight();
				}
			}
			if(isPlay){
				play.play();
			}
		}
	}

	private void playMakeTurn(RouteDirectionInfo next, RouteDirectionInfo nextNext) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			String tParam = getTurnType(next.getTurnType());
			boolean isplay = true;
			if(tParam != null){
				play.turn(tParam);
			} else if(next.getTurnType().isRoundAbout()){
				play.roundAbout(next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut());
			} else if(next.getTurnType().getValue().equals(TurnType.TU) || next.getTurnType().getValue().equals(TurnType.TRU)){
				play.makeUT();
				// do not say it
//				} else if(next.getTurnType().getValue().equals(TurnType.C)){
//					play.goAhead();
			} else {
				isplay = false;
			}
			// add turn after next
			if (nextNext != null) {
				String t2Param = getTurnType(nextNext.getTurnType());
				if (t2Param != null) {
					if(isplay) { play.then(); }
					play.turn(t2Param, next.distance);
				} else if (nextNext.getTurnType().isRoundAbout()) {
					if(isplay) { play.then(); }
					play.roundAbout(next.distance, nextNext.getTurnType().getTurnAngle(), nextNext.getTurnType().getExitOut());
				} else if (nextNext.getTurnType().getValue().equals(TurnType.TU)) {
					if(isplay) { play.then(); }
					play.makeUT(next.distance);
				}
				isplay = true;
			}
			if(isplay){
				play.play();
			}
		}
	}
	
	private String getTurnType(TurnType t){
		if(TurnType.TL.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_LEFT;
		} else if(TurnType.TSHL.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_LEFT_SH;
		} else if(TurnType.TSLL.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_LEFT_SL;
		} else if(TurnType.TR.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_RIGHT;
		} else if(TurnType.TSHR.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_RIGHT_SH;
		} else if(TurnType.TSLR.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_RIGHT_SL;
		} else if(TurnType.KL.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_LEFT_KEEP;
		} else if(TurnType.KR.equals(t.getValue())){
			return AbstractPrologCommandPlayer.A_RIGHT_KEEP;
		}
		return null;
	}
	
	public void gpsLocationLost(){
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			play.gpsLocationLost().play();
		}
	}

	public void newRouteIsCalculated(boolean newRoute) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			if (!newRoute) {
				// suppress "route recalculated" prompt for GPX-routing, it makes no sense
				// suppress "route recalculated" prompt for 60sec (this workaround now outdated after more intelligent route recalculation and directional voice prompt suppression)
				// if (router.getCurrentGPXRoute() == null && (System.currentTimeMillis() - lastTimeRouteRecalcAnnounced > 60000)) {
				if (router.getCurrentGPXRoute() == null) {
					play.routeRecalculated(router.getLeftDistance()).play();
					currentStatus = STATUS_UNKNOWN;
					// lastTimeRouteRecalcAnnounced = System.currentTimeMillis();
				}
			} else {
				play.newRouteCalculated(router.getLeftDistance()).play();
				currentStatus = STATUS_UNKNOWN;
			}
		} else if (player == null) {
			pendingCommand = new VoiceCommandPending(!newRoute ? VoiceCommandPending.ROUTE_RECALCULATED
					: VoiceCommandPending.ROUTE_CALCULATED, this);
			currentStatus = STATUS_UNKNOWN;
		}
		nextRouteDirection = null;
	}

	public void arrivedDestinationPoint() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.arrivedAtDestination().play();
		}
	}
	
	public void arrivedIntermediatePoint() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.arrivedAtIntermediatePoint().play();
		}
	}

	public void onApplicationTerminate(Context ctx) {
		if (player != null) {
			player.clear();
		}
	}
	
	/**
	 * Command to wait until voice player is initialized 
	 */
	private class VoiceCommandPending {
		public static final int ROUTE_CALCULATED = 1;
		public static final int ROUTE_RECALCULATED = 2;
		protected final int type;
		private final VoiceRouter voiceRouter;
		
		public VoiceCommandPending(int type, VoiceRouter voiceRouter){
			this.type = type;
			this.voiceRouter = voiceRouter;
		}

		public void play(CommandBuilder newCommand) {
			int left = voiceRouter.router.getLeftDistance();
			if (left > 0) {
				if (type == ROUTE_CALCULATED) {
					newCommand.newRouteCalculated(left).play();
				} else if (type == ROUTE_RECALCULATED) {
					newCommand.routeRecalculated(left).play();
				}
			}
		}
	}

}
