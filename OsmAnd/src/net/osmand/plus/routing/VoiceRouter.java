package net.osmand.plus.routing;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;


public class VoiceRouter {
	private static final int STATUS_UTWP_TOLD = -1;
	private static final int STATUS_UNKNOWN = 0;
	private static final int STATUS_LONG_PREPARE = 1;
	private static final int STATUS_PREPARE = 2;
	private static final int STATUS_TURN_IN = 3;
	private static final int STATUS_TURN = 4;
	private static final int STATUS_TOLD = 5;
	
	private final RoutingHelper router;
	private boolean mute = false;
	private CommandPlayer player;
    private final OsmandSettings settings;
 
	private int currentStatus = STATUS_UNKNOWN;
	private boolean playedAndArriveAtTarget = false;
	private float playGoAheadDist = 0;
	private long lastAnnouncedSpeedLimit = 0;
	private long lastAnnouncedOffRoute = 0;
	private long waitAnnouncedSpeedLimit = 0;
	private long waitAnnouncedOffRoute = 0;

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
	private Term empty;

    //remember when last announcement was made
    private long lastAnnouncement = 0;


    public interface VoiceMessageListener {
    	void onVoiceMessage();
    }
    private List<VoiceMessageListener> voiceMessageListeners;
    private Handler handler;
    
	public VoiceRouter(RoutingHelper router, final OsmandSettings settings, CommandPlayer player) {
		this.router = router;
		this.player = player;
        this.settings = settings;

		empty = new Struct("");
		voiceMessageListeners = new ArrayList<VoiceRouter.VoiceMessageListener>();
		Looper looper = Looper.myLooper();
		if (looper == null) {
			looper = Looper.getMainLooper();
		}
		handler = new Handler(looper);
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
        lastAnnouncement = System.currentTimeMillis();
		return player.newCommandBuilder();
	}
	
	
	
