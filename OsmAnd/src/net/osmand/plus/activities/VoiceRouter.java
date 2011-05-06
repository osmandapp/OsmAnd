package net.osmand.plus.activities;

import net.osmand.plus.OsmandSettings.ApplicationMode;
import net.osmand.plus.activities.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.activities.RoutingHelper.TurnType;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayer.CommandBuilder;


public class VoiceRouter {
	// 0 - unknown, 1 - notify prepare, 2 - notify to turn after , 3 - notify to turn
	private final int STATUS_UNKNOWN = 0;
	private final int STATUS_3000_PREPARE = 1;
	private final int STATUS_800_PREPARE = 2;
	private final int STATUS_200_TURN = 3;
	private final int STATUS_TURN = 4;
	private final int STATUS_TOLD = 5;
	
	private final RoutingHelper router;
	private boolean mute = false;
	private CommandPlayer player;
	
	private int currentDirection = 0;
 
	private int currentStatus = STATUS_UNKNOWN;


	public VoiceRouter(RoutingHelper router, CommandPlayer player) {
		this.router = router;
		this.player = player;
		updateAppMode();
	}
	
	public void setPlayer(CommandPlayer player) {
		this.player = player;
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
	
	protected int PREPARE_LONG_DISTANCE_ST = 2500;
	protected int PREPARE_LONG_DISTANCE_END = 3000;
	
	protected int PREPARE_DISTANCE = 0;
	protected int TURN_IN_DISTANCE = 0;
	protected int TURN_DISTANCE = 0;
	
	public void updateAppMode(){
		if(router.getAppMode() == ApplicationMode.PEDESTRIAN){
			PREPARE_DISTANCE = 400;
			TURN_IN_DISTANCE = 150;
			TURN_DISTANCE = 30;
		} else if(router.getAppMode() == ApplicationMode.BICYCLE){
			PREPARE_DISTANCE = 550;
			TURN_IN_DISTANCE = 200;
			TURN_DISTANCE = 55;
		} else {
			PREPARE_DISTANCE = 800;
			TURN_IN_DISTANCE = 300;
			TURN_DISTANCE = 70;
		}
	}
	
	
	
	protected void updateStatus(){
		// directly after turn (go - ahead dist)
		// < 800m prepare
		// < 200m turn in
		// < 50m turn
		if(currentDirection != router.currentDirectionInfo){
			currentDirection = router.currentDirectionInfo;
			currentStatus = STATUS_UNKNOWN;
		}
		RouteDirectionInfo next = router.getNextRouteDirectionInfo();
		int dist = router.getDistanceToNextRouteDirection();
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
		if(dist == 0){
			// nothing said possibly that's wrong case we should say before that
			// however it should be checked manually !?
			return;
		}
		
		RouteDirectionInfo nextNext = router.getNextNextRouteDirectionInfo();
		
		if(currentStatus == STATUS_UNKNOWN){
			if(dist > PREPARE_DISTANCE){
				CommandBuilder play = getNewCommandPlayerToPlay();
				if(play != null){
					play.goAhead(dist).play();
				}
				currentStatus = STATUS_3000_PREPARE;
			} else if (dist < TURN_IN_DISTANCE){
				// should already told it
				currentStatus = STATUS_TURN;
			}
		}
		
		
		if(currentStatus <= STATUS_TURN && dist <= TURN_DISTANCE){
			CommandBuilder play = getNewCommandPlayerToPlay();
			if(play != null){
				String tParam = getTurnType(next.turnType);
				boolean isplay = true;
				if(tParam != null){
					play.turn(tParam);
				} else if(next.turnType.isRoundAbout()){
					play.roundAbout(next.turnType.getTurnAngle(), next.turnType.getExitOut());
				} else if(next.turnType.getValue().equals(TurnType.TU)){
					play.makeUT();
					// do not say it
//				} else if(next.turnType.getValue().equals(TurnType.C)){
//					play.goAhead();
				} else {
					isplay = false;
				}
				
				if (nextNext != null && next.distance <= TURN_IN_DISTANCE) {
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
			currentStatus = STATUS_TOLD;
		} else if(currentStatus <= STATUS_200_TURN && dist <= TURN_IN_DISTANCE){
			CommandBuilder play = getNewCommandPlayerToPlay();
			if (play != null) {
				String tParam = getTurnType(next.turnType);
				boolean isPlay = true;
				if (tParam != null) {
					play.turn(tParam, dist);
				} else if (next.turnType.isRoundAbout()) {
					play.roundAbout(dist,  next.turnType.getTurnAngle(), next.turnType.getExitOut());
				} else if (next.turnType.getValue().equals(TurnType.TU)) {
					play.makeUT(dist);
				} else {
					isPlay = false;
				}

				if (nextNext != null && next.distance <= TURN_DISTANCE) {
					TurnType t = nextNext.turnType;
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
			currentStatus = STATUS_TURN;
		} else if(currentStatus <= STATUS_800_PREPARE && dist <= PREPARE_DISTANCE){
			CommandBuilder play = getNewCommandPlayerToPlay();
			if(play != null){
				String tParam = getTurnType(next.turnType);
				if(tParam != null){
					play.prepareTurn(tParam, dist).play();
				} else if(next.turnType.isRoundAbout()){
					play.prepareRoundAbout(dist).play();
				} else if(next.turnType.getValue().equals(TurnType.TU)){
					play.prepareMakeUT(dist).play();
				} 
			}
			currentStatus = STATUS_200_TURN;
		} else if((currentStatus <= STATUS_800_PREPARE && dist <= PREPARE_DISTANCE)
				|| (currentStatus <= STATUS_3000_PREPARE && dist <= PREPARE_LONG_DISTANCE_END && dist >= PREPARE_LONG_DISTANCE_ST)){
			CommandBuilder play = getNewCommandPlayerToPlay();
			if(play != null){
				String tParam = getTurnType(next.turnType);
				if(tParam != null){
					play.prepareTurn(tParam, dist).play();
				} else if(next.turnType.isRoundAbout()){
					play.prepareRoundAbout(dist).play();
				} else if(next.turnType.getValue().equals(TurnType.TU)){
					play.prepareMakeUT(dist).play();
				} 
			}
			currentStatus = currentStatus <= STATUS_3000_PREPARE ? STATUS_800_PREPARE : STATUS_200_TURN;
		}
		
		
		
		
		
	}
	
	private String getTurnType(TurnType t){
		if(TurnType.TL.equals(t.getValue())){
			return CommandPlayer.A_LEFT;
		} else if(TurnType.TSHL.equals(t.getValue())){
			return CommandPlayer.A_LEFT_SH;
		} else if(TurnType.TSLL.equals(t.getValue())){
			return CommandPlayer.A_LEFT_SL;
		} else if(TurnType.TR.equals(t.getValue())){
			return CommandPlayer.A_RIGHT;
		} else if(TurnType.TSHR.equals(t.getValue())){
			return CommandPlayer.A_RIGHT_SH;
		} else if(TurnType.TSLR.equals(t.getValue())){
			return CommandPlayer.A_RIGHT_SL;
		}
		return null;
	}

	public void newRouteIsCalculated(boolean updateRoute) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			if (updateRoute) {
				play.routeRecalculated(router.getLeftDistance()).play();
			} else {
				play.newRouteCalculated(router.getLeftDistance()).play();
			}
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

}
