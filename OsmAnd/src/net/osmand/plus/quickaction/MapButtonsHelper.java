package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.QuickActionType.CREATE_CATEGORY;
import static net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState.DEFAULT_BUTTON_ID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.routes.actions.*;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.ShowHideMapillaryAction;
import net.osmand.plus.quickaction.actions.*;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.configure.buttons.*;
import net.osmand.util.Algorithms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by rosty on 12/27/16.
 */

public class MapButtonsHelper {

	public interface QuickActionUpdatesListener {

		void onActionsUpdated();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface QuickActionCategoryType {
	}

	@QuickActionCategoryType
	public static final QuickActionType TYPE_ADD_ITEMS = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_create_items).category(CREATE_CATEGORY);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_CONFIGURE_MAP = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_configure_map).category(QuickActionType.CONFIGURE_MAP).iconRes(R.drawable.ic_layer_top);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_NAVIGATION = new QuickActionType(0, "").
			nameRes(R.string.shared_string_navigation).category(QuickActionType.NAVIGATION).iconRes(R.drawable.ic_action_gdirections_dark);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_CONFIGURE_SCREEN = new QuickActionType(0, "").
			nameRes(R.string.map_widget_config).category(QuickActionType.CONFIGURE_SCREEN);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_SETTINGS = new QuickActionType(0, "").
			nameRes(R.string.shared_string_settings).category(QuickActionType.SETTINGS).iconRes(R.drawable.ic_action_settings);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_MAP_INTERACTIONS = new QuickActionType(0, "").
			nameRes(R.string.key_event_category_map_interactions).category(QuickActionType.MAP_INTERACTIONS).iconRes(R.drawable.ic_action_map_move_up);
	@QuickActionCategoryType
	public static final QuickActionType TYPE_MY_PLACES = new QuickActionType(0, "").
			nameRes(R.string.shared_string_my_places).category(QuickActionType.MY_PLACES).iconRes(R.drawable.ic_action_favorite);

	@QuickActionCategoryType
	public static final QuickActionType TYPE_INTERFACE = new QuickActionType(0, "").
			nameRes(R.string.shared_string_interface).category(QuickActionType.INTERFACE).iconRes(R.drawable.ic_action_ui_customization);

	public static List<QuickActionType> collectQuickActionCategoryType(Class<?> typeClass) {
		List<QuickActionType> annotatedFields = new ArrayList<>();
		Field[] fields = typeClass.getDeclaredFields();

		for (Field field : fields) {
			if (field.isAnnotationPresent(QuickActionCategoryType.class)) {
				try {
					annotatedFields.add((QuickActionType) field.get(null));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return annotatedFields;
	}

	@Nullable
	public static QuickActionType getCategoryActionTypeFromId(int typeId) {
		for (QuickActionType type : collectQuickActionCategoryType(MapButtonsHelper.class)) {
			if (type.getCategory() == typeId) {
				return type;
			}
		}
		return null;
	}

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final QuickActionSerializer serializer = new QuickActionSerializer();
	private final Gson gson = new GsonBuilder().registerTypeAdapter(QuickAction.class, serializer).create();

	private Map3DButtonState map3DButtonState;
	private MyLocationButtonState myLocationButtonState;
	private NavigationMenuButtonState navigationMenuButtonState;
	private QuickSearchButtonState quickSearchButtonState;
	private ZoomInButtonState zoomInButtonState;
	private ZoomOutButtonState zoomOutButtonState;
	private ConfigureMapButtonState configureMapButtonState;
	private DrawerMenuButtonState drawerMenuButtonState;
	private CompassButtonState compassButtonState;
	private List<QuickActionButtonState> quickActionStates = new ArrayList<>();

	private List<QuickActionType> enabledTypes = new ArrayList<>();
	private Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();
	private Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();
	private Set<QuickActionUpdatesListener> updatesListeners = new HashSet<>();
	private final Collator collator = OsmAndCollator.primaryCollator();

	public MapButtonsHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		updateActionTypes();
		initDefaultButtons();
	}

	private void initDefaultButtons() {
		compassButtonState = new CompassButtonState(app);
		configureMapButtonState = new ConfigureMapButtonState(app);
		drawerMenuButtonState = new DrawerMenuButtonState(app);
		map3DButtonState = new Map3DButtonState(app);
		myLocationButtonState = new MyLocationButtonState(app);
		navigationMenuButtonState = new NavigationMenuButtonState(app);
		quickSearchButtonState = new QuickSearchButtonState(app);
		zoomInButtonState = new ZoomInButtonState(app);
		zoomOutButtonState = new ZoomOutButtonState(app);
	}

	public void addUpdatesListener(@NonNull QuickActionUpdatesListener listener) {
		Set<QuickActionUpdatesListener> listeners = new HashSet<>(this.updatesListeners);
		listeners.add(listener);
		updatesListeners = listeners;
	}

	public void removeUpdatesListener(@NonNull QuickActionUpdatesListener listener) {
		Set<QuickActionUpdatesListener> listeners = new HashSet<>(this.updatesListeners);
		listeners.remove(listener);
		updatesListeners = listeners;
	}

	private void notifyUpdates() {
		for (QuickActionUpdatesListener listener : updatesListeners) {
			listener.onActionsUpdated();
		}
	}

	@NonNull
	public Map3DButtonState getMap3DButtonState() {
		return map3DButtonState;
	}

	@NonNull
	public CompassButtonState getCompassButtonState() {
		return compassButtonState;
	}

	@NonNull
	public MyLocationButtonState getMyLocationButtonState() {
		return myLocationButtonState;
	}

	@NonNull
	public NavigationMenuButtonState getNavigationMenuButtonState() {
		return navigationMenuButtonState;
	}

	@NonNull
	public QuickSearchButtonState getQuickSearchButtonState() {
		return quickSearchButtonState;
	}

	@NonNull
	public ZoomInButtonState getZoomInButtonState() {
		return zoomInButtonState;
	}

	@NonNull
	public ZoomOutButtonState getZoomOutButtonState() {
		return zoomOutButtonState;
	}

	@NonNull
	public ConfigureMapButtonState getConfigureMapButtonState() {
		return configureMapButtonState;
	}

	@NonNull
	public DrawerMenuButtonState getDrawerMenuButtonState() {
		return drawerMenuButtonState;
	}

	@NonNull
	public List<QuickActionButtonState> getButtonsStates() {
		return quickActionStates;
	}

	@NonNull
	public List<MapButtonState> getDefaultButtonsStates() {
		return Arrays.asList(configureMapButtonState, quickSearchButtonState,
				compassButtonState, drawerMenuButtonState, navigationMenuButtonState,
				map3DButtonState, myLocationButtonState, zoomInButtonState, zoomOutButtonState
		);
	}

	@NonNull
	public List<QuickAction> getFlattenedQuickActions() {
		List<QuickAction> actions = new ArrayList<>();
		for (QuickActionButtonState buttonState : quickActionStates) {
			actions.addAll(buttonState.getQuickActions());
		}
		return actions;
	}

	@NonNull
	public List<QuickActionButtonState> getEnabledButtonsStates() {
		List<QuickActionButtonState> list = new ArrayList<>();
		for (QuickActionButtonState buttonState : quickActionStates) {
			if (buttonState.isEnabled()) {
				list.add(buttonState);
			}
		}
		return list;
	}

	public void addQuickAction(@NonNull QuickActionButtonState buttonState, @NonNull QuickAction action) {
		buttonState.getQuickActions().add(action);
		onQuickActionsChanged(buttonState);
	}

	public void deleteQuickAction(@NonNull QuickActionButtonState buttonState, @NonNull QuickAction action) {
		buttonState.getQuickActions().remove(action);
		onQuickActionsChanged(buttonState);
	}

	public void updateQuickAction(@NonNull QuickActionButtonState buttonState, @NonNull QuickAction action) {
		List<QuickAction> actions = buttonState.getQuickActions();
		int index = actions.indexOf(action);
		if (index >= 0) {
			actions.set(index, action);
		}
		onQuickActionsChanged(buttonState);
	}

	public void updateQuickActions(@NonNull QuickActionButtonState buttonState, @NonNull List<QuickAction> actions) {
		List<QuickAction> quickActions = buttonState.getQuickActions();
		quickActions.clear();
		quickActions.addAll(actions);
		onQuickActionsChanged(buttonState);
	}

	public void setQuickActionFabState(@NonNull QuickActionButtonState buttonState, boolean enabled) {
		buttonState.setEnabled(enabled);
		notifyUpdates();
	}

	private void onQuickActionsChanged(@NonNull QuickActionButtonState buttonState) {
		buttonState.saveActions(gson);
		notifyUpdates();
	}

	@NonNull
	public String convertActionsToJson(@NonNull List<QuickAction> quickActions) {
		Type type = new TypeToken<List<QuickAction>>() {}.getType();
		return gson.toJson(quickActions, type);
	}

	@Nullable
	public List<QuickAction> parseActionsFromJson(@NonNull String json) {
		Type type = new TypeToken<List<QuickAction>>() {}.getType();
		return gson.fromJson(json, type);
	}

	public void onButtonStateChanged(@NonNull QuickActionButtonState buttonState) {
		notifyUpdates();
	}

	@NonNull
	public List<QuickAction> collectQuickActionsByType(@NonNull QuickActionType type) {
		List<QuickAction> actions = new ArrayList<>();
		for (QuickAction action : getFlattenedQuickActions()) {
			if (action.getType() == type.getId()) {
				actions.add(action);
			}
		}
		return actions;
	}

	public boolean isActionNameUnique(@NonNull List<QuickAction> actions, @NonNull QuickAction quickAction) {
		for (QuickAction action : actions) {
			if (quickAction.getId() != action.getId()
					&& Algorithms.stringsEqual(quickAction.getExtendedName(app), action.getExtendedName(app))) {
				return false;
			}
		}
		return true;
	}

	@NonNull
	public QuickAction generateUniqueActionName(@NonNull List<QuickAction> actions, @NonNull QuickAction action) {
		int number = 0;
		String name = action.getName(app);
		while (true) {
			number++;
			action.setName(name + " (" + number + ")");
			if (isActionNameUnique(actions, action)) {
				return action;
			}
		}
	}

	@NonNull
	public String generateUniqueButtonName(@NonNull String name) {
		int number = 0;
		while (true) {
			number++;
			String newName = name + " (" + number + ")";
			if (isActionButtonNameUnique(newName)) {
				return newName;
			}
		}
	}

	public boolean hasEnabledButtons() {
		for (QuickActionButtonState buttonState : quickActionStates) {
			if (buttonState.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	public void updateActionTypes() {
		List<QuickActionType> allTypes = new ArrayList<>();
		allTypes.add(FavoriteAction.TYPE);
		allTypes.add(GPXAction.TYPE);
		allTypes.add(MarkerAction.TYPE);
		allTypes.add(RouteAction.TYPE);
		allTypes.add(MapScrollUpAction.TYPE);
		allTypes.add(MapScrollDownAction.TYPE);
		allTypes.add(MapScrollLeftAction.TYPE);
		allTypes.add(MapScrollRightAction.TYPE);
		allTypes.add(MapZoomInAction.TYPE);
		allTypes.add(MapZoomOutAction.TYPE);
		allTypes.add(MoveToMyLocationAction.TYPE);
		// configure map
		allTypes.add(ShowHideFavoritesAction.TYPE);
		allTypes.add(ShowHideGpxTracksAction.TYPE);
		allTypes.add(ShowHidePoiAction.TYPE);
		allTypes.add(MapStyleAction.TYPE);
		allTypes.add(DayNightModeAction.TYPE);
		allTypes.add(ShowHideTransportLinesAction.TYPE);
		allTypes.add(ShowHideMapillaryAction.TYPE);
		allTypes.add(ShowHideCycleRoutesAction.TYPE);
		allTypes.add(ShowHideMtbRoutesAction.TYPE);
		allTypes.add(ShowHideHikingRoutesAction.TYPE);
		allTypes.add(ShowHideDifficultyClassificationAction.TYPE);
		allTypes.add(ShowHideSkiSlopesAction.TYPE);
		allTypes.add(ShowHideHorseRoutesAction.TYPE);
		allTypes.add(ShowHideWhitewaterSportsAction.TYPE);
		allTypes.add(ShowHideFitnessTrailsAction.TYPE);
		allTypes.add(ShowHideRunningRoutesAction.TYPE);
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
		allTypes.add(NextAppProfileAction.TYPE);
		allTypes.add(PreviousAppProfileAction.TYPE);
		allTypes.add(ChangeMapOrientationAction.TYPE);
		// interface
		allTypes.add(OpenNavigationViewAction.TYPE);
		allTypes.add(OpenSearchViewAction.TYPE);
		allTypes.add(ShowHideDrawerAction.TYPE);
		allTypes.add(NavigatePreviousScreenAction.TYPE);
		allTypes.add(LockScreenAction.TYPE);

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

		updateActiveActions();
	}

	public void updateActiveActions() {
		quickActionStates = createButtonsStates();
	}

	@NonNull
	public List<QuickActionButtonState> createButtonsStates() {
		List<QuickActionButtonState> list = new ArrayList<>();
		List<String> actionsKeys = settings.QUICK_ACTION_BUTTONS.getStringsList();
		if (!Algorithms.isEmpty(actionsKeys)) {
			Set<String> uniqueKeys = new LinkedHashSet<>(actionsKeys);
			for (String key : uniqueKeys) {
				if (!Algorithms.isEmpty(key)) {
					QuickActionButtonState buttonState = new QuickActionButtonState(app, key);
					buttonState.parseQuickActions(gson);
					list.add(buttonState);
				}
			}
		}
		return list;
	}

	public void resetQuickActionsForMode(@NonNull ApplicationMode appMode) {
		for (QuickActionButtonState buttonState : getButtonsStates()) {
			buttonState.resetForMode(appMode);
		}
		updateActionTypes();
	}

	public void copyQuickActionsFromMode(@NonNull ApplicationMode toAppMode, @NonNull ApplicationMode fromAppMode) {
		for (QuickActionButtonState buttonState : getButtonsStates()) {
			buttonState.copyForMode(fromAppMode, toAppMode);
		}
		updateActionTypes();
	}

	@NonNull
	public Map<QuickActionType, List<QuickActionType>> produceTypeActionsListWithHeaders(@Nullable QuickActionButtonState buttonState) {
		Map<QuickActionType, List<QuickActionType>> quickActions = new HashMap<>();

		filterQuickActions(buttonState, TYPE_ADD_ITEMS, quickActions);
		filterQuickActions(buttonState, TYPE_CONFIGURE_MAP, quickActions);
		filterQuickActions(buttonState, TYPE_NAVIGATION, quickActions);
//		filterQuickActions(buttonState, TYPE_CONFIGURE_SCREEN, actionTypes);
		filterQuickActions(buttonState, TYPE_SETTINGS, quickActions);
		filterQuickActions(buttonState, TYPE_MAP_INTERACTIONS, quickActions);
		filterQuickActions(buttonState, TYPE_MY_PLACES, quickActions);
		filterQuickActions(buttonState, TYPE_INTERFACE, quickActions);

		return quickActions;
	}

	public void filterQuickActions(@NonNull QuickActionType filter,
	                               @NonNull List<QuickActionType> actionTypes) {
		filterQuickActions(null, filter, actionTypes);
	}

	public void filterQuickActions(@Nullable QuickActionButtonState buttonState,
	                               @NonNull QuickActionType filter,
	                               @NonNull List<QuickActionType> actionTypes) {
		Set<Integer> set = new TreeSet<>();
		if (buttonState != null) {
			for (QuickAction action : buttonState.getQuickActions()) {
				set.add(action.getActionType().getId());
			}
		}
		for (QuickActionType type : enabledTypes) {
			if (type.getCategory() == filter.getCategory()) {
				if (!type.isActionEditable()) {
					boolean instanceInList = set.contains(type.getId());
					if (!instanceInList) {
						actionTypes.add(type);
					}
				} else {
					actionTypes.add(type);
				}
			}
		}
	}

	private void filterQuickActions(@Nullable QuickActionButtonState buttonState,
	                                @NonNull QuickActionType filter,
	                                @NonNull Map<QuickActionType, List<QuickActionType>> actionTypes) {
		List<QuickActionType> categoryActions = actionTypes.get(filter);
		if (categoryActions == null) {
			categoryActions = new ArrayList<>();
		} else {
			categoryActions.clear();
		}

		filterQuickActions(buttonState, filter, categoryActions);

		if (!Algorithms.isEmpty(categoryActions)) {
			categoryActions.sort((o1, o2) -> compareNames(app.getString(o1.getNameRes()), app.getString(o2.getNameRes())));
			actionTypes.put(filter, categoryActions);
		}
	}

	public int compareNames(@NonNull String item1, @NonNull String item2) {
		return collator.compare(item1, item2);
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

	public boolean isActionButtonNameUnique(@NonNull String name) {
		return getButtonStateByName(name) == null;
	}

	@Nullable
	public QuickActionButtonState getButtonStateByName(@NonNull String name) {
		for (QuickActionButtonState buttonState : quickActionStates) {
			if (Algorithms.stringsEqual(buttonState.getName(), name)) {
				return buttonState;
			}
		}
		return null;
	}

	@Nullable
	public QuickActionButtonState getActionButtonStateById(@NonNull String id) {
		for (QuickActionButtonState buttonState : quickActionStates) {
			if (Algorithms.stringsEqual(buttonState.getId(), id)) {
				return buttonState;
			}
		}
		return null;
	}

	@Nullable
	public MapButtonState getMapButtonStateById(@NonNull String id) {
		for (MapButtonState buttonState : getDefaultButtonsStates()) {
			if (Algorithms.stringsEqual(buttonState.getId(), id)) {
				return buttonState;
			}
		}
		return getActionButtonStateById(id);
	}

	@Nullable
	public QuickActionButtonState getButtonStateByAction(@NonNull QuickAction action) {
		long id = action.getId();
		for (QuickActionButtonState buttonState : quickActionStates) {
			if (buttonState.getQuickAction(id) != null) {
				return buttonState;
			}
		}
		return null;
	}

	@NonNull
	public QuickActionButtonState createNewButtonState() {
		String id = DEFAULT_BUTTON_ID + "_" + System.currentTimeMillis();
		return new QuickActionButtonState(app, id);
	}

	public void addQuickActionButtonState(@NonNull QuickActionButtonState buttonState) {
		settings.QUICK_ACTION_BUTTONS.addValue(buttonState.getId());
		updateActiveActions();
		notifyUpdates();
	}

	public void removeQuickActionButtonState(@NonNull QuickActionButtonState buttonState) {
		settings.QUICK_ACTION_BUTTONS.removeValue(buttonState.getId());
		buttonState.onButtonStateRemoved();
		updateActiveActions();
		notifyUpdates();
	}

	public void setQuickActionStates(@NonNull Collection<QuickActionButtonState> buttonStates) {
		settings.QUICK_ACTION_BUTTONS.clearAll();
		for (QuickActionButtonState state : buttonStates) {
			settings.QUICK_ACTION_BUTTONS.addValue(state.getId());
		}
		updateActiveActions();
		notifyUpdates();
	}

	@NonNull
	public static QuickAction produceAction(@NonNull QuickAction action) {
		return action.getActionType().createNew(action);
	}
}