	public void updateAppMode(){
		// turn prompt starts either at distance, or if actual-lead-time(currentSpeed) < maximum-lead-time  
		// lead time criterion only for TURN_IN and TURN
		PREPARE_LONG_DISTANCE = 3500;             // [105 sec] - 120 km/h
		PREPARE_LONG_DISTANCE_END = 3000;         // [ 90 sec] - 120 km/h
		if(router.getAppMode().isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)){
			// prepare_long_distance warning not needed for pedestrian
			PREPARE_LONG_DISTANCE_END = PREPARE_LONG_DISTANCE + 100; // do not play
			// prepare distance is not needed for pedestrian
			//PREPARE_DISTANCE = 200;           // [100 sec]
			//PREPARE_DISTANCE_END = 150 + 100; // [ 75 sec] + not play
			PREPARE_DISTANCE = 100;           // [ 50 sec]
			PREPARE_DISTANCE_END = 70;        // [ 35 sec]
			TURN_IN_DISTANCE = 50;            //   25 sec, (was 100m, 50 sec)
			TURN_IN_DISTANCE_END = 30;        //   15 sec  (was  70m, 35 sec)
			TURN_DISTANCE = 15;               //   7,5sec (was  25m, 12 sec). Check if this works with GPS accuracy!
			TURN_DEFAULT_SPEED = DEFAULT_SPEED = 2f;  //   7,2 km/h
		} else if(router.getAppMode().isDerivedRoutingFrom(ApplicationMode.BICYCLE)){
			PREPARE_LONG_DISTANCE = 500;      // [100 sec]
			PREPARE_LONG_DISTANCE_END = 300;  // [ 60 sec]
			PREPARE_DISTANCE = 200;           // [ 40 sec] (was 500m, 100sec)
			PREPARE_DISTANCE_END = 120;       // [ 24 sec] (was 350m,  70sec)
			TURN_IN_DISTANCE = 80;            //   16 sec  (was 225m,  45sec)
			TURN_IN_DISTANCE_END = 60;        //   12 sec  (was  80m,  16sec)
			TURN_DISTANCE = 30;               //    6 sec  (was  45m,   9sec). Check if this works with GPS accuracy!
			TURN_DEFAULT_SPEED = DEFAULT_SPEED = 5;   //  18 km/h
		} else if(router.getAppMode().isDerivedRoutingFrom(ApplicationMode.CAR)){
			PREPARE_DISTANCE = 1500;          // [125 sec]
			PREPARE_DISTANCE_END = 1200;      // [100 sec]
			TURN_IN_DISTANCE = 390;           //   30 sec
			TURN_IN_DISTANCE_END = 182;       //   14 sec
			TURN_DISTANCE = 50;               //    7 sec
			TURN_DEFAULT_SPEED = 7f;          //   25 km/h
			DEFAULT_SPEED = 13;               //   48 km/h
		} else {
			DEFAULT_SPEED = router.getAppMode().getDefaultSpeed();
			TURN_DEFAULT_SPEED = DEFAULT_SPEED / 2;
			PREPARE_LONG_DISTANCE = (int) (DEFAULT_SPEED * 305);
			PREPARE_LONG_DISTANCE_END = (int) (DEFAULT_SPEED * 225);
			PREPARE_DISTANCE = (int) (DEFAULT_SPEED * 125);	
			PREPARE_DISTANCE_END = (int) (DEFAULT_SPEED * 100);
			TURN_IN_DISTANCE = (int) (DEFAULT_SPEED * 30);
			TURN_IN_DISTANCE_END = (int) (DEFAULT_SPEED * 14);
			TURN_DISTANCE = (int) (DEFAULT_SPEED * 7);
		}
	}
	
	public boolean isDistanceLess(float currentSpeed, double dist, double etalon){
		if(currentSpeed <= 0) {
			currentSpeed = DEFAULT_SPEED;
		}
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
	
	public void announceOffRoute(double dist) {
		long ms = System.currentTimeMillis();
		if(waitAnnouncedOffRoute == 0 || ms - lastAnnouncedOffRoute > waitAnnouncedOffRoute) {
			CommandBuilder p = getNewCommandPlayerToPlay();
			if (p != null) {
				notifyOnVoiceMessage();
				p.offRoute(dist).play();
			}
			if(waitAnnouncedOffRoute == 0) {
				waitAnnouncedOffRoute = 60000;	
			} else {
				waitAnnouncedOffRoute *= 2.5;
			}
			lastAnnouncedOffRoute = ms;
		}
	}

	public void announceBackOnRoute() {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			notifyOnVoiceMessage();
			p.backOnRoute().play();
		}
	}

	public void approachWaypoint(Location location, List<LocationPointWrapper> points){
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}
		notifyOnVoiceMessage();
		double[] dist = new double[1];
		makeSound();
		String text = getText(location, points, dist);
		p.goAhead(dist[0], null).andArriveAtWayPoint(text).play();
	}

	public void approachFavorite(Location location, List<LocationPointWrapper> points){
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}
		notifyOnVoiceMessage();
		double[] dist = new double[1];
		makeSound();
		String text = getText(location, points, dist);
		p.goAhead(dist[0], null).andArriveAtFavorite(text).play();
	}
	
	public void approachPoi(Location location, List<LocationPointWrapper> points){
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}

		notifyOnVoiceMessage();
		double[] dist = new double[1];
		String text = getText(location, points,  dist);
		p.goAhead(dist[0], null).andArriveAtPoiWaypoint(text).play();
	}

	public void announceWaypoint(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}
		notifyOnVoiceMessage();
		makeSound();
		String text = getText(null, points,null);
		p.arrivedAtWayPoint(text).play();
	}
	
	public void announceFavorite(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}
		notifyOnVoiceMessage();
		makeSound();
		String text = getText(null, points,null);
		p.arrivedAtFavorite(text).play();
	}
	
	public void announcePoi(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p == null){
			return;
		}
		notifyOnVoiceMessage();
		String text = getText(null, points,null);
		p.arrivedAtPoi(text).play();
	}

	protected String getText(Location location, List<LocationPointWrapper> points, double[] dist) {
		String text = "";
		for (LocationPointWrapper point : points) {
			// need to calculate distance to nearest point
			if (text.length() == 0) {
				if (location != null && dist != null) {
					dist[0] = point.getDeviationDistance() + 
							MapUtils.getDistance(location.getLatitude(), location.getLongitude(),
									point.getPoint().getLatitude(), point.getPoint().getLongitude());
				}
			} else {
				text += ", ";
			}
			text += point.getPoint().getName(router.getApplication());
		}
		return text;
	}

	public void announceAlarm(AlarmInfoType type) {
		if (type == AlarmInfoType.SPEED_LIMIT) {
			announceSpeedAlarm();
		} else if (type == AlarmInfoType.SPEED_CAMERA) {
			if (router.getSettings().SPEAK_SPEED_CAMERA.get()) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					notifyOnVoiceMessage();
					p.attention(type+"").play();
				}
			}
		} else {
			if (router.getSettings().SPEAK_TRAFFIC_WARNINGS.get()) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					notifyOnVoiceMessage();
					p.attention(type+"").play();
				}
			}
		}
	}

	public void announceSpeedAlarm() {
		long ms = System.currentTimeMillis();
		if (waitAnnouncedSpeedLimit == 0) {
			// wait 10 seconds before announcement
			if (ms - lastAnnouncedSpeedLimit > 120 * 1000) {
				waitAnnouncedSpeedLimit = ms;
			}	
		} else {
			// if we wait before more than 20 sec (reset counter)
			if (ms - waitAnnouncedSpeedLimit > 20 * 1000) {
				waitAnnouncedSpeedLimit = 0;
			} else if (router.getSettings().SPEAK_SPEED_LIMIT.get()  && ms - waitAnnouncedSpeedLimit > 10 * 1000 ) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					notifyOnVoiceMessage();
					lastAnnouncedSpeedLimit = ms;
					waitAnnouncedSpeedLimit = 0;
					p.speedAlarm().play();
				}
			}
		}
	}
	
	private boolean isTargetPoint(NextDirectionInfo info) {
		boolean in = info != null && info.intermediatePoint;
		boolean target = info == null || info.directionInfo == null
				|| info.directionInfo.distance == 0;
		return in || target;
	}

	private boolean needsInforming() {
		final Integer repeat = settings.KEEP_INFORMING.get();
		if (repeat == null || repeat == 0) return false;

		final long notBefore = lastAnnouncement + repeat * 60 * 1000L;

		return System.currentTimeMillis() > notBefore;
	}

    /**
	 * Updates status of voice guidance 
	 * @param currentLocation 
	 */
	protected void updateStatus(Location currentLocation, boolean repeat) {
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
        RouteSegmentResult currentSegment = router.getCurrentSegmentResult();
		if (nextInfo.directionInfo == null) {
			return;
		}
		int dist = nextInfo.distanceTo;
		RouteDirectionInfo next = nextInfo.directionInfo;

		// if routing is changed update status to unknown
		if (next != nextRouteDirection) {
			nextRouteDirection = next;
			currentStatus = STATUS_UNKNOWN;
			playedAndArriveAtTarget = false;
			playGoAheadDist = 0;
		}

		if (!repeat) {
			if (dist == 0) {
				return;
			} else if (needsInforming()) {
				playGoAhead(dist, getSpeakableStreetName(currentSegment, next));
				return;
			} else if (currentStatus == STATUS_TOLD) {
				// nothing said possibly that's wrong case we should say before that
				// however it should be checked manually ?
				return;
			}
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

		NextDirectionInfo nextNextInfo = router.getNextRouteDirectionInfoAfter(nextInfo, new NextDirectionInfo(), !repeat);
		if ((repeat || statusNotPassed(STATUS_TURN)) && isDistanceLess(speed, dist, TURN_DISTANCE, TURN_DEFAULT_SPEED)) {
			if (next.distance < TURN_IN_DISTANCE_END && nextNextInfo != null) {
				playMakeTurn(currentSegment, next, nextNextInfo.directionInfo);
			} else {
				playMakeTurn(currentSegment, next, null);
			}
			if(next.distance < TURN_IN_DISTANCE && isTargetPoint(nextNextInfo)) {
				andSpeakArriveAtPoint(nextNextInfo);
			}
			nextStatusAfter(STATUS_TURN);
		} else if ((repeat || statusNotPassed(STATUS_TURN_IN)) && isDistanceLess(speed, dist, TURN_IN_DISTANCE)) {
			if (repeat || dist >= TURN_IN_DISTANCE_END) {
				if ((isDistanceLess(speed, next.distance, TURN_DISTANCE) || next.distance < TURN_IN_DISTANCE_END) &&
						nextNextInfo != null) {
					playMakeTurnIn(currentSegment, next, dist, nextNextInfo.directionInfo);
				} else {
					playMakeTurnIn(currentSegment, next, dist, null);
				}
				playAndArriveAtDestination(repeat, nextInfo, currentSegment);
			}
			nextStatusAfter(STATUS_TURN_IN);
			// } else if (statusNotPassed(STATUS_PREPARE) && isDistanceLess(speed, dist, PREPARE_DISTANCE)) {
		} else if ((repeat || statusNotPassed(STATUS_PREPARE)) && (dist <= PREPARE_DISTANCE)) {
			if (repeat || dist >= PREPARE_DISTANCE_END) {
				if (!repeat && (next.getTurnType().keepLeft() || next.getTurnType().keepRight())){
					// do not play prepare for keep left/right
				} else {
					playPrepareTurn(currentSegment, next, dist);
					playAndArriveAtDestination(repeat, nextInfo, currentSegment);
				}
			}
			nextStatusAfter(STATUS_PREPARE);
			// } else if (statusNotPassed(STATUS_LONG_PREPARE) && isDistanceLess(speed, dist, PREPARE_LONG_DISTANCE)){
		} else if ((repeat || statusNotPassed(STATUS_LONG_PREPARE)) && (dist <= PREPARE_LONG_DISTANCE)) {
			if (repeat || dist >= PREPARE_LONG_DISTANCE_END) {
				playPrepareTurn(currentSegment, next, dist);
				playAndArriveAtDestination(repeat, nextInfo, currentSegment);
			}
			nextStatusAfter(STATUS_LONG_PREPARE);
		} else if (statusNotPassed(STATUS_UNKNOWN)) {
			// strange how we get here but
			nextStatusAfter(STATUS_UNKNOWN);
		} else if (repeat || (statusNotPassed(STATUS_TURN_IN) && dist < playGoAheadDist)) {
			playGoAheadDist = 0;
			playGoAhead(dist, getSpeakableStreetName(currentSegment, next));
		}
	}

	private void playAndArriveAtDestination(boolean repeat, NextDirectionInfo nextInfo,
			RouteSegmentResult currentSegment) {
		RouteDirectionInfo next = nextInfo.directionInfo;
		if(isTargetPoint(nextInfo) && (!playedAndArriveAtTarget || repeat)) {
			if(next.getTurnType().goAhead()) {
				playGoAhead(nextInfo.distanceTo, getSpeakableStreetName(currentSegment, next));
			}
			andSpeakArriveAtPoint(nextInfo);
			playedAndArriveAtTarget = true;
		}
	}

	public void announceCurrentDirection(Location currentLocation) {
		synchronized (router) {
			if (currentStatus != STATUS_UTWP_TOLD) {
				updateStatus(currentLocation, true);
			} else if (playMakeUTwp()) {
				playGoAheadDist = 0;
			}
		}
	}

	
	private boolean playMakeUTwp() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			notifyOnVoiceMessage();
			play.makeUTwp().play();
			return true;
		}
		return false;
	}

	private void playGoAhead(int dist, Term streetName) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			notifyOnVoiceMessage();
			play.goAhead(dist, streetName).play();
		}
	}

	public Term getSpeakableStreetName(RouteSegmentResult currentSegment, RouteDirectionInfo i) {
		if(i == null || !router.getSettings().SPEAK_STREET_NAMES.get()){
			return empty;
		}
		if (player != null && player.supportsStructuredStreetNames()) {
			Struct next = new Struct(new Term[] { getTermString(getSpeakablePointName(i.getRef())),
					getTermString(getSpeakablePointName(i.getStreetName())),
					getTermString(getSpeakablePointName(i.getDestinationName())) });
			Term current = empty;
			if (currentSegment != null) {
				RouteDataObject obj = currentSegment.getObject();
				current = new Struct(new Term[] { getTermString(getSpeakablePointName(obj.getRef())),
						getTermString(getSpeakablePointName(obj.getName())),
						getTermString(getSpeakablePointName(obj.getDestinationName())) });
			}
			Struct voice = new Struct("voice", next, current );
			return voice;
		} else {
			Term rf = getTermString(getSpeakablePointName(i.getRef()));
			if(rf == empty) {
				rf = getTermString(getSpeakablePointName(i.getStreetName()));
			}
			return rf;
		}
	}
	
	private Term getTermString(String s) {
		if(!Algorithms.isEmpty(s)) {
			return new Struct(s);
		}
		return empty;
	}

	public String getSpeakablePointName(String pn) {
		// Replace characters which may produce unwanted tts sounds:
		if(pn != null) {
			pn = pn.replace('-', ' ');
			pn = pn.replace(':', ' ');
			if ((player != null) && (!"de".equals(player.getLanguage()))) {
				pn = pn.replace("\u00df", "ss"); // Helps non-German tts voices to pronounce German Strasse (=street)
			}
			if ((player != null) && ("en".startsWith(player.getLanguage()))) {
				pn = pn.replace("SR", "S R");    // Avoid SR (as for State Route or Strada Regionale) be pronounced as "Senior" in English tts voice
			}
		}
		return pn;
	}

	private void playPrepareTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			String tParam = getTurnType(next.getTurnType());
			if(tParam != null){
				notifyOnVoiceMessage();
				play.prepareTurn(tParam, dist, getSpeakableStreetName(currentSegment, next)).play();
			} else if(next.getTurnType().isRoundAbout()){
				notifyOnVoiceMessage();
				play.prepareRoundAbout(dist, next.getTurnType().getExitOut(), getSpeakableStreetName(currentSegment, next)).play();
			} else if(next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU){
				notifyOnVoiceMessage();
				play.prepareMakeUT(dist, getSpeakableStreetName(currentSegment, next)).play();
			} 
		}
	}

	private void playMakeTurnIn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist, RouteDirectionInfo pronounceNextNext) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			String tParam = getTurnType(next.getTurnType());
			boolean isPlay = true;
			if (tParam != null) {
				play.turn(tParam, dist, getSpeakableStreetName(currentSegment, next));
			} else if (next.getTurnType().isRoundAbout()) {
				play.roundAbout(dist, next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(), getSpeakableStreetName(currentSegment, next));
			} else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
				play.makeUT(dist, getSpeakableStreetName(currentSegment, next));
			} else {
				isPlay = false;
			}
			// small preparation to next after next
			if (pronounceNextNext != null) {
				TurnType t = pronounceNextNext.getTurnType();
				isPlay = true;
				if (next.getTurnType().getValue() == TurnType.C && 
						TurnType.C != t.getValue()) {
					play.goAhead(dist, getSpeakableStreetName(currentSegment, next));
				}
				if (TurnType.TL == t.getValue() || TurnType.TSHL == t.getValue() || TurnType.TSLL == t.getValue()
						|| TurnType.TU == t.getValue() || TurnType.KL == t.getValue()) {
					play.then().bearLeft( getSpeakableStreetName(currentSegment, next));
				} else if (TurnType.TR == t.getValue() || TurnType.TSHR == t.getValue() || TurnType.TSLR == t.getValue()
						|| TurnType.KR == t.getValue()) {
					play.then().bearRight( getSpeakableStreetName(currentSegment, next));
				}
			}
			if(isPlay){
				notifyOnVoiceMessage();
				play.play();
			}
		}
	}
	
	private void andSpeakArriveAtPoint(NextDirectionInfo info) {
		if (isTargetPoint(info)) {
			String pointName = info == null ? "" : info.pointName;
			CommandBuilder play = getNewCommandPlayerToPlay();
			if (play != null) {
				notifyOnVoiceMessage();
				if (info != null && info.intermediatePoint) {
					play.andArriveAtIntermediatePoint(getSpeakablePointName(pointName)).play();
				} else {
					play.andArriveAtDestination(getSpeakablePointName(pointName)).play();
				}
			}
		}
	}

	private void playMakeTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, RouteDirectionInfo nextNext) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			String tParam = getTurnType(next.getTurnType());
			boolean isplay = true;
			if(tParam != null){
				play.turn(tParam, getSpeakableStreetName(currentSegment, next));
			} else if(next.getTurnType().isRoundAbout()){
				play.roundAbout(next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(),  getSpeakableStreetName(currentSegment, next));
			} else if(next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU){
				play.makeUT( getSpeakableStreetName(currentSegment, next));
				// do not say it
//				} else if(next.getTurnType().getValue() == TurnType.C)){
//					play.goAhead();
			} else {
				isplay = false;
			}
			// add turn after next
			if (nextNext != null) {
				String t2Param = getTurnType(nextNext.getTurnType());
				if (t2Param != null) {
					if(isplay) { play.then(); }
					play.turn(t2Param, next.distance, empty);
				} else if (nextNext.getTurnType().isRoundAbout()) {
					if(isplay) { play.then(); }
					play.roundAbout(next.distance, nextNext.getTurnType().getTurnAngle(), nextNext.getTurnType().getExitOut(), empty);
				} else if (nextNext.getTurnType().getValue() == TurnType.TU) {
					if(isplay) { play.then(); }
					play.makeUT(next.distance, empty);
				}
				isplay = true;
			}
			if(isplay){
				notifyOnVoiceMessage();
				play.play();
			}
		}
	}
	
	private String getTurnType(TurnType t){
		if(TurnType.TL == t.getValue()){
			return AbstractPrologCommandPlayer.A_LEFT;
		} else if(TurnType.TSHL == t.getValue()){
			return AbstractPrologCommandPlayer.A_LEFT_SH;
		} else if(TurnType.TSLL == t.getValue()){
			return AbstractPrologCommandPlayer.A_LEFT_SL;
		} else if(TurnType.TR == t.getValue()){
			return AbstractPrologCommandPlayer.A_RIGHT;
		} else if(TurnType.TSHR == t.getValue()){
			return AbstractPrologCommandPlayer.A_RIGHT_SH;
		} else if(TurnType.TSLR == t.getValue()){
			return AbstractPrologCommandPlayer.A_RIGHT_SL;
		} else if(TurnType.KL == t.getValue()){
			return AbstractPrologCommandPlayer.A_LEFT_KEEP;
		} else if(TurnType.KR == t.getValue()){
			return AbstractPrologCommandPlayer.A_RIGHT_KEEP;
		}
		return null;
	}
	
	public void gpsLocationLost(){
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			notifyOnVoiceMessage();
			play.gpsLocationLost().play();
		}
	}
	
	public void gpsLocationRecover() {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if (play != null) {
			notifyOnVoiceMessage();
			play.gpsLocationRecover().play();
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
					notifyOnVoiceMessage();
					play.routeRecalculated(router.getLeftDistance(), router.getLeftTime()).play();
					currentStatus = STATUS_UNKNOWN;
					// lastTimeRouteRecalcAnnounced = System.currentTimeMillis();
				}
			} else {
				notifyOnVoiceMessage();
				play.newRouteCalculated(router.getLeftDistance(), router.getLeftTime()).play();
				currentStatus = STATUS_UNKNOWN;
			}
		} else if (player == null) {
			pendingCommand = new VoiceCommandPending(!newRoute ? VoiceCommandPending.ROUTE_RECALCULATED
					: VoiceCommandPending.ROUTE_CALCULATED, this);
			currentStatus = STATUS_UNKNOWN;
		}
		nextRouteDirection = null;
	}

	public void arrivedDestinationPoint(String name) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			notifyOnVoiceMessage();
			play.arrivedAtDestination(getSpeakablePointName(name)).play();
		}
	}
	
	public void arrivedIntermediatePoint(String name) {
		CommandBuilder play = getNewCommandPlayerToPlay();
		if(play != null){
			notifyOnVoiceMessage();
			play.arrivedAtIntermediatePoint(getSpeakablePointName(name)).play();
		}
	}

	// This is not needed, used are only arrivedIntermediatePoint (for points on the route) or announceWaypoint (for points near the route=)
	//public void arrivedWayPoint(String name) {
	//	CommandBuilder play = getNewCommandPlayerToPlay();
	//	if(play != null){
	// notifyOnVoiceMessage();
	//		play.arrivedAtWayPoint(getSpeakablePointName(name)).play();
	//	}
	//}

	public void onApplicationTerminate(Context ctx) {
		if (player != null) {
			player.clear();
		}
	}

	public void interruptRouteCommands() {
		if (player != null){
			player.stop();
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
			int time = voiceRouter.router.getLeftTime();
			if (left > 0) {
				if (type == ROUTE_CALCULATED) {
					notifyOnVoiceMessage();
					newCommand.newRouteCalculated(left, time).play();
				} else if (type == ROUTE_RECALCULATED) {
					notifyOnVoiceMessage();
					newCommand.routeRecalculated(left, time).play();
				}
			}
		}
	}

	private void makeSound(){
		SoundPool sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		int soundClick = -1;
		boolean success = true;
		try {
			soundClick = sp.load(settings.getContext().getAssets().openFd("sounds/airhorn.ogg"), 1);
		} catch (IOException e){
			e.printStackTrace();
			success = false;
		}
		if (success){
			sp.play(soundClick, 1 ,1, 0, 0, 1);
		}
	}

	public void addVoiceMessageListener(VoiceMessageListener voiceMessageListener) {
		synchronized (voiceMessageListeners) {
			if (!voiceMessageListeners.contains(voiceMessageListener)) {
				voiceMessageListeners.add(voiceMessageListener);
			}
		}
	}
	
	public void removeVoiceMessageListener(VoiceMessageListener voiceMessageListener) {
		synchronized (voiceMessageListeners) {
			if (voiceMessageListeners.contains(voiceMessageListener)) {
				voiceMessageListeners.remove(voiceMessageListener);
			}
		}
	}

	public void notifyOnVoiceMessage() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				synchronized (voiceMessageListeners) {
					for (final VoiceMessageListener voiceMessageListener : voiceMessageListeners) {
						Runnable closure = new Runnable() {
							public void run() {
								if (settings.WAKE_ON_VOICE.get() > 0) {
									synchronized (voiceMessageListeners) {
										if (voiceMessageListeners
												.contains(voiceMessageListener)
												&& (settings.WAKE_ON_VOICE.get() > 0)) {
											voiceMessageListener
													.onVoiceMessage();
										}
									}
								}
							}
						};
						handler.post(closure);
					}
				}
			}
		});
	}
}
