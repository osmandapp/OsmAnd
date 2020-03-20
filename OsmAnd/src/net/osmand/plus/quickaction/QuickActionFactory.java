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
		quickActionsTypes.add(NewAction.TYPE);
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
		for(QuickActionType t : getActionTypes()) {
			if(t.getId() == type) {
				return t.createNew();
			}
		}
		return new QuickAction();
	}

	public static QuickAction produceAction(QuickAction quickAction) {
		return quickAction.getActionType().createNew(quickAction);
	}

	public static @DrawableRes int getActionIcon(int type) {

		switch (type) {

			case ShowHideFavoritesAction.TYPE:
				return R.drawable.ic_action_fav_dark;

			case ShowHidePoiAction.TYPE:
				return R.drawable.ic_action_gabout_dark;

			case ParkingAction.TYPE:
				return R.drawable.ic_action_parking_dark;

			case TakeAudioNoteAction.TYPE:
				return R.drawable.ic_action_micro_dark;

			case TakePhotoNoteAction.TYPE:
				return R.drawable.ic_action_photo_dark;

			case TakeVideoNoteAction.TYPE:
				return R.drawable.ic_action_video_dark;

			case ShowHideOSMBugAction.TYPE:
				return R.drawable.ic_action_bug_dark;


			case MapStyleAction.TYPE:
				return R.drawable.ic_map;

			case MapSourceAction.TYPE:
				return R.drawable.ic_world_globe_dark;

			case MapOverlayAction.TYPE:
				return R.drawable.ic_layer_top;

			case MapUnderlayAction.TYPE:
				return R.drawable.ic_layer_bottom;

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

			case ShowHideFavoritesAction.TYPE:
				return R.string.quick_action_showhide_favorites_title;

			case ShowHidePoiAction.TYPE:
				return R.string.quick_action_showhide_poi_title;

			case ParkingAction.TYPE:
				return R.string.quick_action_add_parking;

			case TakeAudioNoteAction.TYPE:
				return R.string.quick_action_take_audio_note;

			case TakePhotoNoteAction.TYPE:
				return R.string.quick_action_take_photo_note;

			case TakeVideoNoteAction.TYPE:
				return R.string.quick_action_take_video_note;

			case ShowHideOSMBugAction.TYPE:
				return R.string.quick_action_showhide_osmbugs_title;


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
			case ShowHideFavoritesAction.TYPE:
			case ShowHidePoiAction.TYPE:
			case ParkingAction.TYPE:
			case TakeAudioNoteAction.TYPE:
			case TakePhotoNoteAction.TYPE:
			case TakeVideoNoteAction.TYPE:
			case ShowHideOSMBugAction.TYPE:
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
