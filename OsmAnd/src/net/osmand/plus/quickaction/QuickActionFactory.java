package net.osmand.plus.quickaction;


import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuickActionFactory {

    public String quickActionListToString(List<QuickAction> quickActions){

        String json = new Gson().toJson(quickActions);
        return json;
    }

    public List<QuickAction> parseActiveActionsList(String json) {

        Type type = new TypeToken<List<QuickAction>>(){}.getType();
        ArrayList<QuickAction> quickActions = new Gson().fromJson(json, type);

        return quickActions != null ? quickActions : new ArrayList<QuickAction>();
    }

    public static List<QuickAction> produceTypeActionsList() {

        ArrayList<QuickAction> quickActions = new ArrayList<>();

        quickActions.add(new MarkerAction());
        quickActions.add(new FavoriteAction());

        return quickActions;
    }

    public static QuickAction newActionByType(int type){

        switch (type){

            case NewAction.TYPE: return new NewAction();

            case MarkerAction.TYPE: return new MarkerAction();

            case FavoriteAction.TYPE: return new FavoriteAction();

            default: return new QuickAction();
        }
    }

    public static QuickAction produceAction(QuickAction quickAction){

        switch (quickAction.type){

            case NewAction.TYPE: return new NewAction(quickAction);

            case MarkerAction.TYPE: return new MarkerAction(quickAction);

            case FavoriteAction.TYPE: return new FavoriteAction(quickAction);

            default: return quickAction;
        }
    }

    public static class NewAction extends QuickAction {

        public static final int TYPE = 1;

        protected NewAction(){
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_new_action;
            iconRes = R.drawable.ic_action_plus;
        }

        public NewAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            AddQuickActionDialog dialog = new AddQuickActionDialog();
            dialog.show(activity.getSupportFragmentManager(), AddQuickActionDialog.TAG);
        }

        @Override
        public void drawUI(ViewGroup parent) {

            //TODO inflate view & fill with params
        }
    }

    public static class MarkerAction extends QuickAction {

        public static final int TYPE = 2;

        private MarkerAction(){
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_marker;
            iconRes = R.drawable.ic_action_flag_dark;
        }

        public MarkerAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            //TODO do some action
        }

        @Override
        public void drawUI(ViewGroup parent) {

            //TODO inflate view & fill with params
        }
    }

    public static class FavoriteAction extends QuickAction {

        public static final int TYPE = 3;

        public FavoriteAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_favorite;
            iconRes = R.drawable.ic_action_fav_dark;
        }

        public FavoriteAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            //TODO do some action
        }

        @Override
        public void drawUI(ViewGroup parent) {

            //TODO inflate view & fill with params
        }
    }
}
