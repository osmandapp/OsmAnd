package net.osmand.plus.search;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.render.RenderingIcons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public class QuickSearchSubCategoriesFragment extends BaseOsmAndDialogFragment {

    public static final String TAG = QuickSearchSubCategoriesFragment.class.getName();
    private static final String CATEGORY_NAME_KEY = "category_key";
    private static final String ALL_SELECTED_KEY = "all_selected";
    private OnFiltersSelectedListener listener;
    private OsmandApplication app;
    private UiUtilities uiUtilities;
    private PoiCategory poiCategory;
    private List<PoiType> poiTypeList;
    private List<PoiType> selectedPoiTypeList;
    private SubCategoriesAdapter adapter;
    private EditText searchEditText;
    private ListView listView;
    private View headerSelectAll;
    private View headerShadow;
    private View footerShadow;
    private boolean selectAll;
    private boolean nightMode;


    public static void showInstance(@NonNull FragmentManager fm, @NonNull PoiCategory poiCategory,
                                    boolean selectAll, @NonNull OnFiltersSelectedListener listener) {
        QuickSearchSubCategoriesFragment fragment = new QuickSearchSubCategoriesFragment();
        fragment.setPoiCategory(poiCategory);
        fragment.setSelectAll(selectAll);
        fragment.setListener(listener);
        fragment.show(fm, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = getMyApplication();
        uiUtilities = app.getUIUtilities();
        nightMode = !app.getSettings().isLightContent();
        selectedPoiTypeList = new ArrayList<>();
        if (savedInstanceState != null) {
            poiCategory = app.getPoiTypes().getPoiCategoryByName(savedInstanceState.getString(CATEGORY_NAME_KEY));
            selectAll = savedInstanceState.getBoolean(ALL_SELECTED_KEY);
        }
        poiTypeList = new ArrayList<>(poiCategory.getPoiTypes());
        if (selectAll) {
            selectedPoiTypeList.addAll(poiTypeList);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CATEGORY_NAME_KEY, poiCategory.getKeyName());
        outState.putBoolean(ALL_SELECTED_KEY, selectAll);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subcategories, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        Drawable icClose = app.getUIUtilities().getIcon(R.drawable.ic_arrow_back,
                nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light);
        toolbar.setNavigationIcon(icClose);
        toolbar.setNavigationContentDescription(R.string.shared_string_close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkedHashSet<String> list = new LinkedHashSet<>();
                for (PoiType poiType : selectedPoiTypeList) {
                    list.add(poiType.getKeyName());
                }
                listener.onFiltersSelected(list);
                dismiss();
            }
        });
        TextView title = root.findViewById(R.id.title);
        title.setText(poiCategory.getTranslation());
        listView = root.findViewById(R.id.list);
        headerShadow = inflater.inflate(R.layout.list_shadow_header, listView, false);
        footerShadow = inflater.inflate(R.layout.list_shadow_footer, listView, false);
        headerSelectAll = inflater.inflate(R.layout.select_all_switch_list_item, listView, false);
        final SwitchCompat selectAllSwitch = headerSelectAll.findViewById(R.id.select_all);
        selectAllSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectAll = !selectAll;
                selectAllSwitch.setChecked(selectAll);
                selectAll(selectAll);
            }
        });
        listView.addFooterView(footerShadow);
        listView.addHeaderView(headerSelectAll);
        searchEditText = root.findViewById(R.id.search);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchSubCategory(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        ImageView searchIcon = root.findViewById(R.id.search_icon);
        searchIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_search_dark, nightMode));
        ImageView searchCloseIcon = root.findViewById(R.id.search_close);
        searchCloseIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_cancel, nightMode));
        searchCloseIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSearch();
            }
        });
        Collections.sort(poiTypeList, new Comparator<PoiType>() {
            @Override
            public int compare(PoiType poiType, PoiType t1) {
                return poiType.getTranslation().compareTo(t1.getTranslation());
            }
        });
        adapter = new SubCategoriesAdapter(app, new ArrayList<PoiType>(poiTypeList));
        listView.setAdapter(adapter);
        return root;
    }

    private void clearSearch() {
        searchEditText.setText("");
        AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
    }

    private void selectAll(boolean selectAll) {
        selectedPoiTypeList.clear();
        if (selectAll) {
            selectedPoiTypeList.addAll(poiTypeList);
        }
        adapter.notifyDataSetChanged();
    }

    private void searchSubCategory(String search) {
        List<PoiType> result = new ArrayList<>();
        if (search.isEmpty()) {
            listView.removeHeaderView(headerShadow);
            listView.removeHeaderView(headerSelectAll);
            listView.addHeaderView(headerSelectAll);
            result.addAll(new ArrayList<>(poiTypeList));
        } else {
            listView.removeHeaderView(headerSelectAll);
            listView.removeHeaderView(headerShadow);
            listView.removeFooterView(footerShadow);
            listView.addHeaderView(headerShadow);
            for (PoiType poiType : poiTypeList) {
                if (poiType.getTranslation().toLowerCase().contains(search.toLowerCase())) {
                    result.add(poiType);
                }
            }
            if (result.isEmpty()) {
                listView.removeHeaderView(headerShadow);
                listView.removeFooterView(footerShadow);
            } else {
                listView.addHeaderView(headerShadow);
                listView.addFooterView(footerShadow);
            }
        }
        adapter.clear();
        adapter.addAll(result);
        adapter.notifyDataSetChanged();
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public void setPoiCategory(PoiCategory poiCategory) {
        this.poiCategory = poiCategory;
    }

    public void setListener(OnFiltersSelectedListener listener) {
        this.listener = listener;
    }

    private class SubCategoriesAdapter extends ArrayAdapter<PoiType> {

        private UiUtilities uiUtilities;
        private boolean nightMode;
        private int activeColorRes;
        private int secondaryColorRes;

        public SubCategoriesAdapter(OsmandApplication app, List<PoiType> items) {
            super(app, R.layout.profile_data_list_item_child, items);
            uiUtilities = app.getUIUtilities();
            nightMode = !app.getSettings().isLightContent();
            activeColorRes = nightMode
                    ? R.color.icon_color_active_dark
                    : R.color.icon_color_active_light;
            secondaryColorRes = nightMode
                    ? R.color.icon_color_secondary_dark
                    : R.color.icon_color_secondary_light;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = UiUtilities.getInflater(app, nightMode)
                        .inflate(R.layout.profile_data_list_item_child, parent, false);
            }
            final PoiType poiType = getItem(position);
            final boolean selected = selectedPoiTypeList.contains(poiType);
            int tintColorRes = selected ? activeColorRes : secondaryColorRes;
            if (poiType != null) {
                TextView title = convertView.findViewById(R.id.title_tv);
                title.setText(poiType.getTranslation());
                final CheckBox checkBox = convertView.findViewById(R.id.check_box);
                checkBox.setChecked(selected);
                checkBox.setClickable(false);
                CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, tintColorRes)));
                convertView.findViewById(R.id.sub_title_tv).setVisibility(View.GONE);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selected) {
                            selectedPoiTypeList.remove(poiType);
                        } else {
                            selectedPoiTypeList.add(poiType);
                        }
                        notifyDataSetChanged();
                    }
                });
                ImageView icon = convertView.findViewById(R.id.icon);
                int iconRes = RenderingIcons.getBigIconResourceId(poiType.getIconKeyName());
                if (iconRes == 0) {
                    iconRes = RenderingIcons.getBigIconResourceId(poiType.getOsmTag() + "_" + poiType.getOsmValue());
                    if (iconRes == 0) {
//                        TODO: change default icon
                        iconRes = R.drawable.ic_person;
                    }
                }
                icon.setImageDrawable(uiUtilities.getIcon(iconRes, tintColorRes));
            }
            return convertView;
        }
    }

    public interface OnFiltersSelectedListener {
        void onFiltersSelected(LinkedHashSet<String> filters);
    }
}
