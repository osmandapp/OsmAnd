package net.osmand.plus.routing;


import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_LONG_PREPARE_TURN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_PREPARE_TURN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_IN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_NOW;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.routing.data.StreetName;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.ExitInfo;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VoiceRouter {

	private static final int STATUS_UTWP_TOLD = -1;
	private static final int STATUS_UNKNOWN = 0;
	private static final int STATUS_LONG_PREPARE = 1;
	private static final int STATUS_PREPARE = 2;
	private static final int STATUS_TURN_IN = 3;
	private static final int STATUS_TURN = 4;
	private static final int STATUS_TOLD = 5;

	public static final String TO_REF = "toRef";
	public static final String TO_STREET_NAME = "toStreetName";
	public static final String TO_DEST = "toDest";
	public static final String FROM_REF = "fromRef";
	public static final String FROM_STREET_NAME = "fromStreetName";
	public static final String FROM_DEST = "fromDest";

	protected CommandPlayer player;

	protected final OsmandApplication app;
	protected final RoutingHelper router;
	protected OsmandSettings settings;
	private ApplicationMode appMode;
	private AnnounceTimeDistances atd;

	private int currentStatus = STATUS_UNKNOWN;
	private boolean playedAndArriveAtTarget;
	private float playGoAheadDist;
	private long lastAnnouncedSpeedLimit;
	private long waitAnnouncedSpeedLimit;
	private long lastAnnouncedOffRoute;
	private long waitAnnouncedOffRoute;
	private boolean suppressDest;
	private boolean announceBackOnRoute;
	// private long lastTimeRouteRecalcAnnounced = 0;
	// Remember when last announcement was made
	private long lastAnnouncement;


	private SoundPool soundPool;
	private int soundClick = -1;

	private VoiceCommandPending pendingCommand;
	private RouteDirectionInfo nextRouteDirection;


	public interface VoiceMessageListener {
		void onVoiceMessage(List<String> listCommands, List<String> played);
	}

	private List<WeakReference<VoiceMessageListener>> voiceMessageListeners = new ArrayList<>();
    
	VoiceRouter(RoutingHelper router) {
		this.router = router;
		this.app = router.getApplication();
		this.settings = app.getSettings();
		updateAppMode();

		OsmAndAppCustomizationListener customizationListener = new OsmAndAppCustomizationListener() {
			@Override
			public void onOsmAndSettingsCustomized() {
				settings = app.getSettings();
			}
		};
		app.getAppCustomization().addListener(customizationListener);

		if (!isMute()) {
			loadCameraSound();
		}
		settings.VOICE_MUTE.addListener(new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				app.runInUIThread(() -> {
					if (!isMute() && soundPool == null) {
						loadCameraSound();
					}
				});
			}
		});
	}

	private void loadCameraSound() {
		if (soundPool == null) {
			AudioAttributes attr = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_NOTIFICATION)
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build();
			soundPool = new SoundPool.Builder().setAudioAttributes(attr).setMaxStreams(5).build();
		}
		if (soundClick == -1) {
			try {
				// Taken unaltered from https://freesound.org/people/Corsica_S/sounds/91926/ under license http://creativecommons.org/licenses/by/3.0/ :
				AssetFileDescriptor assetFileDescriptor = app.getAssets().openFd("sounds/ding.ogg");
				soundClick = soundPool.load(assetFileDescriptor, 1);
				assetFileDescriptor.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setPlayer(CommandPlayer player) {
		this.player = player;
		if (pendingCommand != null && player != null) {
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
		settings.VOICE_MUTE.set(mute);
	}

	public void setMuteForMode(ApplicationMode mode, boolean mute) {
		settings.VOICE_MUTE.setModeValue(mode, mute);
	}

	public boolean isMute() {
		return settings.VOICE_MUTE.get();
	}

	public boolean isMuteForMode(ApplicationMode mode) {
		return settings.VOICE_MUTE.getModeValue(mode);
	}

	private CommandBuilder getNewCommandPlayerToPlay() {
		if (player == null) {
			return null;
		}
		lastAnnouncement = System.currentTimeMillis();
		return player.newCommandBuilder();
	}


	public void updateAppMode() {
		appMode = router.getAppMode() == null ? settings.getApplicationMode() : router.getAppMode();
		atd = new AnnounceTimeDistances(appMode, app);
	}

	public int calculateImminent(float dist, Location loc) {
		return atd.getImminentTurnStatus(dist, loc);
	}

	private void nextStatusAfter(int previousStatus) {
		//STATUS_UNKNOWN=0 -> STATUS_LONG_PREPARE=1 -> STATUS_PREPARE=2 -> STATUS_TURN_IN=3 -> STATUS_TURN=4 -> STATUS_TOLD=5
		if (previousStatus != STATUS_TOLD) {
			this.currentStatus = previousStatus + 1;
			if (previousStatus == STATUS_TURN) {
				waitAnnouncedOffRoute = 0;
			}
		} else {
			this.currentStatus = previousStatus;
		}
	}
	
	private boolean statusNotPassed(int statusToCheck) {
		return currentStatus <= statusToCheck;
	}

	public AnnounceTimeDistances getAnnounceTimeDistances() {
		return atd;
	}

	public RouteDirectionInfo getNextRouteDirection() {
		return nextRouteDirection;
	}

	public float getArrivalDistance() {
		return atd.getArrivalDistance();
	}

	public void announceOffRoute(double dist) {
		if (settings.SPEAK_ROUTE_DEVIATION.get() && dist > atd.getOffRouteDistance()) {
			long ms = System.currentTimeMillis();
			if (waitAnnouncedOffRoute == 0 || ms - lastAnnouncedOffRoute > waitAnnouncedOffRoute) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					p.offRoute(dist);
					announceBackOnRoute = true;
				}
				play(p);
				if (waitAnnouncedOffRoute == 0) {
					waitAnnouncedOffRoute = 60000;
				} else {
					waitAnnouncedOffRoute *= 2.5;
				}
				lastAnnouncedOffRoute = ms;
			// Avoid offRoute/onRoute loop, #16571:
			} else if (announceBackOnRoute && (dist < 0.3 * atd.getOffRouteDistance())) {
				announceBackOnRoute();
			}
		}
	}

	private void announceBackOnRoute() {
		//if (announceBackOnRoute) {
		if (settings.SPEAK_ROUTE_DEVIATION.get()) {
			CommandBuilder p = getNewCommandPlayerToPlay();
			if (p != null) {
				p.backOnRoute();
			}
			play(p);
		}
		announceBackOnRoute = false;
		waitAnnouncedOffRoute = 0;
		//}
	}

	public void approachWaypoint(Location location, List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			double[] dist = new double[1];
			makeSound();
			String text = getText(location, points, dist);
			p.goAhead(dist[0], new StreetName()).andArriveAtWayPoint(text);
		}
		play(p);
	}

	public void approachFavorite(Location location, List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			double[] dist = new double[1];
			makeSound();
			String text = getText(location, points, dist);
			p.goAhead(dist[0], new StreetName()).andArriveAtFavorite(text);
		}
		play(p);
	}
	
	public void approachPoi(Location location, List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			double[] dist = new double[1];
			String text = getText(location, points, dist);
			p.goAhead(dist[0], new StreetName()).andArriveAtPoi(text);
		}
		play(p);
	}

	public void announceWaypoint(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			makeSound();
			String text = getText(null, points, null);
			p.arrivedAtWayPoint(text);
		}
		play(p);
	}
	
	public void announceFavorite(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			makeSound();
			String text = getText(null, points, null);
			p.arrivedAtFavorite(text);
		}
		play(p);
	}
	
	public void announcePoi(List<LocationPointWrapper> points) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			String text = getText(null, points, null);
			p.arrivedAtPoi(text);
		}
		play(p);
	}

	protected String getText(Location location, List<LocationPointWrapper> points, double[] dist) {
		String text = "";
		for (LocationPointWrapper point : points) {
			// Need to calculate distance to nearest point
			if (text.length() == 0) {
				if (location != null && dist != null) {
					dist[0] = point.getDeviationDistance() +
							MapUtils.getDistance(location.getLatitude(), location.getLongitude(),
									point.getPoint().getLatitude(), point.getPoint().getLongitude());
				}
			} else {
				text += ", ";
			}
			text += PointDescription.getSimpleName(point.getPoint(), router.getApplication());
		}
		return text;
	}

	public void announceAlarm(AlarmInfo info, float speed) {
		AlarmInfoType type = info.getType();
		if (type == AlarmInfoType.SPEED_LIMIT) {
			announceSpeedAlarm(info.getIntValue(), speed);
		} else {
			OsmandSettings settings = router.getSettings();
			boolean speakTrafficWarnings = settings.SPEAK_TRAFFIC_WARNINGS.get();
			boolean speakTunnels = type == AlarmInfoType.TUNNEL && settings.SPEAK_TUNNELS.get();
			boolean speakPedestrian = type == AlarmInfoType.PEDESTRIAN && settings.SPEAK_PEDESTRIAN.get();
			boolean speakSpeedCamera = type == AlarmInfoType.SPEED_CAMERA && settings.SPEAK_SPEED_CAMERA.get();
			boolean speakPrefType = type == AlarmInfoType.TUNNEL || type == AlarmInfoType.PEDESTRIAN || type == AlarmInfoType.SPEED_CAMERA;

			if (speakSpeedCamera || speakPedestrian || speakTunnels || speakTrafficWarnings && !speakPrefType) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					p.attention(String.valueOf(type));
				}
				play(p);
				// See Issue 2377: Announce destination again - after some motorway tolls roads split shortly after the toll
				if (type == AlarmInfoType.TOLL_BOOTH) {
					suppressDest = false;
				}
			}
		}
	}

	public void announceSpeedAlarm(int maxSpeed, float speed) {
		long ms = System.currentTimeMillis();
		if (waitAnnouncedSpeedLimit == 0) {
			//  Wait 10 seconds before announcement
			if (ms - lastAnnouncedSpeedLimit > 120 * 1000) {
				waitAnnouncedSpeedLimit = ms;
			}
		} else {
			// If we wait before more than 20 sec (reset counter)
			if (ms - waitAnnouncedSpeedLimit > 20 * 1000) {
				waitAnnouncedSpeedLimit = 0;
			} else if (router.getSettings().SPEAK_SPEED_LIMIT.get() && ms - waitAnnouncedSpeedLimit > 10 * 1000) {
				CommandBuilder p = getNewCommandPlayerToPlay();
				if (p != null) {
					lastAnnouncedSpeedLimit = ms;
					waitAnnouncedSpeedLimit = 0;
					p.speedAlarm(maxSpeed, speed);
				}
				play(p);
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
		Integer repeat = settings.KEEP_INFORMING.get();
		if (repeat == null || repeat == 0) return false;

		long notBefore = lastAnnouncement + repeat * 60 * 1000L;

		return System.currentTimeMillis() > notBefore;
	}

	/**
	* Updates status of voice guidance
	* @param currentLocation
	*/
	protected void updateStatus(Location currentLocation, boolean repeat) {
		float speed = atd.getSpeed(currentLocation);

		NextDirectionInfo nextInfo = router.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		RouteSegmentResult currentSegment = router.getCurrentSegmentResult();
		if (nextInfo == null || nextInfo.directionInfo == null) {
			return;
		}
		int dist = nextInfo.distanceTo;
		RouteDirectionInfo next = nextInfo.directionInfo;

		// If routing is changed update status to unknown
		if (next != nextRouteDirection) {
			nextRouteDirection = next;
			currentStatus = STATUS_UNKNOWN;
			suppressDest = false;
			playedAndArriveAtTarget = false;
			announceBackOnRoute = false;
			if (playGoAheadDist != -1) {
				playGoAheadDist = 0;
			}
		}

		if (!repeat) {
			if (dist <= 0) {
				return;
			} else if (needsInforming()) {
				playGoAhead(dist, next, getSpeakableStreetName(currentSegment, next, false));
				return;
			} else if (currentStatus == STATUS_TOLD) {
				// nothing said possibly that's wrong case we should say before that
				// however it should be checked manually ?
				return;
			}
		}

		if (currentStatus == STATUS_UNKNOWN) {
			// Play "Continue for ..." if (1) after route calculation if no other prompt is due, or (2) after a turn if next turn is more than PREPARE_LONG_DISTANCE away
			if ((playGoAheadDist == -1) || !atd.isTurnStateActive(0, dist, STATE_LONG_PREPARE_TURN)) {
				// 10 seconds
				playGoAheadDist = dist - 10 * speed;
			}
		}

		NextDirectionInfo nextNextInfo = router.getNextRouteDirectionInfoAfter(nextInfo, new NextDirectionInfo(), true);  //I think "true" is correct here, not "!repeat"
		// Note: getNextRouteDirectionInfoAfter(nextInfo, x, y).distanceTo is distance from nextInfo, not from current position!
		// STATUS_TURN = "Turn (now)"
		if ((repeat || statusNotPassed(STATUS_TURN)) && atd.isTurnStateActive(speed, dist, STATE_TURN_NOW)) {
			if (nextNextInfo != null && !atd.isTurnStateNotPassed(0, nextNextInfo.distanceTo, STATE_TURN_IN)) {
				playMakeTurn(currentSegment, next, nextNextInfo);
			} else {
				playMakeTurn(currentSegment, next, null);
			}
			if (!next.getTurnType().goAhead() && isTargetPoint(nextNextInfo) && nextNextInfo != null) {
				// !goAhead() avoids isolated "and arrive.." prompt, as goAhead() is not pronounced
				if (!atd.isTurnStateNotPassed(0, nextNextInfo.distanceTo, STATE_TURN_IN)) {
					// Issue #2865: Ensure a distance associated with the destination arrival is always announced, either here, or in subsequent "Turn in" prompt
					// Distance fon non-straights already announced in "Turn (now)"'s nextnext  code above
					if ((nextNextInfo != null) && (nextNextInfo.directionInfo != null) && nextNextInfo.directionInfo.getTurnType().goAhead()) {
						playThen();
						playGoAhead(nextNextInfo.distanceTo, next, new StreetName());
					}
					playAndArriveAtDestination(nextNextInfo);
				} else if (!atd.isTurnStateNotPassed(0, nextNextInfo.distanceTo / 1.2f, STATE_TURN_IN)) {
					// 1.2 is safety margin should the subsequent "Turn in" prompt not fit in amy more
					playThen();
					playGoAhead(nextNextInfo.distanceTo, next, new StreetName());
					playAndArriveAtDestination(nextNextInfo);
				}
			}
			nextStatusAfter(STATUS_TURN);

		// STATUS_TURN_IN = "Turn in ..."
		} else if ((repeat || statusNotPassed(STATUS_TURN_IN)) && atd.isTurnStateActive(speed, dist, STATE_TURN_IN)) {
			if (repeat || atd.isTurnStateNotPassed(0, dist, STATE_TURN_IN)) {
				if (nextNextInfo != null && (atd.isTurnStateActive(speed, nextNextInfo.distanceTo, STATE_TURN_NOW)
						|| !atd.isTurnStateNotPassed(speed, nextNextInfo.distanceTo, STATE_TURN_IN))) {
					playMakeTurnIn(currentSegment, next, atd.calcDistanceWithoutDelay(speed, dist), nextNextInfo.directionInfo);
				} else {
					playMakeTurnIn(currentSegment, next, atd.calcDistanceWithoutDelay(speed, dist), null);
				}
				playGoAndArriveAtDestination(repeat, nextInfo, currentSegment);
			}
			nextStatusAfter(STATUS_TURN_IN);

		// STATUS_PREPARE = "Turn after ..."
		} else if ((repeat || statusNotPassed(STATUS_PREPARE)) && atd.isTurnStateActive(0, dist, STATE_PREPARE_TURN)) {
			if (repeat || atd.isTurnStateNotPassed(0, dist, STATE_PREPARE_TURN)) {
				if (!repeat && (next.getTurnType().keepLeft() || next.getTurnType().keepRight())) {
					// Do not play prepare for keep left/right
				} else {
					playPrepareTurn(currentSegment, next, atd.calcDistanceWithoutDelay(speed, dist));
					playGoAndArriveAtDestination(repeat, nextInfo, currentSegment);
				}
			}
			nextStatusAfter(STATUS_PREPARE);

		// STATUS_LONG_PREPARE =  also "Turn after ...", we skip this now, users said this is obsolete
		} else if ((repeat || statusNotPassed(STATUS_LONG_PREPARE)) && atd.isTurnStateActive(0, dist, STATE_LONG_PREPARE_TURN)) {
			if (repeat || atd.isTurnStateNotPassed(0, dist, STATE_LONG_PREPARE_TURN)) {
				playPrepareTurn(currentSegment, next, dist);
				playGoAndArriveAtDestination(repeat, nextInfo, currentSegment);
			}
			nextStatusAfter(STATUS_LONG_PREPARE);

		// STATUS_UNKNOWN = "Continue for ..." if (1) after route calculation no other prompt is due, or (2) after a turn if next turn is more than PREPARE_LONG_DISTANCE away
		} else if (statusNotPassed(STATUS_UNKNOWN)) {
			// Strange how we get here but
			nextStatusAfter(STATUS_UNKNOWN);
		} else if (repeat || (statusNotPassed(STATUS_PREPARE) && dist < playGoAheadDist)) {
			playGoAheadDist = 0;
			playGoAhead(dist, next, getSpeakableStreetName(currentSegment, next, false));
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
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			p.makeUTwp();
			play(p);
			return true;
		}
		play(p);
		return false;
	}

	void playThen() {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			p.then();
		}
		play(p);
	}

	private void playGoAhead(int dist, RouteDirectionInfo next, StreetName streetName) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		String tParam = getTurnType(next.getTurnType());
		ExitInfo exitInfo = next.getExitInfo();
		if (p != null) {
			p.goAhead(dist, streetName);
			if (tParam != null && exitInfo != null && !Algorithms.isEmpty(exitInfo.getRef()) && settings.SPEAK_EXIT_NUMBER_NAMES.get()) {
				String stringRef = getSpeakableExitRef(exitInfo.getRef());
				p.then().takeExit(tParam, stringRef, getIntRef(exitInfo.getRef()), getSpeakableExitName(next, exitInfo, true));
			}
		}
		play(p);
	}

	private StreetName getSpeakableStreetName(RouteSegmentResult currentSegment, RouteDirectionInfo i, boolean includeDest) {
		Map<String, String> result = new HashMap<>();
		if (i == null || !router.getSettings().SPEAK_STREET_NAMES.get()) {
			return new StreetName(result);
		}
		if (player != null && player.supportsStructuredStreetNames()) {
			// Issue 2377: Play Dest here only if not already previously announced, to avoid repetition
			if (includeDest == true) {
				result.put(TO_REF, getNonNullString(getSpeakablePointName(i.getRef())));
				result.put(TO_STREET_NAME, getNonNullString(getSpeakablePointName(i.getStreetName())));
				String dest = cutLongDestination(getSpeakablePointName(i.getDestinationName()));
				result.put(TO_DEST, getNonNullString(dest));
			} else {
				result.put(TO_REF, getNonNullString(getSpeakablePointName(i.getRef())));
				result.put(TO_STREET_NAME, getNonNullString(getSpeakablePointName(i.getStreetName())));
				result.put(TO_DEST, "");
			}
			if (currentSegment != null) {
				// Issue 2377: Play Dest here only if not already previously announced, to avoid repetition
				if (includeDest == true) {
					RouteDataObject obj = currentSegment.getObject();
					result.put(FROM_REF, getNonNullString(getSpeakablePointName(obj.getRef(settings.MAP_PREFERRED_LOCALE.get(),
							settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection()))));
					result.put(FROM_STREET_NAME, getNonNullString(getSpeakablePointName(obj.getName(settings.MAP_PREFERRED_LOCALE.get(),
							settings.MAP_TRANSLITERATE_NAMES.get()))));
					String dest = cutLongDestination(getSpeakablePointName(obj.getDestinationName(settings.MAP_PREFERRED_LOCALE.get(),
							settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection())));
					result.put(FROM_DEST, getNonNullString(dest));
				} else {
					RouteDataObject obj = currentSegment.getObject();
					result.put(FROM_REF, getNonNullString(getSpeakablePointName(obj.getRef(settings.MAP_PREFERRED_LOCALE.get(),
							settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection()))));
					result.put(FROM_STREET_NAME, getNonNullString(getSpeakablePointName(obj.getName(settings.MAP_PREFERRED_LOCALE.get(),
							settings.MAP_TRANSLITERATE_NAMES.get()))));
					result.put(FROM_DEST, "");
				}
			}
		} else {
			result.put(TO_REF, getNonNullString(getSpeakablePointName(i.getRef())));
			result.put(TO_STREET_NAME, getNonNullString(getSpeakablePointName(i.getStreetName())));
			result.put(TO_DEST, "");
		}
		// Delimit refs if followed by street names to create a brief pause. Need to apply in sync for toRef and fromRef.
		String refDelimiter = ", "; 
		//Issue #16256: Some TTS engines, at least in English, pronounce coincidental number concatenations like "nn, nnn" as "thousands":
		if ((player != null) && (player.getLanguage().startsWith("en"))) {
			refDelimiter = "; ";
		}
		String toRef = result.get(TO_REF);
		String fromRef = result.get(FROM_REF);
		if (!Algorithms.isEmpty(toRef) && !Algorithms.isEmpty(result.get(TO_STREET_NAME))) {
			result.put(TO_REF, toRef + refDelimiter);
			if (!Algorithms.isEmpty(fromRef)) {
				result.put(FROM_REF, fromRef + refDelimiter);
			}
		}
		return new StreetName(result);
	}

	private StreetName getSpeakableExitName(RouteDirectionInfo routeInfo, ExitInfo exitInfo, boolean includeDest) {
		Map<String, String> result = new HashMap<>();
		if (exitInfo == null || !router.getSettings().SPEAK_STREET_NAMES.get()) {
			return new StreetName(result);
		}
		result.put(TO_REF, getNonNullString(getSpeakablePointName(exitInfo.getRef())));
		String dest = cutLongDestination(getSpeakablePointName(routeInfo.getDestinationName()));
		result.put(TO_DEST, getNonNullString(dest));
		result.put(TO_STREET_NAME, "");
		return new StreetName(result);
	}

	private String getNonNullString(String speakablePointName) {
		return speakablePointName == null ? "" : speakablePointName;
	}

	private String getSpeakablePointName(String pn) {
		// Replace characters which may produce unwanted TTS sounds:
		String pl = "";
		if (player != null) {
			pl = player.getLanguage();
		}
		if (pn != null) {
			pn = pn.replace('-', ' ');
			pn = pn.replace(':', ' ');
			pn = pn.replace(";", ", ");   // Trailing blank prevents punctuation being pronounced. Replace by comma for better intonation.
			pn = pn.replace("/", ", ");   // Slash is actually pronounced by many TTS engines, creating an awkward voice prompt, better replace by comma.
			if (!pl.startsWith("de")) {
				pn = pn.replace("\u00df", "ss");    // Helps non-German TTS voices to pronounce German Straße (=street)
			}
			if (pl.startsWith("en")) {
				pn = pn.replace("SR", "S R");       // Avoid SR (as for State Route or Strada Regionale) be pronounced as "Senior" in English TTS voice
				pn = pn.replace("Dr.", "Dr ");      // Avoid pause many English TTS voices introduce after period
			}
			if (pl.startsWith("de")) {
				if (pn.startsWith("St ")) {
					pn = pn.replace("St ", "S T "); // German Staatsstrasse, abbreviated St, often mispronounced
				}
			}
		}
		return pn;
	}

	private void playPrepareTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			String tParam = getTurnType(next.getTurnType());
			if (tParam != null) {
				p.prepareTurn(tParam, dist, getSpeakableStreetName(currentSegment, next, true));
			} else if (next.getTurnType().isRoundAbout()) {
				p.prepareRoundAbout(dist, next.getTurnType().getExitOut(), getSpeakableStreetName(currentSegment, next, true));
			} else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
				p.prepareMakeUT(dist, getSpeakableStreetName(currentSegment, next, true));
			}
		}
		play(p);
	}

	private String getSpeakableExitRef(String exit) {
		StringBuilder sb = new StringBuilder();
		if (exit != null) {
			exit = exit.replace('-', ' ');
			exit = exit.replace(':', ' ');
			//	Add spaces between digits and letters for better pronunciation
			int length = exit.length();
			for (int i = 0; i < length; i++) {
				if (i + 1 < length && Character.isDigit(exit.charAt(i)) && Character.isLetter(exit.charAt(i + 1))) {
					sb.append(exit.charAt(i));
					sb.append(' ');
				} else {
					sb.append(exit.charAt(i));
				}
			}
		}
		return sb.toString();
	}

	private int getIntRef(String stringRef) {
		int intRef = Algorithms.findFirstNumberEndIndex(stringRef);
		if (intRef > 0) {
			try {
				intRef = (int) Float.parseFloat(stringRef.substring(0, intRef));
			} catch (RuntimeException e) {
				intRef = -1;
			}
		}
		return intRef;
	}

	private void playMakeTurnIn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist, RouteDirectionInfo pronounceNextNext) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			String tParam = getTurnType(next.getTurnType());
			boolean isPlay = true;
			ExitInfo exitInfo = next.getExitInfo();
			if (tParam != null) {
				if (exitInfo != null && !Algorithms.isEmpty(exitInfo.getRef()) && settings.SPEAK_EXIT_NUMBER_NAMES.get()) {
					String stringRef = getSpeakableExitRef(exitInfo.getRef());
					p.takeExit(tParam, dist, stringRef, getIntRef(exitInfo.getRef()), getSpeakableExitName(next, exitInfo, true));
				} else {
					p.turn(tParam, dist, getSpeakableStreetName(currentSegment, next, true));
				}
				suppressDest = true;
			} else if (next.getTurnType().isRoundAbout()) {
				p.roundAbout(dist, next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(), getSpeakableStreetName(currentSegment, next, true));
				// Other than in prepareTurn, in prepareRoundabout we do not announce destination, so we can repeat it one more time
				suppressDest = false;
			} else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
				p.makeUT(dist, getSpeakableStreetName(currentSegment, next, true));
				suppressDest = true;
			} else {
				isPlay = false;
			}
			// 'then keep' preparation for next after next. (Also announces an interim straight segment, which is not pronounced above.)
			if (pronounceNextNext != null) {
				TurnType t = pronounceNextNext.getTurnType();
				isPlay = true;
				if (t.getValue() != TurnType.C && next.getTurnType().getValue() == TurnType.C) {
					p.goAhead(dist, getSpeakableStreetName(currentSegment, next, true));
				}
				if (t.getValue() == TurnType.TL || t.getValue() == TurnType.TSHL || t.getValue() == TurnType.TSLL
						|| t.getValue() == TurnType.TU || t.getValue() == TurnType.KL) {
					p.then().bearLeft(getSpeakableStreetName(currentSegment, next, false));
				} else if (t.getValue() == TurnType.TR || t.getValue() == TurnType.TSHR || t.getValue() == TurnType.TSLR
						|| t.getValue() == TurnType.TRU || t.getValue() == TurnType.KR) {
					p.then().bearRight(getSpeakableStreetName(currentSegment, next, false));
				}
			}
			if (isPlay) {
				play(p);
			}
		}
	}

	private void playGoAndArriveAtDestination(boolean repeat, NextDirectionInfo nextInfo, RouteSegmentResult currentSegment) {
		RouteDirectionInfo next = nextInfo.directionInfo;
		if (isTargetPoint(nextInfo) && (!playedAndArriveAtTarget || repeat)) {
			if (next.getTurnType().goAhead()) {
				playGoAhead(nextInfo.distanceTo, next, getSpeakableStreetName(currentSegment, next, false));
				playAndArriveAtDestination(nextInfo);
				playedAndArriveAtTarget = true;
			} else if (nextInfo != null &&
					atd.isTurnStateActive(0, nextInfo.distanceTo / 2, STATE_TURN_IN)) {
				playAndArriveAtDestination(nextInfo);
				playedAndArriveAtTarget = true;
			}
		}
	}
	
	private void playAndArriveAtDestination(NextDirectionInfo info) {
		if (isTargetPoint(info)) {
			String pointName = (info == null || info.pointName == null) ? "" : info.pointName;
			CommandBuilder p = getNewCommandPlayerToPlay();
			if (p != null) {
				if (info != null && info.intermediatePoint) {
					p.andArriveAtIntermediatePoint(getSpeakablePointName(pointName));
				} else {
					p.andArriveAtDestination(getSpeakablePointName(pointName));
				}
			}
			play(p);
		}
	}

	private void playMakeTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, NextDirectionInfo nextNextInfo) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			String tParam = getTurnType(next.getTurnType());
			ExitInfo exitInfo = next.getExitInfo();
			boolean isplay = true;
			if (tParam != null) {
				if (exitInfo != null && !Algorithms.isEmpty(exitInfo.getRef()) && settings.SPEAK_EXIT_NUMBER_NAMES.get()) {
					String stringRef = getSpeakableExitRef(exitInfo.getRef());
					p.takeExit(tParam, stringRef, getIntRef(exitInfo.getRef()), getSpeakableExitName(next, exitInfo, !suppressDest));
				} else {
					p.turn(tParam, getSpeakableStreetName(currentSegment, next, !suppressDest));
				}
			} else if (next.getTurnType().isRoundAbout()) {
				p.roundAbout(next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(), getSpeakableStreetName(currentSegment, next, !suppressDest));
			} else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
				p.makeUT(getSpeakableStreetName(currentSegment, next, !suppressDest));
				// Do not announce goAheads
				//} else if (next.getTurnType().getValue() == TurnType.C)) {
				//	play.goAhead();
			} else {
				isplay = false;
			}
			// Add turn after next
			if ((nextNextInfo != null) && (nextNextInfo.directionInfo != null)) {

				// This case only needed should we want a prompt at the end of straight segments (equivalent of makeTurn) when nextNextInfo should be announced again there.
				if (nextNextInfo.directionInfo.getTurnType().getValue() != TurnType.C && next.getTurnType().getValue() == TurnType.C) {
					p.goAhead();
					isplay = true;
				}

				String t2Param = getTurnType(nextNextInfo.directionInfo.getTurnType());
				if (t2Param != null) {
					if (isplay) {
						p.then();
						p.turn(t2Param, nextNextInfo.distanceTo, new StreetName());
					}
				} else if (nextNextInfo.directionInfo.getTurnType().isRoundAbout()) {
					if (isplay) {
						p.then();
						p.roundAbout(nextNextInfo.distanceTo, nextNextInfo.directionInfo.getTurnType().getTurnAngle(), nextNextInfo.directionInfo.getTurnType().getExitOut(), new StreetName());
					}
				} else if (nextNextInfo.directionInfo.getTurnType().getValue() == TurnType.TU) {
					if (isplay) {
						p.then();
						p.makeUT(nextNextInfo.distanceTo, new StreetName());
					}
				}
			}
			if (isplay) {
				play(p);
			}
		}
	}
	
	private String getTurnType(@NonNull TurnType t) {
		if (TurnType.TL == t.getValue()) {
			return CommandPlayer.A_LEFT;
		} else if (TurnType.TSHL == t.getValue()) {
			return CommandPlayer.A_LEFT_SH;
		} else if (TurnType.TSLL == t.getValue()) {
			return CommandPlayer.A_LEFT_SL;
		} else if (TurnType.TR == t.getValue()) {
			return CommandPlayer.A_RIGHT;
		} else if (TurnType.TSHR == t.getValue()) {
			return CommandPlayer.A_RIGHT_SH;
		} else if (TurnType.TSLR == t.getValue()) {
			return CommandPlayer.A_RIGHT_SL;
		} else if (TurnType.KL == t.getValue()) {
			return CommandPlayer.A_LEFT_KEEP;
		} else if (TurnType.KR == t.getValue()) {
			return CommandPlayer.A_RIGHT_KEEP;
		}
		return null;
	}

	public void gpsLocationLost() {
		if (settings.SPEAK_GPS_SIGNAL_STATUS.get()) {
			CommandBuilder p = getNewCommandPlayerToPlay();
			if (p != null) {
				p.gpsLocationLost();
			}
			play(p);
		}
	}

	public void gpsLocationRecover() {
		if (settings.SPEAK_GPS_SIGNAL_STATUS.get()) {
			CommandBuilder p = getNewCommandPlayerToPlay();
			if (p != null) {
				p.gpsLocationRecover();
			}
			play(p);
		}
	}

	public void newRouteIsCalculated(boolean newRoute) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			if (newRoute) {
				p.newRouteCalculated(router.getLeftDistance(), router.getLeftTime());
			} else if (settings.SPEAK_ROUTE_RECALCULATION.get()) {
				p.routeRecalculated(router.getLeftDistance(), router.getLeftTime());
			}
		} else if (player == null && (newRoute || settings.SPEAK_ROUTE_RECALCULATION.get())) {
			pendingCommand = new VoiceCommandPending(!newRoute ? VoiceCommandPending.ROUTE_RECALCULATED : VoiceCommandPending.ROUTE_CALCULATED, this);
		}
		play(p);
		if (newRoute) {
			playGoAheadDist = -1;
			waitAnnouncedOffRoute = 0;
		}
		currentStatus = STATUS_UNKNOWN;
		suppressDest = false;
		nextRouteDirection = null;
	}

	public void arrivedDestinationPoint(String name) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			p.arrivedAtDestination(getSpeakablePointName(name));
		}
		play(p);
	}
	
	public void arrivedIntermediatePoint(String name) {
		CommandBuilder p = getNewCommandPlayerToPlay();
		if (p != null) {
			p.arrivedAtIntermediatePoint(getSpeakablePointName(name));
		}
		play(p);
	}

	// This is not needed, used are only arrivedIntermediatePoint (for points on the route) or announceWaypoint (for points near the route=)
	//public void arrivedWayPoint(String name) {
	//	CommandBuilder p = getNewCommandPlayerToPlay();
	//	if (p != null) {
	//		p.arrivedAtWayPoint(getSpeakablePointName(name));
	//	}
	//  play(p);
	//}

	public void onApplicationTerminate() {
		if (player != null) {
			player.clear();
		}
	}

	public void interruptRouteCommands() {
		if (player != null) {
			player.stop();
		}
	}

	/**
	 * Command to wait until voice player is initialized
	 */
	private class VoiceCommandPending {

		public static final int ROUTE_CALCULATED = 1;
		public static final int ROUTE_RECALCULATED = 2;

		private final int type;
		private final VoiceRouter voiceRouter;

		public VoiceCommandPending(int type, @NonNull VoiceRouter voiceRouter) {
			this.type = type;
			this.voiceRouter = voiceRouter;
		}

		public void play(CommandBuilder newCommand) {
			int left = voiceRouter.router.getLeftDistance();
			int time = voiceRouter.router.getLeftTime();
			if (left > 0) {
				if (type == ROUTE_CALCULATED) {
					newCommand.newRouteCalculated(left, time);
				} else if (type == ROUTE_RECALCULATED) {
					newCommand.routeRecalculated(left, time);
				}
				VoiceRouter.this.play(newCommand);
			}
		}
	}

	private void play(CommandBuilder p) {
		if (p != null) {
			List<String> played = p.play();
			notifyOnVoiceMessage(p.getCommandsList(), played);
		} else {
			notifyOnVoiceMessage(Collections.emptyList(), Collections.emptyList());
		}
	}

	private void makeSound() {
		if (isMute()) {
			return;
		}
		if (soundPool != null && soundClick != -1) {
			soundPool.play(soundClick, 1, 1, 0, 0, 1);
		}
	}

	public void addVoiceMessageListener(@NonNull VoiceMessageListener voiceMessageListener) {
		voiceMessageListeners = Algorithms.updateWeakReferencesList(voiceMessageListeners, voiceMessageListener, true);
	}

	public void removeVoiceMessageListener(@NonNull VoiceMessageListener voiceMessageListener) {
		voiceMessageListeners = Algorithms.updateWeakReferencesList(voiceMessageListeners, voiceMessageListener, false);
	}

	private void notifyOnVoiceMessage(List<String> listCommands, List<String> played) {
		List<WeakReference<VoiceMessageListener>> voiceMessageListeners = this.voiceMessageListeners;
		for (WeakReference<VoiceMessageListener> weakReference : voiceMessageListeners) {
			VoiceMessageListener lnt = weakReference.get();
			if (lnt != null) {
				lnt.onVoiceMessage(listCommands, played);
			}
		}
	}

	private String cutLongDestination(String destination) {
		if (destination == null) {
			return null;
		}
		String[] words = destination.split(",");
		if (words.length > 3) {
			return words[0] + "," + words[1] + "," + words[2];
		}
		return destination;
	}
}
