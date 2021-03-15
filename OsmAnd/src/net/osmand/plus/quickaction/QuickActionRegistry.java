package net.osmand.plus.quickaction;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.actions.DayNightModeAction;
import net.osmand.plus.quickaction.actions.FavoriteAction;
import net.osmand.plus.quickaction.actions.GPXAction;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.quickaction.actions.MarkerAction;
import net.osmand.plus.quickaction.actions.NavAddDestinationAction;
import net.osmand.plus.quickaction.actions.NavAddFirstIntermediateAction;
import net.osmand.plus.quickaction.actions.NavAutoZoomMapAction;
import net.osmand.plus.quickaction.actions.NavDirectionsFromAction;
import net.osmand.plus.quickaction.actions.NavRemoveNextDestination;
import net.osmand.plus.quickaction.actions.NavReplaceDestinationAction;
import net.osmand.plus.quickaction.actions.NavResumePauseAction;
import net.osmand.plus.quickaction.actions.NavStartStopAction;
import net.osmand.plus.quickaction.actions.NavVoiceAction;
import net.osmand.plus.quickaction.actions.ShowHideCoordinatesWidgetAction;
import net.osmand.plus.quickaction.actions.ShowHideFavoritesAction;
import net.osmand.plus.quickaction.actions.ShowHideGpxTracksAction;
import net.osmand.plus.quickaction.actions.ShowHideMapillaryAction;
import net.osmand.plus.quickaction.actions.ShowHidePoiAction;
import net.osmand.plus.quickaction.actions.ShowHideTransportLinesAction;
import net.osmand.plus.quickaction.actions.SwitchProfileAction;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by rosty on 12/27/16.
 */

public class QuickActionRegistry {

	public interface QuickActionUpdatesListener {

		void onActionsUpdated();
	}

