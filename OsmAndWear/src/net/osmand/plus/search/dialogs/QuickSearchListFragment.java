package net.osmand.plus.search.dialogs;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.data.PointDescription;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.search.listitems.QuickSearchBottomShadowListItem;
import net.osmand.plus.search.listitems.QuickSearchButtonListItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchListItemType;
import net.osmand.plus.search.listitems.QuickSearchTopShadowListItem;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class QuickSearchListFragment extends OsmAndListFragment {

	protected OsmandApplication app;
	private QuickSearchDialogFragment dialogFragment;
	private QuickSearchListAdapter listAdapter;
	private boolean touching;
	private boolean scrolling;
	private boolean showResult;

	enum SearchListFragmentType {
		HISTORY,
		CATEGORIES,
		ADDRESS,
		MAIN
	}

	public abstract SearchListFragmentType getType();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_dialog_list_layout, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ListView listView = getListView();
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
	}

	@Override
	public void onListItemClick(@NonNull ListView listView, @NonNull View view, int position, long id) {
		int index = position - listView.getHeaderViewsCount();
		if (index < listAdapter.getCount()) {
			QuickSearchListItem item = listAdapter.getItem(index);
			if (item != null) {
				if (item.getType() == QuickSearchListItemType.BUTTON) {
					((QuickSearchButtonListItem) item).getOnClickListener().onClick(view);
				} else if (item.getType() == QuickSearchListItemType.SEARCH_RESULT) {
					SearchResult sr = item.getSearchResult();

					if (sr.objectType == ObjectType.POI
							|| sr.objectType == ObjectType.LOCATION
							|| sr.objectType == ObjectType.HOUSE
							|| sr.objectType == ObjectType.FAVORITE
							|| sr.objectType == ObjectType.RECENT_OBJ
							|| sr.objectType == ObjectType.WPT
							|| sr.objectType == ObjectType.STREET_INTERSECTION
							|| sr.objectType == ObjectType.GPX_TRACK) {

						showResult(sr);
					} else if (sr.objectType == ObjectType.INDEX_ITEM) {
						processIndexItemClick((IndexItem) sr.relatedObject);
					} else {
						if (sr.objectType == ObjectType.CITY || sr.objectType == ObjectType.VILLAGE || sr.objectType == ObjectType.STREET) {
							showResult = true;
						}
						dialogFragment.completeQueryWithObject(sr);
					}
				}
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		app = getMyApplication();
		boolean nightMode = !app.getSettings().isLightContent();
		dialogFragment = (QuickSearchDialogFragment) getParentFragment();
		listAdapter = new QuickSearchListAdapter(app, requireMapActivity());
		listAdapter.setAccessibilityAssistant(dialogFragment.getAccessibilityAssistant());
		listAdapter.setUseMapCenter(dialogFragment.isUseMapCenter());
		setListAdapter(listAdapter);
		ListView listView = getListView();
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
		listView.setOnTouchListener((v, event) -> {
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
		dialogFragment.onSearchListFragmentResume(this);
	}

	public boolean isShowResult() {
		return showResult;
	}

	public void showResult(SearchResult searchResult) {
		showResult = false;
		if (searchResult.objectType == ObjectType.GPX_TRACK) {
			GPXInfo gpxInfo = (GPXInfo) searchResult.relatedObject;
			if (dialogFragment.getSearchType().isTargetPoint()) {
				File file = gpxInfo.getFile();
				if (file != null) {
					selectTrack(file);
				}
			} else {
				showTrackMenuFragment(gpxInfo);
			}
		} else if (searchResult.location != null) {
			Pair<PointDescription, Object> pair = QuickSearchListItem.getPointDescriptionObject(app, searchResult);

			dialogFragment.hideToolbar();
			dialogFragment.hide();

			showOnMap(getMapActivity(), dialogFragment,
					searchResult.location.getLatitude(), searchResult.location.getLongitude(),
					searchResult.preferredZoom, pair.first, pair.second);
		}
	}

	private void selectTrack(@NonNull File file) {
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
		if (selectedGpxFile != null) {
			selectTrack(selectedGpxFile.getGpxFile());
		} else {
			GpxFileLoaderTask.loadGpxFile(file, getMapActivity(), gpxFile -> {
				selectTrack(gpxFile);
				return true;
			});
		}
	}

	private void selectTrack(@NonNull GpxFile gpxFile) {
		MapRouteInfoMenu routeInfoMenu = requireMapActivity().getMapRouteInfoMenu();
		routeInfoMenu.selectTrack(gpxFile, true);

		dialogFragment.dismissAllowingStateLoss();
	}

	public static void showOnMap(MapActivity mapActivity, QuickSearchDialogFragment dialogFragment,
	                             double latitude, double longitude, int zoom,
	                             @Nullable PointDescription pointDescription, Object object) {
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			QuickSearchType searchType = dialogFragment.getSearchType();
			if (searchType.isTargetPoint()) {
				String name = null;
				if (pointDescription != null) {
					String typeName = pointDescription.getTypeName();
					if (!Algorithms.isEmpty(typeName)) {
						name = mapActivity.getString(R.string.ltr_or_rtl_combine_via_comma, pointDescription.getName(), typeName);
					} else {
						name = pointDescription.getName();
					}
				}
				mapActivity.getMapLayers().getMapActionsHelper().selectAddress(name, latitude, longitude, searchType);

				dialogFragment.dismissAllowingStateLoss();
				mapActivity.getMapLayers().getMapActionsHelper().showRouteInfoMenu();
			} else {
				app.getSettings().setMapLocationToShow(latitude, longitude, zoom, pointDescription, true, object);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				dialogFragment.reloadHistory();
			}
		}
	}

	private void showTrackMenuFragment(@NonNull GPXInfo gpxInfo) {
		MapActivity mapActivity = requireMapActivity();
		SearchHistoryHelper.getInstance(app).addNewItemToHistory(gpxInfo, HistorySource.SEARCH);
		File file = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), gpxInfo.getFileName());
		String path = file.getAbsolutePath();
		TrackMenuFragment.showInstance(mapActivity, path, false, false, null, QuickSearchDialogFragment.TAG, null);
		dialogFragment.dismissAllowingStateLoss();
	}

	private void processIndexItemClick(@NonNull IndexItem indexItem) {
		FragmentActivity activity = requireActivity();
		DownloadIndexesThread thread = app.getDownloadThread();

		DownloadValidationManager manager = new DownloadValidationManager(app);
		if (thread.isDownloading(indexItem)) {
			manager.makeSureUserCancelDownload(activity, indexItem);
		} else {
			manager.startDownload(activity, indexItem);
		}
	}

	public void updateLocation(Float heading) {
		if (listAdapter != null && !touching && !scrolling) {
			dialogFragment.getAccessibilityAssistant().lockEvents();
			listAdapter.notifyDataSetChanged();
			dialogFragment.getAccessibilityAssistant().unlockEvents();
			View selected = dialogFragment.getAccessibilityAssistant().getFocusedView();
			if (selected != null) {
				try {
					int position = getListView().getPositionForView(selected);
					if ((position != AdapterView.INVALID_POSITION) && (position >= getListView().getHeaderViewsCount())) {
						dialogFragment.getNavigationInfo().updateTargetDirection(
								listAdapter.getItem(position - getListView().getHeaderViewsCount()).getSearchResult().location,
								heading.floatValue());
					}
				} catch (Exception ignored) {
				}
			}
		}
	}

	public void updateListAdapter(List<QuickSearchListItem> listItems, boolean append) {
		updateListAdapter(listItems, append, true);
	}

	public void updateListAdapter(List<QuickSearchListItem> listItems, boolean append, boolean addShadows) {
		if (listAdapter != null) {
			List<QuickSearchListItem> list = new ArrayList<>(listItems);
			if (list.size() > 0) {
				showResult = false;
				if (addShadows) {
					list.add(0, new QuickSearchTopShadowListItem(app));
					list.add(new QuickSearchBottomShadowListItem(app));
				}
			}
			listAdapter.setListItems(list);
			if (!append) {
				getListView().setSelection(0);
			}
		}
	}

	public void addListItem(QuickSearchListItem listItem) {
		if (listItem != null) {
			if (listAdapter.getCount() == 0) {
				List<QuickSearchListItem> list = new ArrayList<>();
				list.add(new QuickSearchTopShadowListItem(app));
				list.add(listItem);
				list.add(new QuickSearchBottomShadowListItem(app));
				listAdapter.setListItems(list);
			} else {
				QuickSearchListItem lastItem = listAdapter.getItem(listAdapter.getCount() - 1);
				if (lastItem.getType() == QuickSearchListItemType.BOTTOM_SHADOW) {
					listAdapter.insertListItem(listItem, listAdapter.getCount() - 1);
				} else {
					listAdapter.addListItem(listItem);
				}
			}
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@NonNull
	public MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
	}
}