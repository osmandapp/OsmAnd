package net.osmand.plus.settings.fragments;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ImportedSettingsItemsAdapter extends
		RecyclerView.Adapter<ImportedSettingsItemsAdapter.ItemViewHolder> {
	private final Map<ExportType, List<?>> itemsMap;
	private final List<ExportType> itemsTypes;
	private final UiUtilities uiUtils;
	private final OsmandApplication app;
	private final boolean nightMode;
	private final OnItemClickListener listener;

	ImportedSettingsItemsAdapter(@NonNull OsmandApplication app, Map<ExportType, List<?>> itemsMap,
								 boolean nightMode, OnItemClickListener listener) {
		this.app = app;
		this.itemsMap = itemsMap;
		this.nightMode = nightMode;
		this.listener = listener;
		uiUtils = app.getUIUtilities();
		itemsTypes = new ArrayList<>(itemsMap.keySet());
		Collections.sort(itemsTypes);
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		View view = inflater.inflate(R.layout.list_item_import, parent, false);
		return new ItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
		ExportType currentItemType = itemsTypes.get(position);
		boolean isLastItem = itemsTypes.size() - 1 == position;
		int activeColorRes = ColorUtilities.getActiveColorId(nightMode);

		holder.title.setTextColor(app.getColor(activeColorRes));
		holder.title.setTypeface(FontCache.getMediumFont());
		holder.divider.setVisibility(isLastItem ? View.VISIBLE : View.GONE);
		holder.itemView.setOnClickListener(view -> listener.onItemClick(currentItemType));
		holder.subTitle.setText(String.format(
				app.getString(R.string.ltr_or_rtl_combine_via_colon),
				app.getString(R.string.items_added),
				itemsMap.get(currentItemType).size())
		);

		switch (currentItemType) {
			case PROFILE:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_settings, activeColorRes));
				holder.title.setText(R.string.shared_string_settings);
				break;
			case QUICK_ACTIONS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_quick_action, activeColorRes));
				holder.title.setText(R.string.configure_screen_quick_action);
				break;
			case POI_TYPES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_info_dark, activeColorRes));
				holder.title.setText(R.string.shared_string_search);
				break;
			case MAP_SOURCES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_layers, activeColorRes));
				holder.title.setText(R.string.configure_map);
				break;
			case CUSTOM_RENDER_STYLE:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_map_style, activeColorRes));
				holder.title.setText(R.string.shared_string_rendering_style);
				break;
			case CUSTOM_ROUTING:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
				holder.title.setText(R.string.shared_string_routing);
				break;
			case AVOID_ROADS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_alert, activeColorRes));
				holder.title.setText(R.string.avoid_road);
				break;
			case MULTIMEDIA_NOTES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_photo_dark, activeColorRes));
				holder.title.setText(R.string.notes);
				break;
			case TRACKS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
				holder.title.setText(R.string.shared_string_tracks);
				break;
			case OSM_NOTES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_osm_note_add, activeColorRes));
				holder.title.setText(R.string.osm_notes);
				break;
			case OSM_EDITS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_openstreetmap_logo, activeColorRes));
				holder.title.setText(R.string.osm_edits);
				break;
			case FAVORITES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_favorite, activeColorRes));
				holder.title.setText(R.string.shared_string_favorites);
				break;
			case STANDARD_MAPS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_map, activeColorRes));
				holder.title.setText(R.string.standard_maps);
				break;
			case ROAD_MAPS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_map, activeColorRes));
				holder.title.setText(R.string.download_roads_only_maps);
				break;
			case WIKI_AND_TRAVEL:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_wikipedia, activeColorRes));
				holder.title.setText(R.string.wikipedia_and_travel_maps);
				break;
			case TERRAIN_DATA:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_terrain, activeColorRes));
				holder.title.setText(R.string.topography_maps);
				break;
			case DEPTH_DATA:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_anchor, activeColorRes));
				holder.title.setText(R.string.nautical_maps);
				break;
			case TTS_VOICE:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_volume_up, activeColorRes));
				holder.title.setText(R.string.local_indexes_cat_tts);
				break;
			case VOICE:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_volume_up, activeColorRes));
				holder.title.setText(R.string.local_indexes_cat_voice);
				break;
			case GLOBAL:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_settings, activeColorRes));
				holder.title.setText(R.string.osmand_settings);
				break;
			case ACTIVE_MARKERS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_flag, activeColorRes));
				holder.title.setText(R.string.map_markers);
				break;
			case HISTORY_MARKERS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_flag, activeColorRes));
				holder.title.setText(R.string.markers_history);
				break;
			case SEARCH_HISTORY:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_history, activeColorRes));
				holder.title.setText(R.string.shared_string_search_history);
				break;
			case NAVIGATION_HISTORY:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_gdirections_dark, activeColorRes));
				holder.title.setText(R.string.navigation_history);
				break;
			case ONLINE_ROUTING_ENGINES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_world_globe_dark, activeColorRes));
				holder.title.setText(R.string.online_routing_engines);
				break;
			case ITINERARY_GROUPS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_flag, activeColorRes));
				holder.title.setText(R.string.shared_string_itinerary);
				break;
			case FAVORITES_BACKUP:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_folder_favorites, activeColorRes));
				holder.title.setText(R.string.favorites_backup);
				break;
		}
	}

	@Override
	public int getItemCount() {
		return itemsMap.keySet().size();
	}

	public static class ItemViewHolder extends RecyclerView.ViewHolder {
		ImageView icon;
		TextView title;
		TextView subTitle;
		View divider;

		ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			subTitle = itemView.findViewById(R.id.sub_title);
			icon = itemView.findViewById(R.id.icon);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}

	interface OnItemClickListener {
		void onItemClick(@NonNull ExportType type);
	}
}
