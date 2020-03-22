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
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
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
import net.osmand.plus.quickaction.actions.NavReplaceDestinationAction;
import net.osmand.plus.quickaction.actions.NavResumePauseAction;
import net.osmand.plus.quickaction.actions.NavStartStopAction;
import net.osmand.plus.quickaction.actions.NavVoiceAction;
import net.osmand.plus.quickaction.actions.NewAction;
import net.osmand.plus.quickaction.actions.ShowHideFavoritesAction;
import net.osmand.plus.quickaction.actions.ShowHideGpxTracksAction;
import net.osmand.plus.quickaction.actions.ShowHidePoiAction;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by rosty on 12/27/16.
 */

public class QuickActionRegistry {

    public interface QuickActionUpdatesListener{

        void onActionsUpdated();
    }

	public static final QuickActionType TYPE_ADD_ITEMS = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_create_items).category(QuickActionType.CREATE_CATEGORY);
	public static final QuickActionType TYPE_CONFIGURE_MAP = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_configure_map).category(QuickActionType.CONFIGURE_MAP);
	public static final QuickActionType TYPE_NAVIGATION = new QuickActionType(0, "").
			nameRes(R.string.quick_action_add_navigation).category(QuickActionType.NAVIGATION);


    private final OsmandSettings settings;

    private final List<QuickAction> quickActions;
    private final Map<String, Boolean> fabStateMap;
    private final Gson gson;

	private QuickActionUpdatesListener updatesListener;

    public QuickActionRegistry(OsmandSettings settings) {

        this.settings = settings;
		gson = new GsonBuilder().registerTypeAdapter(QuickAction.class, new JsonSerializer<QuickAction>() {
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
		}).registerTypeAdapter(QuickAction.class, new JsonDeserializer<QuickAction>() {

			@Override
			public QuickAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				JsonObject obj = json.getAsJsonObject();
				// TODO improve search iteration
				List<QuickActionType> types = QuickActionRegistry.getActionTypes();
				QuickActionType found = null;
				if(obj.has("actionType")) {
					String actionType = obj.get("actionType").getAsString();
					for(QuickActionType q : types) {
						if(Algorithms.stringsEqual(q.getStringId(), actionType)) {
							found = q;
							break;
						}
					}
				} else if(obj.has("type")) {
					int type = obj.get("type").getAsInt();
					for(QuickActionType q : types) {
						if(q.getId() == type) {
							found = q;
							break;
						}
					}
				}
				if(found != null) {
					QuickAction qa = found.createNew();
					if(obj.has("name")) {
						qa.setName(obj.get("name").getAsString());
					}
					if(obj.has("id")) {
						qa.setId(obj.get("id").getAsLong());
					}
					if(obj.has("params")) {
						qa.setParams(
								(Map<String, String>) context.deserialize(obj.get("params"),
										new TypeToken<HashMap<String, String>>(){}.getType())
						);
					}
					return qa;
				}
				return null;
			}
		}).create();
		quickActions = parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
		fabStateMap = getQuickActionFabStateMapFromJson(settings.QUICK_ACTION.get());

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
		List<QuickAction> actions = getQuickActions();
		Set<Integer> activeTypesInts = new TreeSet<>();
		for (QuickActionType activeType : getActionTypes()) {
			activeTypesInts.add(activeType.getId());
		}
		List<QuickAction> filteredActions = new ArrayList<>();
		for (QuickAction action : actions) {
			if (activeTypesInts.contains(action.getActionType().getId())) {
				filteredActions.add(action);
			}
		}
		return filteredActions;
	}

    public void addQuickAction(QuickAction action){
        quickActions.add(action);
        settings.QUICK_ACTION_LIST.set(quickActionListToString(quickActions));
    }

    public void deleteQuickAction(QuickAction action){
        int index = quickActions.indexOf(action);
        if (index >= 0) {
        	quickActions.remove(index);
        }
        settings.QUICK_ACTION_LIST.set(quickActionListToString(quickActions));
    }

    public void deleteQuickAction(int id){

        int index = -1;
        for (QuickAction action: quickActions){
            if (action.id == id) {
            	index = quickActions.indexOf(action);
            }
        }
        if (index >= 0) {
        	quickActions.remove(index);
        }
        settings.QUICK_ACTION_LIST.set(quickActionListToString(quickActions));
    }

    public void updateQuickAction(QuickAction action){
        int index = quickActions.indexOf(action);
        if (index >= 0) {
        	quickActions.set(index, action);
        }
        settings.QUICK_ACTION_LIST.set(quickActionListToString(quickActions));
    }

    public void updateQuickActions(List<QuickAction>  quickActions){
        this.quickActions.clear();
        this.quickActions.addAll(quickActions);
        settings.QUICK_ACTION_LIST.set(quickActionListToString(this.quickActions));
    }

    public QuickAction getQuickAction(long id){
        for (QuickAction action: quickActions){
            if (action.id == id) {
            	return action;
            }
        }
        return null;
    }

    public boolean isNameUnique(QuickAction action, Context context){
        for (QuickAction a: quickActions){
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
        Boolean result = fabStateMap.get(settings.APPLICATION_MODE.get().getStringKey());
        return result != null && result;
    }

    public void setQuickActionFabState(boolean isOn) {
        fabStateMap.put(settings.APPLICATION_MODE.get().getStringKey(), isOn);
        settings.QUICK_ACTION.set(new Gson().toJson(fabStateMap));
    }

    private Map<String, Boolean> getQuickActionFabStateMapFromJson(String json) {
        Type type = new TypeToken<HashMap<String, Boolean>>() {
        }.getType();
        HashMap<String, Boolean> quickActions = new Gson().fromJson(json, type);

        return quickActions != null ? quickActions : new HashMap<String, Boolean>();
    }


	private String quickActionListToString(List<QuickAction> quickActions) {
		return gson.toJson(quickActions);
	}

	public List<QuickAction> parseActiveActionsList(String json) {
		Type type = new TypeToken<List<QuickAction>>() {}.getType();
		List<QuickAction> quickActions = gson.fromJson(json, type);
		List<QuickActionType> actionTypes = QuickActionRegistry.getActionTypes();
		List<QuickAction> rquickActions = new ArrayList<>(quickActions.size());
		if(quickActions != null) {
			for(QuickAction qa : rquickActions) {
				if(qa != null) {
					rquickActions.add(qa);
				}
			}
		}
		return rquickActions;
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

	public static QuickAction newActionByStringType(String actionType) {
		for (QuickActionType t : getActionTypes()) {
			if (Algorithms.objectEquals(actionType, t.getStringId())) {
				return t.createNew();
			}
		}
		return null;
	}

	public static QuickAction newActionByType(int type) {
		for (QuickActionType t : getActionTypes()) {
			if (t.getId() == type) {
				return t.createNew();
			}
		}
		return null;
	}

	public static QuickAction produceAction(QuickAction quickAction) {
		return quickAction.getActionType().createNew(quickAction);
	}

}
