package net.osmand.plus.quickaction;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.AndroidUtils;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.SelectCategoryDialogFragment;
import net.osmand.plus.osmedit.OsmEditsUploadListener;
import net.osmand.plus.osmedit.OsmEditsUploadListenerHelper;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickActionFactory {

    public String quickActionListToString(List<QuickAction> quickActions) {

        String json = new Gson().toJson(quickActions);
        return json;
    }

    public List<QuickAction> parseActiveActionsList(String json) {

        Type type = new TypeToken<List<QuickAction>>() {
        }.getType();
        ArrayList<QuickAction> quickActions = new Gson().fromJson(json, type);

        return quickActions != null ? quickActions : new ArrayList<QuickAction>();
    }

    public static List<QuickAction> produceTypeActionsList() {

        ArrayList<QuickAction> quickActions = new ArrayList<>();

        quickActions.add(new MarkerAction());
        quickActions.add(new FavoriteAction());

        return quickActions;
    }

    public static QuickAction newActionByType(int type) {

        switch (type) {

            case NewAction.TYPE:
                return new NewAction();

            case MarkerAction.TYPE:
                return new MarkerAction();

            case FavoriteAction.TYPE:
                return new FavoriteAction();

            default:
                return new QuickAction();
        }
    }

    public static QuickAction produceAction(QuickAction quickAction) {

        switch (quickAction.type) {

            case NewAction.TYPE:
                return new NewAction(quickAction);

            case MarkerAction.TYPE:
                return new MarkerAction(quickAction);

            case FavoriteAction.TYPE:
                return new FavoriteAction(quickAction);

            default:
                return quickAction;
        }
    }

    public static class NewAction extends QuickAction {

        public static final int TYPE = 1;

        protected NewAction() {
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
        public void drawUI(ViewGroup parent, MapActivity activity) {

            //TODO inflate view & fill with params
        }
    }

    public static class MarkerAction extends QuickAction {

        public static final int TYPE = 2;

        private MarkerAction() {
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

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            PointDescription pointDescription = new PointDescription(
                    latLon.getLatitude(),
                    latLon.getLongitude());

            if (pointDescription.isLocation() && pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(activity)))
                pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");

            activity.getMapActions().addMapMarker(
                    latLon.getLatitude(),
                    latLon.getLongitude(),
                    pointDescription);
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            if (parent.getChildCount() == 0) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.quick_action_add_marker, parent, false);

                parent.addView(view);
            }
        }
    }

    public static class FavoriteAction extends QuickAction {

        public static final int TYPE = 3;

        public static final String KEY_NAME = "name";
        public static final String KEY_DIALOG = "dialog";

        private FavoriteAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_favorite;
            iconRes = R.drawable.ic_action_fav_dark;
        }

        public FavoriteAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(final MapActivity activity) {

            final LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            final String title = getParams().get(KEY_NAME);

            if (title == null || title.isEmpty()) {

                final Dialog progressDialog = new ProgressDialog(activity);
                progressDialog.setCancelable(false);
                progressDialog.setTitle(R.string.search_address);
                progressDialog.show();

                GeocodingLookupService.AddressLookupRequest lookupRequest = new GeocodingLookupService.AddressLookupRequest(latLon,

                        new GeocodingLookupService.OnAddressLookupResult() {

                            @Override
                            public void geocodingDone(String address) {

                                progressDialog.dismiss();
                                activity.getContextMenu().getFavoritePointEditor().add(latLon, address, "");
                            }

                        }, null);

                activity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);

            } else activity.getContextMenu().getFavoritePointEditor().add(latLon, title, "");
        }

        @Override
        public void drawUI(final ViewGroup parent, MapActivity activity) {

            FavouritesDbHelper helper = activity.getMyApplication().getFavorites();

            String category = helper.getFavoriteGroups().size() > 0
                    ? helper.getFavoriteGroups().get(0).name
                    : activity.getString(R.string.shared_string_favorites);

            View root;

            if (parent.getChildCount() == 0) {

                root = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.quick_action_add_favorite, parent, false);

                parent.addView(root);

            } else root = parent.getChildAt(0);

            AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx)
                    root.findViewById(R.id.category_edit);

            categoryEdit.setText(category);
            categoryEdit.setFocusable(false);

            if (!getParams().isEmpty()){

                ((EditText) root.findViewById(R.id.name_edit)).setText(getParams().get(KEY_NAME));
                ((SwitchCompat) root.findViewById(R.id.saveButton)).setChecked(Boolean.getBoolean(getParams().get(KEY_DIALOG)));
            }
        }

        @Override
        public void fillParams(View root) {

            getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
            getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
        }
    }
}
