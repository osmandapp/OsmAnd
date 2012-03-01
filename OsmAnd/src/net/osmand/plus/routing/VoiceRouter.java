package net.osmand.plus.routing;

import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.routing.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper.TurnType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import android.content.Context;
import android.location.Location;


public class VoiceRouter {
	private final int STATUS_UNKNOWN = 0;
	private final int STATUS_LONG_PREPARE = 1;
	private final int STATUS_PREPARE = 2;
	private final int STATUS_TURN_IN = 3;
	private final int STATUS_TURN = 4;
	private final int STATUS_TOLD = 5;
	
	private final RoutingHelper router;
	private boolean mute = false;
	private CommandPlayer player;
	
	private int currentDirection = 0;
 
	private int currentStatus = STATUS_UNKNOWN;

	private long lastTimeRouteRecalcAnnounced = 0;
	private long lastTimeMakeUTwpAnnounced = 0;
	
	// default speed to have comfortable announcements (if actual speed is higher than it would be problem)
	// Speed in m/s 
	protected float DEFAULT_SPEED = 12;
		
	protected int PREPARE_LONG_DISTANCE = 3000;
	protected int PREPARE_LONG_DISTANCE_END = 2000;
	
	protected int PREPARE_DISTANCE = 0;
	protected int PREPARE_DISTANCE_END = 0;
	protected int TURN_IN_DISTANCE = 0;
	protected int TURN_IN_DISTANCE_END = 0;
	protected int TURN_DISTANCE = 0;
	
