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
import net.osmand.plus.settings.enums.FavoritesSortMode;
import net.osmand.util.MapUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SelectFavouriteBottomSheet extends MenuBottomSheetDialogFragment implements OsmAndLocationListener, OsmAndCompassListener {

	public static final String TAG = SelectFavouriteBottomSheet.class.getSimpleName();

	protected FavouritesHelper favouritesHelper;
	private final List<FavouritePoint> points = new ArrayList<>();

	private RecyclerView recyclerView;
	private FavouritesAdapter adapter;

	protected float lastHeading;
	protected Location location;
	protected boolean locationUpdateStarted;
	protected FavoritesSortMode sortMode = FavoritesSortMode.getDefaultSortMode();
	protected boolean compassUpdateAllowed = true;

	private FavoritesListener favoritesListener;

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		sortMode = settings.FAVORITES_SORT_MODE.get();

		adapter = new FavouritesAdapter(requireContext(), points);
		favouritesHelper = app.getFavoritesHelper();
		if (favouritesHelper.isFavoritesLoaded()) {
			loadFavorites();
		} else {
			favouritesHelper.addListener(getFavouritesListener());
		}
		recyclerView = (RecyclerView) inflate(R.layout.recyclerview);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		sortFavourites();
		BottomSheetItemTitleWithDescrAndButton[] title = new BottomSheetItemTitleWithDescrAndButton[1];
		title[0] = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonIcons(null, getIconForButton(sortMode.next()))
				.setButtonTitle(getTextForButton(sortMode.next()))
				.setOnButtonClickListener(v -> {
					sortMode = sortMode.next();
					settings.FAVORITES_SORT_MODE.set(sortMode);

					sortFavourites();
					FavoritesSortMode next = sortMode.next();
					title[0].setButtonIcons(null, getIconForButton(next));
					title[0].setButtonText(getTextForButton(next));
					title[0].setDescription(getTextForButton(sortMode));
				})
				.setDescription(getTextForButton(sortMode))
				.setTitle(getString(R.string.favourites))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr_and_button)
				.create();
		items.add(title[0]);

		adapter.setItemClickListener(v -> {
			int position = recyclerView.getChildAdapterPosition(v);
			if (position == RecyclerView.NO_POSITION) {
				return;
			}
			onFavouriteSelected(points.get(position));
		});
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(recyclerView)
				.create());
	}

	private FavoritesListener getFavouritesListener() {
		if (favoritesListener == null) {
			favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					loadFavorites();
					adapter.notifyDataSetChanged();
				}
			};
		}
		return favoritesListener;
	}

	private void loadFavorites() {
		points.clear();
		points.addAll(app.getFavoritesHelper().getVisibleFavouritePoints());
		if (points.isEmpty()) {
			points.addAll(app.getFavoritesHelper().getFavouritePoints());
		}
	}

	private void sortFavourites() {
		Collator collator = Collator.getInstance();
		Location location = app.getLocationProvider().getLastStaleKnownLocation();
		LatLon latLon = location != null ? new LatLon(location.getLatitude(), location.getLongitude())
				: app.getMapViewTrackingUtilities().getMapLocation();

		FavouritesComparator comparator = new FavouritesComparator(app, collator, latLon, sortMode);
		Collections.sort(points, comparator);

		adapter.notifyDataSetChanged();
		recyclerView.getLayoutManager().scrollToPosition(0);
	}

	private Drawable getIconForButton(@NonNull FavoritesSortMode sortMode) {
		int colorId = nightMode ? R.color.multi_selection_menu_close_btn_dark : R.color.multi_selection_menu_close_btn_light;
		return getIcon(sortMode.getIconId(), colorId);
	}

	@NonNull
	private String getTextForButton(@NonNull FavoritesSortMode sortMode) {
		return getString(sortMode.getNameId());
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}

	private void updateLocationUi() {
		if (compassUpdateAllowed && adapter != null) {
			app.runInUIThread(adapter::notifyDataSetChanged);
		}
	}

	@Override
	public void updateLocation(@NonNull Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		if (Math.abs(MapUtils.degreesDiff(lastHeading, value)) > 5) {
			lastHeading = value;
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
		if (favoritesListener != null) {
			app.getFavoritesHelper().removeListener(favoritesListener);
			favoritesListener = null;
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

	protected abstract void onFavouriteSelected(@NonNull FavouritePoint favourite);
}
