package net.osmand.plus.mapmarkers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.base.BaseNestedFragment;
import net.osmand.plus.mapmarkers.HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TracksAdapter.ItemVisibilityCallback;
import net.osmand.plus.mapmarkers.SelectionMarkersGroupBottomSheetDialogFragment.AddMarkersGroupFragmentListener;
import net.osmand.plus.mapmarkers.adapters.MapMarkerItemViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkersGroupsAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.SelectTrackTabsFragment.GpxDataItemSelectionListener;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.EmptyStateRecyclerView;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

public class MapMarkersGroupsFragment extends BaseNestedFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "MapMarkersGroupsFragment";

	private MapMarkersGroupsAdapter adapter;

	private final Paint backgroundPaint = new Paint();
	private final Paint textPaint = new Paint();

	private Snackbar snackbar;
	private View mainView;
	private String groupIdToOpen;
	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = (MapActivity) requireActivity();
		mainView = inflate(R.layout.fragment_map_markers_groups, container, false);

		Fragment selectionMarkersGroupFragment = getChildFragmentManager().findFragmentByTag(SelectionMarkersGroupBottomSheetDialogFragment.TAG);
		if (selectionMarkersGroupFragment != null) {
			((SelectionMarkersGroupBottomSheetDialogFragment) selectionMarkersGroupFragment).setListener(createAddMarkersGroupFragmentListener());
		}
		Fragment historyMarkerMenuFragment = getChildFragmentManager().findFragmentByTag(HistoryMarkerMenuBottomSheetDialogFragment.TAG);
		if (historyMarkerMenuFragment instanceof HistoryMarkerMenuBottomSheetDialogFragment fragment) {
			fragment.setListener(createHistoryMarkerMenuListener());
		}

		EmptyStateRecyclerView recyclerView = mainView.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		backgroundPaint.setColor(ColorUtilities.getDividerColor(app, nightMode));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		textPaint.setTextSize(getResources().getDimension(R.dimen.default_desc_text_size));
		textPaint.setFakeBoldText(true);
		textPaint.setAntiAlias(true);

		String delStr = getString(R.string.shared_string_delete).toUpperCase();
		String moveToHistoryStr = getString(R.string.move_to_history).toUpperCase();
		Rect bounds = new Rect();

		textPaint.getTextBounds(delStr, 0, delStr.length(), bounds);
		int delStrWidth = bounds.width();
		int textHeight = bounds.height();

		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			private final float marginSides = getResources().getDimension(R.dimen.list_content_padding);
			private boolean iconHidden;

			@Override
			public int getSwipeDirs(@NonNull RecyclerView recyclerView, 
			                        @NonNull RecyclerView.ViewHolder viewHolder) {
				boolean markerViewHolder = viewHolder instanceof MapMarkerItemViewHolder;
				int pos = viewHolder.getAdapterPosition();
				if (markerViewHolder && pos != -1) {
					MapMarker marker = (MapMarker) adapter.getItem(pos);
					if (marker.history) {
						return ItemTouchHelper.LEFT;
					} else {
						return ItemTouchHelper.RIGHT;
					}
				} else {
					return 0;
				}
			}

			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView,
			                      @NonNull RecyclerView.ViewHolder viewHolder,
			                      @NonNull RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder instanceof MapMarkerItemViewHolder) {
					if (!iconHidden && isCurrentlyActive) {
						((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.GONE);
						compassUpdateAllowed = false;
						iconHidden = true;
					}
					View itemView = viewHolder.itemView;
					int colorIcon;
					int colorText;
					if (Math.abs(dX) > itemView.getWidth() / 2) {
						colorIcon = R.color.map_widget_blue;
						colorText = R.color.map_widget_blue;
					} else {
						colorIcon = ColorUtilities.getDefaultIconColorId(nightMode);
						colorText = ColorUtilities.getSecondaryTextColorId(nightMode);
					}
					textPaint.setColor(ContextCompat.getColor(app, colorText));
					Drawable icon = uiUtilities.getIcon(
							dX > 0 ? R.drawable.ic_action_history : R.drawable.ic_action_delete_dark,
							colorIcon);
					int iconWidth = icon.getIntrinsicWidth();
					int iconHeight = icon.getIntrinsicHeight();
					float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
					float iconMarginTop = ((float) itemView.getHeight() - (float) iconHeight) / 2;
					int iconTopY = itemView.getTop() + (int) iconMarginTop;
					int iconLeftX;
					if (dX > 0) {
						iconLeftX = itemView.getLeft() + (int) marginSides;
						c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
						c.drawText(moveToHistoryStr, itemView.getLeft() + 2 * marginSides + iconWidth, itemView.getTop() + textMarginTop + textHeight, textPaint);
					} else {
						iconLeftX = itemView.getRight() - iconWidth - (int) marginSides;
						c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), backgroundPaint);
						c.drawText(delStr, itemView.getRight() - iconWidth - 2 * marginSides - delStrWidth, itemView.getTop() + textMarginTop + textHeight, textPaint);
					}
					icon.setBounds(iconLeftX, iconTopY, iconLeftX + iconWidth, iconTopY + iconHeight);
					icon.draw(c);
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}

			@Override
			public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerItemViewHolder) {
					((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.VISIBLE);
					iconHidden = false;
					compassUpdateAllowed = true;
				}
				super.clearView(recyclerView, viewHolder);
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				int pos = viewHolder.getAdapterPosition();
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker marker) {
					int snackbarStringRes;
					if (direction == ItemTouchHelper.RIGHT) {
						app.getMapMarkersHelper().moveMapMarkerToHistory((MapMarker) item);
						snackbarStringRes = R.string.marker_moved_to_history;
					} else {
						app.getMapMarkersHelper().removeMarker((MapMarker) item);
						snackbarStringRes = R.string.item_removed;
					}
					updateAdapter();
					snackbar = Snackbar.make(viewHolder.itemView, snackbarStringRes, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, view -> {
								if (direction == ItemTouchHelper.RIGHT) {
									app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
								} else {
									app.getMapMarkersHelper().addMarker(marker);
								}
								updateAdapter();
							});
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			}
		};
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
		itemTouchHelper.attachToRecyclerView(recyclerView);

		adapter = new MapMarkersGroupsAdapter(mapActivity);
		adapter.setListener(new MapMarkersGroupsAdapter.MapMarkersGroupsAdapterListener() {
			@Override
			public void onItemClick(@NonNull View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker marker) {
					if (!marker.history) {
						if (settings.SELECT_MARKER_ON_SINGLE_TAP.get()) {
							app.getMapMarkersHelper().moveMarkerToTop(marker);
							updateAdapter();
						} else {
							FavouritePoint fav = marker.favouritePoint == null
									? app.getFavoritesHelper().getVisibleFavByLatLon(marker.point)
									: marker.favouritePoint;
							if (fav != null) {
								showMap(marker.point, fav.getPointDescription(mapActivity), fav);
								return;
							}

							WptPt pt = marker.wptPt == null
									? app.getSelectedGpxHelper().getVisibleWayPointByLatLon(marker.point)
									: marker.wptPt;
							if (pt != null) {
								showMap(marker.point, new WptLocationPoint(pt).getPointDescription(mapActivity), pt);
								return;
							}

							Amenity mapObj = mapActivity.getMapLayers().getMapMarkersLayer().getMapObjectByMarker(marker);
							PointDescription desc = mapObj == null
									? marker.getPointDescription(mapActivity)
									: mapActivity.getMapLayers().getPoiMapLayer().getObjectName(mapObj);
							showMap(marker.point, desc, mapObj == null ? marker : mapObj);
						}
					} else {
						showHistoryMenuFragment(pos, marker);
					}
				}
			}

			private void showMap(LatLon latLon, PointDescription desc, Object objToShow) {
				settings.setMapLocationToShow(latLon.getLatitude(),
						latLon.getLongitude(), 15, desc, true, objToShow);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				if (getParentFragment() instanceof DialogFragment parent) {
					parent.dismiss();
				}
			}

			private void showHistoryMenuFragment(int position, MapMarker marker) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					HistoryMarkerMenuBottomSheetDialogFragment.showInstance(
							activity, getChildFragmentManager(), 
							createHistoryMarkerMenuListener(), position, marker);
				}
			}
		});
		adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
			@Override
			public void onChanged() {
				changeFabVisibilityIfNeeded();
			}

			@Override
			public void onItemRangeInserted(int positionStart, int itemCount) {
				changeFabVisibilityIfNeeded();
			}

			@Override
			public void onItemRangeRemoved(int positionStart, int itemCount) {
				changeFabVisibilityIfNeeded();
			}
		});

		View emptyView = mainView.findViewById(R.id.empty_view);
		mainView.findViewById(R.id.import_button).setOnClickListener(v -> openAddGroupMenu());
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			emptyImageView.setImageResource(nightMode ? R.drawable.ic_empty_state_marker_group_night : R.drawable.ic_empty_state_marker_group_day);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		recyclerView.setEmptyView(emptyView);
		recyclerView.setAdapter(adapter);

		mainView.findViewById(R.id.add_group_fab).setOnClickListener(v -> openAddGroupMenu());

		if (groupIdToOpen != null) {
			int groupHeaderPosition = adapter.getGroupHeaderPosition(groupIdToOpen);
			if (groupHeaderPosition != -1) {
				((EmptyStateRecyclerView) mainView.findViewById(R.id.list)).scrollToPosition(groupHeaderPosition);
			}
		}

		return mainView;
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		return null;
	}

	private HistoryMarkerMenuFragmentListener createHistoryMarkerMenuListener() {
		return new HistoryMarkerMenuFragmentListener() {
			@Override
			public void onMakeMarkerActive(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
					updateAdapter();
				}
			}

			@Override
			public void onDeleteMarker(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().removeMarker((MapMarker) item);
					updateAdapter();
				}
			}
		};
	}

	void setGroupIdToOpen(String groupIdToOpen) {
		this.groupIdToOpen = groupIdToOpen;
	}

	private void changeFabVisibilityIfNeeded() {
		mainView.findViewById(R.id.add_group_fab).setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
	}

	private void openAddGroupMenu() {
		FragmentManager childFragmentManager = getChildFragmentManager();
		AddMarkersGroupFragmentListener listener = createAddMarkersGroupFragmentListener();
		SelectionMarkersGroupBottomSheetDialogFragment.showInstance(childFragmentManager, listener);
	}

	@NonNull
	private AddMarkersGroupFragmentListener createAddMarkersGroupFragmentListener() {
		return new AddMarkersGroupFragmentListener() {
			@Override
			public void favouritesOnClick() {
				AddFavouritesGroupBottomSheetDialogFragment.showInstance(getChildFragmentManager());
			}

			@Override
			public void waypointsOnClick() {
				SelectTrackTabsFragment.showInstance(requireActivity().getSupportFragmentManager(), getGpxDataItemSelectionListener(), getItemVisibilityCallback());
			}
		};
	}

	private GpxDataItemSelectionListener getGpxDataItemSelectionListener() {
		return gpxDataItem -> {
			if (gpxDataItem != null) {
				GpxTrackAnalysis analysis = gpxDataItem.getAnalysis();
				if (analysis != null && analysis.getWptCategoryNamesSet() != null && analysis.getWptCategoryNamesSet().size() > 1) {
					Fragment parent = getParentFragment();
					if (parent != null) {
						FragmentManager fragmentManager = parent.getChildFragmentManager();
						String path = gpxDataItem.getFile().getParentFile().absolutePath();
						SelectWptCategoriesBottomSheetDialogFragment.showInstance(fragmentManager, path);
					}
				} else {
					GpxSelectionHelper selectionHelper = app.getSelectedGpxHelper();
					File gpx = SharedUtil.jFile(gpxDataItem.getFile());
					if (selectionHelper.getSelectedFileByPath(gpx.getAbsolutePath()) == null) {
						GpxFileLoaderTask.loadGpxFile(gpx, getActivity(), gpxFile -> {
							GpxSelectionParams params = GpxSelectionParams.newInstance()
									.showOnMap().selectedAutomatically().saveSelection();
							selectionHelper.selectGpxFile(gpxFile, params);
							app.getMapMarkersHelper().addOrEnableGpxGroup(gpx);
							return true;
						});
					} else {
						app.getMapMarkersHelper().addOrEnableGpxGroup(gpx);
					}
				}
			}
		};
	}

	private ItemVisibilityCallback getItemVisibilityCallback() {
		return trackItem -> {
			GpxDataItem item = trackItem.getDataItem();
			if (item != null) {
				GpxTrackAnalysis analysis = item.getAnalysis();
				return analysis != null && analysis.getWptPoints() > 0;
			}
			return false;
		};
	}

	void updateAdapter() {
		if (adapter != null) {
			adapter.updateDisplayedData();
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
	}

	void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	void hideSnackbar() {
		if (adapter != null) {
			adapter.hideSnackbar();
		}
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		if (adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}
}
