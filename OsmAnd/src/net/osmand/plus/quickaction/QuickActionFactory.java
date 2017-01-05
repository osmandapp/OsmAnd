package net.osmand.plus.quickaction;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.StringRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.mapcontextmenu.editors.EditCategoryDialogFragment;
import net.osmand.plus.mapcontextmenu.editors.SelectCategoryDialogFragment;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.osmedit.EditPoiData;
import net.osmand.plus.osmedit.EditPoiDialogFragment;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
import net.osmand.render.RenderingRulesStorage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static net.osmand.plus.osmedit.AdvancedEditPoiFragment.addPoiToStringSet;

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

    public List<QuickAction> parseAndFilterActiveActionsList(String json) {

        Type type = new TypeToken<List<QuickAction>>() {
        }.getType();
        ArrayList<QuickAction> quickActions = new Gson().fromJson(json, type);

        return quickActions != null ? quickActions : new ArrayList<QuickAction>();
    }

    public static List<QuickAction> produceTypeActionsListWithHeaders() {

        ArrayList<QuickAction> quickActions = new ArrayList<>();

        quickActions.add(new QuickAction(0, R.string.quick_action_add_create_items));
        quickActions.add(new FavoriteAction());
        quickActions.add(new GPXAction());
        quickActions.add(new MarkerAction());

        if (OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class) != null) {

            quickActions.add(new TakeAudioNoteAction());
            quickActions.add(new TakePhotoNoteAction());
            quickActions.add(new TakeVideoNoteAction());
        }

        if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {

            quickActions.add(new AddPOIAction());
            quickActions.add(new AddOSMBugAction());
        }

        if (OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class) != null) {

            quickActions.add(new ParkingAction());
        }

        quickActions.add(new QuickAction(0, R.string.quick_action_add_configure_map));
        quickActions.add(new MapStyleAction());
        quickActions.add(new ShowHideFavoritesAction());
        quickActions.add(new ShowHidePoiAction());

        quickActions.add(new QuickAction(0, R.string.quick_action_add_navigation));
        quickActions.add(new NavigationVoiceAction());

        return quickActions;
    }

    public static List<QuickAction> produceTypeActionsListWithHeaders(List<QuickAction> active) {

        ArrayList<QuickAction> quickActions = new ArrayList<>();

        quickActions.add(new QuickAction(0, R.string.quick_action_add_create_items));
        quickActions.add(new FavoriteAction());
        quickActions.add(new GPXAction());

        QuickAction marker = new MarkerAction();

        if (!marker.hasInstanceInList(active)) {
            quickActions.add(marker);
        }

        if (OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class) != null) {

            QuickAction audio = new TakeAudioNoteAction();
            QuickAction photo = new TakePhotoNoteAction();
            QuickAction vedio = new TakeVideoNoteAction();

            if (!audio.hasInstanceInList(active)) {
                quickActions.add(audio);
            }

            if (!photo.hasInstanceInList(active)) {
                quickActions.add(photo);
            }

            if (!vedio.hasInstanceInList(active)) {
                quickActions.add(vedio);
            }
        }

        if (OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null) {

            quickActions.add(new AddPOIAction());
            quickActions.add(new AddOSMBugAction());
        }

        if (OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class) != null) {

            QuickAction parking = new ParkingAction();

            if (!parking.hasInstanceInList(active)) {
                quickActions.add(parking);
            }
        }

        quickActions.add(new QuickAction(0, R.string.quick_action_add_configure_map));
        quickActions.add(new MapStyleAction());

        QuickAction favorites = new ShowHideFavoritesAction();

        if (!favorites.hasInstanceInList(active)) {
            quickActions.add(favorites);
        }

        quickActions.add(new ShowHidePoiAction());

        QuickAction voice = new NavigationVoiceAction();

        if (!voice.hasInstanceInList(active)) {

            quickActions.add(new QuickAction(0, R.string.quick_action_add_navigation));
            quickActions.add(voice);
        }

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

            case GPXAction.TYPE:
                return new GPXAction();

            case ParkingAction.TYPE:
                return new ParkingAction();

            case TakeAudioNoteAction.TYPE:
                return new TakeAudioNoteAction();

            case TakePhotoNoteAction.TYPE:
                return new TakePhotoNoteAction();

            case TakeVideoNoteAction.TYPE:
                return new TakeVideoNoteAction();

            case NavigationVoiceAction.TYPE:
                return new NavigationVoiceAction();

            case AddOSMBugAction.TYPE:
                return new AddOSMBugAction();

            case AddPOIAction.TYPE:
                return new AddPOIAction();

            case MapStyleAction.TYPE:
                return new MapStyleAction();

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

            case GPXAction.TYPE:
                return new GPXAction(quickAction);

            case ParkingAction.TYPE:
                return new ParkingAction(quickAction);

            case TakeAudioNoteAction.TYPE:
                return new TakeAudioNoteAction(quickAction);

            case TakePhotoNoteAction.TYPE:
                return new TakePhotoNoteAction(quickAction);

            case TakeVideoNoteAction.TYPE:
                return new TakeVideoNoteAction(quickAction);

            case NavigationVoiceAction.TYPE:
                return new NavigationVoiceAction(quickAction);

            case AddOSMBugAction.TYPE:
                return new AddOSMBugAction(quickAction);

            case AddPOIAction.TYPE:
                return new AddPOIAction(quickAction);

            case MapStyleAction.TYPE:
                return new MapStyleAction(quickAction);

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

                if (getParams().get(KEY_NAME).isEmpty() && Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)) == 0) {

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
        public boolean fillParams(View root, MapActivity activity) {

            getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
            getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));

            return true;
        }

        private void fillGroupParams(View root, String name, int color) {

            if (color == 0) color = R.color.color_favorite;

            ((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(name);
            ((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);

            getParams().put(KEY_CATEGORY_NAME, name);
            getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
        }
    }

    public static class ShowHideFavoritesAction extends QuickAction {

        public static final int TYPE = 4;

        protected ShowHideFavoritesAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_showhide_favorites_title;
            iconRes = R.drawable.ic_action_fav_dark;
            autogeneratedTitle = true;
        }

        public ShowHideFavoritesAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            activity.getMyApplication().getSettings().SHOW_FAVORITES.set(
                    !activity.getMyApplication().getSettings().SHOW_FAVORITES.get());

            activity.getMapLayers().updateLayers(activity.getMapView());
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_showhides_favorites_discr);

            parent.addView(view);
        }

        @Override
        public String getActionText(OsmandApplication application) {

            return application.getSettings().SHOW_FAVORITES.get()
                    ? application.getString(R.string.quick_action_favorites_hide)
                    : application.getString(R.string.quick_action_favorites_show);
        }

        @Override
        public boolean isActionWithSlash(OsmandApplication application) {

            return application.getSettings().SHOW_FAVORITES.get();
        }
    }

    public static class ShowHidePoiAction extends QuickAction {

        public static final int TYPE = 5;

        public static final String KEY_FILTERS = "filters";

        private transient EditText title;

        protected ShowHidePoiAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_showhide_poi_title;
            iconRes = R.drawable.ic_action_gabout_dark;
            autogeneratedTitle = true;
        }

        public ShowHidePoiAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public String getActionText(OsmandApplication application) {

            return !isCurrentFilters(application)
                    ? application.getString(R.string.quick_action_poi_show, getName(application))
                    : application.getString(R.string.quick_action_poi_hide, getName(application));
        }

        @Override
        public boolean isActionWithSlash(OsmandApplication application) {

            return isCurrentFilters(application);
        }

        @Override
        public void setAutoGeneratedTitle(EditText title) {
            this.title = title;
        }

        @Override
        public void execute(MapActivity activity) {

            PoiFiltersHelper pf = activity.getMyApplication().getPoiFilters();
            List<PoiUIFilter> poiFilters = loadPoiFilters(activity.getMyApplication().getPoiFilters());

            if (!isCurrentFilters(pf.getSelectedPoiFilters(), poiFilters)){

                pf.clearSelectedPoiFilters();

                for (PoiUIFilter filter : poiFilters) {
                    pf.addSelectedPoiFilter(filter);
                }

            } else pf.clearSelectedPoiFilters();



            activity.getMapLayers().updateLayers(activity.getMapView());
        }

        private boolean isCurrentFilters(OsmandApplication application) {

            PoiFiltersHelper pf = application.getPoiFilters();
            List<PoiUIFilter> poiFilters = loadPoiFilters(application.getPoiFilters());

            return isCurrentFilters(pf.getSelectedPoiFilters(), poiFilters);
        }

        private boolean isCurrentFilters(Set<PoiUIFilter> currentPoiFilters, List<PoiUIFilter> poiFilters){

            if (currentPoiFilters.size() != poiFilters.size()) return false;

            return currentPoiFilters.containsAll(poiFilters);
        }

        @Override
        public void drawUI(ViewGroup parent, final MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_show_hide_poi, parent, false);

            RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
            Button addFilter = (Button) view.findViewById(R.id.btnAddCategory);

            final Adapter adapter = new Adapter(!getParams().isEmpty()
                    ? loadPoiFilters(activity.getMyApplication().getPoiFilters())
                    : new ArrayList<PoiUIFilter>());

            list.setAdapter(adapter);

            addFilter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSingleChoicePoiFilterDialog(activity.getMyApplication(), activity, adapter);
                }
            });

            parent.addView(view);
        }

        public class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

            private List<PoiUIFilter> filters;

            public Adapter(List<PoiUIFilter> filters) {
                this.filters = filters;
            }

            private void addItem(PoiUIFilter filter) {

                if (!filters.contains(filter)) {

                    filters.add(filter);
                    savePoiFilters(filters);

                    notifyDataSetChanged();
                }
            }

            @Override
            public Holder onCreateViewHolder(ViewGroup parent, int viewType) {

                return new Holder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.quick_action_deletable_list_item, parent, false));
            }

            @Override
            public void onBindViewHolder(final Holder holder, final int position) {

                final PoiUIFilter filter = filters.get(position);

                Object res = filter.getIconResource();

                if (res instanceof String && RenderingIcons.containsBigIcon(res.toString())) {
                    holder.icon.setImageResource(RenderingIcons.getBigIconResourceId(res.toString()));
                } else {
                    holder.icon.setImageResource(R.drawable.mx_user_defined);
                }

                holder.title.setText(filter.getName());
                holder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String oldTitle = getTitle(filters);

                        filters.remove(position);
                        savePoiFilters(filters);

                        notifyDataSetChanged();

                        if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(getName(holder.title.getContext()))) {

                            String newTitle = getTitle(filters);
                            title.setText(newTitle);
                        }
                    }
                });
            }

            @Override
            public int getItemCount() {
                return filters.size();
            }

            class Holder extends RecyclerView.ViewHolder {

                private TextView title;
                private ImageView icon;
                private ImageView delete;

                public Holder(View itemView) {
                    super(itemView);

                    title = (TextView) itemView.findViewById(R.id.title);
                    icon = (ImageView) itemView.findViewById(R.id.icon);
                    delete = (ImageView) itemView.findViewById(R.id.delete);
                }
            }
        }

        public void savePoiFilters(List<PoiUIFilter> poiFilters) {

            List<String> filters = new ArrayList<>();

            for (PoiUIFilter f : poiFilters) {
                filters.add(f.getFilterId());
            }

            getParams().put(KEY_FILTERS, TextUtils.join(",", filters));
        }

        private List<PoiUIFilter> loadPoiFilters(PoiFiltersHelper helper) {

            List<String> filters = new ArrayList<>();

            String filtersId = getParams().get(KEY_FILTERS);

            if (filtersId != null && !filtersId.trim().isEmpty()) {
                Collections.addAll(filters, filtersId.split(","));
            }

            List<PoiUIFilter> poiFilters = new ArrayList<>();

            for (String f : filters) {

                PoiUIFilter filter = helper.getFilterById(f);

                if (filter != null) {
                    poiFilters.add(filter);
                }
            }

            return poiFilters;
        }

        private void showSingleChoicePoiFilterDialog(final OsmandApplication app, final MapActivity activity, final Adapter filtersAdapter) {

            final PoiFiltersHelper poiFilters = app.getPoiFilters();
            final ContextMenuAdapter adapter = new ContextMenuAdapter();

            adapter.addItem(new ContextMenuItem.ItemBuilder()
                    .setTitleId(R.string.shared_string_search, app)
                    .setIcon(R.drawable.ic_action_search_dark).createItem());

            final List<PoiUIFilter> list = new ArrayList<>();
            list.add(poiFilters.getCustomPOIFilter());

            for (PoiUIFilter f : poiFilters.getTopDefinedPoiFilters()) {
                addFilterToList(adapter, list, f);
            }
            for (PoiUIFilter f : poiFilters.getSearchPoiFilters()) {
                addFilterToList(adapter, list, f);
            }

            final ArrayAdapter<ContextMenuItem> listAdapter =
                    adapter.createListAdapter(activity, app.getSettings().isLightContent());
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String oldTitle = getTitle(filtersAdapter.filters);

                    filtersAdapter.addItem(list.get(which));

                    if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(getName(activity))) {

                        String newTitle = getTitle(filtersAdapter.filters);
                        title.setText(newTitle);
                    }
                }

            });
            builder.setTitle(R.string.show_poi_over_map);
            builder.setNegativeButton(R.string.shared_string_dismiss, null);

            final AlertDialog alertDialog = builder.create();

            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    Drawable drawable = app.getIconsCache().getThemedIcon(R.drawable.ic_action_multiselect);
                    neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                }
            });

            alertDialog.show();
        }

        private String getTitle(List<PoiUIFilter> filters) {

            if (filters.isEmpty()) return "";

            return filters.size() > 1
                    ? filters.get(0).getName() + " +" + (filters.size() - 1)
                    : filters.get(0).getName();
        }

        private void addFilterToList(final ContextMenuAdapter adapter,
                                     final List<PoiUIFilter> list,
                                     final PoiUIFilter f) {
            list.add(f);
            ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder();

            builder.setTitle(f.getName());

            if (RenderingIcons.containsBigIcon(f.getIconId())) {
                builder.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
            } else {
                builder.setIcon(R.drawable.mx_user_defined);
            }

            builder.setColor(ContextMenuItem.INVALID_ID);
            builder.setSkipPaintingWithoutColor(true);
            adapter.addItem(builder.createItem());
        }

        @Override
        public boolean fillParams(View root, MapActivity activity) {
            return !getParams().isEmpty() && (getParams().get(KEY_FILTERS) != null || !getParams().get(KEY_FILTERS).isEmpty());
        }
    }

    public static class GPXAction extends QuickAction {

        public static final int TYPE = 6;

        public static final String KEY_NAME = "name";
        public static final String KEY_DIALOG = "dialog";
        public static final String KEY_CATEGORY_NAME = "category_name";
        public static final String KEY_CATEGORY_COLOR = "category_color";

        private GPXAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_gpx;
            iconRes = R.drawable.ic_action_flag_dark;
        }

        public GPXAction(QuickAction quickAction) {
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
                                activity.getContextMenu().addWptPt(latLon, address,
                                        getParams().get(KEY_CATEGORY_NAME),
                                        Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)),
                                        !Boolean.valueOf(getParams().get(KEY_DIALOG)));
                            }

                        }, null);

                activity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);

            } else activity.getContextMenu().addWptPt(latLon, title,
                    getParams().get(KEY_CATEGORY_NAME),
                    Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)),
                    !Boolean.valueOf(getParams().get(KEY_DIALOG)));
        }

        @Override
        public void drawUI(final ViewGroup parent, final MapActivity activity) {

            final View root = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_add_gpx, parent, false);

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

                if (getParams().get(KEY_NAME).isEmpty() && Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)) == 0) {

                    categoryEdit.setText("");
                    categoryImage.setColorFilter(activity.getResources().getColor(R.color.icon_color));
                }

            } else {

                categoryEdit.setText("");
                categoryImage.setColorFilter(activity.getResources().getColor(R.color.icon_color));

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
        public boolean fillParams(View root, MapActivity activity) {

            getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
            getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));

            return true;
        }

        private void fillGroupParams(View root, String name, int color) {

            if (color == 0) color = R.color.icon_color;

            ((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(name);
            ((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);

            getParams().put(KEY_CATEGORY_NAME, name);
            getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
        }
    }

    public static class ParkingAction extends QuickAction {

        public static final int TYPE = 7;

        private ParkingAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_parking;
            iconRes = R.drawable.ic_action_parking_dark;
        }

        public ParkingAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            ParkingPositionPlugin plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);

            if (plugin != null) {

                LatLon latLon = activity.getMapView()
                        .getCurrentRotatedTileBox()
                        .getCenterLatLon();

                plugin.showAddParkingDialog(activity, latLon.getLatitude(), latLon.getLongitude());
            }
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_add_parking_discr);

            parent.addView(view);
        }
    }

    public static class TakeAudioNoteAction extends QuickAction {
        public static final int TYPE = 8;

        protected TakeAudioNoteAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_take_audio_note;
            iconRes = R.drawable.ic_action_micro_dark;
        }

        public TakeAudioNoteAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
            if (plugin != null)
                plugin.recordAudio(latLon.getLatitude(), latLon.getLongitude(), activity);
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_take_audio_note_discr);

            parent.addView(view);
        }
    }

    public static class TakeVideoNoteAction extends QuickAction {
        public static final int TYPE = 9;

        protected TakeVideoNoteAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_take_video_note;
            iconRes = R.drawable.ic_action_video_dark;
        }

        public TakeVideoNoteAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
            if (plugin != null)
                plugin.recordVideo(latLon.getLatitude(), latLon.getLongitude(), activity);
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_take_video_note_discr);

            parent.addView(view);
        }
    }

    public static class TakePhotoNoteAction extends QuickAction {
        public static final int TYPE = 10;

        protected TakePhotoNoteAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_take_photo_note;
            iconRes = R.drawable.ic_action_photo_dark;
        }

        public TakePhotoNoteAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
            if (plugin != null)
                plugin.takePhoto(latLon.getLatitude(), latLon.getLongitude(), activity, false);
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_take_photo_note_discr);

            parent.addView(view);
        }
    }

    public static class NavigationVoiceAction extends QuickAction {
        public static final int TYPE = 11;

        protected NavigationVoiceAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_navigation_voice;
            iconRes = R.drawable.ic_action_volume_up;
            autogeneratedTitle = true;
        }

        public NavigationVoiceAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            boolean voice = activity.getMyApplication().getSettings().VOICE_MUTE.get();

            activity.getMyApplication().getSettings().VOICE_MUTE.set(!voice);
            activity.getRoutingHelper().getVoiceRouter().setMute(!voice);
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_navigation_voice_discr);

            parent.addView(view);
        }

        @Override
        public String getActionText(OsmandApplication application) {

            return application.getSettings().VOICE_MUTE.get()
                    ? application.getString(R.string.quick_action_navigation_voice_off)
                    : application.getString(R.string.quick_action_navigation_voice_on);
        }

        @Override
        public boolean isActionWithSlash(OsmandApplication application) {

            return application.getSettings().VOICE_MUTE.get();
        }
    }

    public static class AddOSMBugAction extends QuickAction {
        public static final int TYPE = 12;

        protected AddOSMBugAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_osm_bug;
            iconRes = R.drawable.ic_action_bug_dark;
        }

        public AddOSMBugAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
            if (plugin != null)
                plugin.openOsmNote(activity, latLon.getLatitude(), latLon.getLongitude());
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_with_text, parent, false);

            ((TextView) view.findViewById(R.id.text)).setText(
                    R.string.quick_action_add_osm_bug_discr);

            parent.addView(view);
        }
    }


    public static class AddPOIAction extends QuickAction {
        public static final int TYPE = 13;
        public static final String KEY_TAG = "key_tag";

        protected AddPOIAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_add_poi;
            iconRes = R.drawable.ic_action_gabout_dark;
        }

        public AddPOIAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            LatLon latLon = activity.getMapView()
                    .getCurrentRotatedTileBox()
                    .getCenterLatLon();

            OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
            if (plugin != null) {
                EditPoiDialogFragment editPoiDialogFragment =
//                        EditPoiDialogFragment.createAddPoiInstance(latLon.getLatitude(), latLon.getLongitude(),
//                                activity.getMyApplication());
                    EditPoiDialogFragment.createInstance(new Node(latLon.getLatitude(), latLon.getLongitude(), -1), true, getTagsFromParams());
                editPoiDialogFragment.show(activity.getSupportFragmentManager(),
                        EditPoiDialogFragment.TAG);
            }
        }

        @Override
        public void drawUI(ViewGroup parent, MapActivity activity) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_add_poi_layout, parent, false);

            OsmandApplication application = activity.getMyApplication();
            Drawable deleteDrawable = application.getIconsCache().getPaintedIcon(R.drawable.ic_action_remove_dark,
                    activity.getResources().getColor(R.color.dash_search_icon_dark));

            LinearLayout editTagsLineaLayout =
                    (LinearLayout) view.findViewById(R.id.editTagsList);

            final MapPoiTypes mapPoiTypes = application.getPoiTypes();
            final TagAdapterLinearLayoutHack mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getTagsFromParams(), deleteDrawable);
            // It is possible to not restart initialization every time, and probably move initialization to appInit
            Map<String, PoiType> translatedTypes = mapPoiTypes.getAllTranslatedNames(true);
            HashSet<String>      tagKeys         = new HashSet<>();
            HashSet<String>      valueKeys       = new HashSet<>();
            for (AbstractPoiType abstractPoiType : translatedTypes.values()) {
                addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
            }
            addPoiToStringSet(mapPoiTypes.getOtherMapCategory(), tagKeys, valueKeys);
            tagKeys.addAll(EditPoiDialogFragment.BASIC_TAGS);
            mAdapter.setTagData(tagKeys.toArray(new String[tagKeys.size()]));
            mAdapter.setValueData(valueKeys.toArray(new String[valueKeys.size()]));
            Button addTagButton = (Button) view.findViewById(R.id.addTagButton);
            addTagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAdapter.addTagView("", "");
                }
            });

            mAdapter.updateViews();

            parent.addView(view);
        }

        private class TagAdapterLinearLayoutHack {
            private final LinearLayout         linearLayout;
            private final Map<String, String>  tagsData;
            private final ArrayAdapter<String> tagAdapter;
            private final ArrayAdapter<String> valueAdapter;
            private final Drawable             deleteDrawable;

            public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
                                              Map<String, String> tagsData,
                                              Drawable deleteDrawable) {
                this.linearLayout = linearLayout;
                this.tagsData = tagsData;
                this.deleteDrawable = deleteDrawable;

                tagAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
                valueAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
            }

            public void updateViews() {
                linearLayout.removeAllViews();
                List<Entry<String, String>> entries = new ArrayList<>(tagsData.entrySet());
                for (Entry<String, String> tag : entries) {
                    if (tag.getKey().equals(EditPoiData.POI_TYPE_TAG)
                            || tag.getKey().equals(OSMSettings.OSMTagKey.NAME.getValue()))
                        continue;
                    addTagView(tag.getKey(), tag.getValue());
                }
            }

            public void addTagView(String tg, String vl) {
                View convertView = LayoutInflater.from(linearLayout.getContext())
                        .inflate(R.layout.poi_tag_list_item, null, false);
                final AutoCompleteTextView tagEditText =
                        (AutoCompleteTextView) convertView.findViewById(R.id.tagEditText);
                ImageButton deleteItemImageButton =
                        (ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
                deleteItemImageButton.setImageDrawable(deleteDrawable);
                final String[] previousTag = new String[]{tg};
                deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        linearLayout.removeView((View) v.getParent());
                        tagsData.remove(tagEditText.getText().toString());
                        setTagsIntoParams(tagsData);
                    }
                });
                final AutoCompleteTextView valueEditText =
                        (AutoCompleteTextView) convertView.findViewById(R.id.valueEditText);
                tagEditText.setText(tg);
                tagEditText.setAdapter(tagAdapter);
                tagEditText.setThreshold(1);
                tagEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            String s = tagEditText.getText().toString();
                            tagsData.remove(previousTag[0]);
                            tagsData.put(s.toString(), valueEditText.getText().toString());
                            previousTag[0] = s.toString();
                            setTagsIntoParams(tagsData);
                        } else {
                            tagAdapter.getFilter().filter(tagEditText.getText());
                        }
                    }
                });

                valueEditText.setText(vl);
                valueEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        tagsData.put(tagEditText.getText().toString(), s.toString());
                        setTagsIntoParams(tagsData);
                    }
                });

                initAutocompleteTextView(valueEditText, valueAdapter);

                linearLayout.addView(convertView);
                tagEditText.requestFocus();
            }

            public void setTagData(String[] tags) {
                tagAdapter.clear();
                for (String s : tags) {
                    tagAdapter.add(s);
                }
                tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
                tagAdapter.notifyDataSetChanged();
            }

            public void setValueData(String[] values) {
                valueAdapter.clear();
                for (String s : values) {
                    valueAdapter.add(s);
                }
                valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
                valueAdapter.notifyDataSetChanged();
            }
        }

        private static void initAutocompleteTextView(final AutoCompleteTextView textView,
                                                     final ArrayAdapter<String> adapter) {

            textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        adapter.getFilter().filter(textView.getText());
                    }
                }
            });
        }

        @Override
        public boolean fillParams(View root, MapActivity activity) {
            return !getParams().isEmpty() && (getParams().get(KEY_TAG) != null || !getTagsFromParams().isEmpty());
        }

        private Map<String, String> getTagsFromParams() {
            Map<String, String> quickActions = null;
            if (getParams().get(KEY_TAG) != null) {
                String json = getParams().get(KEY_TAG);
                Type type = new TypeToken<LinkedHashMap<String, String>>() {
                }.getType();
                quickActions = new Gson().fromJson(json, type);
            }
            return quickActions != null ? quickActions : new LinkedHashMap<String, String>();
        }

        private void setTagsIntoParams(Map<String, String> tags) {
            getParams().put(KEY_TAG, new Gson().toJson(tags));
        }
    }

    public static class MapStyleAction extends SwitchableAction {

        public static final int TYPE = 14;

        private static String KEY_STYLES = "styles";

        protected MapStyleAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_map_style;
            iconRes = R.drawable.ic_map;
        }

        public MapStyleAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

            List<String> mapStyles = getFilteredStyles();

            String curStyle = activity.getMyApplication().getSettings().RENDERER.get();
            int index = mapStyles.indexOf(curStyle);
            String nextStyle = mapStyles.get(0);

            if (index >= 0 && index + 1 < mapStyles.size()){
                nextStyle = mapStyles.get(index + 1);
            }

            RenderingRulesStorage loaded = activity.getMyApplication()
                    .getRendererRegistry().getRenderer(nextStyle);

            if (loaded != null) {

                OsmandMapTileView view = activity.getMapView();
                view.getSettings().RENDERER.set(nextStyle);

                activity.getMyApplication().getRendererRegistry().setCurrentSelectedRender(loaded);
                ConfigureMapMenu.refreshMapComplete(activity);

                Toast.makeText(activity, activity.getString(R.string.quick_action_map_style_switch, nextStyle), Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(activity, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
            }
        }

        protected List<String> getFilteredStyles(){

            List<String> filtered = new ArrayList<>();
            boolean enabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) != null;

            if (enabled) return loadMapStyles();
            else {

                for (String style : loadMapStyles()) {

                    if (!style.equals(RendererRegistry.NAUTICAL_RENDER)){
                        filtered.add(style);
                    }
                }
            }

            return filtered;
        }


        @Override
        protected int getAddBtnText() {
            return R.string.quick_action_map_style_action;
        }

        @Override
        protected int getDiscrHint() {
            return R.string.quick_action_map_style_discrp;
        }

        @Override
        protected int getDiscrTitle() {
            return R.string.quick_action_map_styles;
        }

        @Override
        protected String getListKey() {
            return KEY_STYLES;
        }

        @Override
        protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder bld = new AlertDialog.Builder(activity);
                    bld.setTitle(R.string.renderers);

                    final OsmandApplication app = activity.getMyApplication();
                    final List<String> visibleNamesList = new ArrayList<>();
                    final Collection<String> rendererNames = app.getRendererRegistry().getRendererNames();
                    final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
                    final boolean nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;

                    for (String item : items) {

                        if (nauticalPluginDisabled && item.equals(RendererRegistry.NAUTICAL_RENDER)) {
                            continue;
                        }

                        visibleNamesList.add(item.replace('_', ' ').replace('-', ' '));
                    }

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.dialog_text_item);

                    arrayAdapter.addAll(visibleNamesList);
                    bld.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            String renderer = visibleNamesList.get(i);
                            RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);

                            if (loaded != null) {

                                adapter.addItem(renderer, activity);
                            }

                            dialogInterface.dismiss();
                        }
                    });

                    bld.setNegativeButton(R.string.shared_string_dismiss, null);
                    bld.show();
                }
            };
        }
    }

    public static class MapOverlayAction extends SwitchableAction {

        public static final int TYPE = 15;

        private static String KEY_OVERLAYS = "overlays";

        protected MapOverlayAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_map_overlay;
            iconRes = R.drawable.ic_layer_top_dark;
        }

        public MapOverlayAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

        }

        @Override
        protected int getAddBtnText() {
            return R.string.quick_action_map_overlay_action;
        }

        @Override
        protected int getDiscrHint() {
            return R.string.quick_action_map_overlay_dscr;
        }

        @Override
        protected int getDiscrTitle() {
            return R.string.quick_action_map_overlay_title;
        }

        @Override
        protected String getListKey() {
            return KEY_OVERLAYS;
        }

        @Override
        protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            };
        }
    }


    public static class MapUnderlayAction extends SwitchableAction {

        public static final int TYPE = 16;

        private static String KEY_UNDERLAYS = "underlays";

        protected MapUnderlayAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_map_underlay;
            iconRes = R.drawable.ic_layer_bottom_dark;
        }

        public MapUnderlayAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

        }

        @Override
        protected int getAddBtnText() {
            return R.string.quick_action_map_underlay_action;
        }

        @Override
        protected int getDiscrHint() {
            return R.string.quick_action_map_underlay_dscr;
        }

        @Override
        protected int getDiscrTitle() {
            return R.string.quick_action_map_underlay_title;
        }

        @Override
        protected String getListKey() {
            return KEY_UNDERLAYS;
        }

        @Override
        protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            };
        }
    }

    public static class MapSourceAction extends SwitchableAction {

        public static final int TYPE = 17;

        private static String KEY_SOURCE = "source";

        protected MapSourceAction() {
            id = System.currentTimeMillis();
            type = TYPE;
            nameRes = R.string.quick_action_map_source;
            iconRes = R.drawable.ic_map;
        }

        public MapSourceAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void execute(MapActivity activity) {

        }

        @Override
        protected int getAddBtnText() {
            return R.string.quick_action_map_source_action;
        }

        @Override
        protected int getDiscrHint() {
            return R.string.quick_action_map_source_dscr;
        }

        @Override
        protected int getDiscrTitle() {
            return R.string.quick_action_map_source_title;
        }

        @Override
        protected String getListKey() {
            return KEY_SOURCE;
        }

        @Override
        protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            };
        }
    }

    protected static abstract class SwitchableAction extends QuickAction{

        private transient EditText title;

        public SwitchableAction() {
        }

        public SwitchableAction(QuickAction quickAction) {
            super(quickAction);
        }

        @Override
        public void setAutoGeneratedTitle(EditText title) {
            this.title = title;
        }

        private String getTitle(List<String> filters) {

            if (filters.isEmpty()) return "";

            return filters.size() > 1
                    ? filters.get(0) + " +" + (filters.size() - 1)
                    : filters.get(0);
        }

        @Override
        public void drawUI(ViewGroup parent, final MapActivity activity) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quick_action_switchable_action, parent, false);

            final RecyclerView list = (RecyclerView) view.findViewById(R.id.list);

            final QuickActionItemTouchHelperCallback touchHelperCallback = new QuickActionItemTouchHelperCallback();
            final ItemTouchHelper touchHelper =  new ItemTouchHelper(touchHelperCallback);

            final Adapter adapter = new Adapter(new QuickActionListFragment.OnStartDragListener() {
                @Override
                public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                    touchHelper.startDrag(viewHolder);
                }
            });

            touchHelperCallback.setItemMoveCallback(adapter);
            touchHelper.attachToRecyclerView(list);

            if (!getParams().isEmpty()){
                adapter.addItems(loadMapStyles());
            }

            list.setAdapter(adapter);

            TextView dscrTitle = (TextView) view.findViewById(R.id.textDscrTitle);
            TextView dscrHint = (TextView) view.findViewById(R.id.textDscrHint);
            Button addBtn = (Button) view.findViewById(R.id.btnAdd);

            dscrTitle.setText(getDiscrTitle());
            dscrHint.setText(getDiscrHint());
            addBtn.setText(getAddBtnText());
            addBtn.setOnClickListener(getOnAddBtnClickListener(activity, adapter));

            parent.addView(view);
        }

        @Override
        public boolean fillParams(View root, MapActivity activity) {
            return !getParams().isEmpty() && (getParams().get(getListKey()) != null || !getParams().get(getListKey()).isEmpty());
        }

        protected class Adapter extends RecyclerView.Adapter<Adapter.ItemHolder> implements QuickActionItemTouchHelperCallback.OnItemMoveCallback {

            private List<String> itemsList = new ArrayList<>();
            private final QuickActionListFragment.OnStartDragListener onStartDragListener;

            public Adapter(QuickActionListFragment.OnStartDragListener onStartDragListener) {
                this.onStartDragListener = onStartDragListener;
                this.itemsList = new ArrayList<>();
            }

            @Override
            public Adapter.ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new Adapter.ItemHolder(inflater.inflate(R.layout.quick_action_switchable_item, parent, false));
            }

            @Override
            public void onBindViewHolder(final Adapter.ItemHolder holder, final int position) {
                final String item = itemsList.get(position);

                holder.title.setText(item);

                holder.handleView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEventCompat.getActionMasked(event) ==
                                MotionEvent.ACTION_DOWN) {
                            onStartDragListener.onStartDrag(holder);
                        }
                        return false;
                    }
                });

                holder.closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String oldTitle = getTitle(itemsList);

                        deleteItem(position);

                        if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(getName(holder.handleView.getContext()))) {

                            String newTitle = getTitle(itemsList);
                            title.setText(newTitle);
                        }
                    }
                });
            }

            @Override
            public int getItemCount() {
                return itemsList.size();
            }

            public void deleteItem(int position) {

                if (position == -1)
                    return;

                itemsList.remove(position);

                saveMapStyles(itemsList);
                notifyItemRemoved(position);
            }

            public void addItems(List<String> data) {

                if (!itemsList.containsAll(data)) {

                    itemsList.addAll(data);

                    saveMapStyles(itemsList);
                    notifyDataSetChanged();
                }
            }

            public void addItem(String item, Context context) {

                if (!itemsList.contains(item)) {

                    String oldTitle = getTitle(itemsList);

                    int oldSize = itemsList.size();
                    itemsList.add(item);

                    saveMapStyles(itemsList);
                    notifyItemRangeInserted(oldSize, itemsList.size() - oldSize);

                    if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(getName(context))) {

                        String newTitle = getTitle(itemsList);
                        title.setText(newTitle);
                    }
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

                int selectedPosition = viewHolder.getAdapterPosition();
                int targetPosition = target.getAdapterPosition();

                if (selectedPosition < 0 || targetPosition < 0) {
                    return false;
                }

                String oldTitle = getTitle(itemsList);

                Collections.swap(itemsList, selectedPosition, targetPosition);
                if (selectedPosition - targetPosition < -1) {

                    notifyItemMoved(selectedPosition, targetPosition);
                    notifyItemMoved(targetPosition - 1, selectedPosition);

                } else if (selectedPosition - targetPosition > 1) {

                    notifyItemMoved(selectedPosition, targetPosition);
                    notifyItemMoved(targetPosition + 1, selectedPosition);

                } else {

                    notifyItemMoved(selectedPosition, targetPosition);
                }

                notifyItemChanged(selectedPosition);
                notifyItemChanged(targetPosition);

                if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(getName(recyclerView.getContext()))) {

                    String newTitle = getTitle(itemsList);
                    title.setText(newTitle);
                }

                return true;
            }

            @Override
            public void onViewDropped(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                saveMapStyles(itemsList);
            }

            public class ItemHolder extends RecyclerView.ViewHolder {
                public TextView title;
                public ImageView handleView;
                public ImageView closeBtn;

                public ItemHolder(View itemView) {
                    super(itemView);

                    title = (TextView) itemView.findViewById(R.id.title);
                    handleView = (ImageView) itemView.findViewById(R.id.handle_view);
                    closeBtn = (ImageView) itemView.findViewById(R.id.closeImageButton);
                }
            }
        }

        protected void saveMapStyles(List<String> styles) {
            getParams().put(getListKey(), TextUtils.join(",", styles));
        }

        protected List<String> loadMapStyles() {

            List<String> styles = new ArrayList<>();

            String filtersId = getParams().get(getListKey());

            if (filtersId != null && !filtersId.trim().isEmpty()) {
                Collections.addAll(styles, filtersId.split(","));
            }

            return styles;
        }

        protected abstract @StringRes int getAddBtnText();
        protected abstract @StringRes int getDiscrHint();
        protected abstract @StringRes int getDiscrTitle();
        protected abstract String getListKey();
        protected abstract View.OnClickListener getOnAddBtnClickListener(MapActivity activity, final Adapter adapter);
    }
}
