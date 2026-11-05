package net.osmand.plus.mapmarkers.adapters;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.settings.fragments.HistoryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapMarkersHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;

	private final OsmandApplication app;
	private List<Object> items = new ArrayList<>();
	private Map<Integer, List<MapMarker>> markerGroups = new HashMap<>();
	private MapMarkersHistoryAdapterListener listener;
	private Snackbar snackbar;
	private final boolean nightMode;

	public MapMarkersHistoryAdapter(OsmandApplication app) {
		this.app = app;
		nightMode = !app.getSettings().isLightContent();
		createHeaders();
	}

	public void createHeaders() {
		items = new ArrayList<>();
		markerGroups = new HashMap<>();

		if (app.getSettings().MAP_MARKERS_HISTORY.get()) {
			List<MapMarker> markersHistory = app.getMapMarkersHelper().getMapMarkersHistory();
			List<Pair<Long, MapMarker>> pairs = new ArrayList<>();
			for (MapMarker marker : markersHistory) {
				pairs.add(new Pair<>(marker.visitedDate, marker));
			}
			HistoryAdapter.createHistoryGroups(pairs, markerGroups, items);
		}
	}

	public void setAdapterListener(MapMarkersHistoryAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		if (viewType == MARKER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.onItemClick(view);
				}
			});
			return new MapMarkerItemViewHolder(view);
		} else if (viewType == HEADER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_header, viewGroup, false);
			return new MapMarkerHeaderViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		UiUtilities iconsCache = app.getUIUtilities();
		if (holder instanceof MapMarkerItemViewHolder) {
			MapMarkerItemViewHolder itemViewHolder = (MapMarkerItemViewHolder) holder;
			MapMarker marker = (MapMarker) getItem(position);
			itemViewHolder.iconReorder.setVisibility(View.GONE);

			int color = ColorUtilities.getDefaultIconColorId(nightMode);
			int actionIconColor = nightMode ? R.color.icon_color_primary_dark : R.color.icon_color_primary_light;
			itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag, color));

			itemViewHolder.title.setText(marker.getName(app));

			String desc = app.getString(R.string.passed, OsmAndFormatter.getFormattedDate(app, marker.visitedDate));
			String markerGroupName = marker.groupName;
			if (markerGroupName != null) {
				if (markerGroupName.isEmpty()) {
					markerGroupName = app.getString(R.string.shared_string_favorites);
				}
				desc += " • " + markerGroupName;
			}
			itemViewHolder.description.setText(desc);

			itemViewHolder.optionsBtn.setBackground(AppCompatResources.getDrawable(app, nightMode ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
			itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_reset_to_default_dark, actionIconColor));
			itemViewHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int position = itemViewHolder.getAdapterPosition();
					if (position < 0) {
						return;
					}
					app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);

					snackbar = Snackbar.make(itemViewHolder.itemView, app.getString(R.string.marker_moved_to_active), Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
								}
							});
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			});
			itemViewHolder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			itemViewHolder.iconDirection.setVisibility(View.GONE);
			itemViewHolder.leftPointSpace.setVisibility(View.GONE);
			itemViewHolder.rightPointSpace.setVisibility(View.GONE);
			boolean lastItem = position == getItemCount() - 1;
			if ((getItemCount() > position + 1 && getItemViewType(position + 1) == HEADER_TYPE) || lastItem) {
				itemViewHolder.divider.setVisibility(View.GONE);
			} else {
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.divider_color_light));
				itemViewHolder.divider.setVisibility(View.VISIBLE);
			}
			itemViewHolder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		} else if (holder instanceof MapMarkerHeaderViewHolder) {
			MapMarkerHeaderViewHolder dateViewHolder = (MapMarkerHeaderViewHolder) holder;
			Integer dateHeader = (Integer) getItem(position);

			dateViewHolder.disableGroupSwitch.setVisibility(View.GONE);
			dateViewHolder.title.setText(HistoryAdapter.getDateForHeader(app, dateHeader));
			dateViewHolder.clearButton.setVisibility(View.VISIBLE);
			dateViewHolder.articleDescription.setVisibility(View.GONE);

			dateViewHolder.clearButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					List<MapMarker> group = markerGroups.get(dateHeader);
					if (group == null) {
						return;
					}
					app.getMapMarkersHelper().removeMarkers(group);
					snackbar = Snackbar.make(holder.itemView, app.getString(R.string.n_items_removed), Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									for (MapMarker marker : group) {
										app.getMapMarkersHelper().addMarker(marker);
									}
								}
							});
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			});
		}
	}

	public void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof Integer) {
			return HEADER_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	public interface MapMarkersHistoryAdapterListener {

		void onItemClick(View view);
	}
}
