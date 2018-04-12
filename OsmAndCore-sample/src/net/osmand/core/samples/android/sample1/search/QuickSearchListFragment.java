package net.osmand.core.samples.android.sample1.search;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleFormatter;
import net.osmand.core.samples.android.sample1.SampleUtils;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class QuickSearchListFragment extends ListFragment {

	private QuickSearchDialogFragment dialogFragment;
	private QuickSearchListAdapter listAdapter;
	private boolean touching;
	private boolean scrolling;

	enum SearchListFragmentType {
		CATEGORIES,
		MAIN
	}

	public abstract SearchListFragmentType getType();

	public SampleApplication getMyApplication() {
		return (SampleApplication) getActivity().getApplication();
	}

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
					if (scrolling) {
						dialogFragment.hideKeyboard();
					}
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
		int index = position - l.getHeaderViewsCount();
		if (index < listAdapter.getCount()) {
			QuickSearchListItem item = listAdapter.getItem(index);
			if (item != null) {
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

						showOnMap(sr);
					} else {
						dialogFragment.completeQueryWithObject(item.getSearchResult());
					}
				}
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
		listView.setBackgroundColor(getResources().getColor(R.color.ctx_menu_info_view_bg_light));
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
		int screenOrientation = SampleUtils.getScreenOrientation(getActivity());
		listAdapter.setScreenOrientation(screenOrientation);
		dialogFragment.onSearchListFragmentResume(this);
	}

	private void showOnMap(SearchResult searchResult) {
		if (searchResult.location != null) {
			SampleApplication app = getMyApplication();
			String lang = searchResult.requiredSearchPhrase.getSettings().getLang();
			boolean transliterate = searchResult.requiredSearchPhrase.getSettings().isTransliterate();
			PointDescription pointDescription = null;
			Object object = searchResult.object;
			switch (searchResult.objectType) {
				case POI:
					Amenity a = (Amenity) object;
					String poiSimpleFormat = SampleFormatter.getPoiStringWithoutType(a, lang, transliterate);
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
					pointDescription.setIconName(QuickSearchListItem.getAmenityIconName(a));
					break;
				case HOUSE:
					String typeNameHouse = null;
					String name = searchResult.localeName;
					if (searchResult.relatedObject instanceof City) {
						name = ((City) searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true) + " " + name;
					} else if (searchResult.relatedObject instanceof Street) {
						String s = ((Street) searchResult.relatedObject).getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						typeNameHouse = ((Street) searchResult.relatedObject).getCity().getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
						name = s + " " + name;
					} else if (searchResult.localeRelatedObjectName != null) {
						name = searchResult.localeRelatedObjectName + " " + name;
					}
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeNameHouse, name);
					pointDescription.setIconName("ic_action_building");
					break;
				case LOCATION:
					pointDescription = new PointDescription(
							searchResult.location.getLatitude(), searchResult.location.getLongitude());
					pointDescription.setIconName("ic_action_world_globe");
					break;
				case STREET_INTERSECTION:
					String typeNameIntersection = QuickSearchListItem.getTypeName(app, searchResult);
					if (Algorithms.isEmpty(typeNameIntersection)) {
						typeNameIntersection = null;
					}
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS,
							typeNameIntersection, QuickSearchListItem.getName(app, searchResult));
					pointDescription.setIconName("ic_action_intersection");
					break;
			}

			dialogFragment.hideToolbar();
			dialogFragment.hide();

			getMainActivity().showOnMap(searchResult.location, searchResult.preferredZoom);
			getMainActivity().getContextMenu().show(searchResult.location, pointDescription, object);
		}
	}

	public MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}

	public void updateLocation(LatLon latLon, Float heading) {
		if (listAdapter != null && !touching && !scrolling) {
			listAdapter.setLocation(latLon);
			listAdapter.setHeading(heading);
			listAdapter.notifyDataSetChanged();
		}
	}

	public void updateListAdapter(List<QuickSearchListItem> listItems, boolean append) {
		if (listAdapter != null) {
			listAdapter.setListItems(listItems);
			if (!append) {
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