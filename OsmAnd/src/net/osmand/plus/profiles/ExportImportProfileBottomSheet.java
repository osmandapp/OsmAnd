package net.osmand.plus.profiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportImportProfileBottomSheet extends BasePreferenceBottomSheet {

    private static final Log LOG = PlatformUtil.getLog(ExportImportProfileBottomSheet.class);

    public static final String TAG = ExportImportProfileBottomSheet.class.getSimpleName();

    private static final String STATE_KEY = "EXPORT_IMPORT_DIALOG_STATE_KEY";

    private OsmandApplication app;

    private Context context;

    private OsmandSettings settings;

    private ApplicationMode profile;

    private boolean includeAdditionalData = false;

    private boolean containsAdditionalData = false;

    private State state;

    private List<AdditionalDataWrapper> dataList = new ArrayList<>();

    private ExpandableListView listView;

    private ProfileAdditionalDataAdapter adapter;

    private List<? super Object> dataToOperate = new ArrayList<>();

    private List<SettingsHelper.SettingsItem> settingsItems;

    private File file;

    private SettingsHelper.ProfileSettingsItem profileSettingsItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = requiredMyApplication();
        context = requireContext();
        settings = app.getSettings();
        Bundle bundle = getArguments();
        if (bundle != null) {
            this.state = (State) getArguments().getSerializable(STATE_KEY);
        }
        if (state == State.IMPORT) {
            containsAdditionalData = checkAdditionalDataContains();
        } else {
            dataList = getAdditionalData();
            for (AdditionalDataWrapper dataWrapper : dataList) {
                dataToOperate.addAll(dataWrapper.getItems());
            }
        }
//        dataList = state == State.IMPORT ? getDataFromSettingsItems() : getAdditionalData();
    }

    private ApplicationMode getAppModeFromItems() {
        for (SettingsHelper.SettingsItem item : settingsItems) {
            if (item.getType().equals(SettingsHelper.SettingsItemType.PROFILE)) {
                profileSettingsItem = ((SettingsHelper.ProfileSettingsItem) item);
                return ((SettingsHelper.ProfileSettingsItem) item).getAppMode();
            }
        }
        return getAppMode();
    }

    @Override
    public void createMenuItems(Bundle savedInstanceState) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        profile = state == State.IMPORT ? getAppModeFromItems() : getAppMode();

        int profileColor = profile.getIconColorInfo().getColor(nightMode);
        int colorNoAlpha = ContextCompat.getColor(context, profileColor);

        Drawable backgroundIcon = UiUtilities.getColoredSelectableDrawable(context, colorNoAlpha, 0.3f);
        Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.10f)), backgroundIcon};

        items.add(new TitleItem(state == State.EXPORT ?
                getString(R.string.export_profile)
                : getString(R.string.import_profile)));

        BaseBottomSheetItem profileItem = new BottomSheetItemWithCompoundButton.Builder()
                .setChecked(true)
                .setCompoundButtonColorId(profileColor)
                .setButtonTintList(ColorStateList.valueOf(getResolvedColor(profileColor)))
                .setDescription(BaseSettingsFragment.getAppModeDescription(context, profile))
                .setIcon(getIcon(profile.getIconRes(), profileColor))
                .setTitle(profile.toHumanString(context))
                .setBackground(new LayerDrawable(layers))
                .setLayoutId(R.layout.preference_profile_item_with_radio_btn)
                .create();
        items.add(profileItem);

        if (state == State.IMPORT && containsAdditionalData || state == State.EXPORT && !dataList.isEmpty()) {
            BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
                    .setDescription(state == State.EXPORT ?
                            getString(R.string.export_profile_dialog_description)
                            : getString(R.string.import_profile_dialog_description))
                    .setLayoutId(R.layout.bottom_sheet_item_pref_info)
                    .create();
            items.add(descriptionItem);

            final View additionalDataView = View.inflate(new ContextThemeWrapper(context, themeRes),
                    R.layout.bottom_sheet_item_additional_data, null);
            listView = additionalDataView.findViewById(R.id.list);
            Switch switchItem = additionalDataView.findViewById(R.id.switchItem);
            switchItem.setTextColor(getResources().getColor(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light));

            switchItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    includeAdditionalData = !includeAdditionalData;
                    listView.setVisibility(includeAdditionalData ?
                            View.VISIBLE : View.GONE);
                    if (includeAdditionalData && state == State.IMPORT) {
                        updateDataFromSettingsItems();
                    }
                }
            });
            adapter = new ProfileAdditionalDataAdapter(requiredMyApplication(), dataList, profileColor);
            listView.setAdapter(adapter);
            final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
                    .setCustomView(additionalDataView)
                    .create();
            items.add(titleItem);
        }
    }

    private void updateDataFromSettingsItems() {

        List<AdditionalDataWrapper> dataList = new ArrayList<>();
        List<QuickAction> quickActions = new ArrayList<>();
        List<PoiUIFilter> poiUIFilters = new ArrayList<>();
        for (SettingsHelper.SettingsItem item : settingsItems) {
            if (item.getType().equals(SettingsHelper.SettingsItemType.QUICK_ACTION_LIST)) {
                        quickActions.addAll(((SettingsHelper.QuickActionSettingsItem) item).getQuickActions());
            } else if (item.getType().equals(SettingsHelper.SettingsItemType.POI_UI_FILTERS_LIST)) {
//                poiUIFilters.addAll()
            }
        }
        if (!quickActions.isEmpty()) {
            dataList.add(new AdditionalDataWrapper(
                    AdditionalDataWrapper.Type.QUICK_ACTIONS,
                    quickActions));
        }
        if (!poiUIFilters.isEmpty()) {
            dataList.add(new AdditionalDataWrapper(
                    AdditionalDataWrapper.Type.POI_TYPES,
                    poiUIFilters));
        }
        for (AdditionalDataWrapper dataWrapper : dataList) {
            dataToOperate.addAll(dataWrapper.getItems());
        }
        adapter.updateList(dataList);

//        app.getSettingsHelper().importSettings(settingsItems, file, new SettingsHelper.SettingsImportListener() {
//            @Override
//            public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsHelper.SettingsItem> items) {
//
//            }
//        });
    }

    private Boolean checkAdditionalDataContains() {
        boolean containsData = false;
        for (SettingsHelper.SettingsItem item : settingsItems) {
            containsData = item.getType().equals(SettingsHelper.SettingsItemType.QUICK_ACTION_LIST)
                    || item.getType().equals(SettingsHelper.SettingsItemType.POI_UI_FILTERS_LIST)
                    || item.getType().equals(SettingsHelper.SettingsItemType.MAP_SOURCES_LIST);
            if (containsData) {
                break;
            }
        }
        return containsData;
    }

    private List<AdditionalDataWrapper> getAdditionalData() {
        List<AdditionalDataWrapper> dataList = new ArrayList<>();

        QuickActionFactory factory = new QuickActionFactory();
        List<QuickAction> quickActions = factory.parseActiveActionsList(settings.QUICK_ACTION_LIST.get());
        if (!quickActions.isEmpty()) {
            dataList.add(new AdditionalDataWrapper(
                    AdditionalDataWrapper.Type.QUICK_ACTIONS, quickActions));
        }

        List<PoiUIFilter> poiList = app.getPoiFilters().getUserDefinedPoiFilters(false);
        if (!poiList.isEmpty()) {
            dataList.add(new AdditionalDataWrapper(
                    AdditionalDataWrapper.Type.POI_TYPES,
                    poiList
            ));
        }

        final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>(settings.getTileSourceEntries());
        List<MapSourceWrapper> mapSourceWrapperList = new ArrayList<>();
        for (Map.Entry<String, String> entry : entriesMap.entrySet()) {
            mapSourceWrapperList.add(new MapSourceWrapper(entry.getKey(), entry.getValue()));
        }
        if (!mapSourceWrapperList.isEmpty()) {
            dataList.add(new AdditionalDataWrapper(
                    AdditionalDataWrapper.Type.MAP_SOURCES,
                    mapSourceWrapperList
            ));
        }

        return dataList;
    }

    public void setSettingsItems(List<SettingsHelper.SettingsItem> settingsItems) {
        this.settingsItems = settingsItems;
    }

    @Override
    protected int getRightBottomButtonTextId() {
        return state == State.EXPORT ? R.string.shared_string_export : R.string.shared_string_import;
    }

    @Override
    protected void onRightBottomButtonClick() {
        super.onRightBottomButtonClick();
        if (state == State.EXPORT) {
            prepareFile();
        } else {
            importSettings();
        }
    }

    private void importSettings() {
        if (includeAdditionalData) {

        } else {
         List<SettingsHelper.SettingsItem> list = new ArrayList<>();
         list.add(profileSettingsItem);
            app.getSettingsHelper().importSettings(list, file, new SettingsHelper.SettingsImportListener() {

                @Override
                public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsHelper.SettingsItem> items) {
                    if (succeed) {
                        app.showShortToastMessage(app.getString(R.string.file_imported_successfully, ""));
                    } else if (!empty) {
                        app.showShortToastMessage(app.getString(R.string.file_import_error, "", app.getString(R.string.shared_string_unexpected_error)));
                    }
                }
            });
        }
        dismiss();
    }

    @Override
    protected int getDismissButtonTextId() {
        return R.string.shared_string_cancel;
    }

    @Override
    protected boolean useScrollableItemsContainer() {
        return false;
    }

    private List<SettingsHelper.SettingsItem> prepareSettingsItems() {
        List<SettingsHelper.SettingsItem> settingsItems = new ArrayList<>();
        settingsItems.add(new SettingsHelper.ProfileSettingsItem(app.getSettings(), profile));

        if (includeAdditionalData) {
            List<QuickAction> quickActions = new ArrayList<>();
            List<PoiUIFilter> poiUIFilters = new ArrayList<>();
            List<MapSourceWrapper> mapSourceWrappers = new ArrayList<>();
            for (Object object : dataToOperate) {
                if (object instanceof QuickAction) {
                    quickActions.add((QuickAction) object);
                } else if (object instanceof PoiUIFilter) {
                    poiUIFilters.add((PoiUIFilter) object);
                } else if (object instanceof MapSourceWrapper) {
                    mapSourceWrappers.add((MapSourceWrapper) object);
                }
            }
            if (!quickActions.isEmpty()) {
                settingsItems.add(new SettingsHelper.QuickActionSettingsItem(app, quickActions));
            }
            if (!poiUIFilters.isEmpty()) {
                settingsItems.add(new SettingsHelper.PoiUiFilterSettingsItem(app.getSettings(), poiUIFilters));
            }
            if (!mapSourceWrappers.isEmpty()) {
                settingsItems.add(new SettingsHelper.MapSourcesSettingsItem(app.getSettings(), mapSourceWrappers));
            }
        }
        return settingsItems;
    }

    private void prepareFile() {
        if (app != null) {
            File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            String fileName = profile.toHumanString(context);
            app.getSettingsHelper().exportSettings(tempDir, fileName, new SettingsHelper.SettingsExportListener() {
                @Override
                public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
                    if (succeed) {
                        shareProfile(file, profile);
                    } else {
                        app.showToastMessage(R.string.export_profile_failed);
                    }
                }
            }, prepareSettingsItems());
        }
    }

    private void shareProfile(@NonNull File file, @NonNull ApplicationMode profile) {
        try {
            Context ctx = requireContext();
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.exported_osmand_profile, profile.toHumanString(ctx)));
            sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), file));
            sendIntent.setType("*/*");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(sendIntent);
            dismiss();
        } catch (Exception e) {
            Toast.makeText(context, R.string.export_profile_failed, Toast.LENGTH_SHORT).show();
            LOG.error("Share profile error", e);
        }
    }

    public static boolean showInstance(@NonNull FragmentManager fragmentManager,
                                       State state,
                                       Fragment target,
                                       @NonNull ApplicationMode appMode) {
        try {
            Bundle bundle = new Bundle();
            bundle.putSerializable(STATE_KEY, state);
            ExportImportProfileBottomSheet fragment = new ExportImportProfileBottomSheet();
            fragment.setArguments(bundle);
            fragment.setAppMode(appMode);
            fragment.setTargetFragment(target, 0);
            fragment.show(fragmentManager, TAG);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean showInstance(@NonNull FragmentManager fragmentManager,
                                       State state,
                                       File file,
                                       List<SettingsHelper.SettingsItem> items) {
        try {
            Bundle bundle = new Bundle();
            bundle.putSerializable(STATE_KEY, state);
            ExportImportProfileBottomSheet fragment = new ExportImportProfileBottomSheet();
            fragment.setArguments(bundle);
            fragment.setSettingsItems(items);
            fragment.setFile(file);
            fragment.show(fragmentManager, TAG);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public enum State {
        EXPORT,
        IMPORT
    }

    boolean isContainsInDataToOperate(AdditionalDataWrapper.Type type, Object object) {
        return dataToOperate.contains(object);

//        for (AdditionalDataWrapper data : dataList) {
//            if (data.getType() == type) {
//                return data.getItems().contains(object);
//            }
//        }
//        return false;
    }

    class ProfileAdditionalDataAdapter extends OsmandBaseExpandableListAdapter {

        private OsmandApplication app;

        private List<AdditionalDataWrapper> list;

        private int profileColor;


        ProfileAdditionalDataAdapter(OsmandApplication app, List<AdditionalDataWrapper> list, int profileColor) {
            this.app = app;
            this.list = list;
            this.profileColor = profileColor;
        }

        public void updateList(List<AdditionalDataWrapper> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getGroupCount() {
            return list.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return list.get(i).getItems().size();
        }

        @Override
        public Object getGroup(int i) {
            return list.get(i);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return list.get(groupPosition).getItems().get(childPosition);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition * 10000 + childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View group = convertView;
            if (group == null) {
                LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                group = inflater.inflate(R.layout.profile_data_list_item_group, parent, false);
            }


            boolean isLastGroup = groupPosition == getGroupCount() - 1;
            final AdditionalDataWrapper.Type type = list.get(groupPosition).getType();

            TextView titleTv = group.findViewById(R.id.title_tv);
            TextView subTextTv = group.findViewById(R.id.sub_text_tv);
            final CheckBox checkBox = group.findViewById(R.id.check_box);
            ImageView expandIv = group.findViewById(R.id.explist_indicator);
            View divider = group.findViewById(R.id.divider);

            titleTv.setText(getGroupTitle(type));
            divider.setVisibility(isExpanded || isLastGroup ? View.GONE : View.VISIBLE);
            CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, profileColor)));

            final List<?> listItems = list.get(groupPosition).getItems();
            subTextTv.setText(String.valueOf(listItems.size()));

            checkBox.setChecked(dataToOperate.containsAll(listItems));
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (checkBox.isChecked()) {
                        for (Object object : listItems) {
                            if (!isContainsInDataToOperate(type, object)) {
                                dataToOperate.add(object);
                            }
                        }
                    } else {
                        dataToOperate.removeAll(listItems);
                    }
                    notifyDataSetInvalidated();
                }
            });

            adjustIndicator(app, groupPosition, isExpanded, group, true);

            return group;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View child = convertView;
            if (child == null) {
                LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                child = inflater.inflate(R.layout.profile_data_list_item_child, parent, false);
            }
            final Object currentItem = list.get(groupPosition).getItems().get(childPosition);


            boolean isLastGroup = groupPosition == getGroupCount() - 1;
            final AdditionalDataWrapper.Type type = list.get(groupPosition).getType();

            TextView title = child.findViewById(R.id.title_tv);
            final CheckBox checkBox = child.findViewById(R.id.check_box);
            ImageView icon = child.findViewById(R.id.icon);
            View divider = child.findViewById(R.id.divider);

            divider.setVisibility(isLastChild && !isLastGroup ? View.VISIBLE : View.GONE);
            CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, profileColor)));

            checkBox.setChecked(isContainsInDataToOperate(type, currentItem));
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (checkBox.isChecked()) {
                        dataToOperate.add(currentItem);
                    } else {
                        dataToOperate.remove(currentItem);
                    }
                }
            });

            switch (type) {
                case QUICK_ACTIONS:
                    title.setText(((QuickAction) currentItem).getName(app.getApplicationContext()));
                    icon.setVisibility(View.INVISIBLE);
                    icon.setImageResource(R.drawable.ic_action_info_dark);
                    break;
                case POI_TYPES:
                    title.setText(((PoiUIFilter) currentItem).getName());
                    icon.setVisibility(View.VISIBLE);
                    int iconRes = RenderingIcons.getBigIconResourceId(((PoiUIFilter) currentItem).getIconId());
                    icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes != 0 ? iconRes : R.drawable.ic_person, profileColor));
                    break;
                case MAP_SOURCES:
                    title.setText(((MapSourceWrapper) currentItem).getName());
                    icon.setVisibility(View.INVISIBLE);
                    icon.setImageResource(R.drawable.ic_action_info_dark);
                    break;
                default:
                    return child;
            }
            return child;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return false;
        }

        private int getGroupTitle(AdditionalDataWrapper.Type type) {
            switch (type) {
                case QUICK_ACTIONS:
                    return R.string.configure_screen_quick_action;
                case POI_TYPES:
                    return R.string.poi_dialog_poi_type;
                case MAP_SOURCES:
                    return R.string.quick_action_map_source_title;
                case CUSTOM_RENDER_STYLE:
                    return R.string.shared_string_custom_rendering_style;
                case ROUTING:
                    return R.string.shared_string_routing;
                default:
                    return R.string.access_empty_list;
            }
        }
    }

    public class MapSourceWrapper {
        private String name;
        private String url;

        MapSourceWrapper(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }
    }
}
