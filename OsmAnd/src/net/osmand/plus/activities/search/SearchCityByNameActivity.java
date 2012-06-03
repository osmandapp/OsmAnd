package net.osmand.plus.activities.search;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import android.os.AsyncTask;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<City> {

	private RegionAddressRepository region;
	
	@Override
	protected Comparator<? super City> createComparator() {
		final Collator cs = Collator.getInstance();
		final boolean en = getMyApplication().getSettings().usingEnglishNames();
		final StringMatcherMode startsWith = CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH;
		return new CityComparator(startsWith, cs,  en);
	}
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, City, List<City>>(){
			@Override
			protected void onPostExecute(List<City> result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_city);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_cities);
				progress.setVisibility(View.VISIBLE);
			}
			
			
			@Override
			protected List<City> doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
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
	protected void filterLoop(String query, Collection<City> list) {
		if(!initializeTaskIsFinished() || query.length() <= 2){
			super.filterLoop(query, list);
		} else {
			region.fillWithSuggestedCities(query, new ResultMatcher<City>() {
				@Override
				public boolean isCancelled() {
					return namesFilter.isCancelled;
				}

				@Override
				public boolean publish(City object) {
					Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, object);
					msg.sendToTarget();
					return true;
				}
			}, locationToSearch);
		}
	}

	
	@Override
	public String getText(City obj) {
		LatLon l = obj.getLocation();
		if (getFilter().length() > 2 && locationToSearch != null && l != null) {
			String name = obj.getName(region.useEnglishNames());
			if (obj.getType() != null) {
				name += " [" + OsmAndFormatter.toPublicString(obj.getType(), this) + "]";
			}
			return name + " - " + //$NON-NLS-1$
					OsmAndFormatter.getFormattedDistance((int) MapUtils.getDistance(l, locationToSearch), this);
		} else {
			return obj.getName(region.useEnglishNames());
		}
	}
	
	@Override
	public void itemSelected(City obj) {
		settings.setLastSearchedCity(obj.getId(), obj.getName(region.useEnglishNames()), obj.getLocation());
		if (region.getCityById(obj.getId(), obj.getName(region.useEnglishNames())) == null) {
			region.addCityToPreloadedList((City) obj);
		}
		finish();
	}
	
	private final class CityComparator implements Comparator<City> {
		private final StringMatcherMode startsWith;
		private final Collator cs;
		private final boolean en;

		private CityComparator(StringMatcherMode startsWith, Collator cs,
				boolean en) {
			this.startsWith = startsWith;
			this.cs = cs;
			this.en = en;
		}

		@Override
		public int compare(City lhs, City rhs) {
			final String part = getFilter().toString();
			
			int compare = compareCityType(lhs, rhs);
			if (compare != 0) {
				return compare;
			}
			boolean st1 = CollatorStringMatcher.cmatches(cs, lhs.getName(en), part, startsWith);
			boolean st2 = CollatorStringMatcher.cmatches(cs, rhs.getName(en), part, startsWith);
		    if(st1 != st2) {
		    	return st1 ? 1 : -1;
		    }
			compare = cs.compare(lhs.getName(en), rhs.getName(en));
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
			if(c1 == c2){
				return 0;
			}
			return c1? 1 : -1;
		}
	}

}
