package net.osmand.plus.quickaction;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.TakeAudioNoteAction;
import net.osmand.plus.audionotes.TakePhotoNoteAction;
import net.osmand.plus.audionotes.TakeVideoNoteAction;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingAction;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.quickaction.actions.AddOSMBugAction;
import net.osmand.plus.quickaction.actions.AddPOIAction;
import net.osmand.plus.quickaction.actions.ContourLinesAction;
import net.osmand.plus.quickaction.actions.HillshadeAction;
import net.osmand.plus.quickaction.actions.MapSourceAction;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rosty on 12/27/16.
 */

public class QuickActionRegistry {

    public interface QuickActionUpdatesListener{

        void onActionsUpdated();
    }

    private final QuickActionFactory factory;
    private final OsmandSettings settings;

    private final List<QuickAction> quickActions;
    private final Map<String, Boolean> fabStateMap;

    private QuickActionUpdatesListener updatesListener;

    public QuickActionRegistry(OsmandSettings settings) {

        this.factory = new QuickActionFactory();
        this.settings = settings;

        quickActions = factory.parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
        fabStateMap = getQuickActionFabStateMapFromJson(settings.QUICK_ACTION.get());
    }

    public void setUpdatesListener(QuickActionUpdatesListener updatesListener) {
        this.updatesListener = updatesListener;
    }

    public void notifyUpdates() {
        if (updatesListener != null) updatesListener.onActionsUpdated();
    }

    public List<QuickAction> getQuickActions() {
        List<QuickAction> actions = new ArrayList<>();
        actions.addAll(quickActions);
        return actions;
    }

	public List<QuickAction> getFilteredQuickActions() {

		List<QuickAction> actions = getQuickActions();
		List<QuickAction> filteredActions = new ArrayList<>();

		for (QuickAction action : actions) {
			boolean skip = false;
			if (OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class) == null) {

				if (action.type == TakeAudioNoteAction.TYPE || action.type == TakePhotoNoteAction.TYPE
						|| action.type == TakeVideoNoteAction.TYPE) {
					skip = true;
				}
			}

			if (OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class) == null) {
				if (action.type == ParkingAction.TYPE) {
					skip = true;
				}
			}

			if (OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null) {
				if (action.type == MapStyleAction.TYPE) {
					if (((MapStyleAction) QuickActionFactory.produceAction(action)).getFilteredStyles().isEmpty()) {
						skip = true;
					}
				}
			}

			if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
				if (action.type == MapSourceAction.TYPE) {
					skip = true;
				}
			}
			if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) == null) {
				if (action.type == AddPOIAction.TYPE) {
					skip = true;
				}
				if (action.type == AddOSMBugAction.TYPE) {
					skip = true;
				}
			}
			if (OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null) {
				if (action.type == ContourLinesAction.TYPE) {
					skip = true;
				}
				if (action.type == HillshadeAction.TYPE) {
					skip = true;
				}
			}
			if (!skip) {
				filteredActions.add(action);
			}
		}

		return filteredActions;
	}

    public void addQuickAction(QuickAction action){
        quickActions.add(action);
        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void deleteQuickAction(QuickAction action){
        int index = quickActions.indexOf(action);
        if (index >= 0) {
        	quickActions.remove(index);
        }
        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
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
        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void updateQuickAction(QuickAction action){
        int index = quickActions.indexOf(action);
        if (index >= 0) {
        	quickActions.set(index, action);
        }
        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void updateQuickActions(List<QuickAction>  quickActions){
        this.quickActions.clear();
        this.quickActions.addAll(quickActions);
        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(this.quickActions));
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
}
