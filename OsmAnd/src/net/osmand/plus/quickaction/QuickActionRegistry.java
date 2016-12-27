package net.osmand.plus.quickaction;

import net.osmand.plus.OsmandSettings;

import java.util.ArrayList;
import java.util.List;

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

    private QuickActionUpdatesListener updatesListener;

    public QuickActionRegistry(OsmandSettings settings) {

        this.factory = new QuickActionFactory();
        this.settings = settings;

        quickActions = factory.parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
    }

    public void setUpdatesListener(QuickActionUpdatesListener updatesListener) {
        this.updatesListener = updatesListener;
    }

    public void notifyUpdates() {
        if (updatesListener != null) updatesListener.onActionsUpdated();
    }

    public List<QuickAction> getQuickActions() {

        ArrayList<QuickAction> actions = new ArrayList<>();
        actions.addAll(quickActions);

        return actions;
    }

    public void addQuickAction(QuickAction action){

        quickActions.add(action);

        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void deleteQuickAction(QuickAction action){

        int index = quickActions.indexOf(action);

        if (index >= 0) quickActions.remove(index);

        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void deleteQuickAction(int id){

        int index = -1;

        for (QuickAction action: quickActions){

            if (action.id == id)
                index = quickActions.indexOf(action);
        }

        if (index >= 0) quickActions.remove(index);

        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void updateQuickAction(QuickAction action){

        int index = quickActions.indexOf(action);

        if (index >= 0) quickActions.set(index, action);

        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(quickActions));
    }

    public void updateQuickActions(List<QuickAction>  quickActions){

        this.quickActions.clear();
        this.quickActions.addAll(quickActions);

        settings.QUICK_ACTION_LIST.set(factory.quickActionListToString(this.quickActions));
    }

    public QuickAction getQuickAction(long id){

        for (QuickAction action: quickActions){

            if (action.id == id) return action;
        }

        return null;
    }
}