	public static final QuickActionType TYPE_ADD_ITEMS = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_create_items).category(QuickActionType.CREATE_CATEGORY);
	public static final QuickActionType TYPE_CONFIGURE_MAP = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_configure_map).category(QuickActionType.CONFIGURE_MAP);
	public static final QuickActionType TYPE_NAVIGATION = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_navigation).category(QuickActionType.NAVIGATION);
	public static final QuickActionType TYPE_CONFIGURE_SCREEN = new QuickActionType(0, "").
			nameRes(R.string.map_widget_config).category(QuickActionType.CONFIGURE_SCREEN);


	private final OsmandSettings settings;

	private List<QuickAction> quickActions;
	private final Gson gson;
	private List<QuickActionType> quickActionTypes = new ArrayList<>();
	private Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();
	private Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();

	private QuickActionUpdatesListener updatesListener;

	public QuickActionRegistry(OsmandSettings settings) {
		this.settings = settings;
		gson = new GsonBuilder().registerTypeAdapter(QuickAction.class, new QuickActionSerializer()).create();
		updateActionTypes();
	}

	public void setUpdatesListener(QuickActionUpdatesListener updatesListener) {
		this.updatesListener = updatesListener;
	}

	public void notifyUpdates() {
		if (updatesListener != null) updatesListener.onActionsUpdated();
	}

	public List<QuickAction> getQuickActions() {
		return new ArrayList<>(quickActions);
	}

	public List<QuickAction> getFilteredQuickActions() {
		return getQuickActions();
	}

	public void addQuickAction(QuickAction action) {
		quickActions.add(action);
		saveActions();
	}

	private void saveActions() {
		Type type = new TypeToken<List<QuickAction>>() {
		}.getType();
		settings.QUICK_ACTION_LIST.set(gson.toJson(quickActions, type));
	}

	public void deleteQuickAction(QuickAction action) {
		quickActions.remove(action);
		saveActions();
	}

	public void updateQuickAction(QuickAction action) {
		int index = quickActions.indexOf(action);
		if (index >= 0) {
			quickActions.set(index, action);
		}
		saveActions();
	}

	public void updateQuickActions(List<QuickAction> quickActions) {
		this.quickActions.clear();
		this.quickActions.addAll(quickActions);
		saveActions();
	}

	public QuickAction getQuickAction(long id) {
		for (QuickAction action : quickActions) {
			if (action.id == id) {
				return action;
			}
		}
		return null;
	}

	public QuickAction getQuickAction(OsmandApplication app, int type, String name, Map<String, String> params) {
		for (QuickAction action : quickActions) {
			if (action.getType() == type
					&& (action.hasCustomName(app) && action.getName(app).equals(name) || !action.hasCustomName(app))
					&& action.getParams().equals(params)) {
				return action;
			}
		}
		return null;
	}

	public boolean isNameUnique(QuickAction action, Context context) {
		for (QuickAction a : quickActions) {
			if (action.id != a.id) {
				if (action.getName(context).equals(a.getName(context))) {
					return false;
				}
			}
		}
		return true;
	}

	public QuickAction generateUniqueName(QuickAction action, Context context) {
		int number = 0;
		String name = action.getName(context);
		while (true) {
			number++;
			action.setName(name + " (" + number + ")");
			if (isNameUnique(action, context)) {
				return action;
			}
		}
	}

	public boolean isQuickActionOn() {
		return settings.QUICK_ACTION.get();
	}

	public void setQuickActionFabState(boolean isOn) {
		settings.QUICK_ACTION.set(isOn);
	}

	private List<QuickAction> parseActiveActionsList(String json) {
		List<QuickAction> resQuickActions;
		if (!Algorithms.isEmpty(json)) {
			Type type = new TypeToken<List<QuickAction>>() {
			}.getType();
			List<QuickAction> quickActions = gson.fromJson(json, type);
			resQuickActions = new ArrayList<>(quickActions.size());
			if (quickActions != null) {
				for (QuickAction qa : quickActions) {
					if (qa != null) {
						resQuickActions.add(qa);
					}
				}
			}
		} else {
			resQuickActions = new ArrayList<>();
		}
		this.quickActions = resQuickActions;
		return resQuickActions;
	}

	public List<QuickActionType> updateActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(FavoriteAction.TYPE);
		quickActionTypes.add(GPXAction.TYPE);
		quickActionTypes.add(MarkerAction.TYPE);
		// configure map
		quickActionTypes.add(ShowHideFavoritesAction.TYPE);
		quickActionTypes.add(ShowHideGpxTracksAction.TYPE);
		quickActionTypes.add(ShowHidePoiAction.TYPE);
		quickActionTypes.add(MapStyleAction.TYPE);
		quickActionTypes.add(DayNightModeAction.TYPE);
		quickActionTypes.add(ShowHideTransportLinesAction.TYPE);
		quickActionTypes.add(ShowHideMapillaryAction.TYPE);
		quickActionTypes.add(ShowHideCoordinatesWidgetAction.TYPE);
		// navigation
		quickActionTypes.add(NavVoiceAction.TYPE);
		quickActionTypes.add(NavDirectionsFromAction.TYPE);
		quickActionTypes.add(NavAddDestinationAction.TYPE);
		quickActionTypes.add(NavAddFirstIntermediateAction.TYPE);
		quickActionTypes.add(NavReplaceDestinationAction.TYPE);
		quickActionTypes.add(NavAutoZoomMapAction.TYPE);
		quickActionTypes.add(NavStartStopAction.TYPE);
		quickActionTypes.add(NavResumePauseAction.TYPE);
		quickActionTypes.add(SwitchProfileAction.TYPE);
		quickActionTypes.add(NavRemoveNextDestination.TYPE);
		OsmandPlugin.registerQuickActionTypesPlugins(quickActionTypes);

		Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();
		Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();
		for (QuickActionType qt : quickActionTypes) {
			quickActionTypesInt.put(qt.getId(), qt);
			quickActionTypesStr.put(qt.getStringId(), qt);
		}
		this.quickActionTypes = quickActionTypes;
		this.quickActionTypesInt = quickActionTypesInt;
		this.quickActionTypesStr = quickActionTypesStr;
		// reparse to get new quick actions
		parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
		return quickActionTypes;
	}

	public List<QuickActionType> produceTypeActionsListWithHeaders() {
		List<QuickActionType> result = new ArrayList<>();
		filterQuickActions(TYPE_ADD_ITEMS, result);
		filterQuickActions(TYPE_CONFIGURE_MAP, result);
		filterQuickActions(TYPE_NAVIGATION, result);
		filterQuickActions(TYPE_CONFIGURE_SCREEN, result);
		return result;
	}

	private void filterQuickActions(QuickActionType filter, List<QuickActionType> result) {
		result.add(filter);
		Set<Integer> set = new TreeSet<>();
		for (QuickAction qa : quickActions) {
			set.add(qa.getActionType().getId());
		}
		for (QuickActionType t : quickActionTypes) {
			if (t.getCategory() == filter.getCategory()) {
				if (!t.isActionEditable()) {
					boolean instanceInList = set.contains(t.getId());
					if (!instanceInList) {
						result.add(t);
					}
				} else {
					result.add(t);
				}
			}
		}
	}

	public QuickAction newActionByStringType(String actionType) {
		QuickActionType quickActionType = quickActionTypesStr.get(actionType);
		if (quickActionType != null) {
			return quickActionType.createNew();
		}
		return null;
	}

	public QuickAction newActionByType(int type) {
		QuickActionType quickActionType = quickActionTypesInt.get(type);
		if (quickActionType != null) {
			return quickActionType.createNew();
		}
		return null;
	}

	public static QuickAction produceAction(QuickAction quickAction) {
		return quickAction.getActionType().createNew(quickAction);
	}

	private class QuickActionSerializer implements JsonDeserializer<QuickAction>,
			JsonSerializer<QuickAction> {

		@Override
		public QuickAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			QuickActionType found = null;
			if (obj.has("actionType")) {
				String actionType = obj.get("actionType").getAsString();
				found = quickActionTypesStr.get(actionType);
			} else if (obj.has("type")) {
				int type = obj.get("type").getAsInt();
				found = quickActionTypesInt.get(type);
			}
			if (found != null) {
				QuickAction qa = found.createNew();
				if (obj.has("name")) {
					qa.setName(obj.get("name").getAsString());
				}
				if (obj.has("id")) {
					qa.setId(obj.get("id").getAsLong());
				}
				if (obj.has("params")) {
					qa.setParams((Map<String, String>) context.deserialize(obj.get("params"),
							new TypeToken<HashMap<String, String>>() {
							}.getType())
					);
				}
				return qa;
			}
			return null;
		}

		@Override
		public JsonElement serialize(QuickAction src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject el = new JsonObject();
			el.addProperty("actionType", src.getActionType().getStringId());
			el.addProperty("id", src.getId());
			if (src.getRawName() != null) {
				el.addProperty("name", src.getRawName());
			}
			el.add("params", context.serialize(src.getParams()));
			return el;
		}
	}
}
