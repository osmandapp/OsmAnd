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
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(NewAction.TYPE);
		quickActionTypes.add(FavoriteAction.TYPE);
		quickActionTypes.add(GPXAction.TYPE);
		quickActionTypes.add(MarkerAction.TYPE);
		// configure map
		quickActionTypes.add(ShowHideFavoritesAction.TYPE);
		quickActionTypes.add(ShowHideGpxTracksAction.TYPE);
		quickActionTypes.add(ShowHidePoiAction.TYPE);
		quickActionTypes.add(MapStyleAction.TYPE);
		quickActionTypes.add(DayNightModeAction.TYPE);
		// navigation
		quickActionTypes.add(NavVoiceAction.TYPE);
		quickActionTypes.add(NavDirectionsFromAction.TYPE);
		quickActionTypes.add(NavAddDestinationAction.TYPE);
		quickActionTypes.add(NavAddFirstIntermediateAction.TYPE);
		quickActionTypes.add(NavReplaceDestinationAction.TYPE);
		quickActionTypes.add(NavAutoZoomMapAction.TYPE);
		quickActionTypes.add(NavStartStopAction.TYPE);
		quickActionTypes.add(NavResumePauseAction.TYPE);
		OsmandPlugin.registerQuickActionTypesPlugins(quickActionTypes);
		return quickActionTypes;
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

}
