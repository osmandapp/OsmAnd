package net.osmand.plus.routepreparationmenu;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.HorizontalRecyclerBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.mapcontextmenu.other.SelectFavouriteToGoBottomSheet;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.routepreparationmenu.data.PointType;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AddPointBottomSheetDialog extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddPointBottomSheetDialog";
	public static final String POINT_TYPE_KEY = "point_type";

	public static final int ADD_FAVORITE_TO_ROUTE_REQUEST_CODE = 1;

	public static final String FAVORITES = "favorites";
	public static final String MARKERS = "markers";

	private PointType pointType = PointType.START;
	private DialogListener listener;

	public interface DialogListener {
		void onRequestToSelectOnMap(@NonNull PointType pointType);
	}

	public DialogListener getListener() {
		return listener;
	}

	public void setListener(DialogListener listener) {
		this.listener = listener;
	}

	public PointType getPointType() {
		return pointType;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null && args.containsKey(POINT_TYPE_KEY)) {
			pointType = PointType.valueOf(args.getString(POINT_TYPE_KEY));
		}
		items.add(new TitleItem(pointType.getTitle(requireContext())));

		createSearchItem();

		switch (pointType) {
			case START:
				createMyLocItem();
				createSelectOnTheMapItem();
				createFavoritesScrollItem();
				createMarkersScrollItem();
				items.add(new DividerHalfItem(getContext()));
				createSwitchStartAndEndItem();
				break;
			case TARGET:
				createSelectOnTheMapItem();
				createFavoritesScrollItem();
				createMarkersScrollItem();
				items.add(new DividerHalfItem(getContext()));
				createSwitchStartAndEndItem();
				break;
			case INTERMEDIATE:
				createSelectOnTheMapItem();
				createFavoritesScrollItem();
				createMarkersScrollItem();
				break;
			case HOME:
			case WORK:
				createSelectOnTheMapItem();
				createFavoritesScrollItem();
				createMarkersScrollItem();
				break;
			default:
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ADD_FAVORITE_TO_ROUTE_REQUEST_CODE) {
			dismiss();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void createSearchItem() {
		View searchView = inflate(R.layout.bottom_sheet_double_item);
		TextView firstTitle = searchView.findViewById(R.id.first_title);
		TextView secondTitle = searchView.findViewById(R.id.second_title);
		ImageView firstIcon = searchView.findViewById(R.id.first_icon);
		ImageView secondIcon = searchView.findViewById(R.id.second_icon);

		firstTitle.setText(R.string.shared_string_search);
		secondTitle.setText(R.string.shared_string_address);
		firstIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_search_dark));
		secondIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_street_name));

		int dividerColor = ColorUtilities.getDividerColorId(nightMode);
		AndroidUtils.setBackground(getContext(), searchView.findViewById(R.id.first_divider), dividerColor);
		AndroidUtils.setBackground(getContext(), searchView.findViewById(R.id.second_divider), dividerColor);

		searchView.findViewById(R.id.first_item).setOnClickListener(v -> {
			MapActivity activity = (MapActivity) getActivity();
			if (activity != null) {
				activity.getFragmentsHelper().showQuickSearch(getSearchMode(), QuickSearchTab.HISTORY);
			}
			dismiss();
		});
		searchView.findViewById(R.id.second_item).setOnClickListener(v -> {
			MapActivity activity = (MapActivity) getActivity();
			if (activity != null) {
				activity.getFragmentsHelper().showQuickSearch(getSearchMode(), false);
			}
			dismiss();
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(searchView).create());
	}

	private ShowQuickSearchMode getSearchMode() {
		return switch (pointType) {
			case START -> ShowQuickSearchMode.START_POINT_SELECTION;
			case TARGET -> ShowQuickSearchMode.DESTINATION_SELECTION;
			case INTERMEDIATE -> ShowQuickSearchMode.INTERMEDIATE_SELECTION;
			case HOME -> ShowQuickSearchMode.HOME_POINT_SELECTION;
			case WORK -> ShowQuickSearchMode.WORK_POINT_SELECTION;
			default -> ShowQuickSearchMode.START_POINT_SELECTION;
		};
	}

	private void createMyLocItem() {
		BaseBottomSheetItem myLocationItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(OsmAndLocationProvider.isLocationPermissionAvailable(getActivity())
						? R.drawable.ic_action_location_color : R.drawable.ic_action_location_color_lost, 0))
				.setTitle(getString(R.string.shared_string_my_location))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(v -> {
					Activity activity = getActivity();
					if (app != null) {
						if (OsmAndLocationProvider.isLocationPermissionAvailable(app)) {
							TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
							Location myLocation = app.getLocationProvider().getLastKnownLocation();
							if (myLocation != null) {
								LatLon ll = new LatLon(myLocation.getLatitude(), myLocation.getLongitude());
								switch (pointType) {
									case START:
										if (targetPointsHelper.getPointToStart() != null) {
											targetPointsHelper.clearStartPoint(true);
											app.getSettings().backupPointToStart();
										}
										break;
									case TARGET:
										app.showShortToastMessage(R.string.add_destination_point);
										targetPointsHelper.navigateToPoint(ll, true, -1);
										break;
									case INTERMEDIATE:
										app.showShortToastMessage(R.string.add_intermediate_point);
										targetPointsHelper.navigateToPoint(ll, true, targetPointsHelper.getIntermediatePoints().size());
										break;
									case HOME:
										app.showShortToastMessage(R.string.add_home);
										app.getFavoritesHelper().setSpecialPoint(ll, SpecialPointType.HOME, null);
										break;
									case WORK:
										app.showShortToastMessage(R.string.add_work);
										app.getFavoritesHelper().setSpecialPoint(ll, SpecialPointType.WORK, null);
										break;
								}
							} else if (pointType == PointType.START) {
								if (targetPointsHelper.getPointToStart() != null) {
									targetPointsHelper.clearStartPoint(true);
									app.getSettings().backupPointToStart();
								} else {
									targetPointsHelper.updateRouteAndRefresh(false);
								}
							}
						} else if (activity != null) {
							ActivityCompat.requestPermissions(activity,
									new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
											Manifest.permission.ACCESS_COARSE_LOCATION},
									OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
						}
					}
					dismiss();
				}).create();
		items.add(myLocationItem);
	}

	private void createSelectOnTheMapItem() {
		BaseBottomSheetItem selectOnTheMapItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setTitle(getString(R.string.shared_string_select_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(v -> {
					MapActivity activity = (MapActivity) getActivity();
					if (activity != null) {
						DialogListener listener = getListener();
						if (listener != null) {
							listener.onRequestToSelectOnMap(pointType);
						} else {
							MapRouteInfoMenu menu = activity.getMapRouteInfoMenu();
							menu.selectOnScreen(pointType);
						}
					}
					dismiss();
				})
				.create();
		items.add(selectOnTheMapItem);
	}

	private void createMarkersScrollItem() {
		List<Object> items = new ArrayList<>();
		MarkersItemsAdapter adapter = new MarkersItemsAdapter(app, items);
		adapter.setItemClickListener(getAdapterOnClickListener(items));
		MapMarkersHelper helper = app.getMapMarkersHelper();
		items.add(MARKERS);
		items.addAll(helper.getMapMarkers());
		BaseBottomSheetItem scrollItem = new HorizontalRecyclerBottomSheetItem.Builder()
				.setAdapter(adapter)
				.setLayoutId(R.layout.bottom_sheet_item_recyclerview)
				.create();
		this.items.add(scrollItem);
	}

	private void createSwitchStartAndEndItem() {
		View switchStartAndEndView = inflate(R.layout.bottom_sheet_item_simple_56dp);
		TextView title = switchStartAndEndView.findViewById(R.id.title);

		String start = getString(R.string.route_start_point);
		String destination = getString(R.string.route_descr_destination);
		String titleS = getString(R.string.swap_two_places, start, destination);
		SpannableString titleSpan = new SpannableString(titleS);
		int startIndex = titleS.indexOf(start);
		int destinationIndex = titleS.indexOf(destination);
		if (startIndex != -1 && destinationIndex != -1) {
			Typeface typeface = FontCache.getMediumFont();
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), startIndex, startIndex + start.length(), 0);
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), destinationIndex, destinationIndex + destination.length(), 0);
		}
		title.setText(titleSpan);

		BaseBottomSheetItem switchStartAndEndItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_change_navigation_points))
				.setCustomView(switchStartAndEndView)
				.setOnClickListener(v -> {
					callMapActivity(activity -> {
						OsmandApplication app = activity.getApp();
						TargetPointsHelper targetsHelper = app.getTargetPointsHelper();
						TargetPoint startPoint = targetsHelper.getPointToStart();
						if (startPoint == null) {
							app.showShortToastMessage(R.string.route_add_start_point);
							return;
						}
						WaypointDialogHelper.switchStartAndFinish(activity, true);
					});
					dismiss();
				}).create();
		items.add(switchStartAndEndItem);
	}

	private void loadFavoritesItems(List<Object> items, FavouritesHelper helper) {
		items.clear();
		addMainScrollItems(items);
		items.addAll(helper.getVisibleFavouritePoints());
	}

	private void addMainScrollItems(List<Object> items) {
		items.add(FAVORITES);
	}

	private void createFavoritesScrollItem() {
		List<Object> items = new ArrayList<>();
		FavoritesItemsAdapter adapter = new FavoritesItemsAdapter(app, items);
		adapter.setItemClickListener(getAdapterOnClickListener(items));
		FavouritesHelper helper = app.getFavoritesHelper();
		if (helper.isFavoritesLoaded()) {
			loadFavoritesItems(items, helper);
		} else {
			addMainScrollItems(items);
			helper.addListener(new FavoritesListener() {

				private void reloadFavoritesItems() {
					MapActivity mapActivity = (MapActivity) getActivity();
					if (mapActivity != null) {
						loadFavoritesItems(adapter.getItems(), helper);
						adapter.notifyDataSetChanged();
					}
				}

				@Override
				public void onFavoritesLoaded() {
					reloadFavoritesItems();
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
					reloadFavoritesItems();
				}
			});
		}
		BaseBottomSheetItem scrollItem = new HorizontalRecyclerBottomSheetItem.Builder()
				.setAdapter(adapter)
				.setLayoutId(R.layout.bottom_sheet_item_recyclerview)
				.create();
		this.items.add(scrollItem);
	}

	@NonNull
	private OnClickListener getAdapterOnClickListener(@NonNull List<Object> items) {
		return v -> {
			MapActivity mapActivity = getMapActivity();
			RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) v.getTag();
			int position = viewHolder != null ? viewHolder.getAdapterPosition() : RecyclerView.NO_POSITION;
			if (mapActivity == null || position == RecyclerView.NO_POSITION) {
				return;
			}
			Object item = items.get(position);
			if (item.equals(FAVORITES)) {
				SelectFavouriteToGoBottomSheet.showInstance(mapActivity, AddPointBottomSheetDialog.this, pointType);
			} else if (item.equals(MARKERS)) {
				MapRouteInfoMenu menu = mapActivity.getMapRouteInfoMenu();
				menu.selectMapMarker(-1, pointType);
				dismiss();
			} else if (item instanceof MapMarker mapMarker) {
				MapRouteInfoMenu menu = mapActivity.getMapRouteInfoMenu();
				menu.selectMapMarker(mapMarker, pointType);
				dismiss();
			} else {
				TargetPointsHelper targetPointsHelper = mapActivity.getApp().getTargetPointsHelper();
				Pair<LatLon, PointDescription> pair = getLocationAndDescrFromItem(item);
				LatLon ll = pair.first;
				PointDescription name = pair.second;
				if (ll == null) {
					if (item instanceof PointType type) {
						showInstance(mapActivity, type);
					} else {
						dismiss();
					}
				} else {
					FavouritesHelper favorites = app.getFavoritesHelper();
					switch (pointType) {
						case START:
							targetPointsHelper.setStartPoint(ll, true, name);
							break;
						case TARGET:
							targetPointsHelper.navigateToPoint(ll, true, -1, name);
							break;
						case INTERMEDIATE:
							targetPointsHelper.navigateToPoint(ll, true, targetPointsHelper.getIntermediatePoints().size(), name);
							break;
						case HOME:
							favorites.setSpecialPoint(ll, SpecialPointType.HOME, null);
							break;
						case WORK:
							favorites.setSpecialPoint(ll, SpecialPointType.WORK, null);
							break;
						case PARKING:
							favorites.setSpecialPoint(ll, SpecialPointType.PARKING, null);
							break;
					}
					dismiss();
				}
			}
		};
	}

	@NonNull
	private Pair<LatLon, PointDescription> getLocationAndDescrFromItem(@NonNull Object item) {
		PointDescription name = null;
		LatLon ll = null;
		if (item instanceof FavouritePoint point) {
			ll = new LatLon(point.getLatitude(), point.getLongitude());
			name = point.getPointDescription(requireActivity());
		} else if (item instanceof PointType) {
			FavouritesHelper favorites = app.getFavoritesHelper();
			FavouritePoint point = null;
			if (item == PointType.HOME) {
				point = favorites.getSpecialPoint(SpecialPointType.HOME);
			} else if (item == PointType.WORK) {
				point = favorites.getSpecialPoint(SpecialPointType.WORK);
			} else if (item == PointType.PARKING) {
				point = favorites.getSpecialPoint(SpecialPointType.PARKING);
			}
			if (point != null) {
				ll = new LatLon(point.getLatitude(), point.getLongitude());
				name = point.getPointDescription(app);
			}
		}
		return new Pair<>(ll, name);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, @NonNull PointType pointType) {
		return showInstance(mapActivity, pointType, true);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, @NonNull PointType pointType, boolean usedOnMap) {
		if (mapActivity.isActivityDestroyed()) {
			return false;
		}
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(POINT_TYPE_KEY, pointType.name());
			AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}

	private static class ItemViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final ImageView icon;

		ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			icon = itemView.findViewById(R.id.icon);
		}
	}

	private abstract class ScrollItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final List<Object> items;
		private final OsmandApplication app;
		private OnClickListener listener;

		ScrollItemsAdapter(OsmandApplication app, List<Object> items) {
			this.app = app;
			this.items = items;
		}

		public OsmandApplication getApp() {
			return app;
		}

		public List<Object> getItems() {
			return items;
		}

		public OnClickListener getListener() {
			return listener;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
			View view = inflate(R.layout.bottom_sheet_item_with_descr_56dp, viewGroup, false);
			view.setOnClickListener(listener);
			ItemViewHolder viewHolder = new ItemViewHolder(view);
			view.setTag(viewHolder);

			return viewHolder;
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		public Object getItem(int position) {
			return items.get(position);
		}

		void setItemClickListener(OnClickListener listener) {
			this.listener = listener;
		}

		protected void setItemWidth(View itemView, boolean isTitleItem, int itemsSize) {
			Activity activity = getActivity();
			if (activity != null) {
				// 11.5dp is the shadow width
				int shadowWidth = dpToPx(11.5f);
				int bottomSheetWidth = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);

				RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					layoutParams.width = (int) (AndroidUtils.getScreenWidth(activity) / 2.5);
				} else {
					if (isTitleItem) {
						layoutParams.width = itemsSize > 1
								? (bottomSheetWidth / 2 - shadowWidth)
								: (bottomSheetWidth - shadowWidth);
					} else {
						layoutParams.width = (int) (bottomSheetWidth / 2.2 - shadowWidth);
					}
				}
				itemView.setLayoutParams(layoutParams);
			}
		}
	}

	private class FavoritesItemsAdapter extends ScrollItemsAdapter {

		FavoritesItemsAdapter(OsmandApplication app, List<Object> items) {
			super(app, items);
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
			RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(viewGroup, viewType);

			TextView title = viewHolder.itemView.findViewById(R.id.title);
			TextView description = viewHolder.itemView.findViewById(R.id.description);
			if (title != null && description != null) {
				int titleHeight = AndroidUtils.getTextHeight(title.getPaint());
				int descriptionHeight = AndroidUtils.getTextHeight(description.getPaint());
				int minTextHeight = titleHeight + descriptionHeight * 2;
				int defaultItemHeight = getDimensionPixelSize(R.dimen.bottom_sheet_selected_item_title_height);
				if (defaultItemHeight < minTextHeight) {
					viewHolder.itemView.setMinimumHeight(minTextHeight);
				}
			}

			return viewHolder;
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof ItemViewHolder favoriteViewHolder) {
				Object item = getItem(position);
				boolean titleItem = item.equals(FAVORITES);
				if (titleItem) {
					bindFavoritesButton(favoriteViewHolder);
				} else if (item instanceof FavouritePoint) {
					bindFavoritePoint(favoriteViewHolder, (FavouritePoint) item);
				}
				setItemWidth(favoriteViewHolder.itemView, titleItem, getItemCount());
			}
		}

		private void bindFavoritesButton(ItemViewHolder viewHolder) {
			viewHolder.title.setText(R.string.shared_string_favorites);
			viewHolder.icon.setImageDrawable(getContentIcon(R.drawable.ic_action_favorite));
			viewHolder.description.setVisibility(View.GONE);
		}

		private void bindFavoritePoint(ItemViewHolder favoriteViewHolder, FavouritePoint point) {
			favoriteViewHolder.title.setText(point.getDisplayName(app));
			if (point.getSpecialPointType() != null) {
				int iconColor = ColorUtilities.getDefaultIconColorId(nightMode);
				Drawable icon = getIcon(point.getSpecialPointType().getIconId(app), iconColor);
				favoriteViewHolder.icon.setImageDrawable(icon);

				String description = point.getDescription();
				favoriteViewHolder.description.setText(description);
				AndroidUiHelper.updateVisibility(favoriteViewHolder.description, !Algorithms.isEmpty(description));
			} else {
				int defaultFavoritesColor = getColor(R.color.color_favorite);
				int pointColor = app.getFavoritesHelper().getColorWithCategory(point, defaultFavoritesColor);
				int pointIconRes = point.getIconId() == 0 ? R.drawable.ic_action_favorite : point.getIconId();
				BackgroundType backgroundType = point.getBackgroundType() == null
						? BackgroundType.CIRCLE
						: point.getBackgroundType();

				Drawable pointIcon = PointImageUtils.getOrCreate(app, pointColor, false,
						false, pointIconRes, backgroundType);
				favoriteViewHolder.icon.setImageDrawable(pointIcon);

				String description = point.getCategory().isEmpty()
						? getString(R.string.shared_string_favorites)
						: point.getCategory();
				favoriteViewHolder.description.setText(description);
				favoriteViewHolder.description.setVisibility(View.VISIBLE);
			}
		}
	}

	public class MarkersItemsAdapter extends ScrollItemsAdapter {

		MarkersItemsAdapter(OsmandApplication app, List<Object> items) {
			super(app, items);
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof ItemViewHolder markerViewHolder) {
				Object item = getItem(position);
				boolean titleItem = item.equals(MARKERS);
				if (titleItem) {
					markerViewHolder.title.setText(R.string.shared_string_markers);
					markerViewHolder.icon.setImageDrawable(getContentIcon(R.drawable.ic_action_flag));
				} else {
					MapMarker marker = (MapMarker) getItem(position);
					markerViewHolder.title.setText(marker.getName(getContext()));
					markerViewHolder.icon.setImageDrawable(MapMarkerDialogHelper.getMapMarkerIcon(app, marker.colorIndex));
				}
				setItemWidth(markerViewHolder.itemView, titleItem, getItemCount());
				markerViewHolder.description.setVisibility(View.GONE);
			}
		}
	}
}