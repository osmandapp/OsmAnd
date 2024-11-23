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

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.Location;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.WptLocationPoint;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
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

public class MapMarkersGroupsFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "MapMarkersGroupsFragment";

	private OsmandApplication app;
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

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) getActivity();
		boolean night = !mapActivity.getMyApplication().getSettings().isLightContent();
		mainView = UiUtilities.getInflater(mapActivity, night).inflate(R.layout.fragment_map_markers_groups, container, false);

		Fragment selectionMarkersGroupFragment = getChildFragmentManager().findFragmentByTag(SelectionMarkersGroupBottomSheetDialogFragment.TAG);
		if (selectionMarkersGroupFragment != null) {
			((SelectionMarkersGroupBottomSheetDialogFragment) selectionMarkersGroupFragment).setListener(createAddMarkersGroupFragmentListener());
		}
		Fragment historyMarkerMenuFragment = getChildFragmentManager().findFragmentByTag(HistoryMarkerMenuBottomSheetDialogFragment.TAG);
		if (historyMarkerMenuFragment != null) {
			((HistoryMarkerMenuBottomSheetDialogFragment) historyMarkerMenuFragment).setListener(createHistoryMarkerMenuListener());
		}

		EmptyStateRecyclerView recyclerView = mainView.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		backgroundPaint.setColor(ColorUtilities.getDividerColor(getActivity(), night));
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
			public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
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
			public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
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
						colorIcon = ColorUtilities.getDefaultIconColorId(night);
						colorText = ColorUtilities.getSecondaryTextColorId(night);
					}
					textPaint.setColor(ContextCompat.getColor(app, colorText));
					Drawable icon = app.getUIUtilities().getIcon(
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
			public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerItemViewHolder) {
					((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.VISIBLE);
					iconHidden = false;
					compassUpdateAllowed = true;
				}
				super.clearView(recyclerView, viewHolder);
			}

			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				int pos = viewHolder.getAdapterPosition();
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
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
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									if (direction == ItemTouchHelper.RIGHT) {
										app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
									} else {
										app.getMapMarkersHelper().addMarker(marker);
									}
									updateAdapter();
								}
							});
					UiUtilities.setupSnackbar(snackbar, night);
					snackbar.show();
				}
			}
		};
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
		itemTouchHelper.attachToRecyclerView(recyclerView);

		adapter = new MapMarkersGroupsAdapter(mapActivity);
		adapter.setListener(new MapMarkersGroupsAdapter.MapMarkersGroupsAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					OsmandApplication app = mapActivity.getMyApplication();
					if (!marker.history) {
						if (app.getSettings().SELECT_MARKER_ON_SINGLE_TAP.get()) {
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
				mapActivity.getMyApplication().getSettings().setMapLocationToShow(latLon.getLatitude(),
						latLon.getLongitude(), 15, desc, true, objToShow);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				((DialogFragment) getParentFragment()).dismiss();
			}

			private void showHistoryMenuFragment(int pos, MapMarker marker) {
				HistoryMarkerMenuBottomSheetDialogFragment fragment = new HistoryMarkerMenuBottomSheetDialogFragment();
				fragment.setUsedOnMap(false);
				Bundle arguments = new Bundle();
				arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_POSITION, pos);
				arguments.putString(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_NAME, marker.getName(mapActivity));
				arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_COLOR_INDEX, marker.colorIndex);
				arguments.putLong(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_VISITED_DATE, marker.visitedDate);
				fragment.setArguments(arguments);
				fragment.setListener(createHistoryMarkerMenuListener());
				fragment.show(getChildFragmentManager(), HistoryMarkerMenuBottomSheetDialogFragment.TAG);
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
		mainView.findViewById(R.id.import_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openAddGroupMenu();
			}
		});
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			emptyImageView.setImageResource(night ? R.drawable.ic_empty_state_marker_group_night : R.drawable.ic_empty_state_marker_group_day);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		recyclerView.setEmptyView(emptyView);
		recyclerView.setAdapter(adapter);

		mainView.findViewById(R.id.add_group_fab).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openAddGroupMenu();
			}
		});

		if (groupIdToOpen != null) {
			int groupHeaderPosition = adapter.getGroupHeaderPosition(groupIdToOpen);
			if (groupHeaderPosition != -1) {
				((EmptyStateRecyclerView) mainView.findViewById(R.id.list)).scrollToPosition(groupHeaderPosition);
			}
		}

		return mainView;
	}

	private HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener createHistoryMarkerMenuListener() {
		return new HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener() {
			@Override
			public void onMakeMarkerActive(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					if (getMyApplication() != null) {
						getMyApplication().getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
					}
					updateAdapter();
				}
			}

			@Override
			public void onDeleteMarker(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					if (getMyApplication() != null) {
						getMyApplication().getMapMarkersHelper().removeMarker((MapMarker) item);
					}
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
		SelectionMarkersGroupBottomSheetDialogFragment fragment = new SelectionMarkersGroupBottomSheetDialogFragment();
		fragment.setListener(createAddMarkersGroupFragmentListener());
		fragment.setUsedOnMap(false);
		fragment.show(getChildFragmentManager(), SelectionMarkersGroupBottomSheetDialogFragment.TAG);
	}

	private void openAddGroupMenu(AddGroupBottomSheetDialogFragment fragment) {
		fragment.setUsedOnMap(false);
		fragment.setRetainInstance(true);
		fragment.show(getChildFragmentManager(), AddGroupBottomSheetDialogFragment.TAG);
	}

	private AddMarkersGroupFragmentListener createAddMarkersGroupFragmentListener() {
		return new AddMarkersGroupFragmentListener() {
			@Override
			public void favouritesOnClick() {
				openAddGroupMenu(new AddFavouritesGroupBottomSheetDialogFragment());
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
					Bundle args = new Bundle();
					args.putString(SelectWptCategoriesBottomSheetDialogFragment.GPX_FILE_PATH_KEY, gpxDataItem.getFile().getParentFile().absolutePath());

					SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
					fragment.setArguments(args);
					fragment.setUsedOnMap(false);
					fragment.show(getParentFragment().getChildFragmentManager(), SelectWptCategoriesBottomSheetDialogFragment.TAG);
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
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
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

	private OsmandApplication getMyApplication() {
		if (getActivity() != null) {
			return ((MapActivity) getActivity()).getMyApplication();
		}
		return null;
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(() -> {
				if (location == null) {
					location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}
}
