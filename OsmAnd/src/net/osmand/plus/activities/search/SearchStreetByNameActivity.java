package net.osmand.plus.activities.search;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.MapObject.MapObjectComparator;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.resources.RegionAddressRepository;
import android.os.AsyncTask;
import android.os.Message;
import android.view.View;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	
	@Override
	protected Comparator<? super Street> createComparator() {
		return new MapObjectComparator(getMyApplication().getSettings().usingEnglishNames());
	}
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Street, List<Street>>() {
			@Override
			protected void onPostExecute(List<Street> result) {
				setLabelText(R.string.incremental_search_street);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}

			@Override
			protected void onPreExecute() {
				setLabelText(R.string.loading_streets);
				progress.setVisibility(View.VISIBLE);
			}

			@Override
			protected List<Street> doInBackground(Object... params) {
				region = ((OsmandApplication) getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if (region != null) {
					city = region.getCityById(settings.getLastSearchedCity(), settings.getLastSearchedCityName());
					if (city == null) {
						return null;
					}
					region.preloadStreets(city, new ResultMatcher<Street>() {
						@Override
						public boolean publish(Street object) {
							addObjectToInitialList(object);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
					return new ArrayList<Street>(city.getStreets());
				}
				return null;
			}
		};
	}
	
	@Override
	protected void filterLoop(String query, Collection<Street> list) {
		boolean emptyQuery = query == null || query.length() == 0;
		for (Street obj : list) {
			if (namesFilter.isCancelled) {
				break;
			}
			if (emptyQuery || CollatorStringMatcher.cmatches(collator, obj.getNameWithoutCityPart(region.useEnglishNames()), 
					query, StringMatcherMode.CHECK_ONLY_STARTS_WITH)) {
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
				msg.sendToTarget();
			}
		}
		if (!emptyQuery) {
			for (Street obj : list) {
				if (namesFilter.isCancelled) {
					break;
				}
				if (CollatorStringMatcher.cmatches(collator, obj.getNameWithoutCityPart(region.useEnglishNames()), query, StringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING)) {
					Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
					msg.sendToTarget();
				}
			}
		}
	}
	
	@Override
	public String getText(Street obj) {
		return obj.getName(region.useEnglishNames());
	}
	
	
	@Override
	public void itemSelected(Street obj) {
		settings.setLastSearchedStreet(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
		
	}
}
