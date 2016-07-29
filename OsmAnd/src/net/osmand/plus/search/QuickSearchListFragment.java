package net.osmand.plus.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class QuickSearchListFragment extends OsmAndListFragment {

	private QuickSearchDialogFragment dialogFragment;
	private QuickSearchListAdapter listAdapter;
	private boolean touching;
	private boolean scrolling;

	enum SearchListFragmentType {
		HISTORY,
		CATEGORIES,
		MAIN
	}

	public abstract SearchListFragmentType getType();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_dialog_list_layout, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ListView listView = getListView();
		if (listView != null) {
			listView.setOnScrollListener(new AbsListView.OnScrollListener() {
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				}

				public void onScrollStateChanged(AbsListView view, int scrollState) {
					scrolling = (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
				}
			});
			View header = getLayoutInflater(savedInstanceState).inflate(R.layout.list_shadow_header, null);
			View footer = getLayoutInflater(savedInstanceState).inflate(R.layout.list_shadow_footer, null);
			listView.addHeaderView(header, null, false);
			listView.addFooterView(footer, null, false);
		}
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		QuickSearchListItem item = listAdapter.getItem(position - l.getHeaderViewsCount());
		if (item instanceof QuickSearchMoreListItem) {
			((QuickSearchMoreListItem) item).getOnClickListener().onClick(view);
		} else {
			SearchResult sr = item.getSearchResult();

			if (sr.objectType == ObjectType.POI
					|| sr.objectType == ObjectType.LOCATION
					|| sr.objectType == ObjectType.HOUSE
					|| sr.objectType == ObjectType.FAVORITE
					|| sr.objectType == ObjectType.RECENT_OBJ
					|| sr.objectType == ObjectType.WPT
					|| sr.objectType == ObjectType.STREET_INTERSECTION) {

				showOnMap(sr, sr.objectType != ObjectType.RECENT_OBJ);
			} else {
				dialogFragment.completeQueryWithObject(item.getSearchResult());
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		dialogFragment = (QuickSearchDialogFragment) getParentFragment();
		listAdapter = new QuickSearchListAdapter(getMyApplication(), getActivity());
		listAdapter.setUseMapCenter(dialogFragment.isUseMapCenter());
		setListAdapter(listAdapter);
		ListView listView = getListView();
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));
		listView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_POINTER_DOWN:
						touching = true;
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:
					case MotionEvent.ACTION_CANCEL:
						touching = false;
						break;
				}
				return false;
			}
		});
	}

	@Override
	public QuickSearchListAdapter getListAdapter() {
		return listAdapter;
	}

	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}

	public QuickSearchDialogFragment getDialogFragment() {
		return dialogFragment;
	}

	@Override
	public void onResume() {
		super.onResume();
		int screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
		listAdapter.setScreenOrientation(screenOrientation);
		dialogFragment.onSearchListFragmentResume(this);
	}

	private void showOnMap(SearchResult searchResult, boolean showTopbar) {
		if (searchResult.location != null) {
			OsmandApplication app = getMyApplication();
			String lang = searchResult.requiredSearchPhrase.getSettings().getLang();
			PointDescription pointDescription = null;
			Object object = searchResult.object;
			switch (searchResult.objectType) {
				case POI:
					String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType((Amenity) object, lang);
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
					break;
				case RECENT_OBJ:
					HistoryEntry entry = (HistoryEntry) object;
					pointDescription = entry.getName();
					if (pointDescription.isPoi()) {
						Amenity amenity = findAmenity(entry.getName().getName(), entry.getLat(), entry.getLon(), lang);
						if (amenity != null) {
							object = amenity;
							pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI,
									OsmAndFormatter.getPoiStringWithoutType(amenity, lang));
						}
					} else if (pointDescription.isFavorite()) {
						LatLon entryLatLon = new LatLon(entry.getLat(), entry.getLon());
						List<FavouritePoint> favs = app.getFavorites().getFavouritePoints();
						for (FavouritePoint f : favs) {
							if (entryLatLon.equals(new LatLon(f.getLatitude(), f.getLongitude()))
									&& pointDescription.getName().equals(f.getName())) {
								object = f;
								pointDescription = f.getPointDescription();
								break;
							}
						}
					}
					break;
				case FAVORITE:
					FavouritePoint fav = (FavouritePoint) object;
					pointDescription = fav.getPointDescription();
					break;
				case HOUSE:
					String nm = searchResult.localeName;
					if (searchResult.relatedObject instanceof City) {
						nm = ((City) searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true) + " " + nm;
					} else if (searchResult.relatedObject instanceof Street) {
						String s = ((Street) searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						String c = ((Street) searchResult.relatedObject).getCity().getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						nm = s + " " + nm + ", " + c;
					} else if (searchResult.localeRelatedObjectName != null) {
						nm = searchResult.localeRelatedObjectName + " " + nm;
					}
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, nm);
					break;
				case LOCATION:
					LatLon latLon = (LatLon) object;
					pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
					break;
				case STREET_INTERSECTION:
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS,
							QuickSearchListItem.getName(app, searchResult));
					break;
				case WPT:
					GPXUtilities.WptPt wpt = (GPXUtilities.WptPt) object;
					pointDescription = wpt.getPointDescription(getMyApplication());
					break;
			}
			getMapActivity().setQuickSearchTopbarActive(showTopbar);
			if (!showTopbar) {
				dialogFragment.dismiss();
			}
			getMyApplication().getSettings().setMapLocationToShow(
					searchResult.location.getLatitude(), searchResult.location.getLongitude(),
					searchResult.preferredZoom, pointDescription, true, object);

			MapActivity.launchMapActivityMoveToTop(getActivity());
			if (showTopbar) {
				dialogFragment.hide();
			}
		}
	}

	private Amenity findAmenity(String name, double lat, double lon, String lang) {
		OsmandApplication app = getMyApplication();
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, lat, lon, lat, lon, -1, null);

		MapPoiTypes types = app.getPoiTypes();
		for (Amenity amenity : amenities) {
			String amenityName = amenity.getName(lang, true);
			if (Algorithms.isEmpty(amenityName)) {
				AbstractPoiType st = types.getAnyPoiTypeByKey(amenity.getSubType());
				if (st != null) {
					amenityName = st.getTranslation();
				} else {
					amenityName = amenity.getSubType();
				}
			}
			if (name.contains(amenityName)) {
				return amenity;
			}
		}
		return null;
	}

	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public void updateLocation(LatLon latLon, Float heading) {
		if (listAdapter != null && !touching && !scrolling) {
			listAdapter.setLocation(latLon);
			listAdapter.setHeading(heading);
			listAdapter.notifyDataSetChanged();
		}
	}

	public void updateListAdapter(List<QuickSearchListItem> listItems, boolean appended) {
		if (listAdapter != null) {
			listAdapter.setListItems(listItems);
			if (listAdapter.getCount() > 0 && !appended) {
				getListView().setSelection(0);
			}
		}
	}

	public void addListItem(QuickSearchListItem listItem) {
		if (listItem != null) {
			listAdapter.addListItem(listItem);
		}
	}
}