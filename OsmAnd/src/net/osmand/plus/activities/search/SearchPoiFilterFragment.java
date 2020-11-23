/**
 *
 */
package net.osmand.plus.activities.search;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.poi.NominatimPoiFilter;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SearchPoiFilterFragment extends OsmAndListFragment implements SearchActivityChild {

	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	public static final int REQUEST_POI_EDIT = 55;

	private EditText searchEditText;
	private SearchPoiByNameTask currentTask = null;
	private PoiFiltersAdapter poiFitlersAdapter;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.searchpoi, container, false);
		v.findViewById(R.id.SearchFilterLayout).setVisibility(View.VISIBLE);
		((EditText) v.findViewById(R.id.searchEditText)).setHint(R.string.search_poi_category_hint);
		((ImageView) v.findViewById(R.id.search_icon)).setImageDrawable(
				getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_search_dark));

		setupSearchEditText((EditText) v.findViewById(R.id.searchEditText));
		setupOptions((ImageView) v.findViewById(R.id.options));
		v.findViewById(R.id.poiSplitbar).setVisibility(View.GONE);
		return v;
	}

	private void setupOptions(ImageView options) {
		options.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOptionsMenu(v);
			}
		});
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return poiFitlersAdapter;
	}

	private void setupSearchEditText(EditText e) {
		searchEditText = e;
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (currentTask != null) {
					currentTask.cancel(true);
				}
				currentTask = new SearchPoiByNameTask();
				currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, s.toString().trim());
			}
		});
		searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					final PoiUIFilter poiFilter = getApp().getPoiFilters().getSearchByNamePOIFilter();
					poiFilter.setFilterByName(searchEditText.getText().toString());
					showFilterActivity(poiFilter.getFilterId());
					handled = true;
				}
				return handled;
			}
		});
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		poiFitlersAdapter = new PoiFiltersAdapter(getFilters(""));
		setListAdapter(poiFitlersAdapter);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		poiFitlersAdapter.setResult(getFilters(searchEditText == null ? "" : searchEditText.getText().toString()));
	}

	public List<Object> getFilters(String s) {
		List<Object> filters = new ArrayList<Object>();
		OsmandApplication app = getApp();
		if (app == null) {
			return filters;
		}
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		if (Algorithms.isEmpty(s)) {
			filters.addAll(poiFilters.getTopDefinedPoiFilters());
		} else {
			for (PoiUIFilter pf : poiFilters.getTopDefinedPoiFilters()) {
				if (!pf.isStandardFilter() && pf.getName().toLowerCase().startsWith(s.toLowerCase())) {
					filters.add(pf);
				}
			}
			List<AbstractPoiType> res = app.getPoiTypes().getAllTypesTranslatedNames(
					new CollatorStringMatcher(s, StringMatcherMode.CHECK_STARTS_FROM_SPACE));
			final Collator inst = Collator.getInstance();
			Collections.sort(res, new Comparator<AbstractPoiType>() {
				@Override
				public int compare(AbstractPoiType lhs, AbstractPoiType rhs) {
					return inst.compare(lhs.getTranslation(), rhs.getTranslation());
				}

			});
			filters.addAll(res);
			filters.add(poiFilters.getSearchByNamePOIFilter());
			if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null) {
				filters.add(poiFilters.getNominatimPOIFilter());
				filters.add(poiFilters.getNominatimAddressFilter());
			}
		}
		return filters;
	}

	public OsmandApplication getApp() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}


	private void updateIntentToLaunch(Intent intentToLaunch) {
		LatLon loc = null;
		boolean searchAround = false;
		FragmentActivity parent = getActivity();
		if (loc == null && parent instanceof SearchActivity) {
			loc = ((SearchActivity) parent).getSearchPoint();
			searchAround = ((SearchActivity) parent).isSearchAroundCurrentLocation();
		}
		if (loc == null) {
			loc = getApp().getSettings().getLastKnownMapLocation();
		}
		if (loc != null) {
			intentToLaunch.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			intentToLaunch.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
		}
		if (searchAround) {
			intentToLaunch.putExtra(SearchActivity.SEARCH_NEARBY, true);
		}
	}

	@Override
	public void onListItemClick(ListView listView, View v, int position, long id) {
		final Object item = getListAdapter().getItem(position);
		ResourceManager rm = getApp().getResourceManager();
		if (!rm.containsAmenityRepositoryToSearch(false)) {
			Toast.makeText(getActivity(), R.string.data_to_search_poi_not_available, Toast.LENGTH_LONG);
			return;
		}
		if (item instanceof PoiUIFilter) {
			PoiUIFilter model = ((PoiUIFilter) item);
			if (PoiUIFilter.BY_NAME_FILTER_ID.equals(model.getFilterId())
					|| model instanceof NominatimPoiFilter) {
				model.setFilterByName(searchEditText.getText().toString());
			} else {
				model.setFilterByName(model.getSavedFilterByName());
			}
			showFilterActivity(model.getFilterId());
		} else {
			PoiUIFilter custom = getApp().getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + ((AbstractPoiType) item).getKeyName());
			if (custom != null) {
				if (item instanceof PoiType && ((PoiType) item).isAdditional()) {
					// it is already set
				} else {
					custom.setFilterByName(null);
				}
				custom.clearFilter();
				custom.updateTypesToAccept(((AbstractPoiType) item));
				showFilterActivity(custom.getFilterId());
			}
		}
	}

	private void showFilterActivity(String filterId) {
		final Intent newIntent = new Intent(getActivity(), SearchPOIActivity.class);
		newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filterId);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		updateIntentToLaunch(newIntent);
		startActivityForResult(newIntent, 0);
	}

	class SearchPoiByNameTask extends AsyncTask<String, Object, List<Object>> {

		@Override
		protected List<Object> doInBackground(String... params) {
			String filter = params[0];
			return getFilters(filter);
		}

		@Override
		protected void onPostExecute(List<Object> result) {
			if (!isCancelled() && isVisible()) {
				poiFitlersAdapter.setResult(result);
			}
		}

	}


	class PoiFiltersAdapter extends ArrayAdapter<Object> {

		PoiFiltersAdapter(List<Object> list) {
			super(getActivity(), R.layout.searchpoifolder_list, list);
		}

		public void setResult(List<Object> filters) {
			setNotifyOnChange(false);
			clear();
			for (Object o : filters) {
				add(o);
			}
			setNotifyOnChange(true);
			notifyDataSetInvalidated();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoifolder_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.folder_label);
			ImageView icon = (ImageView) row.findViewById(R.id.folder_icon);
			Object item = getItem(position);
			String name;
			if (item instanceof PoiUIFilter) {
				final PoiUIFilter model = (PoiUIFilter) item;
				if (RenderingIcons.containsBigIcon(model.getIconId())) {
					icon.setImageDrawable(RenderingIcons.getBigIcon(getActivity(), model.getIconId()));
				} else if (PoiUIFilter.BY_NAME_FILTER_ID.equals(model.getFilterId()) ||
						model instanceof NominatimPoiFilter) {
					icon.setImageResource(R.drawable.mx_name_finder);
				} else {
					icon.setImageResource(R.drawable.mx_user_defined);
				}
				name = model.getName();
			} else {
				AbstractPoiType st = (AbstractPoiType) item;
				if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
					icon.setImageDrawable(RenderingIcons.getBigIcon(getActivity(), st.getIconKeyName()));
				} else if (st instanceof PoiType
						&& RenderingIcons.containsBigIcon(((PoiType) st).getOsmTag() + "_"
						+ ((PoiType) st).getOsmValue())) {
					icon.setImageResource(RenderingIcons.getBigIconResourceId(((PoiType) st).getOsmTag() + "_"
							+ ((PoiType) st).getOsmValue()));
				} else {
					icon.setImageDrawable(null);
				}
				name = st.getTranslation();
				if (st.isAdditional()) {
					name += " (" + ((PoiType) st).getParentType().getTranslation() + ")";
				}
			}
			label.setText(name);
			return (row);
		}
	}

	private void showOptionsMenu(View v) {
		// Show menu with search all, name finder, name finder poi
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);

		MenuItem item = optionsMenu.getMenu().add(R.string.poi_filter_custom_filter)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_filter_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				PoiUIFilter filter = getApp().getPoiFilters().getCustomPOIFilter();
				filter.clearFilter();
				showFilterActivity(filter.getFilterId());
				return true;
			}
		});
		optionsMenu.show();

	}

	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		if (getActivity() instanceof SearchActivity) {
			((SearchActivity) getActivity()).getClearToolbar(false);
		}
	}

	@Override
	public void locationUpdate(LatLon l) {
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

}
