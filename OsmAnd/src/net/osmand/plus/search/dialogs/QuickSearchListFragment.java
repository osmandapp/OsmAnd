package net.osmand.plus.search.dialogs;

import static net.osmand.search.core.ObjectType.*;

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

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseNestedListFragment;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.search.listitems.QuickSearchBottomShadowListItem;
import net.osmand.plus.search.listitems.QuickSearchButtonListItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchListItemType;
import net.osmand.plus.search.listitems.QuickSearchTopShadowListItem;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.clickable.ClickableWayHelper;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.search.core.SearchResult;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class QuickSearchListFragment extends BaseNestedListFragment {
	private static final Log LOG = PlatformUtil.getLog(QuickSearchListFragment.class);

	private QuickSearchDialogFragment dialogFragment;
	private QuickSearchListAdapter listAdapter;
	private boolean touching;
	private boolean scrolling;
	private boolean showResult;

	public enum SearchListFragmentType {
		HISTORY,
		CATEGORIES,
		ADDRESS,
		MAIN
	}

	@NonNull
	public abstract SearchListFragmentType getType();

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		return inflate(getLayoutId(), container, false);
	}

	@LayoutRes
	protected int getLayoutId() {
		return R.layout.search_dialog_list_layout;
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
		if (index >= 0 && index < listAdapter.getCount()) {
			QuickSearchListItem item = listAdapter.getItem(index);
			if (item != null) {
				if (item.getType() == QuickSearchListItemType.BUTTON) {
					((QuickSearchButtonListItem) item).getOnClickListener().onClick(view);
				} else if (item.getType() == QuickSearchListItemType.SEARCH_RESULT) {
					SearchResult sr = item.getSearchResult();

					if (sr.objectType == POI
							|| sr.objectType == LOCATION
							|| sr.objectType == HOUSE
							|| sr.objectType == FAVORITE
							|| sr.objectType == RECENT_OBJ
							|| sr.objectType == WPT
							|| sr.objectType == STREET_INTERSECTION
							|| sr.objectType == GPX_TRACK) {

						showResult(sr);
					} else if (sr.objectType == INDEX_ITEM) {
						processIndexItemClick((IndexItem) sr.relatedObject);
					} else {
						if (sr.objectType == CITY || sr.objectType == VILLAGE || sr.objectType == STREET) {
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
		dialogFragment = (QuickSearchDialogFragment) getParentFragment();
		listAdapter = new QuickSearchListAdapter(app, requireMapActivity(), nightMode);
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

	public void showResult(@NonNull SearchResult searchResult) {
		this.showResult = false;

		if (searchResult.objectType == GPX_TRACK) {
			showGpxTrackResult(searchResult);
			return;
		}
		if (searchResult.objectType == POI && searchResult.object instanceof Amenity amenity) {
			if (amenity.isRouteArticle() && showTravelArticle(amenity)) {
				return;
			}
		}
		if (searchResult.location != null) {
			showResultWithLocation(searchResult);
		}
	}

	private boolean showTravelArticle(@NonNull Amenity amenity) {
		FragmentActivity activity = getActivity();
		String routeId = amenity.isRouteArticle() ? amenity.getRouteId() : null;
		if (!Algorithms.isEmpty(routeId) && activity != null) {
			dialogFragment.hideToolbar();
			dialogFragment.hide();

			List<String> locales = new ArrayList<>(amenity.getSupportedContentLocales());
			LatLon location = amenity.getLocation();
			TravelArticleIdentifier identifier = new TravelArticleIdentifier(null,
					location.getLatitude(), location.getLongitude(), null, routeId, null);
			return WikivoyageArticleDialogFragment.showInstance(activity.getSupportFragmentManager(), identifier, locales);
		}
		return false;
	}

	private void showResultWithLocation(@NonNull SearchResult searchResult) {
		MapActivity activity = getMapActivity();
		Pair<PointDescription, Object> pair = QuickSearchListItem.getPointDescriptionObject(app, searchResult);

		dialogFragment.hideToolbar();
		dialogFragment.hide();

		if (activity == null) {
			return;
		}
		if (pair.second instanceof Amenity amenity) {
			ClickableWayHelper clickableWayHelper = app.getClickableWayHelper();
			if (amenity.isRouteTrack() && !amenity.isSuperRoute()) {
				TravelHelper travelHelper = app.getTravelHelper();
				TravelGpx travelGpx = new TravelGpx(amenity);

				SearchHistoryHelper historyHelper = app.getSearchHistoryHelper();
				historyHelper.addNewItemToHistory(searchResult.location.getLatitude(),
						searchResult.location.getLongitude(), pair.first, HistorySource.SEARCH);

				travelHelper.openTrackMenu(travelGpx, activity, amenity.getGpxFileName(null), amenity.getLocation(), true);
				return; // TravelGpx
			} else if (clickableWayHelper.isClickableWayAmenity(amenity)) {
				clickableWayHelper.openClickableWayAmenity(amenity, true);
				return; // ClickableWay
			}
		}
		showOnMap(activity, dialogFragment,
				searchResult.location.getLatitude(), searchResult.location.getLongitude(),
				searchResult.preferredZoom, pair.first, pair.second);
	}

	private void showGpxTrackResult(SearchResult searchResult) {
		GPXInfo gpxInfo = (GPXInfo) searchResult.relatedObject;
		if (dialogFragment.getSearchType().isTargetPoint()) {
			File file = gpxInfo.getFile();
			if (file != null) {
				selectTrack(file);
			}
		} else {
			showTrackMenuFragment(gpxInfo);
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
			OsmandApplication app = mapActivity.getApp();
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
				mapActivity.getMapActions().selectAddress(name, latitude, longitude, searchType);

				dialogFragment.dismissAllowingStateLoss();
				mapActivity.getMapActions().showRouteInfoMenu();
			} else {
				app.getSettings().setMapLocationToShow(latitude, longitude, zoom, pointDescription, true, object);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				dialogFragment.reloadHistory();
			}
		}
	}

	private void showTrackMenuFragment(@NonNull GPXInfo gpxInfo) {
		MapActivity mapActivity = requireMapActivity();
		app.getSearchHistoryHelper().addNewItemToHistory(gpxInfo, HistorySource.SEARCH);
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
			if (!list.isEmpty()) {
				showResult = false;
				if (addShadows) {
					list.add(0, new QuickSearchTopShadowListItem(app));
					list.add(new QuickSearchBottomShadowListItem(app));
				}
			}
			listAdapter.setListItems(list);
			if (!append && isVisible()) {
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
}