package net.osmand.plus.activities.search;

import android.os.AsyncTask;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.Postcode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.util.MapUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<City> {

	private RegionAddressRepository region;
	private int searchVillagesMode = -1;
	private Button searchVillages;
	private OsmandSettings osmandSettings;

	@Override
	protected void finishInitializing(List<City> list) {
		// Show villages if cities are not present in this region
		if (list != null && list.isEmpty()) {
			searchVillagesMode = 0;
			searchVillages.setVisibility(View.GONE);
		}
		super.finishInitializing(list);
	}

	@Override
	protected void reset() {
		//This is really only a "clear input text field", hence do not reset settings here
		//searchVillagesMode = -1;
		//osmandSettings.setLastSearchedCity(-1L, "", null);
		super.reset();
	}

	@Override
	protected void addFooterViews() {
		final FrameLayout ll = new FrameLayout(this);
		searchVillages = new Button(this);
		android.widget.FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		searchVillages.setLayoutParams(lp);
		searchVillages.setText(R.string.search_villages_and_postcodes);
		ll.addView(searchVillages);
		searchVillages.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchVillagesMode = 1;
				research();
				searchVillages.setVisibility(View.GONE);
			}
		});
		getListView().addFooterView(ll);
	}


	@Override
	protected Comparator<? super City> createComparator() {
		final StringMatcherMode startsWith = CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH;
		return new CityComparator(startsWith, getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get(),
				getMyApplication().getSettings().MAP_TRANSLITERATE_NAMES.get());
	}

	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, City, List<City>>() {
			@Override
			protected void onPostExecute(List<City> result) {
				setLabelText(R.string.incremental_search_city);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}

			@Override
			protected void onPreExecute() {
				setLabelText(R.string.loading_cities);
				progress.setVisibility(View.VISIBLE);
			}


			@Override
			protected List<City> doInBackground(Object... params) {
				region = ((OsmandApplication) getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if (region != null) {
					// preload cities
					region.preloadCities(new ResultMatcher<City>() {

						@Override
						public boolean publish(City object) {
							addObjectToInitialList(object);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
					return region.getLoadedCities();
				}
				return null;
			}
		};
	}

	@Override
	protected boolean filterLoop(String query, Collection<City> list) {
		final boolean[] result = {false};
		redefineSearchVillagesMode(query);
		if (!initializeTaskIsFinished() || !isVillagesSearchEnabled()) {
			result[0] = super.filterLoop(query, list);
			if (!result[0] && !isVillagesSearchEnabled()) {
				setVillagesSearchEnabled(true);
			}
		}
		if (!result[0]) {
			ResultMatcher<City> resultMatcher = new ResultMatcher<City>() {
				@Override
				public boolean isCancelled() {
					return namesFilter.isCancelled;
				}

				@Override
				public boolean publish(City object) {
					result[0] = true;
					Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, object);
					msg.sendToTarget();
					return true;
				}
			};
			region.fillWithSuggestedCities(query, resultMatcher, isVillagesSearchEnabled(), locationToSearch);
		}
		return result[0];
	}

	private void setVillagesSearchEnabled(final boolean enable) {
		searchVillagesMode = enable ? 0 : -1;
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				searchVillages.setVisibility(enable ? View.GONE : View.VISIBLE);
			}
		});
	}

	private boolean isVillagesSearchEnabled() {
		return searchVillagesMode >= 0;
	}

	private void redefineSearchVillagesMode(String query) {
		if (searchVillagesMode == 1) {
			searchVillagesMode = 0;
		} else if (searchVillagesMode == 0 && !initialListToFilter.isEmpty() && query.isEmpty()) {
			setVillagesSearchEnabled(false);
		} else if (searchVillagesMode == -1 && Postcode.looksLikePostcodeStart(query, region.getCountryName())) {
			setVillagesSearchEnabled(true);
		}
	}


	@Override
	public String getText(City obj) {
		LatLon l = obj.getLocation();
		String name = getShortText(obj);
		if (isVillagesSearchEnabled()) {
			if (obj.getClosestCity() != null) {
				name += " - " + obj.getClosestCity().getName(region.getLang(), region.isTransliterateNames());
				LatLon loc = obj.getClosestCity().getLocation();
				if (loc != null && l != null) {
					name += " " + OsmAndFormatter.getFormattedDistance((int) MapUtils.getDistance(l, loc), getMyApplication());
				}
				return name;
			} else {
				if (obj.getType() != null) {
					name += " - " + OsmAndFormatter.toPublicString(obj.getType(), getMyApplication());
				}
			}
		}
		return name;
	}

	@Override
	public String getShortText(City obj) {
		String lName = obj.getName(region.getLang(), region.isTransliterateNames());
		String name = obj.getName();
		if (!lName.equals(name)) {
			return lName + " / " + name;
		}
		return lName;
	}

	@Override
	public void itemSelected(City obj) {
		settings.setLastSearchedCity(obj.getId(), obj.getName(region.getLang(), region.isTransliterateNames()), obj.getLocation());
		if (region.getCityById(obj.getId(), obj.getName(region.getLang(), region.isTransliterateNames())) == null) {
			region.addCityToPreloadedList(obj);
		}
		quitActivity(SearchStreetByNameActivity.class);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}

	private final class CityComparator implements Comparator<City> {
		private final StringMatcherMode startsWith;
		private final net.osmand.Collator cs;
		private final String lang;
		private boolean transliterate;

		private CityComparator(StringMatcherMode startsWith, String lang, boolean transliterate) {
			this.startsWith = startsWith;
			this.transliterate = transliterate;
			this.cs = OsmAndCollator.primaryCollator();
			this.lang = lang;
		}

		@Override
		public int compare(City lhs, City rhs) {
			final String part = getFilter().toString();

			int compare = compareCityType(lhs, rhs);
			if (compare != 0) {
				return compare;
			}
			boolean st1 = CollatorStringMatcher.cmatches(cs, lhs.getName(lang, transliterate), part, startsWith);
			boolean st2 = CollatorStringMatcher.cmatches(cs, rhs.getName(lang, transliterate), part, startsWith);
			if (st1 != st2) {
				return st1 ? 1 : -1;
			}
			compare = cs.compare(getText(lhs), getText(rhs));
			if (compare != 0) {
				return compare;
			}
			if (locationToSearch != null) {
				double d1 = MapUtils.getDistance(locationToSearch, lhs.getLocation());
				double d2 = MapUtils.getDistance(locationToSearch, rhs.getLocation());
				return -Double.compare(d1, d2);
			}
			return 0;
		}

		private int compareCityType(City lhs, City rhs) {
			boolean c1 = lhs.getType() == CityType.CITY || lhs.getType() == CityType.TOWN;
			boolean c2 = rhs.getType() == CityType.CITY || rhs.getType() == CityType.TOWN;
			if (c1 == c2) {
				return 0;
			}
			return c1 ? 1 : -1;
		}
	}


}
