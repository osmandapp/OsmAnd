package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.search.core.ObjectType.SEARCH_FINISHED;

public class QuickSearchCustomPoiFragment extends DialogFragment implements OnFiltersSelectedListener {

	public static final String TAG = "QuickSearchCustomPoiFragment";
	private static final String QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY = "quick_search_custom_poi_filter_id_key";

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private View view;
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
	private boolean nightMode;
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

	public QuickSearchCustomPoiFragment() {
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		collator = OsmAndCollator.primaryCollator();
		app = getMyApplication();
		uiUtilities = app.getUIUtilities();
		searchUICore = app.getSearchUICore().getCore();
		this.nightMode = !app.getSettings().isLightContent();
		setStyle(STYLE_NO_FRAME, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		poiCategoryList = app.getPoiTypes().getCategories(false);
		Collections.sort(poiCategoryList, new Comparator<PoiCategory>() {
			@Override
			public int compare(PoiCategory poiCategory, PoiCategory t1) {
				return poiCategory.getTranslation().compareTo(t1.getTranslation());
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		LayoutInflater layoutInflater = UiUtilities.getInflater(app, nightMode);
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
		editMode = !filterId.equals(helper.getCustomPOIFilter().getFilterId());

		view = layoutInflater.inflate(R.layout.search_custom_poi, container, false);
		searchProgressBar = view.findViewById(R.id.searchProgressBar);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		Drawable icClose = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark,
				nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light);
		toolbar.setNavigationIcon(icClose);
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (wasChanged) {
					showExitDialog();
				} else {
					dismiss();
				}
			}
		});
		toolbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_color_dark : R.color.app_bar_color_light));
		toolbar.setTitleTextColor(ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light));
		TextView title = view.findViewById(R.id.title);
		if (editMode) {
			title.setText(filter.getName());
		}

		listView = view.findViewById(android.R.id.list);
		listView.setBackgroundColor(getResources().getColor(
				app.getSettings().isLightContent() ? R.color.activity_background_color_light
						: R.color.activity_background_color_dark));

		headerShadow = layoutInflater.inflate(R.layout.list_shadow_header, null);
		headerDescription = layoutInflater.inflate(R.layout.list_item_description, null);
		((TextView) headerDescription.findViewById(R.id.description)).setText(R.string.search_poi_types_descr);
		listView.addHeaderView(headerDescription, null, false);
		View footerShadow = layoutInflater.inflate(R.layout.list_shadow_footer, listView, false);
		listView.addFooterView(footerShadow, null, false);

		subCategoriesAdapter = new SubCategoriesAdapter(app, new ArrayList<PoiType>(), true,
				new SubCategoriesAdapter.SubCategoryClickListener() {
					@Override
					public void onCategoryClick(boolean allSelected) {
						setupAddButton();
					}
				});
		categoryListAdapter = new CategoryListAdapter(app, poiCategoryList);
		listView.setAdapter(categoryListAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				PoiCategory category = categoryListAdapter.getItem(position - listView.getHeaderViewsCount());
				FragmentManager fm = getFragmentManager();
				if (fm != null && category != null) {
					showSubCategoriesFragment(fm, category, false);
				}
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
		bottomBar = view.findViewById(R.id.bottomBar);
		button = view.findViewById(R.id.button);
		barTitle = view.findViewById(R.id.barTitle);
		barSubTitle = view.findViewById(R.id.barSubTitle);

		ImageView searchIcon = view.findViewById(R.id.search_icon);
		searchIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_search_dark, nightMode));
		searchIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchEditText.requestFocus();
				AndroidUtils.showSoftKeyboard(getActivity(), searchEditText);
			}
		});
		searchCloseIcon = view.findViewById(R.id.search_close);
		searchCloseIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_cancel, nightMode));
		searchCloseIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				subCategoriesAdapter.setSelectedItems(new ArrayList<PoiType>());
				clearSearch();
			}
		});
		searchEditText = view.findViewById(R.id.search);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				searchSubCategory(editable.toString());
			}
		});
		view.findViewById(R.id.topBarShadow).setVisibility(View.VISIBLE);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (editMode) {
			QuickSearchDialogFragment quickSearchDialogFragment = getQuickSearchDialogFragment();
			OsmandApplication app = getMyApplication();
			if (app != null && quickSearchDialogFragment != null) {
				app.getSearchUICore().refreshCustomPoiFilters();
				quickSearchDialogFragment.replaceQueryWithUiFilter(filter, "");
				quickSearchDialogFragment.reloadCategories();
			}
		}
		resetSearchTypes();
		super.onDismiss(dialog);
	}

	@Override
	public void onResume() {
		super.onResume();
		saveFilter();
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					} else {
						if (wasChanged) {
							showExitDialog();
						} else {
							dismiss();
						}
						return true;
					}
				}
				return false;
			}
		});
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

	private void startSearchSubCategories(final String text) {
		updateCloseSearchIcon(true);
		searchCancelled = false;
		SearchSettings searchSettings = searchUICore.getSearchSettings().setSearchTypes(ObjectType.POI_TYPE);
		searchUICore.updateSettings(searchSettings);
		searchUICore.search(text, true, new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult searchResult) {
				if (searchResult.objectType == SEARCH_FINISHED) {
					final List<PoiType> selectedSubCategories = getSelectedSubCategories();
					SearchResultCollection resultCollection = searchUICore.getCurrentSearchResult();
					final List<PoiType> results = new ArrayList<>();
					for (SearchResult result : resultCollection.getCurrentSearchResults()) {
						Object poiObject = result.object;
						if (poiObject instanceof PoiType) {
							PoiType poiType = (PoiType) poiObject;
							if (!poiType.isAdditional()) {
								results.add(poiType);
							}
						}
					}
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							resetSearchTypes();
							if (!searchCancelled) {
								subCategoriesAdapter.setSelectedItems(selectedSubCategories);
								showSearchResults(results);
							}
							updateCloseSearchIcon(false);
						}
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

	private void showSearchResults(List<PoiType> poiTypes) {
		listView.setAdapter(subCategoriesAdapter);
		subCategoriesAdapter.clear();
		subCategoriesAdapter.addAll(poiTypes);
		subCategoriesAdapter.notifyDataSetChanged();
		removeAllHeaders();
		listView.addHeaderView(headerShadow, null, false);
		setupAddButton();
	}

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
		Collections.sort(poiTypes, new Comparator<PoiType>() {
			@Override
			public int compare(PoiType poiType1, PoiType poiType2) {
				return collator.compare(poiType1.getTranslation(), poiType2.getTranslation());
			}
		});
		return poiTypes;
	}

	private QuickSearchDialogFragment getQuickSearchDialogFragment() {
		Fragment parent = getParentFragment();
		if (parent instanceof QuickSearchDialogFragment) {
			return (QuickSearchDialogFragment) parent;
		} else if (parent instanceof QuickSearchPoiFilterFragment
				&& parent.getParentFragment() instanceof QuickSearchDialogFragment) {
			return (QuickSearchDialogFragment) parent.getParentFragment();
		} else {
			return null;
		}
	}

	private QuickSearchPoiFilterFragment getQuickSearchPoiFilterFragment() {
		Fragment parent = getParentFragment();
		if (parent instanceof QuickSearchPoiFilterFragment) {
			return (QuickSearchPoiFilterFragment) parent;
		} else {
			return null;
		}
	}

	private int getIconId(PoiCategory category) {
		OsmandApplication app = getMyApplication();
		String id = null;
		if (category != null) {
			if (RenderingIcons.containsBigIcon(category.getIconKeyName())) {
				id = category.getIconKeyName();
			}
		}
		if (id != null) {
			return RenderingIcons.getBigIconResourceId(id);
		} else {
			return 0;
		}
	}

	private void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
		dismissDialog.show();
	}

	public static void showDialog(DialogFragment parentFragment, String filterId) {
		Bundle bundle = new Bundle();
		if (filterId != null) {
			bundle.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
		}
		QuickSearchCustomPoiFragment fragment = new QuickSearchCustomPoiFragment();
		fragment.setArguments(bundle);
		fragment.show(parentFragment.getChildFragmentManager(), TAG);
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
		} else if (filters.size() == 0) {
			filter.setTypeToAccept(poiCategory, false);
		} else {
			filter.selectSubTypesToAccept(poiCategory, filters);
		}
		saveFilter();
		categoryListAdapter.notifyDataSetChanged();
		wasChanged = true;
	}

	private class CategoryListAdapter extends ArrayAdapter<PoiCategory> {

		private OsmandApplication app;

		CategoryListAdapter(OsmandApplication app, List<PoiCategory> items) {
			super(app, R.layout.list_item_icon24_and_menu, items);
			this.app = app;
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.list_item_icon24_and_menu, parent, false);
			}
			PoiCategory category = getItem(position);
			if (category != null) {
				AppCompatImageView iconView = (AppCompatImageView) row.findViewById(R.id.icon);
				row.findViewById(R.id.secondary_icon).setVisibility(View.GONE);
				AppCompatTextView titleView = (AppCompatTextView) row.findViewById(R.id.title);
				titleView.setMaxLines(Integer.MAX_VALUE);
				titleView.setEllipsize(null);
				AppCompatTextView descView = (AppCompatTextView) row.findViewById(R.id.description);
				SwitchCompat check = (SwitchCompat) row.findViewById(R.id.toggle_item);
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
						StringBuilder sb = new StringBuilder();
						for (String st : subtypes) {
							if (sb.length() > 0) {
								sb.append(", ");
							}
							sb.append(app.getPoiTypes().getPoiTranslation(st));
						}
						descView.setText(sb.toString());
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

		private void addRowListener(final PoiCategory category, final SwitchCompat check) {
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					wasChanged = true;
					if (check.isChecked()) {
						FragmentManager fm = getFragmentManager();
						if (fm != null) {
							showSubCategoriesFragment(fm, category, false);
						}
					} else {
						filter.setTypeToAccept(category, false);
						saveFilter();
						notifyDataSetChanged();
					}
				}
			});
		}
	}

	private void updateFilterName(PoiUIFilter filter) {
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
				button.setBackgroundResource(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
				barTitle.setText(R.string.shared_string_show);
				barSubTitle.setVisibility(View.VISIBLE);
				barSubTitle.setText(ctx.getString(R.string.selected_categories) + ": " + filter.getAcceptedTypesCount());
				bottomBarShadow.setVisibility(View.VISIBLE);
				bottomBar.setVisibility(View.VISIBLE);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						if (!editMode) {
							dismiss();
							QuickSearchDialogFragment quickSearchDialogFragment = getQuickSearchDialogFragment();
							if (quickSearchDialogFragment != null) {
								quickSearchDialogFragment.showFilter(filterId);
							}
						} else {
							QuickSearchPoiFilterFragment quickSearchPoiFilterFragment = getQuickSearchPoiFilterFragment();
							if (quickSearchPoiFilterFragment != null) {
								quickSearchPoiFilterFragment.refreshList();
							}
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
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateFilter(subCategoriesAdapter.getSelectedItems());
			}
		});
		bottomBar.setVisibility(View.VISIBLE);
		bottomBarShadow.setVisibility(View.VISIBLE);
	}

	private void updateFilter(List<PoiType> selectedPoiCategoryList) {
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
		subCategoriesAdapter.setSelectedItems(new ArrayList<PoiType>());
		clearSearch();
		saveFilter();
	}

	private void showSubCategoriesFragment(@NonNull FragmentManager fm,
										   @NonNull PoiCategory poiCategory,
										   boolean selectAll) {
		Set<String> acceptedCategories = filter.getAcceptedSubtypes(poiCategory);
		QuickSearchSubCategoriesFragment.showInstance(fm, this, poiCategory, acceptedCategories, selectAll);
	}
}

interface OnFiltersSelectedListener {
	void onFiltersSelected(PoiCategory poiCategory, LinkedHashSet<String> filters);
}
