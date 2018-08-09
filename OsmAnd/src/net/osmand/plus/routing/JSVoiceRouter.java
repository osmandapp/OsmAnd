package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.voice.CommandBuilder;
import net.osmand.plus.voice.JSCommandBuilder;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;

import java.util.HashMap;
import java.util.Map;



public class JSVoiceRouter extends VoiceRouter {

    public JSVoiceRouter(RoutingHelper router, OsmandSettings settings) {
        super(router, settings);
    }

    public Map<String, String> getSpeakableJSStreetName(RouteSegmentResult currentSegment, RouteDirectionInfo i, boolean includeDest) {
        Map<String, String> result = new HashMap<>();
        if (i == null || !router.getSettings().SPEAK_STREET_NAMES.get()) {
            return result;
        }
        if (player != null && player.supportsStructuredStreetNames()) {

            // Issue 2377: Play Dest here only if not already previously announced, to avoid repetition
            if (includeDest == true) {
                result.put("toRef", getNonNullString(getSpeakablePointName(i.getRef())));
                result.put("toStreetName", getNonNullString(getSpeakablePointName(i.getStreetName())));
                result.put("toDest", getNonNullString(getSpeakablePointName(i.getDestinationName())));
            } else {
                result.put("toRef", getNonNullString(getSpeakablePointName(i.getRef())));
                result.put("toStreetName", getNonNullString(getSpeakablePointName(i.getStreetName())));
                result.put("toDest", "");
            }
            if (currentSegment != null) {
                // Issue 2377: Play Dest here only if not already previously announced, to avoid repetition
                if (includeDest == true) {
                    RouteDataObject obj = currentSegment.getObject();
                    result.put("fromRef", getNonNullString(getSpeakablePointName(obj.getRef(settings.MAP_PREFERRED_LOCALE.get(),
                            settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection()))));
                    result.put("fromStreetName", getNonNullString(getSpeakablePointName(obj.getName(settings.MAP_PREFERRED_LOCALE.get(),
                            settings.MAP_TRANSLITERATE_NAMES.get()))));
                    result.put("fromDest", getNonNullString(getSpeakablePointName(obj.getDestinationName(settings.MAP_PREFERRED_LOCALE.get(),
                                    settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection()))));
                } else {
                    RouteDataObject obj = currentSegment.getObject();
                    result.put("fromRef", getNonNullString(getSpeakablePointName(obj.getRef(settings.MAP_PREFERRED_LOCALE.get(),
                            settings.MAP_TRANSLITERATE_NAMES.get(), currentSegment.isForwardDirection()))));
                    result.put("fromStreetName", getNonNullString(getSpeakablePointName(obj.getName(settings.MAP_PREFERRED_LOCALE.get(),
                            settings.MAP_TRANSLITERATE_NAMES.get()))));
                    result.put("fromDest", "");
                }
            }

        } else {
            result.put("toRef", getNonNullString(getSpeakablePointName(i.getRef())));
            result.put("toStreetName", getNonNullString(getSpeakablePointName(i.getStreetName())));
            result.put("toDest", "");
        }
        return result;
    }

    private String getNonNullString(String speakablePointName) {
        return  speakablePointName == null ? "" : speakablePointName;
    }

