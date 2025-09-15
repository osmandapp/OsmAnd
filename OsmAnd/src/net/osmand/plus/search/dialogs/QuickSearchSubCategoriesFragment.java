package net.osmand.plus.search.dialogs;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

import java.util.*;

public class QuickSearchSubCategoriesFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = QuickSearchSubCategoriesFragment.class.getName();
	private static final String CATEGORY_NAME_KEY = "category_key";
	private static final String ALL_SELECTED_KEY = "all_selected";
	private static final String ACCEPTED_CATEGORIES_KEY = "accepted_categories";
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
	private FrameLayout buttonsContainer;
	private boolean selectAll;

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			poiCategory = app.getPoiTypes().getPoiCategoryByName(savedInstanceState.getString(CATEGORY_NAME_KEY));
			selectAll = savedInstanceState.getBoolean(ALL_SELECTED_KEY);
			acceptedCategories = new HashSet<>(savedInstanceState.getStringArrayList(ACCEPTED_CATEGORIES_KEY));
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
				updateAddBtnVisibility();
			}
		});
		if (selectAll || acceptedCategories == null || poiCategory.getPoiTypes().size() == acceptedCategories.size()) {
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
		outState.putStringArrayList(ACCEPTED_CATEGORIES_KEY, new ArrayList<>(getSelectedSubCategories()));
	}

	private List<String> getSelectedSubCategories() {
		List<String> subCategories = new ArrayList<>();
		for (PoiType poiType : adapter.getSelectedItems()) {
			subCategories.add(poiType.getKeyName());
		}
		return subCategories;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = inflate(R.layout.fragment_subcategories, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable icClose = getIcon(R.drawable.ic_arrow_back, color);
		toolbar.setNavigationIcon(icClose);
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismissFragment());
		TextView title = root.findViewById(R.id.title);
		title.setText(poiCategory.getTranslation());

		buttonsContainer = root.findViewById(R.id.bottom_buttons_container);
		updateAddBtnVisibility();
		root.findViewById(R.id.add_button).setOnClickListener(v -> dismissFragment());

		listView = root.findViewById(R.id.list);
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView, int i) {
				if (i == SCROLL_STATE_TOUCH_SCROLL) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int first, int visibleCount, int totalCount) {

			}
		});
		headerShadow = inflater.inflate(R.layout.list_shadow_header, listView, false);
		footerShadow = inflater.inflate(R.layout.list_shadow_footer, listView, false);
		headerSelectAll = inflater.inflate(R.layout.select_all_switch_list_item, listView, false);
		headerSelectAll.setVerticalScrollBarEnabled(false);
		selectAllSwitch = headerSelectAll.findViewById(R.id.select_all);
		selectAllSwitch.setChecked(selectAll);
		selectAllSwitch.setOnClickListener(v -> {
			selectAll = !selectAll;
			selectAllSwitch.setChecked(selectAll);
			adapter.selectAll(selectAll);
			updateAddBtnVisibility();
		});
		listView.addFooterView(footerShadow);
		listView.addHeaderView(headerSelectAll);
		searchEditText = root.findViewById(R.id.search);
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				searchSubCategory(charSequence.toString());
			}
		});
		ImageView searchIcon = root.findViewById(R.id.search_icon);
		searchIcon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_search_dark, nightMode));
		searchIcon.setOnClickListener(view -> {
			searchEditText.requestFocus();
			AndroidUtils.showSoftKeyboard(getActivity(), searchEditText);
		});
		ImageView searchCloseIcon = root.findViewById(R.id.search_close);
		searchCloseIcon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_cancel, nightMode));
		searchCloseIcon.setOnClickListener(view -> clearSearch());
		listView.setAdapter(adapter);
		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		getDialog().setOnKeyListener((dialog, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					return true;
				} else {
					dismissFragment();
					return true;
				}
			}
			return false;
		});
	}

	private void updateAddBtnVisibility() {
		if (buttonsContainer != null) {
			buttonsContainer.setVisibility(adapter.getSelectedItems().isEmpty() ? View.GONE : View.VISIBLE);
		}
	}

	private void dismissFragment() {
		LinkedHashSet<String> list = new LinkedHashSet<>();
		for (PoiType poiType : adapter.getSelectedItems()) {
			list.add(poiType.getKeyName());
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnFiltersSelectedListener) {
			((OnFiltersSelectedListener) fragment).onFiltersSelected(poiCategory, list);
		}
		dismiss();
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

	public void setAcceptedCategories(Set<String> acceptedCategories) {
		this.acceptedCategories = acceptedCategories;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @Nullable Fragment targetFragment,
	                                @NonNull PoiCategory poiCategory,
	                                @Nullable Set<String> acceptedCategories,
	                                boolean selectAll) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			QuickSearchSubCategoriesFragment fragment = new QuickSearchSubCategoriesFragment();
			fragment.setPoiCategory(poiCategory);
			fragment.setSelectAll(selectAll);
			fragment.setAcceptedCategories(acceptedCategories);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(manager, TAG);
		}
	}
}
