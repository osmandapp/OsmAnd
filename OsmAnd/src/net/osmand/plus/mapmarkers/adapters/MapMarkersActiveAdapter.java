package net.osmand.plus.mapmarkers.adapters;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

public class MapMarkersActiveAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder>
		implements MapMarkersItemTouchHelperCallback.ItemTouchHelperAdapter {

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private List<MapMarker> markers;
	private MapMarkersActiveAdapterListener listener;
	private Snackbar snackbar;
	private boolean showDirectionEnabled;

	private final boolean nightMode;
	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache updateLocationViewCache;

	public MapMarkersActiveAdapter(@NonNull MapActivity mapActivity) {
		setHasStableIds(true);
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		uiUtilities = app.getUIUtilities();
		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(mapActivity);
		markers = app.getMapMarkersHelper().getMapMarkers();
		nightMode = !app.getSettings().isLightContent();
		showDirectionEnabled = WidgetsVisibilityHelper.isWidgetEnabled(mapActivity, TOP, MARKERS_TOP_BAR.id);
	}

	public void setShowDirectionEnabled(boolean showDirectionEnabled) {
		this.showDirectionEnabled = showDirectionEnabled;
	}

	public void setAdapterListener(MapMarkersActiveAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
		view.setOnClickListener(v -> listener.onItemClick(v));
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull MapMarkerItemViewHolder holder, int pos) {
		MapMarker marker = markers.get(pos);

		ImageView markerImageViewToUpdate;
		int drawableResToUpdate;
		int markerColor = MapMarker.getColorId(marker.colorIndex);
		int actionIconColor = nightMode ? R.color.icon_color_primary_dark : R.color.icon_color_primary_light;
		LatLon markerLatLon = new LatLon(marker.getLatitude(), marker.getLongitude());
		boolean displayedInWidget = pos < app.getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		if (showDirectionEnabled && displayedInWidget) {
			holder.iconDirection.setVisibility(View.GONE);

			holder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_arrow_marker_diretion, markerColor));
			holder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.list_divider_dark : R.color.markers_top_bar_background));
			holder.title.setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.text_color_primary_dark : R.color.card_and_list_background_light));
			holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_divider_color));
			holder.optionsBtn.setBackground(AppCompatResources.getDrawable(mapActivity, R.drawable.marker_circle_background_on_map_with_inset));
			holder.optionsBtn.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_passed, R.color.card_and_list_background_light));
			holder.iconReorder.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, R.color.icon_color_default_light));
			holder.description.setTextColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_color));

			drawableResToUpdate = R.drawable.ic_arrow_marker_diretion;
			markerImageViewToUpdate = holder.icon;
		} else {
			holder.iconDirection.setVisibility(View.VISIBLE);
			holder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_flag, markerColor));

			holder.mainLayout.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode));
			holder.title.setTextColor(ColorUtilities.getPrimaryTextColor(mapActivity, nightMode));
			holder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.app_bar_main_dark : R.color.divider_color_light));
			holder.optionsBtn.setBackground(AppCompatResources.getDrawable(mapActivity, nightMode ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
			holder.optionsBtn.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_passed, actionIconColor));
			holder.iconReorder.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_item_move));
			holder.description.setTextColor(ColorUtilities.getDefaultIconColor(mapActivity, nightMode));


			drawableResToUpdate = R.drawable.ic_direction_arrow;
			markerImageViewToUpdate = holder.iconDirection;
		}
		if (pos == getItemCount() - 1) {
			holder.bottomShadow.setVisibility(View.VISIBLE);
			holder.divider.setVisibility(View.GONE);
		} else {
			holder.bottomShadow.setVisibility(View.GONE);
			holder.divider.setVisibility(View.VISIBLE);
		}

		holder.point.setVisibility(View.VISIBLE);

		holder.iconReorder.setOnTouchListener((view, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onDragStarted(holder);
			}
			return false;
		});

		holder.title.setText(marker.getName(mapActivity));

		String descr;
		if ((descr = marker.groupName) != null) {
			if (descr.isEmpty()) {
				descr = mapActivity.getString(R.string.shared_string_favorites);
			}
		} else {
			descr = OsmAndFormatter.getFormattedDate(mapActivity, marker.creationDate);
		}
		if (marker.wptPt != null && !Algorithms.isEmpty(marker.wptPt.getCategory())) {
			descr = marker.wptPt.getCategory() + ", " + descr;
		}
		holder.description.setText(descr);

		holder.optionsBtn.setOnClickListener(view -> {
			int position = holder.getAdapterPosition();
			if (position < 0) {
				return;
			}
			MapMarker mapMarker = markers.get(position);

			app.getMapMarkersHelper().moveMapMarkerToHistory(mapMarker);
			changeMarkers();
			notifyDataSetChanged();

			snackbar = Snackbar.make(holder.itemView, mapActivity.getString(R.string.marker_moved_to_history), Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_undo, v -> {
						app.getMapMarkersHelper().restoreMarkerFromHistory(mapMarker, position);
						changeMarkers();
						notifyDataSetChanged();
					});
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		});

		updateLocationViewCache.arrowResId = drawableResToUpdate;
		updateLocationViewCache.arrowColor = showDirectionEnabled && displayedInWidget ? markerColor : 0;
		UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, markerImageViewToUpdate, holder.distance, markerLatLon);
	}

	@Override
	public int getItemCount() {
		return markers.size();
	}

	public MapMarker getItem(int position) {
		return markers.get(position);
	}

	public List<MapMarker> getItems() {
		return markers;
	}

	public void changeMarkers() {
		markers = app.getMapMarkersHelper().getMapMarkers();
	}

	public void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	@Override
	public void onSwipeStarted() {
		listener.onSwipeStarted();
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(markers, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemSwiped(RecyclerView.ViewHolder holder) {
		int pos = holder.getAdapterPosition();
		MapMarker marker = getItem(pos);
		app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
		MapMarkersGroup group = app.getMapMarkersHelper().getMapMarkerGroupById(marker.groupKey,
				ItineraryType.MARKERS);
		if (group != null) {
			app.getMapMarkersHelper().updateGroup(group);
		}
		changeMarkers();
		notifyDataSetChanged();
		snackbar = Snackbar.make(holder.itemView, R.string.marker_moved_to_history, Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_undo, view -> {
					app.getMapMarkersHelper().restoreMarkerFromHistory(marker, pos);
					changeMarkers();
					notifyDataSetChanged();
				});
		UiUtilities.setupSnackbar(snackbar, nightMode);
		snackbar.show();
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	public interface MapMarkersActiveAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onSwipeStarted();
	}
}
