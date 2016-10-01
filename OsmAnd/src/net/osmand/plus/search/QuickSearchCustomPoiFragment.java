package net.osmand.plus.search;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuickSearchCustomPoiFragment extends DialogFragment {

	public static final String TAG = "QuickSearchCustomPoiFragment";
	private static final String QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY = "quick_search_custom_poi_filter_id_key";

	private View view;
	private ListView listView;
	private CategoryListAdapter listAdapter;
	private String filterId;
	private PoiUIFilter filter;
	private PoiFiltersHelper helper;
	private View bottomBar;
	private AppCompatTextView barTitle;
	private AppCompatTextView barButton;

	public QuickSearchCustomPoiFragment() {
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
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

		view = inflater.inflate(R.layout.search_custom_poi, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		listView = (ListView) view.findViewById(android.R.id.list);
		listView.setBackgroundColor(getResources().getColor(
				app.getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));

		View header = getLayoutInflater(savedInstanceState).inflate(R.layout.list_shadow_header, null);
		listView.addHeaderView(header, null, false);
		View footer = inflater.inflate(R.layout.list_shadow_footer, listView, false);
		listView.addFooterView(footer, null, false);
		listAdapter = new CategoryListAdapter(app, app.getPoiTypes().getCategories(false));
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				PoiCategory category = listAdapter.getItem(position - listView.getHeaderViewsCount());
				showDialog(category);
			}
		});

		bottomBar = view.findViewById(R.id.bottomBar);
		barTitle = (AppCompatTextView) view.findViewById(R.id.barTitle);
		barButton = (AppCompatTextView) view.findViewById(R.id.barButton);
		bottomBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				((QuickSearchDialogFragment) getParentFragment()).showFilter(filterId);
			}
		});

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
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

	public static void showDialog(DialogFragment parentFragment, String filterId) {
		Bundle bundle = new Bundle();
		if (filterId != null) {
			bundle.putString(QUICK_SEARCH_CUSTOM_POI_FILTER_ID_KEY, filterId);
		}
		QuickSearchCustomPoiFragment fragment = new QuickSearchCustomPoiFragment();
		fragment.setArguments(bundle);
		fragment.show(parentFragment.getChildFragmentManager(), TAG);
	}

	private class CategoryListAdapter extends ArrayAdapter<PoiCategory> {
		private OsmandApplication app;

		CategoryListAdapter(OsmandApplication app, List<PoiCategory> items) {
			super(app, R.layout.list_item_icon_and_menu, items);
			this.app = app;
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.list_item_icon_and_menu, parent, false);
			}
			PoiCategory category = getItem(position);
			if (category != null) {
				AppCompatImageView iconView = (AppCompatImageView) row.findViewById(R.id.icon);
				AppCompatImageView secondaryIconView = (AppCompatImageView) row.findViewById(R.id.secondary_icon);
				AppCompatTextView titleView = (AppCompatTextView) row.findViewById(R.id.title);
				AppCompatTextView descView = (AppCompatTextView) row.findViewById(R.id.description);
				SwitchCompat check = (SwitchCompat) row.findViewById(R.id.toggle_item);

				boolean categorySelected = filter.isTypeAccepted(category);
				IconsCache ic = app.getIconsCache();
				int iconId = getIconId(category);
				if (iconId != 0) {
					if (categorySelected) {
						iconView.setImageDrawable(ic.getIcon(iconId, R.color.osmand_orange));
					} else {
						iconView.setImageDrawable(ic.getThemedIcon(iconId));
					}
				} else {
					iconView.setImageDrawable(null);
				}
				secondaryIconView.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_additional_option));
				check.setChecked(filter.isTypeAccepted(category));
				String textString = category.getTranslation();
				titleView.setText(textString);
				Set<String> subtypes = filter.getAcceptedSubtypes(category);
				if (categorySelected) {
					if (subtypes == null) {
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
				row.findViewById(R.id.divider).setVisibility(position == getCount() - 1 ? View.GONE : View.VISIBLE);
				addRowListener(category, check);
			}
			return (row);
		}

		private void addRowListener(final PoiCategory category, final SwitchCompat check) {
			check.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (check.isChecked()) {
						filter.setTypeToAccept(category, true);
						showDialog(category);
					} else {
						filter.setTypeToAccept(category, false);
						saveFilter();
					}
					notifyDataSetChanged();
				}
			});
		}
	}

	private void saveFilter() {
		helper.editPoiFilter(filter);
		if (filter.isEmpty()) {
			bottomBar.setVisibility(View.GONE);
		} else {
			barTitle.setText(getContext().getString(R.string.selected_categories) + ": " + filter.getAcceptedTypesCount());
			bottomBar.setVisibility(View.VISIBLE);
		}
	}

	private void showDialog(final PoiCategory poiCategory) {
		final int index = listView.getFirstVisiblePosition();
		View v = listView.getChildAt(0);
		final int top = (v == null) ? 0 : v.getTop();
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		final LinkedHashMap<String, String> subCategories = new LinkedHashMap<String, String>();
		Set<String> acceptedCategories = filter.getAcceptedSubtypes(poiCategory);
		if (acceptedCategories != null) {
			for(String s : acceptedCategories) {
				subCategories.put(s, Algorithms.capitalizeFirstLetterAndLowercase(s));
			}
		}
		for(PoiType pt :  poiCategory.getPoiTypes()) {
			subCategories.put(pt.getKeyName(), pt.getTranslation());
		}

		final String[] array = subCategories.keySet().toArray(new String[0]);
		final Collator cl = Collator.getInstance();
		cl.setStrength(Collator.SECONDARY);
		Arrays.sort(array, 0, array.length, new Comparator<String>() {

			@Override
			public int compare(String object1, String object2) {
				String v1 = subCategories.get(object1);
				String v2 = subCategories.get(object2);
				return cl.compare(v1, v2);
			}
		});
		final String[] visibleNames = new String[array.length];
		final boolean[] selected = new boolean[array.length];

		for (int i = 0; i < array.length; i++) {
			final String subcategory = array[i];
			visibleNames[i] = subCategories.get(subcategory);
			if (acceptedCategories == null) {
				selected[i] = true;
			} else {
				selected[i] = acceptedCategories.contains(subcategory);
			}
		}
		builder.setTitle(poiCategory.getTranslation());

		builder.setCancelable(true);
		builder.setNegativeButton(getContext().getText(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(getContext().getText(R.string.shared_string_apply), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				LinkedHashSet<String> accepted = new LinkedHashSet<String>();
				for (int i = 0; i < selected.length; i++) {
					if(selected[i]){
						accepted.add(array[i]);
					}
				}
				if (subCategories.size() == accepted.size()) {
					filter.selectSubTypesToAccept(poiCategory, null);
				} else if(accepted.size() == 0){
					filter.setTypeToAccept(poiCategory, false);
				} else {
					filter.selectSubTypesToAccept(poiCategory, accepted);
				}
				saveFilter();
				listAdapter.notifyDataSetChanged();
				listView.setSelectionFromTop(index, top);
			}
		});

		/*
		builder.setPositiveButton(getContext().getText(R.string.shared_string_select_all), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selectAllFromCategory(poiCategory);
				listView.setSelectionFromTop(index, top);
			}
		});
		*/

		builder.setMultiChoiceItems(visibleNames, selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				selected[item] = isChecked;
			}
		});
		builder.show();

	}

	public void selectAllFromCategory(PoiCategory poiCategory) {
		filter.updateTypesToAccept(poiCategory);
		saveFilter();
		listAdapter.notifyDataSetChanged();
	}
}
