package net.osmand.plus.quickaction;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.TakeAudioNoteAction;
import net.osmand.plus.audionotes.TakePhotoNoteAction;
import net.osmand.plus.audionotes.TakeVideoNoteAction;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingAction;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.quickaction.actions.AddOSMBugAction;
import net.osmand.plus.quickaction.actions.AddPOIAction;
import net.osmand.plus.quickaction.actions.ContourLinesAction;
import net.osmand.plus.quickaction.actions.DayNightModeAction;
import net.osmand.plus.quickaction.actions.FavoriteAction;
import net.osmand.plus.quickaction.actions.GPXAction;
import net.osmand.plus.quickaction.actions.HillshadeAction;
import net.osmand.plus.quickaction.actions.MapOverlayAction;
import net.osmand.plus.quickaction.actions.MapSourceAction;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.quickaction.actions.MapUnderlayAction;
import net.osmand.plus.quickaction.actions.MarkerAction;
import net.osmand.plus.quickaction.actions.NavAddDestinationAction;
import net.osmand.plus.quickaction.actions.NavAddFirstIntermediateAction;
import net.osmand.plus.quickaction.actions.NavAutoZoomMapAction;
import net.osmand.plus.quickaction.actions.NavDirectionsFromAction;
import net.osmand.plus.quickaction.actions.NavReplaceDestinationAction;
import net.osmand.plus.quickaction.actions.NavResumePauseAction;
import net.osmand.plus.quickaction.actions.NavStartStopAction;
import net.osmand.plus.quickaction.actions.NavVoiceAction;
import net.osmand.plus.quickaction.actions.NewAction;
import net.osmand.plus.quickaction.actions.ShowHideFavoritesAction;
import net.osmand.plus.quickaction.actions.ShowHideGpxTracksAction;
import net.osmand.plus.quickaction.actions.ShowHideOSMBugAction;
import net.osmand.plus.quickaction.actions.ShowHidePoiAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuickActionFactory {

	public String quickActionListToString(List<QuickAction> quickActions) {
		return new Gson().toJson(quickActions);
	}

	public List<QuickAction> parseActiveActionsList(String json) {
		Type type = new TypeToken<List<QuickAction>>() {
		}.getType();
		ArrayList<QuickAction> quickActions = new Gson().fromJson(json, type);
		return quickActions != null ? quickActions : new ArrayList<QuickAction>();
	}

	public static List<QuickAction> produceTypeActionsListWithHeaders(List<QuickAction> active) {
		ArrayList<QuickAction> quickActions = new ArrayList<>();
		quickActions.add(new QuickAction(0, R.string.quick_action_add_create_items));
		quickActions.add(new FavoriteAction());
		quickActions.add(new GPXAction());
		QuickAction marker = new MarkerAction();

		if (!marker.hasInstanceInList(active)) {
			quickActions.add(marker);
		}

		if (OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class) != null) {
			QuickAction audio = new TakeAudioNoteAction();
			QuickAction photo = new TakePhotoNoteAction();
			QuickAction video = new TakeVideoNoteAction();

			if (!audio.hasInstanceInList(active)) {
				quickActions.add(audio);
			}

			if (!photo.hasInstanceInList(active)) {
				quickActions.add(photo);
			}

			if (!video.hasInstanceInList(active)) {
				quickActions.add(video);
			}
		}

		if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {
			quickActions.add(new AddPOIAction());
			quickActions.add(new AddOSMBugAction());
		}

		if (OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class) != null) {
			QuickAction parking = new ParkingAction();
			if (!parking.hasInstanceInList(active)) {
				quickActions.add(parking);
			}
		}

		quickActions.add(new QuickAction(0, R.string.quick_action_add_configure_map));

		QuickAction favorites = new ShowHideFavoritesAction();
		if (!favorites.hasInstanceInList(active)) {
			quickActions.add(favorites);
		}

		quickActions.add(new ShowHideGpxTracksAction());

		quickActions.add(new ShowHidePoiAction());
		if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {
			QuickAction showHideOSMBugAction = new ShowHideOSMBugAction();
			if (!showHideOSMBugAction.hasInstanceInList(active)) {
				quickActions.add(showHideOSMBugAction);
			}
		}

		quickActions.add(new MapStyleAction());
		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null) {
			quickActions.add(new MapSourceAction());
			quickActions.add(new MapOverlayAction());
			quickActions.add(new MapUnderlayAction());
		}

		if (OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			quickActions.add(new ContourLinesAction());
			quickActions.add(new HillshadeAction());
		}

		quickActions.add(new DayNightModeAction());


		QuickAction voice = new NavVoiceAction();
		QuickAction directionFrom = new NavDirectionsFromAction();
		QuickAction addDestination = new NavAddDestinationAction();
		QuickAction addFirstIntermediate = new NavAddFirstIntermediateAction();
		QuickAction replaceDestination = new NavReplaceDestinationAction();
		QuickAction autoZoomMap = new NavAutoZoomMapAction();
		QuickAction startStopNavigation = new NavStartStopAction();
		QuickAction resumePauseNavigation = new NavResumePauseAction();

		ArrayList<QuickAction> navigationQuickActions = new ArrayList<>();

		if (!voice.hasInstanceInList(active)) {
			navigationQuickActions.add(voice);
		}
		if (!directionFrom.hasInstanceInList(active)) {
			navigationQuickActions.add(directionFrom);
		}
		if (!addDestination.hasInstanceInList(active)) {
			navigationQuickActions.add(addDestination);
		}
		if (!addFirstIntermediate.hasInstanceInList(active)) {
			navigationQuickActions.add(addFirstIntermediate);
		}
		if (!replaceDestination.hasInstanceInList(active)) {
			navigationQuickActions.add(replaceDestination);
		}
		if (!autoZoomMap.hasInstanceInList(active)) {
			navigationQuickActions.add(autoZoomMap);
		}
		if (!startStopNavigation.hasInstanceInList(active)) {
			navigationQuickActions.add(startStopNavigation);
		}
		if (!resumePauseNavigation.hasInstanceInList(active)) {
			navigationQuickActions.add(resumePauseNavigation);
		}

		if (navigationQuickActions.size() > 0) {
			quickActions.add(new QuickAction(0, R.string.quick_action_add_navigation));
			quickActions.addAll(navigationQuickActions);
		}

		return quickActions;
	}

	public static QuickAction newActionByType(int type) {

		switch (type) {

			case NewAction.TYPE:
				return new NewAction();

			case MarkerAction.TYPE:
				return new MarkerAction();

			case FavoriteAction.TYPE:
				return new FavoriteAction();

			case ShowHideFavoritesAction.TYPE:
				return new ShowHideFavoritesAction();

			case ShowHidePoiAction.TYPE:
				return new ShowHidePoiAction();

			case GPXAction.TYPE:
				return new GPXAction();

			case ParkingAction.TYPE:
				return new ParkingAction();

			case TakeAudioNoteAction.TYPE:
				return new TakeAudioNoteAction();

			case TakePhotoNoteAction.TYPE:
				return new TakePhotoNoteAction();

			case TakeVideoNoteAction.TYPE:
				return new TakeVideoNoteAction();

			case NavVoiceAction.TYPE:
				return new NavVoiceAction();

			case ShowHideOSMBugAction.TYPE:
				return new ShowHideOSMBugAction();

			case AddOSMBugAction.TYPE:
				return new AddOSMBugAction();

			case AddPOIAction.TYPE:
				return new AddPOIAction();

			case MapStyleAction.TYPE:
				return new MapStyleAction();

			case MapSourceAction.TYPE:
				return new MapSourceAction();

			case MapOverlayAction.TYPE:
				return new MapOverlayAction();

			case MapUnderlayAction.TYPE:
				return new MapUnderlayAction();

			case NavDirectionsFromAction.TYPE:
				return new NavDirectionsFromAction();

			case NavAddDestinationAction.TYPE:
				return new NavAddDestinationAction();

			case NavAddFirstIntermediateAction.TYPE:
				return new NavAddFirstIntermediateAction();

			case NavReplaceDestinationAction.TYPE:
				return new NavReplaceDestinationAction();

			case NavAutoZoomMapAction.TYPE:
				return new NavAutoZoomMapAction();

			case NavStartStopAction.TYPE:
				return new NavStartStopAction();

			case NavResumePauseAction.TYPE:
				return new NavResumePauseAction();

			case DayNightModeAction.TYPE:
				return new DayNightModeAction();

			case ShowHideGpxTracksAction.TYPE:
				return new ShowHideGpxTracksAction();

			case ContourLinesAction.TYPE:
				return new ContourLinesAction();

			case HillshadeAction.TYPE:
				return new HillshadeAction();

			default:
				return new QuickAction();
		}
	}

	public static QuickAction produceAction(QuickAction quickAction) {

		switch (quickAction.type) {

			case NewAction.TYPE:
				return new NewAction(quickAction);

			case MarkerAction.TYPE:
				return new MarkerAction(quickAction);

			case FavoriteAction.TYPE:
				return new FavoriteAction(quickAction);

			case ShowHideFavoritesAction.TYPE:
				return new ShowHideFavoritesAction(quickAction);

			case ShowHidePoiAction.TYPE:
				return new ShowHidePoiAction(quickAction);

			case GPXAction.TYPE:
				return new GPXAction(quickAction);

			case ParkingAction.TYPE:
				return new ParkingAction(quickAction);

			case TakeAudioNoteAction.TYPE:
				return new TakeAudioNoteAction(quickAction);

			case TakePhotoNoteAction.TYPE:
				return new TakePhotoNoteAction(quickAction);

			case TakeVideoNoteAction.TYPE:
				return new TakeVideoNoteAction(quickAction);

			case NavVoiceAction.TYPE:
				return new NavVoiceAction(quickAction);

			case ShowHideOSMBugAction.TYPE:
				return new ShowHideOSMBugAction(quickAction);

			case AddOSMBugAction.TYPE:
				return new AddOSMBugAction(quickAction);

			case AddPOIAction.TYPE:
				return new AddPOIAction(quickAction);

			case MapStyleAction.TYPE:
				return new MapStyleAction(quickAction);

			case MapSourceAction.TYPE:
				return new MapSourceAction(quickAction);

			case MapOverlayAction.TYPE:
				return new MapOverlayAction(quickAction);

			case MapUnderlayAction.TYPE:
				return new MapUnderlayAction(quickAction);

			case NavDirectionsFromAction.TYPE:
				return new NavDirectionsFromAction(quickAction);

			case NavAddDestinationAction.TYPE:
				return new NavAddDestinationAction(quickAction);

			case NavAddFirstIntermediateAction.TYPE:
				return new NavAddFirstIntermediateAction(quickAction);

			case NavReplaceDestinationAction.TYPE:
				return new NavReplaceDestinationAction(quickAction);

			case NavAutoZoomMapAction.TYPE:
				return new NavAutoZoomMapAction(quickAction);

			case NavStartStopAction.TYPE:
				return new NavStartStopAction(quickAction);

			case NavResumePauseAction.TYPE:
				return new NavResumePauseAction(quickAction);

			case DayNightModeAction.TYPE:
				return new DayNightModeAction(quickAction);

			case ShowHideGpxTracksAction.TYPE:
				return new ShowHideGpxTracksAction(quickAction);

			case ContourLinesAction.TYPE:
				return new ContourLinesAction(quickAction);

			case HillshadeAction.TYPE:
				return new HillshadeAction(quickAction);

			default:
				return quickAction;
		}
	}

	public static @DrawableRes int getActionIcon(int type) {

		switch (type) {

			case NewAction.TYPE:
				return R.drawable.ic_action_plus;

			case MarkerAction.TYPE:
				return R.drawable.ic_action_flag_dark;

			case FavoriteAction.TYPE:
				return R.drawable.ic_action_fav_dark;

			case ShowHideFavoritesAction.TYPE:
				return R.drawable.ic_action_fav_dark;

			case ShowHidePoiAction.TYPE:
				return R.drawable.ic_action_gabout_dark;

			case GPXAction.TYPE:
				return R.drawable.ic_action_flag_dark;

			case ParkingAction.TYPE:
				return R.drawable.ic_action_parking_dark;

			case TakeAudioNoteAction.TYPE:
				return R.drawable.ic_action_micro_dark;

			case TakePhotoNoteAction.TYPE:
				return R.drawable.ic_action_photo_dark;

			case TakeVideoNoteAction.TYPE:
				return R.drawable.ic_action_video_dark;

			case NavVoiceAction.TYPE:
				return R.drawable.ic_action_volume_up;

			case ShowHideOSMBugAction.TYPE:
				return R.drawable.ic_action_bug_dark;

			case AddOSMBugAction.TYPE:
				return R.drawable.ic_action_bug_dark;

			case AddPOIAction.TYPE:
				return R.drawable.ic_action_gabout_dark;

			case MapStyleAction.TYPE:
				return R.drawable.ic_map;

			case MapSourceAction.TYPE:
				return R.drawable.ic_world_globe_dark;

			case MapOverlayAction.TYPE:
				return R.drawable.ic_layer_top_dark;

			case MapUnderlayAction.TYPE:
				return R.drawable.ic_layer_bottom_dark;

			case NavDirectionsFromAction.TYPE:
				return R.drawable.ic_action_route_direction_from_here;

			case NavAddDestinationAction.TYPE:
				return R.drawable.ic_action_point_add_destination;

			case NavAddFirstIntermediateAction.TYPE:
				return R.drawable.ic_action_intermediate;

			case NavReplaceDestinationAction.TYPE:
				return R.drawable.ic_action_point_add_destination;

			case NavAutoZoomMapAction.TYPE:
				return R.drawable.ic_action_search_dark;

			case NavStartStopAction.TYPE:
				return R.drawable.ic_action_start_navigation;

			case NavResumePauseAction.TYPE:
				return R.drawable.ic_play_dark;

			case DayNightModeAction.TYPE:
				return R.drawable.ic_action_map_day;

			case ShowHideGpxTracksAction.TYPE:
				return R.drawable.ic_gpx_track;

			case ContourLinesAction.TYPE:
				return R.drawable.ic_plugin_srtm;

			case HillshadeAction.TYPE:
				return R.drawable.ic_action_hillshade_dark;

			default:
				return R.drawable.ic_action_plus;
		}
	}

	public static @StringRes int getActionName(int type) {

		switch (type) {

			case NewAction.TYPE:
				return R.string.quick_action_new_action;

			case MarkerAction.TYPE:
				return R.string.quick_action_add_marker;

			case FavoriteAction.TYPE:
				return R.string.quick_action_add_favorite;

			case ShowHideFavoritesAction.TYPE:
				return R.string.quick_action_showhide_favorites_title;

			case ShowHidePoiAction.TYPE:
				return R.string.quick_action_showhide_poi_title;

			case GPXAction.TYPE:
				return R.string.quick_action_add_gpx;

			case ParkingAction.TYPE:
				return R.string.quick_action_add_parking;

			case TakeAudioNoteAction.TYPE:
				return R.string.quick_action_take_audio_note;

			case TakePhotoNoteAction.TYPE:
				return R.string.quick_action_take_photo_note;

			case TakeVideoNoteAction.TYPE:
				return R.string.quick_action_take_video_note;

			case NavVoiceAction.TYPE:
				return R.string.quick_action_navigation_voice;

			case ShowHideOSMBugAction.TYPE:
				return R.string.quick_action_showhide_osmbugs_title;

			case AddOSMBugAction.TYPE:
				return R.string.quick_action_add_osm_bug;

			case AddPOIAction.TYPE:
				return R.string.quick_action_add_poi;

			case MapStyleAction.TYPE:
				return R.string.quick_action_map_style;

			case MapSourceAction.TYPE:
				return R.string.quick_action_map_source;

			case MapOverlayAction.TYPE:
				return R.string.quick_action_map_overlay;

			case MapUnderlayAction.TYPE:
				return R.string.quick_action_map_underlay;

			case DayNightModeAction.TYPE:
				return R.string.quick_action_day_night_switch_mode;

			case NavDirectionsFromAction.TYPE:
				return R.string.context_menu_item_directions_from;

			case NavAddDestinationAction.TYPE:
				return R.string.quick_action_add_destination;

			case NavAddFirstIntermediateAction.TYPE:
				return R.string.quick_action_add_first_intermediate;

			case NavReplaceDestinationAction.TYPE:
				return R.string.quick_action_replace_destination;

			case NavAutoZoomMapAction.TYPE:
				return R.string.quick_action_auto_zoom;

			case NavStartStopAction.TYPE:
				return R.string.quick_action_start_stop_navigation;

			case NavResumePauseAction.TYPE:
				return R.string.quick_action_resume_pause_navigation;

			case ShowHideGpxTracksAction.TYPE:
				return R.string.quick_action_show_hide_gpx_tracks;

			case ContourLinesAction.TYPE:
				return R.string.quick_action_show_hide_contour_lines;

			case HillshadeAction.TYPE:
				return R.string.quick_action_show_hide_hillshade;

			default:
				return R.string.quick_action_new_action;
		}
	}

	public static boolean isActionEditable(int type) {

		switch (type) {

			case NewAction.TYPE:
			case MarkerAction.TYPE:
			case ShowHideFavoritesAction.TYPE:
			case ShowHidePoiAction.TYPE:
			case ParkingAction.TYPE:
			case TakeAudioNoteAction.TYPE:
			case TakePhotoNoteAction.TYPE:
			case TakeVideoNoteAction.TYPE:
			case NavVoiceAction.TYPE:
			case NavDirectionsFromAction.TYPE:
			case NavAddDestinationAction.TYPE:
			case NavAddFirstIntermediateAction.TYPE:
			case NavReplaceDestinationAction.TYPE:
			case NavAutoZoomMapAction.TYPE:
			case ShowHideOSMBugAction.TYPE:
			case NavStartStopAction.TYPE:
			case NavResumePauseAction.TYPE:
			case DayNightModeAction.TYPE:
			case ShowHideGpxTracksAction.TYPE:
			case ContourLinesAction.TYPE:
			case HillshadeAction.TYPE:
				return false;

			default:
				return true;
		}
	}
}
