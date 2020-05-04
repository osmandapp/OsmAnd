package net.osmand.plus.search;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuickSearchSubCategoriesFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = QuickSearchSubCategoriesFragment.class.getName();
	private static final String CATEGORY_NAME_KEY = "category_key";
	private static final String ALL_SELECTED_KEY = "all_selected";
	private OnFiltersSelectedListener listener;
	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private PoiCategory poiCategory;
	private List<PoiType> poiTypeList;
	private Set<String> acceptedCategories;
	private SubCategoriesAdapter adapter;
	private EditText searchEditText;
	private ListView listView;
	private SwitchCompat selectAllSwitch;
	private View headerSelectAll;
	private View headerShadow;
	private View footerShadow;
	private boolean selectAll;
	private boolean nightMode;

	public static void showInstance(@NonNull FragmentManager fm,
									@NonNull PoiCategory poiCategory,
									@Nullable Set<String> acceptedCategories,
									boolean selectAll,
									@NonNull OnFiltersSelectedListener listener) {
		QuickSearchSubCategoriesFragment fragment = new QuickSearchSubCategoriesFragment();
		fragment.setPoiCategory(poiCategory);
		fragment.setSelectAll(selectAll);
		fragment.setAcceptedCategories(acceptedCategories);
		fragment.setListener(listener);
		fragment.show(fm, TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		uiUtilities = app.getUIUtilities();
		nightMode = !app.getSettings().isLightContent();
		if (savedInstanceState != null) {
			poiCategory = app.getPoiTypes().getPoiCategoryByName(savedInstanceState.getString(CATEGORY_NAME_KEY));
			selectAll = savedInstanceState.getBoolean(ALL_SELECTED_KEY);
		}
		poiTypeList = new ArrayList<>(poiCategory.getPoiTypes());
		Collections.sort(poiTypeList, new Comparator<PoiType>() {
			@Override
			public int compare(PoiType poiType, PoiType t1) {
				return poiType.getTranslation().compareTo(t1.getTranslation());
			}
		});
		adapter = new SubCategoriesAdapter(app, new ArrayList<>(poiTypeList), false, new SubCategoriesAdapter.SubCategoryClickListener() {
			@Override
			public void onCategoryClick(boolean allSelected) {
				selectAll = allSelected;
				selectAllSwitch.setChecked(allSelected);
			}
		});
		if (selectAll || acceptedCategories == null) {
			adapter.setSelectedItems(poiTypeList);
			selectAll = true;
		} else {
			List<PoiType> selected = new ArrayList<>();
			for (PoiType poiType : poiTypeList) {
				if (acceptedCategories.contains(poiType.getKeyName())) {
					selected.add(poiType);
				}
			}
			adapter.setSelectedItems(selected);
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
				for (PoiType poiType : adapter.getSelectedItems()) {
					list.add(poiType.getKeyName());
				}
				listener.onFiltersSelected(poiCategory, list);
				dismiss();
			}
		});
		TextView title = root.findViewById(R.id.title);
		title.setText(poiCategory.getTranslation());
		listView = root.findViewById(R.id.list);
		headerShadow = inflater.inflate(R.layout.list_shadow_header, listView, false);
		footerShadow = inflater.inflate(R.layout.list_shadow_footer, listView, false);
		headerSelectAll = inflater.inflate(R.layout.select_all_switch_list_item, listView, false);
		selectAllSwitch = headerSelectAll.findViewById(R.id.select_all);
		selectAllSwitch.setChecked(selectAll);
		selectAllSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				selectAll = !selectAll;
				selectAllSwitch.setChecked(selectAll);
				adapter.selectAll(selectAll);
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
		listView.setAdapter(adapter);
		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					} else {
						LinkedHashSet<String> list = new LinkedHashSet<>();
						for (PoiType poiType : adapter.getSelectedItems()) {
							list.add(poiType.getKeyName());
						}
						listener.onFiltersSelected(poiCategory, list);
						dismiss();
						return true;
					}
				}
				return false;
			}
		});
	}

	private void clearSearch() {
		searchEditText.setText("");
		AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
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
			listView.addHeaderView(headerShadow);
			for (PoiType poiType : poiTypeList) {
				if (poiType.getTranslation().toLowerCase().contains(search.toLowerCase())) {
					result.add(poiType);
				}
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

	public void setAcceptedCategories(Set<String> acceptedCategories) {
		this.acceptedCategories = acceptedCategories;
	}

	public interface OnFiltersSelectedListener {
		void onFiltersSelected(PoiCategory poiCategory, LinkedHashSet<String> filters);
	}
}
