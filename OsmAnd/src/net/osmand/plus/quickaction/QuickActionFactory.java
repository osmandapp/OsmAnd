package net.osmand.plus.quickaction;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

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
import net.osmand.util.MapUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuickActionFactory {

	public static final QuickActionType TYPE_ADD_ITEMS = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_create_items).category(QuickActionType.CREATE_CATEGORY);
	public static final QuickActionType TYPE_CONFIGURE_MAP = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_configure_map).category(QuickActionType.CONFIGURE_MAP);
	public static final QuickActionType TYPE_NAVIGATION = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_navigation).category(QuickActionType.NAVIGATION);
	)
	public String quickActionListToString(List<QuickAction> quickActions) {
		return new Gson().toJson(quickActions);
	}

	public List<QuickAction> parseActiveActionsList(String json) {
		Type type = new TypeToken<List<QuickAction>>() {
		}.getType();
		ArrayList<QuickAction> quickActions = new Gson().fromJson(json, type);
		return quickActions != null ? quickActions : new ArrayList<QuickAction>();
	}


	public static List<QuickActionType> getActionTypes() {
		List<QuickActionType> quickActionsTypes = new ArrayList<>();
		quickActionsTypes.add(FavoriteAction.TYPE);
		quickActionsTypes.add(GPXAction.TYPE);
		quickActionsTypes.add(MarkerAction.TYPE);

		// FIXME plugins

		if (OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class) != null) {
			quickActionsTypes.add(TakeAudioNoteAction.TYPE);
			quickActionsTypes.add(TakePhotoNoteAction.TYPE);
			quickActionsTypes.add(TakeVideoNoteAction.TYPE);
		}

		if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {
			quickActionsTypes.add(AddPOIAction.TYPE);
			quickActionsTypes.add(AddOSMBugAction.TYPE);
		}

		if (OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class) != null) {
			quickActionsTypes.add(ParkingAction.TYPE);
		}
		// configure map
		quickActionsTypes.add(ShowHideFavoritesAction.TYPE);
		quickActionsTypes.add(ShowHideGpxTracksAction.TYPE);
		quickActionsTypes.add(ShowHidePoiAction.TYPE);

		if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {
			quickActionsTypes.add(ShowHideOSMBugAction.TYPE);
		}

		quickActionsTypes.add(MapStyleAction.TYPE);
		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null) {
			quickActionsTypes.add(MapSourceAction.TYPE);
			quickActionsTypes.add(MapOverlayAction.TYPE);
			quickActionsTypes.add(MapUnderlayAction.TYPE);
		}


		if (OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			quickActionsTypes.add(ContourLinesAction.TYPE);
			quickActionsTypes.add(HillshadeAction.TYPE);
		}

		quickActionsTypes.add(DayNightModeAction.TYPE);

		// navigation
		quickActionsTypes.add(NavVoiceAction.TYPE);
		quickActionsTypes.add(NavDirectionsFromAction.TYPE);
		quickActionsTypes.add(NavAddDestinationAction.TYPE);
		quickActionsTypes.add(NavAddFirstIntermediateAction.TYPE);
		quickActionsTypes.add(NavReplaceDestinationAction.TYPE);
		quickActionsTypes.add(NavAutoZoomMapAction.TYPE);
		quickActionsTypes.add(NavStartStopAction.TYPE);
		quickActionsTypes.add(NavResumePauseAction.TYPE);
		return quickActionsTypes;
	}

	public static List<QuickActionType> produceTypeActionsListWithHeaders(List<QuickAction> active) {
		List<QuickActionType> quickActions = new ArrayList<>();
		List<QuickActionType> tps = getActionTypes();
		filterQuickActions(active, tps, TYPE_ADD_ITEMS, quickActions);
		filterQuickActions(active, tps, TYPE_CONFIGURE_MAP, quickActions);
		filterQuickActions(active, tps, TYPE_NAVIGATION, quickActions);
		return quickActions;
	}

	private static void filterQuickActions(List<QuickAction> active, List<QuickActionType> allTypes, QuickActionType filter,
										   List<QuickActionType> result) {
		result.add(filter);
		for(QuickActionType t : allTypes) {
			if(t.getCategory() == filter.getCategory()) {
				if(t.isActionEditable()) {
					boolean instanceInList = false;
					for(QuickAction qa : active) {
						if(qa.getActionType().getId() == t.getId()) {
							instanceInList = true;
							break;
						}
					}
					if (!instanceInList) {
						result.add(t);
					}
				} else {
					result.add(t);
				}
			}
		}
	}

	public static QuickAction newActionByType(int type) {
		// FIXME
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
		// FIXME
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

			case AddPOIAction.TYPE:
				return R.drawable.ic_action_gabout_dark;

			case MapStyleAction.TYPE:
				return R.drawable.ic_map;

			case MapSourceAction.TYPE:
				return R.drawable.ic_world_globe_dark;

			case MapOverlayAction.TYPE:
				return R.drawable.ic_layer_top;

			case MapUnderlayAction.TYPE:
				return R.drawable.ic_layer_bottom;

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
