package net.osmand.plus.settings.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.LatLon;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;
import net.osmand.plus.search.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;
	private static final int SEARCH_TYPE = 3;
	private static final int TRACK_TYPE = 4;

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache locationViewCache;

	private List<Object> items = new ArrayList<>();
	private List<?> selectedItems = new ArrayList<>();
	private Map<Integer, List<?>> itemsGroups = new HashMap<>();

	private final OnItemSelectedListener listener;
	private final boolean nightMode;

	public HistoryAdapter(@NonNull OsmandApplication app, @Nullable OnItemSelectedListener listener, boolean nightMode) {
		this.app = app;
		this.listener = listener;
		this.nightMode = nightMode;
		uiUtilities = app.getUIUtilities();
		locationViewCache = app.getUIUtilities().getUpdateLocationViewCache();
	}

	public void updateSettingsItems(List<Object> items,
									Map<Integer, List<?>> markerGroups,
									List<?> selectedItems) {
		this.items = items;
		this.itemsGroups = markerGroups;
		this.selectedItems = selectedItems;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(viewGroup.getContext(), nightMode);
		switch (viewType) {
			case HEADER_TYPE: {
				View view = inflater.inflate(R.layout.history_preference_header, viewGroup, false);
				return new HeaderViewHolder(view);
			}
			case MARKER_TYPE: {
				View view = inflater.inflate(R.layout.history_preference_item, viewGroup, false);
				return new MarkerViewHolder(view);
			}
			case SEARCH_TYPE: {
				View view = inflater.inflate(R.layout.history_preference_item, viewGroup, false);
				return new SearchItemViewHolder(view);
			}
			case TRACK_TYPE: {
				View view = inflater.inflate(R.layout.history_gpx_item, viewGroup, false);
				return new TrackViewHolder(view);
			}
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof SearchItemViewHolder) {
			SearchItemViewHolder viewHolder = (SearchItemViewHolder) holder;
			SearchResult searchResult = (SearchResult) getItem(position);

			QuickSearchListItem listItem = new QuickSearchListItem(app, searchResult);
			QuickSearchListAdapter.bindSearchResult((LinearLayout) viewHolder.itemView, listItem);

			viewHolder.compoundButton.setChecked(selectedItems.contains(searchResult));
			viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean selected = !viewHolder.compoundButton.isChecked();
					if (listener != null) {
						listener.onItemSelected(searchResult, selected);
					}
					notifyDataSetChanged();
				}
			});

			int iconColorId = nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light;
			int iconColor = ContextCompat.getColor(app, iconColorId);
			viewHolder.icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));

			boolean lastItem = position == getItemCount() - 1;
			AndroidUiHelper.updateVisibility(viewHolder.divider, lastItem);
			updateCompassVisibility(viewHolder.compassView, searchResult.location);
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), viewHolder.compoundButton);
		} else if (holder instanceof TrackViewHolder) {
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			SearchResult searchResult = (SearchResult) getItem(position);

			QuickSearchListItem listItem = new QuickSearchListItem(app, searchResult);
			GPXInfo gpxInfo = (GPXInfo) searchResult.relatedObject;
			QuickSearchListAdapter.bindGpxTrack(viewHolder.itemView, listItem, gpxInfo);

			viewHolder.compoundButton.setChecked(selectedItems.contains(searchResult));
			viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean selected = !viewHolder.compoundButton.isChecked();
					if (listener != null) {
						listener.onItemSelected(searchResult, selected);
					}
					notifyDataSetChanged();
				}
			});

			int iconColorId = nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light;
			int iconColor = ContextCompat.getColor(app, iconColorId);
			viewHolder.icon.setImageDrawable(UiUtilities.tintDrawable(listItem.getIcon(), iconColor));

			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), viewHolder.compoundButton);
		} else if (holder instanceof MarkerViewHolder) {
			MarkerViewHolder viewHolder = (MarkerViewHolder) holder;
			MapMarker mapMarker = (MapMarker) getItem(position);

			boolean selected = selectedItems.contains(mapMarker);
			int color = selected ? MapMarker.getColorId(mapMarker.colorIndex) : ColorUtilities.getDefaultIconColorId(nightMode);

			viewHolder.title.setText(mapMarker.getName(app));
			viewHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_flag, color));
			viewHolder.compoundButton.setChecked(selected);
			viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean selected = !viewHolder.compoundButton.isChecked();
					if (listener != null) {
						listener.onItemSelected(mapMarker, selected);
					}
					notifyDataSetChanged();
				}
			});
			boolean lastItem = position == getItemCount() - 1;
			AndroidUiHelper.updateVisibility(viewHolder.divider, lastItem);
			updateCompassVisibility(viewHolder.compassView, mapMarker.point);
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), viewHolder.compoundButton);
		} else if (holder instanceof HeaderViewHolder) {
			final HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
			final Integer dateHeader = (Integer) getItem(position);

			viewHolder.title.setText(MapMarkersHistoryAdapter.getDateForHeader(app, dateHeader));
			viewHolder.compoundButton.setChecked(selectedItems.containsAll(itemsGroups.get(dateHeader)));

			viewHolder.itemView.setOnClickListener(v -> {
				List<?> items = itemsGroups.get(dateHeader);
				if (items != null) {
					boolean selected = !viewHolder.compoundButton.isChecked();
					if (listener != null) {
						listener.onCategorySelected(new ArrayList<>(items), selected);
					}
					notifyDataSetChanged();
				}
			});
			AndroidUiHelper.updateVisibility(viewHolder.divider, position > 0);
			AndroidUiHelper.updateVisibility(viewHolder.shadowDivider, position == 0);
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), viewHolder.compoundButton);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof Integer) {
			return HEADER_TYPE;
		} else if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof SearchResult) {
			SearchResult searchResult = (SearchResult) item;
			return searchResult.objectType == ObjectType.GPX_TRACK ? TRACK_TYPE : SEARCH_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	private void updateCompassVisibility(@NonNull View compassView, @Nullable LatLon location) {
		ImageView direction = compassView.findViewById(R.id.direction);
		TextView distanceText = compassView.findViewById(R.id.distance);

		boolean showCompass = location != null;
		if (showCompass) {
			app.getUIUtilities().updateLocationView(locationViewCache, direction, distanceText, location);
		}
		AndroidUiHelper.updateVisibility(compassView, showCompass);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	interface OnItemSelectedListener {

		void onItemSelected(Object item, boolean selected);

		void onCategorySelected(List<Object> type, boolean selected);

	}

	private static class HeaderViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final View divider;
		final View shadowDivider;
		final CompoundButton compoundButton;

		public HeaderViewHolder(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			divider = itemView.findViewById(R.id.divider);
			shadowDivider = itemView.findViewById(R.id.shadow_divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
		}
	}

	private static class SearchItemViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final CompoundButton compoundButton;
		final View divider;
		final View compassView;

		public SearchItemViewHolder(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.imageView);
			divider = itemView.findViewById(R.id.divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
			compassView = itemView.findViewById(R.id.compass_layout);
		}
	}

	private static class MarkerViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final CompoundButton compoundButton;
		final View divider;
		final View compassView;

		public MarkerViewHolder(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.imageView);
			divider = itemView.findViewById(R.id.divider);
			compoundButton = itemView.findViewById(R.id.toggle_item);
			compassView = itemView.findViewById(R.id.compass_layout);
		}
	}

	private static class TrackViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final CompoundButton compoundButton;

		public TrackViewHolder(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.icon);
			compoundButton = itemView.findViewById(R.id.toggle_item);
		}
	}
}
