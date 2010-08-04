package com.osmand.activities;

import android.content.Context;

import com.osmand.OsmandSettings.ApplicationMode;
import com.osmand.activities.RoutingHelper.RouteDirectionInfo;
import com.osmand.activities.RoutingHelper.TurnType;
import com.osmand.voice.CommandPlayer;
import com.osmand.voice.CommandPlayer.CommandBuilder;

public class VoiceRouter {
	
	private final RoutingHelper router;
	private boolean mute = false;
	private CommandPlayer player;
	
	private int currentDirection = 0;
	// 0 - unknown, 1 - notify prepare, 2 - notify to turn after , 3 - notify to turn 
	private int currentStatus = 0;

	public VoiceRouter(RoutingHelper router){
		this.router = router;
		updateAppMode();
	}
	
	protected void init(Context ctx){
		player = CommandPlayer.getInstance(ctx);
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
	
	protected int PREPARE_DISTANCE = 0;
	protected int TURN_IN_DISTANCE = 0;
	protected int TURN_DISTANCE = 0;
	
	public void updateAppMode(){
		if(router.getAppMode() == ApplicationMode.PEDESTRIAN){
			PREPARE_DISTANCE = 400;
			TURN_IN_DISTANCE = 150;
			TURN_DISTANCE = 20;
		} else if(router.getAppMode() == ApplicationMode.BICYCLE){
			PREPARE_DISTANCE = 550;
			TURN_IN_DISTANCE = 200;
			TURN_DISTANCE = 40;
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
			currentStatus = 0;
		}
		RouteDirectionInfo next = router.getNextRouteDirectionInfo();
		int dist = router.getDistanceToNextRouteDirection();
		if(next == null || next.distance == 0) {
			if(currentStatus == 0 && currentDirection > 0){
				CommandBuilder play = getNewCommandPlayerToPlay();
				if(play != null){
					play.goAhead(router.getLeftDistance()).andArriveAtDestination().play();
				}
				currentStatus = 1;
			}
			return;
		}
		if(dist == 0){
			// nothing said possibly that's wrong case we should say before that
			// however it should be checked manually !?
			return;
		}
		
		RouteDirectionInfo nextNext = router.getNextNextRouteDirectionInfo();
		
		if(currentStatus == 0){
			if(dist > PREPARE_DISTANCE){
				CommandBuilder play = getNewCommandPlayerToPlay();
				if(play != null){
					play.goAhead(dist).play();
				}
			} else if (dist < TURN_IN_DISTANCE){
				// should already told it
				currentStatus = 3;
			}
			currentStatus = 1;
		}
		
		
		if(currentStatus <= 3 && dist <= TURN_DISTANCE){
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
			currentStatus = 4;
		} else if(currentStatus <= 2 && dist <= TURN_IN_DISTANCE){
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
			currentStatus = 3;
		} else if(currentStatus <= 1 && dist <= PREPARE_DISTANCE){
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
			currentStatus = 2;
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

	public void newRouteIsCalculated() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			play.newRouteCalculated(router.getLeftDistance()).play();
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
