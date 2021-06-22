package net.osmand.plus.settings.fragments;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ExportSettingsType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ImportedSettingsItemsAdapter extends
		RecyclerView.Adapter<ImportedSettingsItemsAdapter.ItemViewHolder> {
	private Map<ExportSettingsType, List<?>> itemsMap;
	private List<ExportSettingsType> itemsTypes;
	private UiUtilities uiUtils;
	private OsmandApplication app;
	private boolean nightMode;
	private OnItemClickListener listener;

	ImportedSettingsItemsAdapter(@NonNull OsmandApplication app, Map<ExportSettingsType, List<?>> itemsMap,
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
		final ExportSettingsType currentItemType = itemsTypes.get(position);
		boolean isLastItem = itemsTypes.size() - 1 == position;
		int activeColorRes = nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light;

		holder.title.setTextColor(app.getResources().getColor(activeColorRes));
		Typeface typeface = FontCache.getFont(app, app.getString(R.string.font_roboto_medium));
		if (typeface != null) {
			holder.title.setTypeface(typeface);
		}
		holder.divider.setVisibility(isLastItem ? View.VISIBLE : View.GONE);
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(currentItemType);
			}
		});
		holder.subTitle.setText(String.format(
				app.getString(R.string.ltr_or_rtl_combine_via_colon),
				app.getString(R.string.items_added),
				String.valueOf(itemsMap.get(currentItemType).size()))
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
				holder.title.setText(R.string.search_activity);
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
			case OFFLINE_MAPS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_map, activeColorRes));
				holder.title.setText(R.string.shared_string_maps);
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
				holder.title.setText(R.string.general_settings_2);
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
			case ONLINE_ROUTING_ENGINES:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_world_globe_dark, activeColorRes));
				holder.title.setText(R.string.online_routing_engines);
				break;
			case ITINERARY_GROUPS:
				holder.icon.setImageDrawable(uiUtils.getIcon(R.drawable.ic_action_flag, activeColorRes));
				holder.title.setText(R.string.shared_string_itinerary);
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
		void onItemClick(ExportSettingsType type);
	}
}
