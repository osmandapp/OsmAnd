package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.util.MapUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SelectFavouriteBottomSheet extends MenuBottomSheetDialogFragment
		implements OsmAndLocationListener, OsmAndCompassListener {

	public static final String TAG = SelectFavouriteBottomSheet.class.getSimpleName();

	private static final String IS_SORTED = "sorted";
	private static final String SORTED_BY_TYPE = "sorted_by_type";

	private static final int SORT_TYPE_DIST = 1;
	private static final int SORT_TYPE_NAME = 2;
	private static final int SORT_TYPE_CATEGORY = 3;

	protected FavouritesHelper mFavouritesHelper;
	private final List<FavouritePoint> mPoints = new ArrayList<>();

	private FavouritesAdapter mAdapter;

	protected boolean mIsSorted;
	protected float mLastHeading;
	protected Location mLocation;
	protected boolean mLocationUpdateStarted;
	protected int mSortByDist = SORT_TYPE_DIST;
	protected boolean mCompassUpdateAllowed = true;

	private RecyclerView rvPoints;

	private FavoritesListener mFavouritesListener;

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.getBoolean(IS_SORTED)) {
			mSortByDist = savedInstanceState.getInt(SORTED_BY_TYPE);
		}
		mAdapter = new FavouritesAdapter(requireContext(), mPoints);
		mFavouritesHelper = app.getFavoritesHelper();
		if (mFavouritesHelper.isFavoritesLoaded()) {
			loadFavorites();
		} else {
			mFavouritesHelper.addListener(getFavouritesListener());
		}
		rvPoints = (RecyclerView) inflate(R.layout.recyclerview);
		rvPoints.setLayoutManager(new LinearLayoutManager(getContext()));
		sortFavourites();
		BottomSheetItemTitleWithDescrAndButton[] title = new BottomSheetItemTitleWithDescrAndButton[1];
		title[0] = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonIcons(null, getIconForButton(getNextType(mSortByDist)))
				.setButtonTitle(getTextForButton(getNextType(mSortByDist)))
				.setOnButtonClickListener(v -> {
					mSortByDist = getNextType(mSortByDist);
					sortFavourites();
					int next = getNextType(mSortByDist);
					title[0].setButtonIcons(null, getIconForButton(next));
					title[0].setButtonText(getTextForButton(next));
					title[0].setDescription(getTextForButton(mSortByDist));
				})
				.setDescription(getTextForButton(mSortByDist))
				.setTitle(getString(R.string.favourites))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr_and_button)
				.create();
		items.add(title[0]);

		mAdapter.setItemClickListener(v -> {
			int position = rvPoints.getChildAdapterPosition(v);
			if (position == RecyclerView.NO_POSITION) {
				return;
			}
			onFavouriteSelected(mPoints.get(position));
		});
		rvPoints.setAdapter(mAdapter);
		rvPoints.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				mCompassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(rvPoints)
				.create());
	}

	private FavoritesListener getFavouritesListener() {
		if (mFavouritesListener == null) {
			mFavouritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					loadFavorites();
					mAdapter.notifyDataSetChanged();
				}
			};
		}
		return mFavouritesListener;
	}

	private void loadFavorites() {
		mPoints.clear();
		mPoints.addAll(app.getFavoritesHelper().getVisibleFavouritePoints());
		if (mPoints.isEmpty()) {
			mPoints.addAll(app.getFavoritesHelper().getFavouritePoints());
		}
	}

	private void sortFavourites() {
		Collator inst = Collator.getInstance();
		Location stale = app.getLocationProvider().getLastStaleKnownLocation();
		LatLon latLon = stale != null ? new LatLon(stale.getLatitude(), stale.getLongitude()) :
				app.getMapViewTrackingUtilities().getMapLocation();

		Collections.sort(mPoints, (lhs, rhs) -> {
			if (mSortByDist == SORT_TYPE_DIST && latLon != null) {
				double ld = MapUtils.getDistance(latLon, lhs.getLatitude(),
						lhs.getLongitude());
				double rd = MapUtils.getDistance(latLon, rhs.getLatitude(),
						rhs.getLongitude());
				return Double.compare(ld, rd);
			} else if (mSortByDist == SORT_TYPE_CATEGORY) {
				int cat = inst.compare(lhs.getCategoryDisplayName(app), rhs.getCategoryDisplayName(app));
				if (cat != 0) {
					return cat;
				}
			}
			int name = inst.compare(lhs.getDisplayName(app), rhs.getDisplayName(app));
			return name;
		});

		mIsSorted = true;
		mAdapter.notifyDataSetChanged();
		rvPoints.getLayoutManager().scrollToPosition(0);
	}

	private Drawable getIconForButton(int type) {
		return getIcon(type == SORT_TYPE_DIST ? R.drawable.ic_action_list_sort : R.drawable.ic_action_sort_by_name,
				nightMode ? R.color.multi_selection_menu_close_btn_dark : R.color.multi_selection_menu_close_btn_light);
	}

	private String getTextForButton(int sortByDist) {
		int r = R.string.sort_by_distance;
		if (sortByDist == SORT_TYPE_CATEGORY) {
			r = R.string.sort_by_category;
		} else if (sortByDist == SORT_TYPE_NAME) {
			r = R.string.sort_by_name;
		}
		return getString(r);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_SORTED, mIsSorted);
		outState.putInt(SORTED_BY_TYPE, mSortByDist);
	}

	private void startLocationUpdate() {
		if (!mLocationUpdateStarted) {
			mLocationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (mLocationUpdateStarted) {
			mLocationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}

	private void updateLocationUi() {
		if (mCompassUpdateAllowed && mAdapter != null) {
			app.runInUIThread(mAdapter::notifyDataSetChanged);
		}
	}

	@Override
	public void updateLocation(@NonNull Location location) {
		if (!MapUtils.areLatLonEqual(mLocation, location)) {
			mLocation = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		if (Math.abs(MapUtils.degreesDiff(mLastHeading, value)) > 5) {
			mLastHeading = value;
			updateLocationUi();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
		if (mFavouritesListener != null) {
			app.getFavoritesHelper().removeListener(mFavouritesListener);
			mFavouritesListener = null;
		}
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected int getCustomHeight() {
		return dpToPx(300);
	}

	private static int getNextType(int type) {
		return type % SORT_TYPE_CATEGORY + 1;
	}

	protected abstract void onFavouriteSelected(@NonNull FavouritePoint favourite);
}
