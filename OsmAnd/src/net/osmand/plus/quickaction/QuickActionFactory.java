package net.osmand.plus.quickaction;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.EditCategoryDialogFragment;
import net.osmand.plus.mapcontextmenu.editors.SelectCategoryDialogFragment;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
        quickActions.add(new ShowHideFavoritesAction());
        quickActions.add(new ShowHidePoiAction());

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

            case ShowHideFavoritesAction.TYPE:
                return new ShowHideFavoritesAction();

            case ShowHidePoiAction.TYPE:
                return new ShowHidePoiAction();

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

            case ShowHideFavoritesAction.TYPE:
                return new ShowHideFavoritesAction(quickAction);

            case ShowHidePoiAction.TYPE:
                return new ShowHidePoiAction(quickAction);

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

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_add_marker_discr);

            parent.addView(view);
        }
    }

    public static class FavoriteAction extends QuickAction {

        public static final int TYPE = 3;

        public static final String KEY_NAME = "name";
        public static final String KEY_DIALOG = "dialog";
        public static final String KEY_CATEGORY_NAME = "category_name";
        public static final String KEY_CATEGORY_COLOR = "category_color";

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
                                activity.getContextMenu().getFavoritePointEditor().add(latLon, address, "",
                                        getParams().get(KEY_CATEGORY_NAME),
                                        Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)),
                                        !Boolean.valueOf(getParams().get(KEY_DIALOG)));
                            }

                        }, null);

                activity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);

            } else activity.getContextMenu().getFavoritePointEditor().add(latLon, title, "",
                    getParams().get(KEY_CATEGORY_NAME),
                    Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)),
                    !Boolean.valueOf(getParams().get(KEY_DIALOG)));
        }

        @Override
        public void drawUI(final ViewGroup parent, final MapActivity activity) {

            FavouritesDbHelper helper = activity.getMyApplication().getFavorites();

            final View root = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_add_favorite, parent, false);

            parent.addView(root);

            AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) root.findViewById(R.id.category_edit);
            SwitchCompat showDialog = (SwitchCompat) root.findViewById(R.id.saveButton);
            ImageView categoryImage = (ImageView) root.findViewById(R.id.category_image);
            EditText name = (EditText) root.findViewById(R.id.name_edit);

            if (!getParams().isEmpty()) {

                showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
                categoryImage.setColorFilter(Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)));
                name.setText(getParams().get(KEY_NAME));
                categoryEdit.setText(getParams().get(KEY_CATEGORY_NAME));

                if (getParams().get(KEY_NAME).isEmpty() && Integer.valueOf(getParams().get(KEY_CATEGORY_NAME)) == 0) {

                    categoryEdit.setText(activity.getString(R.string.shared_string_favorites));
                    categoryImage.setColorFilter(activity.getResources().getColor(R.color.color_favorite));
                }

            } else if (helper.getFavoriteGroups().size() > 0) {

                FavouritesDbHelper.FavoriteGroup group = helper.getFavoriteGroups().get(0);

                if (group.name.isEmpty() && group.color == 0) {

                    group.name = activity.getString(R.string.shared_string_favorites);

                    categoryEdit.setText(activity.getString(R.string.shared_string_favorites));
                    categoryImage.setColorFilter(activity.getResources().getColor(R.color.color_favorite));

                } else {

                    categoryEdit.setText(group.name);
                    categoryImage.setColorFilter(group.color);
                }

                getParams().put(KEY_CATEGORY_NAME, group.name);
                getParams().put(KEY_CATEGORY_COLOR, String.valueOf(group.color));

            } else {

                categoryEdit.setText(activity.getString(R.string.shared_string_favorites));
                categoryImage.setColorFilter(activity.getResources().getColor(R.color.color_favorite));

                getParams().put(KEY_CATEGORY_NAME, "");
                getParams().put(KEY_CATEGORY_COLOR, "0");
            }

            categoryEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {

                    SelectCategoryDialogFragment dialogFragment = SelectCategoryDialogFragment.createInstance("");

                    dialogFragment.show(
                            activity.getSupportFragmentManager(),
                            SelectCategoryDialogFragment.TAG);

                    dialogFragment.setSelectionListener(new SelectCategoryDialogFragment.CategorySelectionListener() {
                        @Override
                        public void onCategorySelected(String category, int color) {

                            fillGroupParams(root, category, color);
                        }
                    });
                }
            });

            SelectCategoryDialogFragment dialogFragment = (SelectCategoryDialogFragment)
                    activity.getSupportFragmentManager().findFragmentByTag(SelectCategoryDialogFragment.TAG);

            if (dialogFragment != null) {

                dialogFragment.setSelectionListener(new SelectCategoryDialogFragment.CategorySelectionListener() {
                    @Override
                    public void onCategorySelected(String category, int color) {

                        fillGroupParams(root, category, color);
                    }
                });

            } else {

                EditCategoryDialogFragment dialog = (EditCategoryDialogFragment)
                        activity.getSupportFragmentManager().findFragmentByTag(EditCategoryDialogFragment.TAG);

                if (dialog != null) {

                    dialogFragment.setSelectionListener(new SelectCategoryDialogFragment.CategorySelectionListener() {
                        @Override
                        public void onCategorySelected(String category, int color) {

                            fillGroupParams(root, category, color);
                        }
                    });
                }
            }
        }

        @Override
        public void fillParams(View root, MapActivity activity) {

            getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
            getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
        }

        private void fillGroupParams(View root, String name, int color) {

            ((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(name);
            getParams().put(KEY_CATEGORY_NAME, name);

            if (color > 0) {

                ((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);
                getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
            }
        }
    }

    public static class ShowHideFavoritesAction extends QuickAction {

        public static final int TYPE = 4;

        protected ShowHideFavoritesAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quic_action_showhide_favorites_title;
            iconRes = R.drawable.ic_action_fav_dark;
        }

        public ShowHideFavoritesAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            activity.getMyApplication().getSettings().SHOW_FAVORITES.set(
                    !activity.getMyApplication().getSettings().SHOW_FAVORITES.get());
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_showhides_favorites_discr);

            parent.addView(view);
        }
    }

    public static class ShowHidePoiAction extends QuickAction {

        public static final int TYPE = 5;

        protected ShowHidePoiAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quic_action_showhide_poi_title;
            iconRes = R.drawable.ic_action_gabout_dark;
        }

        public ShowHidePoiAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            activity.getMyApplication().getSettings().SHOW_NEARBY_POI.set(
                    !activity.getMyApplication().getSettings().SHOW_NEARBY_POI.get());
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_showhides_poi_discr);

            parent.addView(view);
        }
    }
}
