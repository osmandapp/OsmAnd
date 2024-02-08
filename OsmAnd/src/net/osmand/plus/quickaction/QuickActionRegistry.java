package net.osmand.plus.quickaction;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.ShowHideMapillaryAction;
import net.osmand.plus.quickaction.actions.*;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
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
			nameRes(R.string.shared_string_navigation).category(QuickActionType.NAVIGATION);
	public static final QuickActionType TYPE_CONFIGURE_SCREEN = new QuickActionType(0, "").
			nameRes(R.string.map_widget_config).category(QuickActionType.CONFIGURE_SCREEN);
	public static final QuickActionType TYPE_SETTINGS = new QuickActionType(0, "").
			nameRes(R.string.shared_string_settings).category(QuickActionType.SETTINGS);
	public static final QuickActionType TYPE_OPEN = new QuickActionType(0, "").
			nameRes(R.string.shared_string_open).category(QuickActionType.OPEN);


	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final QuickActionSerializer serializer = new QuickActionSerializer();
	private final Gson gson = new GsonBuilder().registerTypeAdapter(QuickAction.class, serializer).create();

	private List<QuickAction> quickActions;
	private List<QuickActionType> enabledTypes = new ArrayList<>();
	private Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();
	private Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();
	private Set<QuickActionUpdatesListener> updatesListeners = new HashSet<>();

	public QuickActionRegistry(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		updateActionTypes();
	}

	public void addUpdatesListener(@NonNull QuickActionUpdatesListener listener) {
		Set<QuickActionUpdatesListener> updatesListeners = new HashSet<>(this.updatesListeners);
		updatesListeners.add(listener);
		this.updatesListeners = updatesListeners;
	}

	public void removeUpdatesListener(@NonNull QuickActionUpdatesListener listener) {
		Set<QuickActionUpdatesListener> updatesListeners = new HashSet<>(this.updatesListeners);
		updatesListeners.remove(listener);
		this.updatesListeners = updatesListeners;
	}

	private void notifyUpdates() {
		for (QuickActionUpdatesListener listener : updatesListeners) {
			listener.onActionsUpdated();
		}
	}

	@NonNull
	public List<QuickAction> getQuickActions() {
		return new ArrayList<>(quickActions);
	}

	public long getLastModifiedTime() {
		return settings.QUICK_ACTION_LIST.getLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		settings.QUICK_ACTION_LIST.setLastModifiedTime(lastModifiedTime);
	}

	public void addQuickAction(@NonNull QuickAction action) {
		quickActions.add(action);
		onDataChanged();
	}

	public void deleteQuickAction(@NonNull QuickAction action) {
		quickActions.remove(action);
		onDataChanged();
	}

	public void updateQuickAction(@NonNull QuickAction action) {
		int index = quickActions.indexOf(action);
		if (index >= 0) {
			quickActions.set(index, action);
		}
		onDataChanged();
	}

	public void updateQuickActions(List<QuickAction> quickActions) {
		this.quickActions.clear();
		this.quickActions.addAll(quickActions);
		onDataChanged();
	}

	private void onDataChanged() {
		saveActions();
		notifyUpdates();
	}

	private void saveActions() {
		Type type = new TypeToken<List<QuickAction>>() {
		}.getType();
		settings.QUICK_ACTION_LIST.set(gson.toJson(quickActions, type));
	}

	public QuickAction getQuickAction(long id) {
		for (QuickAction action : quickActions) {
			if (action.id == id) {
				return action;
			}
		}
		return null;
	}

	@Nullable
	public QuickAction getQuickAction(int type, String name, Map<String, String> params) {
		for (QuickAction action : quickActions) {
			if (action.getType() == type
					&& (action.hasCustomName(app) && action.getName(app).equals(name) || !action.hasCustomName(app))
					&& action.getParams().equals(params)) {
				return action;
			}
		}
		return null;
	}

	@NonNull
	public List<QuickAction> collectQuickActionsByType(@NonNull QuickActionType type) {
		List<QuickAction> actions = new ArrayList<>();
		for (QuickAction action : quickActions) {
			if (action.getType() == type.getId()) {
				actions.add(action);
			}
		}
		return actions;
	}

	public boolean isNameUnique(@NonNull QuickAction action, @NonNull Context context) {
		for (QuickAction a : quickActions) {
			if (action.id != a.id) {
				if (action.getName(context).equals(a.getName(context))) {
					return false;
				}
			}
		}
		return true;
	}

	@NonNull
	public QuickAction generateUniqueName(@NonNull QuickAction action, @NonNull Context context) {
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
		notifyUpdates();
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

	public void updateActionTypes() {
		List<QuickActionType> allTypes = new ArrayList<>();
		allTypes.add(FavoriteAction.TYPE);
		allTypes.add(GPXAction.TYPE);
		allTypes.add(MarkerAction.TYPE);
		allTypes.add(RouteAction.TYPE);
		// configure map
		allTypes.add(ShowHideFavoritesAction.TYPE);
		allTypes.add(ShowHideGpxTracksAction.TYPE);
		allTypes.add(ShowHidePoiAction.TYPE);
		allTypes.add(MapStyleAction.TYPE);
		allTypes.add(DayNightModeAction.TYPE);
		allTypes.add(ShowHideTransportLinesAction.TYPE);
		allTypes.add(ShowHideMapillaryAction.TYPE);
		// navigation
		allTypes.add(NavVoiceAction.TYPE);
		allTypes.add(NavDirectionsFromAction.TYPE);
		allTypes.add(NavAddDestinationAction.TYPE);
		allTypes.add(NavAddFirstIntermediateAction.TYPE);
		allTypes.add(NavReplaceDestinationAction.TYPE);
		allTypes.add(NavAutoZoomMapAction.TYPE);
		allTypes.add(NavStartStopAction.TYPE);
		allTypes.add(NavResumePauseAction.TYPE);
		allTypes.add(SwitchProfileAction.TYPE);
		allTypes.add(NavRemoveNextDestination.TYPE);
		// settings
		allTypes.add(DisplayPositionAction.TYPE);

		List<QuickActionType> enabledTypes = new ArrayList<>(allTypes);
		PluginsHelper.registerQuickActionTypesPlugins(allTypes, enabledTypes);

		Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();
		Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();
		for (QuickActionType qt : allTypes) {
			quickActionTypesInt.put(qt.getId(), qt);
			quickActionTypesStr.put(qt.getStringId(), qt);
		}
		this.enabledTypes = enabledTypes;
		this.quickActionTypesInt = quickActionTypesInt;
		this.quickActionTypesStr = quickActionTypesStr;

		serializer.setQuickActionTypesStr(quickActionTypesStr);
		serializer.setQuickActionTypesInt(quickActionTypesInt);

		parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
	}

	@NonNull
	public List<QuickActionType> produceTypeActionsListWithHeaders() {
		List<QuickActionType> result = new ArrayList<>();
		filterQuickActions(TYPE_ADD_ITEMS, result);
		filterQuickActions(TYPE_CONFIGURE_MAP, result);
		filterQuickActions(TYPE_NAVIGATION, result);
//		filterQuickActions(TYPE_CONFIGURE_SCREEN, result);
		filterQuickActions(TYPE_SETTINGS, result);
		filterQuickActions(TYPE_OPEN, result);
		return result;
	}

	private void filterQuickActions(@NonNull QuickActionType filter, @NonNull List<QuickActionType> result) {
		result.add(filter);
		Set<Integer> set = new TreeSet<>();
		for (QuickAction qa : quickActions) {
			set.add(qa.getActionType().getId());
		}
		for (QuickActionType t : enabledTypes) {
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

	@Nullable
	public QuickAction newActionByStringType(String actionType) {
		QuickActionType quickActionType = quickActionTypesStr.get(actionType);
		if (quickActionType != null) {
			return quickActionType.createNew();
		}
		return null;
	}

	@Nullable
	public QuickAction newActionByType(int type) {
		QuickActionType quickActionType = quickActionTypesInt.get(type);
		if (quickActionType != null) {
			return quickActionType.createNew();
		}
		return null;
	}

	public void onRenameGpxFile(@NonNull String src, @NonNull String dest) {
		List<QuickAction> gpxActions = collectQuickActionsByType(GPXAction.TYPE);
		for (QuickAction action : gpxActions) {
			String storedPath = action.getParams().get(GPXAction.KEY_GPX_FILE_PATH);
			if (src.equals(storedPath)) {
				action.getParams().put(GPXAction.KEY_GPX_FILE_PATH, dest);
			}
		}
	}

	@NonNull
	public static QuickAction produceAction(@NonNull QuickAction quickAction) {
		return quickAction.getActionType().createNew(quickAction);
	}
}
