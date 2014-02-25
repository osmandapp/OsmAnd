package net.osmand.activities.search;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.OsmAndFormatter;
import net.osmand.OsmandApplication;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.ResultMatcher;
import net.osmand.activities.search.SearchAddressFragment.AddressInformation;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.MapObject.MapObjectComparator;
import net.osmand.data.Street;
import net.osmand.plus.R;
import net.osmand.resources.RegionAddressRepository;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.os.AsyncTask;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private Button searchAllStrets;
	private int searchWithCity = -1; // -1 - default, 0 - filter city, 1 - deep search
	
	@Override
	protected Comparator<? super Street> createComparator() {
		return new MapObjectComparator(getMyApplication().getSettings().usingEnglishNames()) {
			@Override
			public int compare(MapObject o1, MapObject o2) {
				if(searchWithCity >= 0 && city != null) {
					double d1 = MapUtils.getDistance(city.getLocation(), o1.getLocation());
					double d2 = MapUtils.getDistance(city.getLocation(), o2.getLocation());
					return Double.compare(d1, d2);
				}
				return super.compare(o1, o2);
			}
		};
	}
	
	@Override
	protected void reset() {
		searchWithCity = -1;
		super.reset();
	}
	

	@Override
	protected void addFooterViews() {
		final FrameLayout ll = new FrameLayout(this);
		searchAllStrets = new Button(this);
		android.widget.FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		searchAllStrets.setLayoutParams(lp);
		searchAllStrets.setText(R.string.search_street_in_neighborhood_cities);
		ll.addView(searchAllStrets);
		searchAllStrets.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				searchWithCity = 1;
				research();
			}
		});
		getListView().addFooterView(ll);
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
						return new ArrayList<Street>();
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
		if(searchWithCity == -1){
			filter(query, list);
		} else if(searchWithCity == 0){
			for (Street obj : list) {
				if(namesFilter.isCancelled){
					break;
				}
				if(filterObject(obj, query)){
					Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
					msg.sendToTarget();
				}
			}
		} else {
			searchWithCity = 0;
			final List res = region.searchMapObjectsByName(query, new ResultMatcher<MapObject>() {
				@Override
				public boolean publish(MapObject object) {
					if (object instanceof Street) {
						if(city == null ||
								MapUtils.getDistance(city.getLocation(), object.getLocation()) < 100*1000) {
							Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, object);
							msg.sendToTarget();
							return true;
						}
					}
					return false;
				}
				
				@Override
				public boolean isCancelled() {
					return namesFilter.isCancelled;
				}
			});
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					finishInitializing(res);					
				}
			});
		}
		
		
	}


	private void filter(String query, Collection<Street> list) {
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
		if(searchWithCity >= 0 || city == null) {
			String nameWithCity = obj.getName(region.useEnglishNames()) + " - " + obj.getCity().getName(region.useEnglishNames());
			return nameWithCity ;
		}
		return obj.getName(region.useEnglishNames());
	}
	
	@Override
	public String getDistanceText(Street obj) {
		if(searchWithCity >= 0 && city != null) {
			return OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(obj.getLocation(), city.getLocation()),
					getMyApplication());			
		}
		return null;
	}
	
	
	@Override
	public void itemSelected(Street obj) {
		if(!Algorithms.objectEquals(settings.getLastSearchedCity(), obj.getCity().getId())) {
			settings.setLastSearchedCity(obj.getCity().getId(), obj.getCity().getName(), obj.getLocation());
		}
		settings.setLastSearchedStreet(obj.getName(region.useEnglishNames()), obj.getLocation());
//		if(obj.getBuildings().size() == 0){
//			quitActivity(null);
//		} else {
			quitActivity(SearchBuildingByNameActivity.class);
//		}
		
	}
	
	protected AddressInformation getAddressInformation() {
		return AddressInformation.buildCity(this, settings);
	}
}