    /**
     * Updates status of voice guidance
     * @param currentLocation
     */
    @Override
    protected void updateStatus(Location currentLocation, boolean repeat) {
        // Directly after turn: goAhead (dist), unless:
        // < PREPARE_LONG_DISTANCE (e.g. 3500m):         playPrepareTurn (-not played any more-)
        // < PREPARE_DISTANCE      (e.g. 1500m):         playPrepareTurn ("Turn after ...")
        // < TURN_IN_DISTANCE      (e.g. 390m or 30sec): playMakeTurnIn  ("Turn in ...")
        // < TURN_DISTANCE         (e.g. 50m or 7sec):   playMakeTurn    ("Turn ...")
        float speed = DEFAULT_SPEED;
        if (currentLocation != null && currentLocation.hasSpeed()) {
            speed = Math.max(currentLocation.getSpeed(), speed);
        }

        RouteCalculationResult.NextDirectionInfo nextInfo = router.getNextRouteDirectionInfo(new RouteCalculationResult.NextDirectionInfo(), true);
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
                playGoAhead(dist, getSpeakableJSStreetName(currentSegment, next, false));
                return;
            } else if (currentStatus == STATUS_TOLD) {
                // nothing said possibly that's wrong case we should say before that
                // however it should be checked manually ?
                return;
            }
        }

        if (currentStatus == STATUS_UNKNOWN) {
            // Play "Continue for ..." if (1) after route calculation no other prompt is due, or (2) after a turn if next turn is more than PREPARE_LONG_DISTANCE away
            if ((playGoAheadDist == -1) || (dist > PREPARE_LONG_DISTANCE)) {
                playGoAheadDist = dist - 3 * TURN_DISTANCE;
            }
        }

        RouteCalculationResult.NextDirectionInfo nextNextInfo = router.getNextRouteDirectionInfoAfter(nextInfo, new RouteCalculationResult.NextDirectionInfo(), true);  //I think "true" is correct here, not "!repeat"
        // Note: getNextRouteDirectionInfoAfter(nextInfo, x, y).distanceTo is distance from nextInfo, not from current position!

        // STATUS_TURN = "Turn (now)"
        if ((repeat || statusNotPassed(STATUS_TURN)) && isDistanceLess(speed, dist, TURN_DISTANCE, TURN_DEFAULT_SPEED)) {
            if (nextNextInfo.distanceTo < TURN_IN_DISTANCE_END && nextNextInfo != null) {
                playMakeTurn(currentSegment, next, nextNextInfo);
            } else {
                playMakeTurn(currentSegment, next, null);
            }
            if (!next.getTurnType().goAhead() && isTargetPoint(nextNextInfo)) {   // !goAhead() avoids isolated "and arrive.." prompt, as goAhead() is not pronounced
                if (nextNextInfo.distanceTo < TURN_IN_DISTANCE_END) {
                    // Issue #2865: Ensure a distance associated with the destination arrival is always announced, either here, or in subsequent "Turn in" prompt
                    // Distance fon non-straights already announced in "Turn (now)"'s nextnext  code above
                    if ((nextNextInfo != null) && (nextNextInfo.directionInfo != null) && nextNextInfo.directionInfo.getTurnType().goAhead()) {
                        playThen();
                        playGoAhead(nextNextInfo.distanceTo, new HashMap<String, String>());
                    }
                    playAndArriveAtDestination(nextNextInfo);
                } else if (nextNextInfo.distanceTo < 1.2f * TURN_IN_DISTANCE_END) {
                    // 1.2 is safety margin should the subsequent "Turn in" prompt not fit in amy more
                    playThen();
                    playGoAhead(nextNextInfo.distanceTo, new HashMap<String, String>());
                    playAndArriveAtDestination(nextNextInfo);
                }
            }
            nextStatusAfter(STATUS_TURN);

            // STATUS_TURN_IN = "Turn in ..."
        } else if ((repeat || statusNotPassed(STATUS_TURN_IN)) && isDistanceLess(speed, dist, TURN_IN_DISTANCE, 0f)) {
            if (repeat || dist >= TURN_IN_DISTANCE_END) {
                if ((isDistanceLess(speed, nextNextInfo.distanceTo, TURN_DISTANCE, 0f) || nextNextInfo.distanceTo < TURN_IN_DISTANCE_END) &&
                        nextNextInfo != null) {
                    playMakeTurnIn(currentSegment, next, dist - (int) btScoDelayDistance, nextNextInfo.directionInfo);
                } else {
                    playMakeTurnIn(currentSegment, next, dist - (int) btScoDelayDistance, null);
                }
                playGoAndArriveAtDestination(repeat, nextInfo, currentSegment);
            }
            nextStatusAfter(STATUS_TURN_IN);

            // STATUS_PREPARE = "Turn after ..."
        } else if ((repeat || statusNotPassed(STATUS_PREPARE)) && (dist <= PREPARE_DISTANCE)) {
            if (repeat || dist >= PREPARE_DISTANCE_END) {
                if (!repeat && (next.getTurnType().keepLeft() || next.getTurnType().keepRight())) {
                    // Do not play prepare for keep left/right
                } else {
                    playPrepareTurn(currentSegment, next, dist);
                    playGoAndArriveAtDestination(repeat, nextInfo, currentSegment);
                }
            }
            nextStatusAfter(STATUS_PREPARE);

            // STATUS_LONG_PREPARE =  also "Turn after ...", we skip this now, users said this is obsolete
        } else if ((repeat || statusNotPassed(STATUS_LONG_PREPARE)) && (dist <= PREPARE_LONG_DISTANCE)) {
            if (repeat || dist >= PREPARE_LONG_DISTANCE_END) {
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
            playGoAhead(dist, getSpeakableJSStreetName(currentSegment, next, false));
        }
    }

    private void playPrepareTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist) {
        JSCommandBuilder play = (JSCommandBuilder) getNewCommandPlayerToPlay();
        if (play != null) {
            String tParam = getTurnType(next.getTurnType());
            if (tParam != null) {
                notifyOnVoiceMessage();
                play.prepareTurn(tParam, dist, getSpeakableJSStreetName(currentSegment, next, true)).play();
            } else if (next.getTurnType().isRoundAbout()) {
                notifyOnVoiceMessage();
                play.prepareRoundAbout(dist, next.getTurnType().getExitOut(), getSpeakableJSStreetName(currentSegment, next, true)).play();
            } else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
                notifyOnVoiceMessage();
                play.prepareMakeUT(dist, getSpeakableJSStreetName(currentSegment, next, true)).play();
            }
        }
    }

    private void playMakeTurnIn(RouteSegmentResult currentSegment, RouteDirectionInfo next, int dist, RouteDirectionInfo pronounceNextNext) {
        JSCommandBuilder play = (JSCommandBuilder) getNewCommandPlayerToPlay();
        if (play != null) {
            String tParam = getTurnType(next.getTurnType());
            boolean isPlay = true;
            if (tParam != null) {
                play.turn(tParam, dist, getSpeakableJSStreetName(currentSegment, next, true));
                suppressDest = true;
            } else if (next.getTurnType().isRoundAbout()) {
                play.roundAbout(dist, next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(), getSpeakableJSStreetName(currentSegment, next, true));
                // Other than in prepareTurn, in prepareRoundabout we do not announce destination, so we can repeat it one more time
                suppressDest = false;
            } else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
                play.makeUT(dist, getSpeakableJSStreetName(currentSegment, next, true));
                suppressDest = true;
            } else {
                isPlay = false;
            }
            // 'then keep' preparation for next after next. (Also announces an interim straight segment, which is not pronounced above.)
            if (pronounceNextNext != null) {
                TurnType t = pronounceNextNext.getTurnType();
                isPlay = true;
                if (t.getValue() != TurnType.C && next.getTurnType().getValue() == TurnType.C) {
                    play.goAhead(dist, getSpeakableJSStreetName(currentSegment, next, true));
                }
                if (t.getValue() == TurnType.TL || t.getValue() == TurnType.TSHL || t.getValue() == TurnType.TSLL
                        || t.getValue() == TurnType.TU || t.getValue() == TurnType.KL ) {
                    play.then().bearLeft(getSpeakableJSStreetName(currentSegment, next, false));
                } else if (t.getValue() == TurnType.TR || t.getValue() == TurnType.TSHR || t.getValue() == TurnType.TSLR
                        || t.getValue() == TurnType.TRU || t.getValue() == TurnType.KR) {
                    play.then().bearRight(getSpeakableJSStreetName(currentSegment, next, false));
                }
            }
            if (isPlay) {
                notifyOnVoiceMessage();
                play.play();
            }
        }
    }

    private void playGoAhead(int dist, Map<String, String> streetName) {
        CommandBuilder play = getNewCommandPlayerToPlay();
        JSCommandBuilder playJs = (JSCommandBuilder) play;
        if (play != null) {
            notifyOnVoiceMessage();
            playJs.goAhead(dist, streetName).play();
        }
    }

    private void playMakeTurn(RouteSegmentResult currentSegment, RouteDirectionInfo next, RouteCalculationResult.NextDirectionInfo nextNextInfo) {
        JSCommandBuilder play = (JSCommandBuilder) getNewCommandPlayerToPlay();
        if (play != null) {
            String tParam = getTurnType(next.getTurnType());
            boolean isplay = true;
            if (tParam != null) {
                play.turn(tParam, getSpeakableJSStreetName(currentSegment, next, !suppressDest));
            } else if (next.getTurnType().isRoundAbout()) {
                play.roundAbout(next.getTurnType().getTurnAngle(), next.getTurnType().getExitOut(), getSpeakableJSStreetName(currentSegment, next, !suppressDest));
            } else if (next.getTurnType().getValue() == TurnType.TU || next.getTurnType().getValue() == TurnType.TRU) {
                play.makeUT(getSpeakableJSStreetName(currentSegment, next, !suppressDest));
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
                    play.goAhead();
                    isplay = true;
                }

                String t2Param = getTurnType(nextNextInfo.directionInfo.getTurnType());
                if (t2Param != null) {
                    if (isplay) {
                        play.then();
                        play.turn(t2Param, nextNextInfo.distanceTo, new HashMap<String, String>());
                    }
                } else if (nextNextInfo.directionInfo.getTurnType().isRoundAbout()) {
                    if (isplay) {
                        play.then();
                        play.roundAbout(nextNextInfo.distanceTo, nextNextInfo.directionInfo.getTurnType().getTurnAngle(),
                                nextNextInfo.directionInfo.getTurnType().getExitOut(), new HashMap<String, String>());
                    }
                } else if (nextNextInfo.directionInfo.getTurnType().getValue() == TurnType.TU) {
                    if (isplay) {
                        play.then();
                        play.makeUT(nextNextInfo.distanceTo, new HashMap<String, String>());
                    }
                }
            }
            if (isplay) {
                notifyOnVoiceMessage();
                play.play();
            }
        }
    }

    private void playGoAndArriveAtDestination(boolean repeat, RouteCalculationResult.NextDirectionInfo nextInfo, RouteSegmentResult currentSegment) {
        RouteDirectionInfo next = nextInfo.directionInfo;
        if (isTargetPoint(nextInfo) && (!playedAndArriveAtTarget || repeat)) {
            if (next.getTurnType().goAhead()) {
                playGoAhead(nextInfo.distanceTo, getSpeakableJSStreetName(currentSegment, next, false));
                playAndArriveAtDestination(nextInfo);
                playedAndArriveAtTarget = true;
            } else if (nextInfo.distanceTo <= 2 * TURN_IN_DISTANCE) {
                playAndArriveAtDestination(nextInfo);
                playedAndArriveAtTarget = true;
            }
        }
    }

}
