package net.osmand.plus.mapmarkers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.mapmarkers.adapters.MapMarkerItemViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkersGroupsAdapter;
import net.osmand.plus.mapmarkers.SelectionMarkersGroupBottomSheetDialogFragment.AddMarkersGroupFragmentListener;
import net.osmand.plus.widgets.EmptyStateRecyclerView;
import net.osmand.util.MapUtils;

public class MapMarkersGroupsFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "MapMarkersGroupsFragment";

	private MapMarkersGroupsAdapter adapter;
	private Float heading;
	private Location location;
	private boolean locationUpdateStarted;
	private Paint backgroundPaint = new Paint();
	private Paint iconPaint = new Paint();
	private Paint textPaint = new Paint();
	private Snackbar snackbar;
	private boolean compassUpdateAllowed = true;
	private View mainView;
	private String groupIdToOpen;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final boolean night = !mapActivity.getMyApplication().getSettings().isLightContent();
		mainView = inflater.inflate(R.layout.fragment_map_markers_groups, container, false);

		Fragment selectionMarkersGroupFragment = getChildFragmentManager().findFragmentByTag(SelectionMarkersGroupBottomSheetDialogFragment.TAG);
		if (selectionMarkersGroupFragment != null) {
			((SelectionMarkersGroupBottomSheetDialogFragment) selectionMarkersGroupFragment).setListener(createAddMarkersGroupFragmentListener());
		}
		Fragment addGroupFragment = getChildFragmentManager().findFragmentByTag(AddGroupBottomSheetDialogFragment.TAG);
		if (addGroupFragment != null) {
			((AddGroupBottomSheetDialogFragment) addGroupFragment).setListener(createAddGroupListener());
		}

		final EmptyStateRecyclerView recyclerView = (EmptyStateRecyclerView) mainView.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		backgroundPaint.setColor(ContextCompat.getColor(getActivity(), night ? R.color.dashboard_divider_dark : R.color.dashboard_divider_light));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		iconPaint.setAntiAlias(true);
		iconPaint.setFilterBitmap(true);
		iconPaint.setDither(true);
		textPaint.setTextSize(getResources().getDimension(R.dimen.default_desc_text_size));
		textPaint.setFakeBoldText(true);
		textPaint.setAntiAlias(true);

		final String delStr = getString(R.string.shared_string_delete).toUpperCase();
		final String moveToHistoryStr = getString(R.string.move_to_history).toUpperCase();
		final Rect bounds = new Rect();

		textPaint.getTextBounds(delStr, 0, delStr.length(), bounds);
		final int delStrWidth = bounds.width();
		final int textHeight = bounds.height();

		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			private float marginSides = getResources().getDimension(R.dimen.list_content_padding);
			private Bitmap deleteBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_delete_dark);
			private Bitmap historyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_history);
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
						colorIcon = night ? 0 : R.color.icon_color;
						colorText = R.color.dashboard_subheader_text_light;
					}
					if (colorIcon != 0) {
						iconPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getActivity(), colorIcon), PorterDuff.Mode.SRC_IN));
					}
					textPaint.setColor(ContextCompat.getColor(getActivity(), colorText));
					float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
					if (dX > 0) {
						c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) historyBitmap.getHeight()) / 2;
						c.drawBitmap(historyBitmap, itemView.getLeft() + marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(moveToHistoryStr, itemView.getLeft() + 2 * marginSides + historyBitmap.getWidth(),
								itemView.getTop() + textMarginTop + textHeight, textPaint);
					} else {
						c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) deleteBitmap.getHeight()) / 2;
						c.drawBitmap(deleteBitmap, itemView.getRight() - deleteBitmap.getWidth() - marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(delStr, itemView.getRight() - deleteBitmap.getWidth() - 2 * marginSides - delStrWidth,
								itemView.getTop() + textMarginTop + textHeight, textPaint);
					}
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
			public void onSwiped(RecyclerView.ViewHolder viewHolder, final int direction) {
				final int pos = viewHolder.getAdapterPosition();
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					final MapMarker marker = (MapMarker) item;
					int snackbarStringRes;
					if (direction == ItemTouchHelper.RIGHT) {
						mapActivity.getMyApplication().getMapMarkersHelper().moveMapMarkerToHistory((MapMarker) item);
						snackbarStringRes = R.string.marker_moved_to_history;
					} else {
						mapActivity.getMyApplication().getMapMarkersHelper().removeMarker((MapMarker) item);
						snackbarStringRes = R.string.item_removed;
					}
					updateAdapter();
					snackbar = Snackbar.make(viewHolder.itemView, snackbarStringRes, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									if (direction == ItemTouchHelper.RIGHT) {
										mapActivity.getMyApplication().getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
									} else {
										mapActivity.getMyApplication().getMapMarkersHelper().addMarker(marker);
									}
									updateAdapter();
								}
							});
					View snackBarView = snackbar.getView();
					TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
					tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
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
					mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
							15, marker.getPointDescription(mapActivity), true, marker);
					MapActivity.launchMapActivityMoveToTop(mapActivity);
					((DialogFragment) getParentFragment()).dismiss();
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

		final View emptyView = mainView.findViewById(R.id.empty_view);
		mainView.findViewById(R.id.import_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openAddGroupMenu();
			}
		});
		ImageView emptyImageView = (ImageView) emptyView.findViewById(R.id.empty_state_image_view);
		emptyImageView.setImageResource(night ? R.drawable.ic_empty_state_marker_group_night : R.drawable.ic_empty_state_marker_group_day);
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
		fragment.setListener(createAddGroupListener());
		fragment.setUsedOnMap(false);
		fragment.setRetainInstance(true);
		fragment.show(getChildFragmentManager(), AddGroupBottomSheetDialogFragment.TAG);
	}

	private AddGroupBottomSheetDialogFragment.AddGroupListener createAddGroupListener() {
		return new AddGroupBottomSheetDialogFragment.AddGroupListener() {
			@Override
			public void onGroupAdded() {
				updateAdapter();
			}
		};
	}

	private AddMarkersGroupFragmentListener createAddMarkersGroupFragmentListener() {
		return new AddMarkersGroupFragmentListener() {
			@Override
			public void favouritesOnClick() {
				AddFavouritesGroupBottomSheetDialogFragment fragment = new AddFavouritesGroupBottomSheetDialogFragment();
				openAddGroupMenu(fragment);
			}

			@Override
			public void waypointsOnClick() {
				AddTracksGroupBottomSheetDialogFragment fragment = new AddTracksGroupBottomSheetDialogFragment();
				openAddGroupMenu(fragment);
			}
		};
	}

	void updateAdapter() {
		if (adapter != null) {
			adapter.createDisplayGroups();
			adapter.updateShowDirectionMarkers();
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setScreenOrientation(DashLocationFragment.getScreenOrientation(getActivity()));
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
		boolean newLocation = this.location == null && location != null;
		boolean locationChanged = this.location != null && location != null
				&& this.location.getLatitude() != location.getLatitude()
				&& this.location.getLongitude() != location.getLongitude();
		if (newLocation || locationChanged) {
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
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (location == null) {
						location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					}
					MapViewTrackingUtilities utilities = mapActivity.getMapViewTrackingUtilities();
					boolean useCenter = !(utilities.isMapLinkedToLocation() && location != null);

					adapter.setUseCenter(useCenter);
					adapter.setLocation(useCenter ? mapActivity.getMapLocation() : new LatLon(location.getLatitude(), location.getLongitude()));
					adapter.setHeading(useCenter ? -mapActivity.getMapRotate() : heading != null ? heading : 99);
					adapter.notifyDataSetChanged();
				}
			});
		}
	}
}
