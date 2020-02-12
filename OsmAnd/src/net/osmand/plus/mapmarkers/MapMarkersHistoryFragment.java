package net.osmand.plus.mapmarkers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkerHeaderViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkerItemViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;
import net.osmand.plus.widgets.EmptyStateRecyclerView;

public class MapMarkersHistoryFragment extends Fragment implements MapMarkersHelper.MapMarkerChangedListener {

	private MapMarkersHistoryAdapter adapter;
	private OsmandApplication app;
	private Paint backgroundPaint = new Paint();
	private Paint iconPaint = new Paint();
	private Paint textPaint = new Paint();
	private Snackbar snackbar;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final boolean night = !app.getSettings().isLightContent();
		final MapActivity mapActivity = (MapActivity) getActivity();

		backgroundPaint.setColor(ContextCompat.getColor(getActivity(), night ? R.color.divider_color_dark : R.color.divider_color_light));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		iconPaint.setAntiAlias(true);
		iconPaint.setFilterBitmap(true);
		iconPaint.setDither(true);
		textPaint.setTextSize(getResources().getDimension(R.dimen.default_desc_text_size));
		textPaint.setFakeBoldText(true);
		textPaint.setAntiAlias(true);

		final String delStr = getString(R.string.shared_string_delete).toUpperCase();
		final String activateStr = getString(R.string.local_index_mi_restore).toUpperCase();
		Rect bounds = new Rect();

		textPaint.getTextBounds(activateStr, 0, activateStr.length(), bounds);
		final int activateStrWidth = bounds.width();
		final int textHeight = bounds.height();

		Fragment historyMarkerMenuFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(HistoryMarkerMenuBottomSheetDialogFragment.TAG);
		if (historyMarkerMenuFragment != null) {
			((HistoryMarkerMenuBottomSheetDialogFragment) historyMarkerMenuFragment).setListener(createHistoryMarkerMenuListener());
		}

		final View mainView = UiUtilities.getInflater(mapActivity, night).inflate(R.layout.fragment_map_markers_history, container, false);
		final EmptyStateRecyclerView recyclerView = (EmptyStateRecyclerView) mainView.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			private float marginSides = getResources().getDimension(R.dimen.list_content_padding);
			private Bitmap deleteBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_delete_dark);
			private Bitmap resetBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_reset_to_default_dark);
			private boolean iconHidden;

			@Override
			public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerHeaderViewHolder) {
					return 0;
				}
				return super.getSwipeDirs(recyclerView, viewHolder);
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
						iconHidden = true;
					}
					View itemView = viewHolder.itemView;
					int colorIcon;
					int colorText;
					if (Math.abs(dX) > itemView.getWidth() / 2) {
						colorIcon = R.color.map_widget_blue;
						colorText = R.color.map_widget_blue;
					} else {
						colorIcon = night ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
						colorText = night ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
					}
					if (colorIcon != 0) {
						iconPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getActivity(), colorIcon), PorterDuff.Mode.SRC_IN));
					}
					textPaint.setColor(ContextCompat.getColor(getActivity(), colorText));
					float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
					if (dX > 0) {
						c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) deleteBitmap.getHeight()) / 2;
						c.drawBitmap(deleteBitmap, itemView.getLeft() + marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(delStr, itemView.getLeft() + 2 * marginSides + deleteBitmap.getWidth(), itemView.getTop() + textMarginTop + textHeight, textPaint);
					} else {
						c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) resetBitmap.getHeight()) / 2;
						c.drawBitmap(resetBitmap, itemView.getRight() - resetBitmap.getWidth() - marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(activateStr, itemView.getRight() - resetBitmap.getWidth() - 2 * marginSides - activateStrWidth, itemView.getTop() + textMarginTop + textHeight, textPaint);
					}
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}

			@Override
			public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerItemViewHolder) {
					((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.VISIBLE);
					iconHidden = false;
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
					if (direction == ItemTouchHelper.LEFT) {
						app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
						snackbarStringRes = R.string.marker_moved_to_active;
					} else {
						app.getMapMarkersHelper().removeMarker((MapMarker) item);
						snackbarStringRes = R.string.item_removed;
					}
					snackbar = Snackbar.make(viewHolder.itemView, snackbarStringRes, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									if (direction == ItemTouchHelper.LEFT) {
										app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
									} else {
										app.getMapMarkersHelper().addMarker(marker);
									}
								}
							});
					AndroidUtils.setSnackbarTextColor(snackbar, night ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
					snackbar.getView().setBackgroundColor(ContextCompat.getColor(app, night ? R.color.list_background_color_dark : R.color.list_background_color_light));
					snackbar.show();
				}
			}
		};
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
		itemTouchHelper.attachToRecyclerView(recyclerView);

		adapter = new MapMarkersHistoryAdapter(mapActivity.getMyApplication());
		adapter.setAdapterListener(new MapMarkersHistoryAdapter.MapMarkersHistoryAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					HistoryMarkerMenuBottomSheetDialogFragment fragment = new HistoryMarkerMenuBottomSheetDialogFragment();
					fragment.setUsedOnMap(false);
					Bundle arguments = new Bundle();
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_POSITION, pos);
					arguments.putString(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_NAME, marker.getName(mapActivity));
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_COLOR_INDEX, marker.colorIndex);
					arguments.putLong(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_VISITED_DATE, marker.visitedDate);
					fragment.setArguments(arguments);
					fragment.setListener(createHistoryMarkerMenuListener());
					fragment.show(mapActivity.getSupportFragmentManager(), HistoryMarkerMenuBottomSheetDialogFragment.TAG);
				}
			}
		});
		final View emptyView = mainView.findViewById(R.id.empty_view);
		ImageView emptyImageView = (ImageView) emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			emptyImageView.setImageResource(night ? R.drawable.ic_empty_state_marker_history_night : R.drawable.ic_empty_state_marker_history_day);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		recyclerView.setEmptyView(emptyView);
		recyclerView.setAdapter(adapter);

		app.getMapMarkersHelper().addListener(this);

		return mainView;
	}

	void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
		if (adapter != null) {
			adapter.hideSnackbar();
		}
	}

	private HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener createHistoryMarkerMenuListener() {
		return new HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener() {
			@Override
			public void onMakeMarkerActive(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
				}
			}

			@Override
			public void onDeleteMarker(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().removeMarker((MapMarker) item);
				}
			}
		};
	}

	@Override
	public void onDestroy() {
		app.getMapMarkersHelper().removeListener(this);
		super.onDestroy();
	}

	void updateAdapter() {
		if (adapter != null) {
			adapter.createHeaders();
			adapter.notifyDataSetChanged();
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		updateAdapter();
	}

	@Override
	public void onMapMarkersChanged() {
		updateAdapter();
	}
}