	protected VoiceCommandPending pendingCommand = null;


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
		if(router.getAppMode() == ApplicationMode.PEDESTRIAN){
			// prepare distance needed ?
			PREPARE_DISTANCE = 200;     //(100 sec)
			PREPARE_DISTANCE_END = 150; //( 75 sec)
			TURN_IN_DISTANCE = 100;     //  50 sec
			TURN_IN_DISTANCE_END = 70;  //  35 sec
			TURN_DISTANCE = 25;         //  12 sec
			DEFAULT_SPEED = 2f;         //   7,2 km/h
		} else if(router.getAppMode() == ApplicationMode.BICYCLE){
			PREPARE_DISTANCE = 500;     //(100 sec)
			PREPARE_DISTANCE_END = 350; //( 70 sec)
			TURN_IN_DISTANCE = 225;     //  45 sec
			TURN_IN_DISTANCE_END = 80;  //  16 sec
			TURN_DISTANCE = 45;         //   9 sec  
			DEFAULT_SPEED = 5;          //  18 km/h
		} else {
			PREPARE_DISTANCE = 1500;    //(125 sec)
			PREPARE_DISTANCE_END = 1200;//(100 sec)
			TURN_IN_DISTANCE = 300;     //  25 sec
			TURN_IN_DISTANCE_END = 168; //  14 sec
			TURN_DISTANCE = 60;         //   5 sec 
			DEFAULT_SPEED = 12;         //  43 km/h
		}
	}
		
	private boolean isDistanceLess(float currentSpeed, double dist, double etalon){
		if(dist < etalon || ((dist / currentSpeed) < (etalon / DEFAULT_SPEED))){
			return true;
		}
		return false;
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
	
	/**
	 * Updates status of voice guidance 
	 * @param currentLocation 
	 */
	protected void updateStatus(Location currentLocation, boolean makeUturnWhenPossible){
		//   Directly after turn:                goAhead (dist), unless:
		// < PREPARE_LONG_DISTANCE     (3000m):  playPrepareTurn
		// < PREPARE_DISTANCE          (1500m):  playPrepareTurn
		// < TURN_IN_DISTANCE  (300m or 25sec):  playMakeTurnIn
		// < TURN_DISTANCE       (60m or 5sec):  playMakeTurn
		float speed = DEFAULT_SPEED;
		if(currentLocation != null && currentLocation.hasSpeed()){
			speed = Math.max(currentLocation.getSpeed(), speed);
		}

		// for Issue 863
		if (makeUturnWhenPossible == true) {
			//suppress "make UT when possible" message for 60sec
			if (System.currentTimeMillis() - lastTimeMakeUTwpAnnounced > 60000) {
				CommandBuilder play = getNewCommandPlayerToPlay();
				if(play != null){
					play.makeUTwp().play();
					lastTimeMakeUTwpAnnounced = System.currentTimeMillis();
				}
			}
			currentStatus = STATUS_UNKNOWN;
			return;
		}

		RouteDirectionInfo next = router.getNextRouteDirectionInfo();
		int dist = router.getDistanceToNextRouteDirection();
		
		// if routing is changed update status to unknown 
		if(currentDirection != router.currentDirectionInfo){
			currentDirection = router.currentDirectionInfo;
			currentStatus = STATUS_UNKNOWN;
		}
		
		
		// the last turn say 
		if(next == null || next.distance == 0) {
			if(currentStatus == STATUS_UNKNOWN && currentDirection > 0){
				CommandBuilder play = getNewCommandPlayerToPlay();
				if(play != null){
					play.goAhead(router.getLeftDistance()).andArriveAtDestination().play();
				}
				currentStatus = STATUS_TOLD;
			}
			return;
		}
		if(dist == 0 || currentStatus == STATUS_TOLD){
			// nothing said possibly that's wrong case we should say before that
			// however it should be checked manually ?
			return;
		}
		
		
		RouteDirectionInfo nextNext = router.getNextNextRouteDirectionInfo();
		if(statusNotPassed(STATUS_TURN) && isDistanceLess(speed, dist, TURN_DISTANCE)){
			if(next.distance < TURN_IN_DISTANCE_END) {
				playMakeTurn(next, nextNext);
			} else {
				playMakeTurn(next, null);
			}
			nextStatusAfter(STATUS_TURN);
		} else if (statusNotPassed(STATUS_TURN_IN) && isDistanceLess(speed, dist, TURN_IN_DISTANCE)){
			if (dist >= TURN_IN_DISTANCE_END) {
				if(isDistanceLess(speed, next.distance, TURN_DISTANCE) || next.distance < TURN_IN_DISTANCE_END) {
					playMakeTurnIn(next, dist, nextNext);
				} else {
					playMakeTurnIn(next, dist, null);
				}
			}
			nextStatusAfter(STATUS_TURN_IN);
		//} else if (statusNotPassed(STATUS_PREPARE) && isDistanceLess(speed, dist, PREPARE_DISTANCE)) {
		} else if (statusNotPassed(STATUS_PREPARE) && (dist <= PREPARE_DISTANCE)) {
			if (dist >= PREPARE_DISTANCE_END) {
				playPrepareTurn(next, dist);
			}
			nextStatusAfter(STATUS_PREPARE);
		//} else if (statusNotPassed(STATUS_LONG_PREPARE) && isDistanceLess(speed, dist, PREPARE_LONG_DISTANCE)){
		} else if (statusNotPassed(STATUS_LONG_PREPARE) && (dist <= PREPARE_LONG_DISTANCE)){
			if (dist >= PREPARE_LONG_DISTANCE_END) {
				playPrepareTurn(next, dist);
			} 
			nextStatusAfter(STATUS_LONG_PREPARE);
		} else if (statusNotPassed(STATUS_UNKNOWN)){
			//if (dist >= PREPARE_LONG_DISTANCE && !isDistanceLess(speed, dist, PREPARE_LONG_DISTANCE)) {
			if (dist > PREPARE_LONG_DISTANCE) {
				playGoAhead(dist);
			}
			nextStatusAfter(STATUS_UNKNOWN);
		}
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
			String tParam = getTurnType(next.turnType);
			if(tParam != null){
				play.prepareTurn(tParam, dist).play();
			} else if(next.turnType.isRoundAbout()){
				play.prepareRoundAbout(dist).play();
			} else if(next.turnType.getValue().equals(TurnType.TU) || next.turnType.getValue().equals(TurnType.TRU)){
				play.prepareMakeUT(dist).play();
			} 
		}
	}

	private void playMakeTurnIn(RouteDirectionInfo next, int dist, RouteDirectionInfo pronounceNextNext) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			String tParam = getTurnType(next.turnType);
			boolean isPlay = true;
			if (tParam != null) {
				play.turn(tParam, dist);
			} else if (next.turnType.isRoundAbout()) {
				play.roundAbout(dist, next.turnType.getTurnAngle(), next.turnType.getExitOut());
			} else if (next.turnType.getValue().equals(TurnType.TU) || next.turnType.getValue().equals(TurnType.TRU)) {
				play.makeUT(dist);
			} else {
				isPlay = false;
			}
			// small preparation to next after next
			if (pronounceNextNext != null) {
				TurnType t = pronounceNextNext.turnType;
				isPlay = true;
				if (next.turnType.getValue().equals(TurnType.C) && 
						!TurnType.C.equals(t.getValue())) {
					play.goAhead(dist);
				}
				if (TurnType.TL.equals(t.getValue()) || TurnType.TSHL.equals(t.getValue()) || TurnType.TSLL.equals(t.getValue())
						|| TurnType.TU.equals(t.getValue())) {
					play.then().bearLeft();
				} else if (TurnType.TR.equals(t.getValue()) || TurnType.TSHR.equals(t.getValue()) || TurnType.TSLR.equals(t.getValue())) {
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
			String tParam = getTurnType(next.turnType);
			boolean isplay = true;
			if(tParam != null){
				play.turn(tParam);
			} else if(next.turnType.isRoundAbout()){
				play.roundAbout(next.turnType.getTurnAngle(), next.turnType.getExitOut());
			} else if(next.turnType.getValue().equals(TurnType.TU) || next.turnType.getValue().equals(TurnType.TRU)){
				play.makeUT();
				// do not say it
//				} else if(next.turnType.getValue().equals(TurnType.C)){
//					play.goAhead();
			} else {
				isplay = false;
			}
			// add turn after next
			if (nextNext != null) {
				String t2Param = getTurnType(nextNext.turnType);
				if (t2Param != null) {
					if(isplay) { play.then(); }
					play.turn(t2Param, next.distance);
				} else if (nextNext.turnType.isRoundAbout()) {
					if(isplay) { play.then(); }
					play.roundAbout(next.distance, nextNext.turnType.getTurnAngle(), nextNext.turnType.getExitOut());
				} else if (nextNext.turnType.getValue().equals(TurnType.TU)) {
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
		}
		return null;
	}
	
	public void gpsLocationLost(){
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			play.gpsLocationLost().play();
		}
	}

	public void newRouteIsCalculated(boolean updateRoute, boolean makeUturnWhenPossible) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			if (updateRoute) {
				//suppress "route recalculated" prompt while makeUturnWhenPossible is active
				if (makeUturnWhenPossible == false) {
					//suppress "route recaluated" prompt for 60sec
					if (System.currentTimeMillis() - lastTimeRouteRecalcAnnounced > 60000) {
						//suppress "route recaluated" prompt for GPX-rotuing, it makes no sense
						if (router.getCurrentGPXRoute() == null) {
							play.routeRecalculated(router.getLeftDistance()).play();
							lastTimeRouteRecalcAnnounced = System.currentTimeMillis();
						}
					}
				}
			} else {
				play.newRouteCalculated(router.getLeftDistance()).play();
			}
		} else if(player == null){
			pendingCommand = new VoiceCommandPending(updateRoute ? 
					VoiceCommandPending.ROUTE_RECALCULATED : VoiceCommandPending.ROUTE_CALCULATED, this);
		}
		currentDirection = router.currentDirectionInfo;
		currentStatus = 0;
	}

	public void arrivedDestinationPoint() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.arrivedAtDestination().play();
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
//		protected final long timestamp;
		protected final int type;
		private final VoiceRouter voiceRouter;
		
		public VoiceCommandPending(int type, VoiceRouter voiceRouter){
			this.type = type;
			this.voiceRouter = voiceRouter;
//			timestamp = System.currentTimeMillis();
		}

		public void play(CommandBuilder newCommand) {
			int left = voiceRouter.router.getLeftDistance();
			if (left > 0) {
				if (type == ROUTE_CALCULATED) {
					newCommand.newRouteCalculated(left).play();
				} else if (type == ROUTE_RECALCULATED) {
					//suppress "route recaluated" message for 60sec
					if (System.currentTimeMillis() - lastTimeRouteRecalcAnnounced > 60000) {
						if (router.getCurrentGPXRoute() == null) {
							newCommand.routeRecalculated(left).play();
							lastTimeRouteRecalcAnnounced = System.currentTimeMillis();
						}
					}
				}
			}
		}
	}

}
