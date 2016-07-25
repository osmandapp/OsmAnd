package net.osmand.plus.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.Street;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;

import java.util.List;

public abstract class QuickSearchListFragment extends OsmAndListFragment {

	private QuickSearchDialogFragment dialogFragment;
	private QuickSearchListAdapter listAdapter;
	private boolean touching;

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

				dialogFragment.dismiss();
				showOnMap(sr);
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

	private void showOnMap(SearchResult searchResult) {
		if (searchResult.location != null) {
			OsmandApplication app = getMyApplication();
			PointDescription pointDescription = null;
			Object object = searchResult.object;
			switch (searchResult.objectType) {
				case POI:
					String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType(
							(Amenity) object, searchResult.requiredSearchPhrase.getSettings().getLang());
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
					break;
				case RECENT_OBJ:
					SearchHistoryHelper.HistoryEntry entry = (SearchHistoryHelper.HistoryEntry) object;
					pointDescription = entry.getName();
					break;
				case FAVORITE:
					FavouritePoint fav = (FavouritePoint) object;
					pointDescription = fav.getPointDescription();
					break;
				case HOUSE:
					String nm = searchResult.localeName;
					if(searchResult.relatedObject instanceof City) {
						nm = ((City)searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true) + " " + nm;
					} else if(searchResult.relatedObject instanceof Street) {
						String s = ((Street)searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						String c = ((Street)searchResult.relatedObject).getCity().getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						nm = s + " " + nm +", " + c;
					} else if(searchResult.localeRelatedObjectName != null) {
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
			getMyApplication().getSettings().setMapLocationToShow(
					searchResult.location.getLatitude(), searchResult.location.getLongitude(),
					searchResult.preferredZoom, pointDescription, true, object);

			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	public MapActivity getMapActivity() {
		return (MapActivity)getActivity();
	}

	public void updateLocation(LatLon latLon, Float heading) {
		if (listAdapter != null && !touching) {
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