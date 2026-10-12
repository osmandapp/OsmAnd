package net.osmand.plus.settings.fragments.configureitems.viewholders;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.configureitems.MenuItemsAdapterListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

public class RearrangeItemHolder extends ViewHolder implements UnmovableItem {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	@ColorInt
	private final int textColor;
	@ColorInt
	private final int activeColor;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final ImageView actionIcon;
	private final View moveButton;
	private final View divider;

	public RearrangeItemHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		activeColor = ColorUtilities.getActiveColor(app, nightMode);
		textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		icon = itemView.findViewById(R.id.icon);
		actionIcon = itemView.findViewById(R.id.action_icon);
		moveButton = itemView.findViewById(R.id.move_button);
		divider = itemView.findViewById(R.id.divider);

		ImageView moveIcon = itemView.findViewById(R.id.move_icon);
		moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
	}

	public void bindView(@NonNull ContextMenuItem item, @Nullable MenuItemsAdapterListener listener) {
		String id = item.getId();
		boolean isDivider = DRAWER_DIVIDER_ID.equals(id);

		title.setText(getTitle(item));
		title.setTextColor(isDivider ? activeColor : textColor);
		title.setTypeface(isDivider ? FontCache.getMediumFont() : DEFAULT);

		description.setText(getDescription(id));

		setupIcon(item, isDivider);
		setupActionIcon(item, listener, isDivider);

		moveButton.setOnTouchListener((view, event) -> {
			if (listener != null && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onDragStarted(this);
			}
			return false;
		});
		AndroidUiHelper.updateVisibility(divider, isDivider);
		AndroidUiHelper.updateVisibility(moveButton, !item.isHidden());
	}

	private void setupIcon(@NonNull ContextMenuItem item, boolean isDivider) {
		if (isDivider) {
			AndroidUiHelper.updateVisibility(icon, false);
		} else {
			int iconId = item.getIcon();
			boolean hasIcon = iconId != -1;
			icon.setImageDrawable(hasIcon ? uiUtilities.getIcon(iconId, nightMode) : null);
			AndroidUiHelper.setVisibility(hasIcon ? View.VISIBLE : View.INVISIBLE, icon);
		}
	}

	private void setupActionIcon(@NonNull ContextMenuItem item, @Nullable MenuItemsAdapterListener listener, boolean isDivider) {
		actionIcon.setImageDrawable(getActionIcon(item));
		actionIcon.setContentDescription(app.getString(R.string.ltr_or_rtl_combine_via_space,
				app.getString(R.string.quick_action_show_hide_title), getTitle(item)));

		actionIcon.setOnClickListener(MAP_CONTEXT_MENU_MORE_ID.equals(item.getId()) ? null : view -> {
			int adapterPosition = getAdapterPosition();
			if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
				listener.onButtonClicked(adapterPosition);
			}
		});
		AndroidUiHelper.updateVisibility(actionIcon, !isDivider);
	}

	@Override
	public boolean isMovingDisabled() {
		return false;
	}

	@Nullable
	private String getTitle(@NonNull ContextMenuItem item) {
		String id = item.getId();
		if (DRAWER_DIVIDER_ID.equals(id)) {
			return app.getString(R.string.shared_string_divider);
		} else if (MAP_CONTEXT_MENU_CREATE_POI.equals(id)) {
			return app.getString(R.string.create_edit_poi);
		} else if (MAP_CONTEXT_MENU_ADD_ID.equals(id)) {
			return app.getString(R.string.add_edit_favorite);
		} else {
			return item.getTitle();
		}
	}

	@NonNull
	private Drawable getActionIcon(@NonNull ContextMenuItem item) {
		if (MAP_CONTEXT_MENU_MORE_ID.equals(item.getId())) {
			return uiUtilities.getIcon(R.drawable.ic_action_remove, nightMode);
		} else if (item.isHidden()) {
			return uiUtilities.getIcon(R.drawable.ic_action_undo, R.color.color_osm_edit_create);
		} else {
			return uiUtilities.getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
		}
	}

	@StringRes
	private static int getDescription(@Nullable String id) {
		if (Algorithms.isEmpty(id)) {
			return R.string.app_name_osmand;
		}
		switch (id) {
			case DRAWER_FAVORITES_ID:
			case DRAWER_TRACKS_ID:
			case DRAWER_AV_NOTES_ID:
			case DRAWER_OSM_EDITS_ID:
				return R.string.shared_string_my_places;
			case DRAWER_BACKUP_RESTORE_ID:
				return R.string.shared_string_settings;
			case DRAWER_BUILDS_ID:
				return R.string.developer_plugin;
			case GPX_FILES_ID:
			case MAP_CONTEXT_MENU_EDIT_GPX_WP:
			case MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT:
				return R.string.shared_string_trip_recording;
			case MAP_SOURCE_ID:
			case OVERLAY_MAP:
			case UNDERLAY_MAP:
				return R.string.shared_string_online_maps;
			case RECORDING_LAYER:
			case MAP_CONTEXT_MENU_AUDIO_NOTE:
			case MAP_CONTEXT_MENU_VIDEO_NOTE:
			case MAP_CONTEXT_MENU_PHOTO_NOTE:
				return R.string.audionotes_plugin_name;
			case CONTOUR_LINES:
			case TERRAIN_ID:
				return R.string.srtm_plugin_name;
			case OSM_NOTES:
			case OSM_EDITS:
			case MAP_CONTEXT_MENU_CREATE_POI:
			case MAP_CONTEXT_MENU_MODIFY_OSM_NOTE:
			case MAP_CONTEXT_MENU_OPEN_OSM_NOTE:
				return R.string.osm_editing_plugin_name;
			case MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC:
				return R.string.parking_positions;
			case DRAWER_DIVIDER_ID:
				return R.string.divider_descr;
			default:
				return R.string.app_name_osmand;
		}
	}
}
