package net.osmand.plus.search.dialogs;

import static net.osmand.search.core.ObjectType.SEARCH_FINISHED;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class QuickSearchCustomPoiFragment extends BaseFullScreenDialogFragment implements OnFiltersSelectedListener {

	public static final String TAG = QuickSearchCustomPoiFragment.class.getSimpleName();
	private static final String QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY = "quick_search_custom_poi_filter_id_key";

	private ListView listView;
	private CategoryListAdapter categoryListAdapter;
	private SubCategoriesAdapter subCategoriesAdapter;
	private String filterId;
	private PoiUIFilter filter;
	private PoiFiltersHelper helper;
	private View bottomBarShadow;
	private View bottomBar;
	private TextView barTitle;
	private TextView barSubTitle;
	private boolean editMode;
	private boolean wasChanged;
	private EditText searchEditText;
	private FrameLayout button;
	private List<PoiCategory> poiCategoryList;
	private View headerShadow;
	private View headerDescription;
	private ProgressBar searchProgressBar;
	private ImageView searchCloseIcon;
	private SearchUICore searchUICore;
	private boolean searchCancelled;
	private Collator collator;

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
		collator = OsmAndCollator.primaryCollator();
		searchUICore = app.getSearchUICore().getCore();
		poiCategoryList = app.getPoiTypes().getCategories(false);
		Collections.sort(poiCategoryList, (category1, category2) ->
				category1.getTranslation().compareTo(category2.getTranslation()));
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		helper = app.getPoiFilters();
		if (getArguments() != null) {
			filterId = getArguments().getString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY);
		} else if (savedInstanceState != null) {
			filterId = savedInstanceState.getString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY);
		}
		if (filterId != null) {
			filter = helper.getFilterById(filterId);
		}
		if (filter == null) {
			filter = helper.getCustomPOIFilter();
			filter.clearFilter();
		}
		editMode = !Objects.equals(filterId, helper.getCustomPOIFilter().getFilterId());

		View view = inflate(R.layout.search_custom_poi, container, false);
		searchProgressBar = view.findViewById(R.id.searchProgressBar);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable icClose = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, color);
		toolbar.setNavigationIcon(icClose);
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			if (wasChanged) {
				showExitDialog();
			} else {
				dismiss();
			}
		});
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		TextView title = view.findViewById(R.id.title);
		if (editMode) {
			title.setText(filter.getName());
		}

		listView = view.findViewById(android.R.id.list);
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));

		headerShadow = inflate(R.layout.list_shadow_header);
		headerDescription = inflate(R.layout.list_item_description);
		((TextView) headerDescription.findViewById(R.id.description)).setText(R.string.search_poi_types_descr);
		listView.addHeaderView(headerDescription, null, false);
		View footerShadow = inflate(R.layout.list_shadow_footer, listView, false);
		listView.addFooterView(footerShadow, null, false);

		subCategoriesAdapter = new SubCategoriesAdapter(app, new ArrayList<>(), true, allSelected -> setupAddButton());
		categoryListAdapter = new CategoryListAdapter(app, poiCategoryList);
		listView.setAdapter(categoryListAdapter);
		listView.setOnItemClickListener((parent, v, position, id) -> {
			PoiCategory category = categoryListAdapter.getItem(position - listView.getHeaderViewsCount());
			FragmentManager manager = getFragmentManager();
			if (manager != null && category != null) {
				showSubCategoriesFragment(manager, category);
			}
		});
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

		bottomBarShadow = view.findViewById(R.id.bottomBarShadow);
		bottomBar = view.findViewById(R.id.bottom_buttons_container);
		button = view.findViewById(R.id.button);
		barTitle = view.findViewById(R.id.barTitle);
		barSubTitle = view.findViewById(R.id.barSubTitle);

		ImageView searchIcon = view.findViewById(R.id.search_icon);
		searchIcon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_search_dark, !nightMode));
		searchIcon.setOnClickListener(v -> {
			searchEditText.requestFocus();
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.showSoftKeyboard(activity, searchEditText);
			}
		});
		searchCloseIcon = view.findViewById(R.id.search_close);
		searchCloseIcon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_cancel, !nightMode));
		searchCloseIcon.setOnClickListener(v -> {
			subCategoriesAdapter.setSelectedItems(new ArrayList<>());
			clearSearch();
		});
		searchEditText = view.findViewById(R.id.search);
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable editable) {
				searchSubCategory(editable.toString());
			}
		});
		view.findViewById(R.id.topBarShadow).setVisibility(View.VISIBLE);
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		if (editMode) {
			QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
			if (fragment != null) {
				app.getSearchUICore().refreshCustomPoiFilters();
				fragment.replaceQueryWithUiFilter(filter, "");
				fragment.reloadCategories();
			}
		}
		resetSearchTypes();
		super.onDismiss(dialog);
	}

	@Override
	public void onResume() {
		super.onResume();
		saveFilter();
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.setOnKeyListener((_dialog, keyCode, event) -> {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (event.getAction() != KeyEvent.ACTION_DOWN) {
						if (wasChanged) {
							showExitDialog();
						} else {
							dismiss();
						}
					}
					return true;
				}
				return false;
			});
		}
	}

	private void resetSearchTypes() {
		searchUICore.updateSettings(searchUICore.getSearchSettings().resetSearchTypes());
	}

	private void updateCloseSearchIcon(boolean searching) {
		AndroidUiHelper.updateVisibility(searchProgressBar, searching);
		AndroidUiHelper.updateVisibility(searchCloseIcon, !searching);
	}

	private void searchSubCategory(String text) {
		if (text.isEmpty()) {
			cancelSearchSubCategories();
			if (subCategoriesAdapter.getSelectedItems().isEmpty()) {
				listView.setAdapter(categoryListAdapter);
				categoryListAdapter.notifyDataSetChanged();
				removeAllHeaders();
				listView.addHeaderView(headerDescription, null, false);
				saveFilter();
			} else {
				listView.setAdapter(subCategoriesAdapter);
				subCategoriesAdapter.clear();
				subCategoriesAdapter.addAll(subCategoriesAdapter.getSelectedItems());
				subCategoriesAdapter.notifyDataSetChanged();
				removeAllHeaders();
				listView.addHeaderView(headerShadow, null, false);
				setupAddButton();
			}
		} else {
			startSearchSubCategories(text);
		}
	}

	private void cancelSearchSubCategories() {
		searchCancelled = true;
		resetSearchTypes();
		updateCloseSearchIcon(false);
	}

	private void startSearchSubCategories(String text) {
		updateCloseSearchIcon(true);
		searchCancelled = false;
		SearchSettings searchSettings = searchUICore.getSearchSettings().setSearchTypes(ObjectType.POI_TYPE);
		searchUICore.updateSettings(searchSettings);
		searchUICore.search(text, true, new ResultMatcher<>() {
			@Override
			public boolean publish(SearchResult searchResult) {
				if (searchResult.objectType == SEARCH_FINISHED) {
					List<PoiType> selectedSubCategories = getSelectedSubCategories();
					SearchResultCollection resultCollection = searchUICore.getCurrentSearchResult();
					List<PoiType> results = new ArrayList<>();
					for (SearchResult result : resultCollection.getCurrentSearchResults()) {
						Object poiObject = result.object;
						if (poiObject instanceof PoiType poiType) {
							if (!poiType.isAdditional()) {
								results.add(poiType);
							}
						}
					}
					app.runInUIThread(() -> {
						resetSearchTypes();
						if (!searchCancelled) {
							subCategoriesAdapter.setSelectedItems(selectedSubCategories);
							showSearchResults(results);
						}
						updateCloseSearchIcon(false);
					});
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return searchCancelled;
			}
		});
	}

	private void showSearchResults(@NonNull List<PoiType> poiTypes) {
		listView.setAdapter(subCategoriesAdapter);
		subCategoriesAdapter.clear();
		subCategoriesAdapter.addAll(poiTypes);
		subCategoriesAdapter.notifyDataSetChanged();
		removeAllHeaders();
		listView.addHeaderView(headerShadow, null, false);
		setupAddButton();
	}

	@NonNull
	private List<PoiType> getSelectedSubCategories() {
		List<PoiType> poiTypes = new ArrayList<>();
		for (Map.Entry<PoiCategory, LinkedHashSet<String>> entry : filter.getAcceptedTypes().entrySet()) {
			if (entry.getValue() == null) {
				poiTypes.addAll(entry.getKey().getPoiTypes());
			} else {
				for (String key : entry.getValue()) {
					PoiType poiType = app.getPoiTypes().getPoiTypeByKey(key);
					if (poiType != null) {
						poiTypes.add(poiType);
					}
				}
			}
		}
		Collections.sort(poiTypes, (poiType1, poiType2) -> collator.compare(poiType1.getTranslation(), poiType2.getTranslation()));
		return poiTypes;
	}

	@Nullable
	private QuickSearchDialogFragment getQuickSearchDialogFragment() {
		Fragment parent = getParentFragment();
		if (parent instanceof QuickSearchDialogFragment) {
			return (QuickSearchDialogFragment) parent;
		} else if (parent instanceof QuickSearchPoiFilterFragment
				&& parent.getParentFragment() instanceof QuickSearchDialogFragment) {
			return (QuickSearchDialogFragment) parent.getParentFragment();
		}
		return null;
	}

	@Nullable
	private QuickSearchPoiFilterFragment getQuickSearchPoiFilterFragment() {
		Fragment parent = getParentFragment();
		if (parent instanceof QuickSearchPoiFilterFragment) {
			return (QuickSearchPoiFilterFragment) parent;
		}
		return null;
	}

	private int getIconId(@Nullable PoiCategory category) {
		String iconKeyName = category != null ? category.getIconKeyName() : null;
		if (iconKeyName != null && RenderingIcons.containsBigIcon(iconKeyName)) {
			return RenderingIcons.getBigIconResourceId(iconKeyName);
		}
		return 0;
	}

	private void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(requireActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		dismissDialog.show();
	}

	@Override
	public void onFiltersSelected(PoiCategory poiCategory, LinkedHashSet<String> filters) {
		List<String> subCategories = new ArrayList<>();
		Set<String> acceptedCategories = filter.getAcceptedSubtypes(poiCategory);
		if (acceptedCategories != null) {
			subCategories.addAll(acceptedCategories);
		}
		for (PoiType pt : poiCategory.getPoiTypes()) {
			subCategories.add(pt.getKeyName());
		}
		if (subCategories.size() == filters.size()) {
			filter.selectSubTypesToAccept(poiCategory, null);
		} else if (filters.isEmpty()) {
			filter.setTypeToAccept(poiCategory, false);
		} else {
			filter.selectSubTypesToAccept(poiCategory, filters);
		}
		saveFilter();
		categoryListAdapter.notifyDataSetChanged();
		wasChanged = true;
	}

	private class CategoryListAdapter extends ArrayAdapter<PoiCategory> {

		private final OsmandApplication app;

		CategoryListAdapter(@NonNull OsmandApplication app, @NonNull List<PoiCategory> items) {
			super(app, R.layout.list_item_icon24_and_menu, items);
			this.app = app;
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = inflate(R.layout.list_item_icon24_and_menu, parent, false);
			}
			PoiCategory category = getItem(position);
			if (category != null) {
				AppCompatImageView iconView = row.findViewById(R.id.icon);
				row.findViewById(R.id.secondary_icon).setVisibility(View.GONE);
				AppCompatTextView titleView = row.findViewById(R.id.title);
				titleView.setMaxLines(Integer.MAX_VALUE);
				titleView.setEllipsize(null);
				AppCompatTextView descView = row.findViewById(R.id.description);
				SwitchCompat check = row.findViewById(R.id.toggle_item);
				UiUtilities.setupCompoundButton(check, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

				boolean categorySelected = filter.isTypeAccepted(category);
				UiUtilities ic = app.getUIUtilities();
				int iconId = getIconId(category);
				if (iconId == 0) {
					iconId = R.drawable.mx_special_custom_category;
				}
				if (categorySelected) {
					iconView.setImageDrawable(ic.getIcon(iconId, R.color.osmand_orange));
				} else {
					iconView.setImageDrawable(ic.getThemedIcon(iconId));
				}
				check.setOnCheckedChangeListener(null);
				check.setChecked(filter.isTypeAccepted(category));
				String textString = category.getTranslation();
				titleView.setText(textString);
				Set<String> subtypes = filter.getAcceptedSubtypes(category);
				if (categorySelected) {
					LinkedHashSet<String> poiTypes = filter.getAcceptedTypes().get(category);
					if (subtypes == null || (poiTypes != null && category.getPoiTypes().size() == poiTypes.size())) {
						descView.setText(getString(R.string.shared_string_all));
					} else {
						StringBuilder builder = new StringBuilder();
						for (String type : subtypes) {
							if (!Algorithms.isEmpty(builder)) {
								builder.append(", ");
							}
							builder.append(app.getPoiTypes().getPoiTranslation(type));
						}
						descView.setText(builder.toString());
					}
					descView.setVisibility(View.VISIBLE);
				} else {
					descView.setVisibility(View.GONE);
				}
				row.findViewById(R.id.divider).setVisibility(View.GONE);
				row.findViewById(R.id.divider_vertical).setVisibility(View.VISIBLE);
				addRowListener(category, check);
			}
			return (row);
		}

		private void addRowListener(@NonNull PoiCategory category, @NonNull SwitchCompat check) {
			check.setOnCheckedChangeListener((buttonView, isChecked) -> {
				wasChanged = true;
				if (check.isChecked()) {
					FragmentManager manager = getFragmentManager();
					if (manager != null) {
						showSubCategoriesFragment(manager, category);
					}
				} else {
					filter.setTypeToAccept(category, false);
					saveFilter();
					notifyDataSetChanged();
				}
			});
		}
	}

	private void updateFilterName(@NonNull PoiUIFilter filter) {
		if (!editMode) {
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = filter.getAcceptedTypes();
			List<PoiCategory> categories = new ArrayList<>(acceptedTypes.keySet());
			if (categories.size() == 1) {
				String name = "";
				PoiCategory category = categories.get(0);
				LinkedHashSet<String> filters = acceptedTypes.get(category);
				if (filters == null || filters.size() > 1) {
					name = category.getTranslation();
				} else {
					PoiType poiType = category.getPoiTypeByKeyName(filters.iterator().next());
					if (poiType != null) {
						name = poiType.getTranslation();
					}
				}
				if (!Algorithms.isEmpty(name)) {
					filter.setName(Algorithms.capitalizeFirstLetter(name));
				}
			} else {
				filter.setName(getString(R.string.poi_filter_custom_filter));
			}
		}
	}

	@SuppressLint("SetTextI18n")
	private void saveFilter() {
		updateFilterName(filter);
		helper.editPoiFilter(filter);
		Context ctx = getContext();
		if (ctx != null) {
			if (filter.isEmpty()) {
				bottomBarShadow.setVisibility(View.GONE);
				bottomBar.setVisibility(View.GONE);
			} else {
				UiUtilities.setMargins(button, 0, 0, 0, 0);
				button.setBackgroundResource(ColorUtilities.getActiveColorId(nightMode));
				barTitle.setText(R.string.shared_string_show);
				barSubTitle.setVisibility(View.VISIBLE);
				barSubTitle.setText(getString(R.string.selected_categories) + ": " + filter.getAcceptedTypesCount());
				bottomBarShadow.setVisibility(View.VISIBLE);
				bottomBar.setVisibility(View.VISIBLE);
				button.setOnClickListener(v -> {
					dismiss();
					if (!editMode) {
						dismiss();
						QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
						if (fragment != null) {
							fragment.showFilter(filterId);
						}
					} else {
						QuickSearchPoiFilterFragment fragment = getQuickSearchPoiFilterFragment();
						if (fragment != null) {
							fragment.refreshList();
						}
					}
				});
			}
		}
	}

	private void removeAllHeaders() {
		listView.removeHeaderView(headerDescription);
		listView.removeHeaderView(headerShadow);
	}

	private void clearSearch() {
		searchEditText.setText("");
		AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
	}

	private void setupAddButton() {
		if (subCategoriesAdapter.getSelectedItems().isEmpty()) {
			bottomBar.setVisibility(View.GONE);
			bottomBarShadow.setVisibility(View.GONE);
			return;
		}
		int startMargin = (int) app.getResources().getDimension(R.dimen.content_padding);
		int topMargin = (int) app.getResources().getDimension(R.dimen.content_padding_small);
		UiUtilities.setMargins(button, startMargin, topMargin, startMargin, topMargin);
		barSubTitle.setVisibility(View.GONE);
		barTitle.setText(R.string.shared_string_add);
		button.setBackgroundResource(nightMode ? R.drawable.dlg_btn_primary_dark : R.drawable.dlg_btn_primary_light);
		button.setOnClickListener(view -> updateFilter(subCategoriesAdapter.getSelectedItems()));
		bottomBar.setVisibility(View.VISIBLE);
		bottomBarShadow.setVisibility(View.VISIBLE);
	}

	private void updateFilter(@NonNull List<PoiType> selectedPoiCategoryList) {
		wasChanged = true;
		if (selectedPoiCategoryList.isEmpty()) {
			return;
		}
		HashMap<PoiCategory, LinkedHashSet<String>> map = new HashMap<>();
		for (PoiType poiType : selectedPoiCategoryList) {
			if (map.containsKey(poiType.getCategory())) {
				map.get(poiType.getCategory()).add(poiType.getKeyName());
			} else {
				LinkedHashSet<String> list = new LinkedHashSet<>();
				list.add(poiType.getKeyName());
				map.put(poiType.getCategory(), list);
			}
		}

		for (Map.Entry<PoiCategory, LinkedHashSet<String>> entry : map.entrySet()) {
			PoiCategory poiCategory = entry.getKey();
			List<PoiType> poiTypes = poiCategory.getPoiTypes();
			LinkedHashSet<String> filters = entry.getValue();
			if (filters.isEmpty()) {
				filter.setTypeToAccept(poiCategory, false);
			} else if (poiTypes != null && poiTypes.size() == filters.size()) {
				filter.selectSubTypesToAccept(poiCategory, null);
			} else {
				filter.selectSubTypesToAccept(poiCategory, filters);
			}
		}
		subCategoriesAdapter.clear();
		subCategoriesAdapter.setSelectedItems(new ArrayList<>());
		clearSearch();
		saveFilter();
	}

	private void showSubCategoriesFragment(@NonNull FragmentManager manager, @NonNull PoiCategory category) {
		Set<String> subtypes = filter.getAcceptedSubtypes(category);
		QuickSearchSubCategoriesFragment.showInstance(manager, this, category, subtypes, false);
	}

	public static void showInstance(@NonNull DialogFragment parentFragment, @Nullable String filterId) {
		FragmentManager childFragmentManager = parentFragment.getChildFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			Bundle bundle = new Bundle();
			if (filterId != null) {
				bundle.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
			}
			QuickSearchCustomPoiFragment fragment = new QuickSearchCustomPoiFragment();
			fragment.setArguments(bundle);
			fragment.show(childFragmentManager, TAG);
		}
	}
}

interface OnFiltersSelectedListener {
	void onFiltersSelected(PoiCategory poiCategory, LinkedHashSet<String> filters);
}
