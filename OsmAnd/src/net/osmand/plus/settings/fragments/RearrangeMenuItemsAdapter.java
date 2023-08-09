package net.osmand.plus.settings.fragments;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTOUR_LINES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_AV_NOTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BACKUP_RESTORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_FAVORITES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSM_EDITS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRACKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.GPX_FILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ACTIONS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AUDIO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CREATE_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_EDIT_GPX_WP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_OPEN_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_PHOTO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_VIDEO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_RENDERING_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_SOURCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OPEN_STREET_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_EDITS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_NOTES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OVERLAY_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.RECORDING_LAYER;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.UNDERLAY_MAP;
import static net.osmand.plus.settings.fragments.ConfigureMenuItemsFragment.MAIN_BUTTONS_QUANTITY;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.fragments.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RearrangeMenuItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final boolean nightMode;
	private List<RearrangeMenuAdapterItem> items;
	private MenuItemsAdapterListener listener;
	private final int activeColorRes;
	private final int textColorRes;


	public RearrangeMenuItemsAdapter(OsmandApplication app,
									 List<RearrangeMenuAdapterItem> items, boolean nightMode) {
		this.app = app;
		this.items = items;
		uiUtilities = app.getUIUtilities();
		this.nightMode = nightMode;
		activeColorRes = ColorUtilities.getActiveColorId(nightMode);
		textColorRes = ColorUtilities.getPrimaryTextColorId(nightMode);
	}

	@Override
	public int getItemViewType(int position) {
		RearrangeMenuAdapterItem item = items.get(position);
		return item.type.ordinal();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		AdapterItemType type = AdapterItemType.values()[viewType];
		View view;
		switch (type) {
			case DESCRIPTION:
				view = inflater.inflate(R.layout.list_item_description_with_image, parent, false);
				return new DescriptionHolder(view);
			case MENU_ITEM:
				view = inflater.inflate(R.layout.profile_edit_list_item, parent, false);
				return new ItemHolder(view);
			case DIVIDER:
				view = inflater.inflate(R.layout.divider, parent, false);
				return new DividerHolder(view);
			case HEADER:
				view = inflater.inflate(R.layout.list_item_move_header, parent, false);
				return new HeaderHolder(view);
			case BUTTON:
				view = inflater.inflate(R.layout.preference_button, parent, false);
				return new ButtonHolder(view);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		RearrangeMenuAdapterItem item = items.get(position);
		if (holder instanceof DescriptionHolder) {
			DescriptionHolder h = (DescriptionHolder) holder;
			ScreenType screenType = (ScreenType) item.value;
			int paddingStart = (int) app.getResources().getDimension(R.dimen.dashboard_map_toolbar);
			int paddingTop = (int) app.getResources().getDimension(R.dimen.content_padding);
			h.description.setText(String.format(app.getString(R.string.reorder_or_hide_from), app.getString(screenType.titleRes)));
			h.image.setImageResource(nightMode ? screenType.imageNightRes : screenType.imageDayRes);
			if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
				h.deviceImage.setImageResource(nightMode
						? R.drawable.img_settings_device_bottom_dark
						: R.drawable.img_settings_device_bottom_light);
				h.imageContainer.setPadding(paddingStart, 0, paddingStart, paddingTop);
			} else {
				h.deviceImage.setImageResource(nightMode
						? R.drawable.img_settings_device_top_dark
						: R.drawable.img_settings_device_top_light);
				h.imageContainer.setPadding(paddingStart, paddingTop, paddingStart, 0);
			}
		} else if (holder instanceof ItemHolder) {
			ItemHolder h = (ItemHolder) holder;
			ContextMenuItem menuItem = (ContextMenuItem) item.value;
			String id = menuItem.getId();
			if (DRAWER_DIVIDER_ID.equals(id)) {
				h.title.setText(R.string.shared_string_divider);
				h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_medium)));
				h.title.setTextColor(app.getColor(activeColorRes));
				h.title.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimension(R.dimen.default_list_text_size));
				h.description.setText(R.string.divider_descr);
				h.icon.setVisibility(View.GONE);
				h.actionIcon.setVisibility(View.GONE);
				h.moveButton.setVisibility(View.VISIBLE);
				h.divider.setVisibility(View.VISIBLE);
			} else if (Algorithms.equalsToAny(id, SHOW_CATEGORY_ID, TERRAIN_CATEGORY_ID, OPEN_STREET_MAP, ROUTES_ID, MAP_RENDERING_CATEGORY_ID)) {
				h.title.setText(menuItem.getTitle());
				h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_medium)));
				h.title.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimension(R.dimen.default_list_text_size));
				h.description.setVisibility(View.GONE);
				h.icon.setVisibility(View.GONE);
				h.actionIcon.setVisibility(View.GONE);
				h.moveButton.setVisibility(View.GONE);
				h.divider.setVisibility(View.VISIBLE);
				h.movable = false;
			} else {
				if (menuItem.getIcon() != -1) {
					h.icon.setImageDrawable(uiUtilities.getIcon(menuItem.getIcon(), nightMode));
					h.icon.setVisibility(View.VISIBLE);
				} else {
					h.icon.setVisibility(View.INVISIBLE);
				}
				h.actionIcon.setVisibility(View.VISIBLE);
				h.actionIcon.setContentDescription(app.getString(R.string.ltr_or_rtl_combine_via_space,
						app.getString(R.string.quick_action_show_hide_title),
						menuItem.getTitle()));
				h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_regular)));
				h.title.setText(menuItem.getTitle());
				h.title.setTextColor(app.getColor(textColorRes));
				h.description.setText(getDescription(menuItem.getId()));
				h.divider.setVisibility(View.GONE);
				h.moveButton.setVisibility(View.VISIBLE);
			}
			h.actionIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int pos = holder.getAdapterPosition();
					if (listener != null && pos != RecyclerView.NO_POSITION) {
						listener.onButtonClicked(pos);
					}
				}
			});
			h.moveButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
						listener.onDragStarted(holder);
					}
					return false;
				}
			});
			if (!menuItem.isHidden()
					&& !id.equals(SHOW_CATEGORY_ID)
					&& !id.equals(TERRAIN_CATEGORY_ID)
					&& !id.equals(OPEN_STREET_MAP)
					&& !id.equals(ROUTES_ID)
					&& !id.equals(MAP_RENDERING_CATEGORY_ID)) {
				h.moveButton.setVisibility(View.VISIBLE);
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
			} else {
				h.moveButton.setVisibility(View.GONE);
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_undo, R.color.color_osm_edit_create));
			}
			if (id.equals(MAP_CONTEXT_MENU_MORE_ID)) {
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, nightMode));
				h.actionIcon.setOnClickListener(null);
			}
			if (id.equals(MAP_CONTEXT_MENU_CREATE_POI)) {
				h.title.setText(R.string.create_edit_poi);
			}
			if (id.equals(MAP_CONTEXT_MENU_ADD_ID)) {
				h.title.setText(R.string.add_edit_favorite);
			}
		} else if (holder instanceof HeaderHolder) {
			HeaderHolder h = (HeaderHolder) holder;
			HeaderItem header = (HeaderItem) item.value;
			h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_medium)));
			h.title.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimension(R.dimen.default_list_text_size));
			h.title.setText(header.titleRes);
			if (header.descrRes == R.string.additional_actions_descr) {
				h.description.setText(String.format(app.getString(header.descrRes), app.getString(R.string.shared_string_actions)));
			} else {
				h.description.setText(header.descrRes);
			}
			h.moveIcon.setVisibility(View.GONE);
			h.movable = header.titleRes == R.string.additional_actions;
		} else if (holder instanceof ButtonHolder) {
			ButtonHolder h = (ButtonHolder) holder;
			ButtonItem button = (ButtonItem) item.value;
			h.title.setText(app.getString(button.titleRes));
			h.icon.setImageDrawable(uiUtilities.getIcon(button.iconRes, activeColorRes));
			h.button.setOnClickListener(button.listener);
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, ContextCompat.getColor(app, activeColorRes), 0.3f);
			AndroidUtils.setBackground(h.itemView, drawable);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Object itemFrom = items.get(from).value;
		Object itemTo = items.get(to).value;

		if (itemFrom instanceof ContextMenuItem
				&& ((ContextMenuItem) itemFrom).getId().startsWith(MAP_CONTEXT_MENU_ACTIONS)
				&& itemTo instanceof HeaderItem
				&& ((HeaderItem) itemTo).titleRes == R.string.additional_actions) {
			ContextMenuItem menuItemFrom = (ContextMenuItem) itemFrom;
			int headerMaxIndex = MAIN_BUTTONS_QUANTITY + 2;
			if (to >= headerMaxIndex || menuItemFrom.getId().equals(MAP_CONTEXT_MENU_MORE_ID)) {
				return false;
			}
			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}

		if (itemFrom instanceof ContextMenuItem
				&& itemTo instanceof ContextMenuItem) {
			ContextMenuItem menuItemFrom = (ContextMenuItem) itemFrom;
			ContextMenuItem menuItemTo = (ContextMenuItem) itemTo;

			int orderFrom = menuItemFrom.getOrder();
			int orderTo = menuItemTo.getOrder();

			if (menuItemTo.isHidden()) {
				return false;
			}

//			item "Actions" should not left "Main actions" section
			if (menuItemFrom.getId().equals(MAP_CONTEXT_MENU_MORE_ID) || menuItemTo.getId().equals(MAP_CONTEXT_MENU_MORE_ID)) {
				int additionalHeaderIndex = 0;
				for (int i = 0; i < items.size(); i++) {
					Object value = items.get(i).getValue();
					if (value instanceof HeaderItem && ((HeaderItem) value).titleRes == R.string.additional_actions) {
						additionalHeaderIndex = i;
						break;
					}
				}
				if (to >= additionalHeaderIndex || from > additionalHeaderIndex) {
					return false;
				}
			}

			menuItemFrom.setOrder(orderTo);
			menuItemTo.setOrder(orderFrom);

			listener.onItemMoved(menuItemFrom.getId(), orderTo);
			listener.onItemMoved(menuItemTo.getId(), orderFrom);

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}
		return false;
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	private static class DescriptionHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {
		private final ImageView image;
		private final ImageView deviceImage;
		private final TextView description;
		private final FrameLayout imageContainer;

		DescriptionHolder(@NonNull View itemView) {
			super(itemView);
			image = itemView.findViewById(R.id.image);
			deviceImage = itemView.findViewById(R.id.device_image);
			description = itemView.findViewById(R.id.description);
			imageContainer = itemView.findViewById(R.id.image_container);
		}

		@Override
		public boolean isMovingDisabled() {
			return true;
		}
	}

	private static class ItemHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {

		private final TextView title;
		private final TextView description;
		private final ImageView icon;
		private final ImageView actionIcon;
		private final ImageView moveIcon;
		private final FrameLayout moveButton;
		private final View divider;
		private boolean movable = true;

		ItemHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			actionIcon = itemView.findViewById(R.id.action_icon);
			icon = itemView.findViewById(R.id.icon);
			moveIcon = itemView.findViewById(R.id.move_icon);
			moveButton = itemView.findViewById(R.id.move_button);
			divider = itemView.findViewById(R.id.divider);
		}

		@Override
		public boolean isMovingDisabled() {
			return !movable;
		}
	}

	private static class DividerHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {
		View divider;

		DividerHolder(View itemView) {
			super(itemView);
			divider = itemView.findViewById(R.id.divider);
		}

		@Override
		public boolean isMovingDisabled() {
			return true;
		}
	}

	private static class HeaderHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {
		private final ImageView moveIcon;
		private final TextView title;
		private final TextView description;
		private boolean movable = true;

		HeaderHolder(@NonNull View itemView) {
			super(itemView);
			moveIcon = itemView.findViewById(R.id.move_icon);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.summary);
		}

		@Override
		public boolean isMovingDisabled() {
			return !movable;
		}
	}

	private static class ButtonHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {
		private final View button;
		private final ImageView icon;
		private final TextView title;

		ButtonHolder(@NonNull View itemView) {
			super(itemView);
			button = itemView;
			icon = itemView.findViewById(android.R.id.icon);
			title = itemView.findViewById(android.R.id.title);
		}

		@Override
		public boolean isMovingDisabled() {
			return true;
		}
	}

	public static class RearrangeMenuAdapterItem {
		private final AdapterItemType type;
		private final Object value;

		public RearrangeMenuAdapterItem(AdapterItemType type, Object value) {
			this.type = type;
			this.value = value;
		}

		public AdapterItemType getType() {
			return type;
		}

		public Object getValue() {
			return value;
		}
	}


	public static class ButtonItem {
		@StringRes
		private final int titleRes;
		@DrawableRes
		private final int iconRes;
		private final View.OnClickListener listener;

		public ButtonItem(int titleRes, int iconRes, View.OnClickListener listener) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}

	public static class HeaderItem {
		@StringRes
		private final int titleRes;
		@StringRes
		private final int descrRes;

		public HeaderItem(int titleRes, int descrRes) {
			this.titleRes = titleRes;
			this.descrRes = descrRes;
		}
	}

	public enum AdapterItemType {
		DESCRIPTION,
		MENU_ITEM,
		DIVIDER,
		HEADER,
		BUTTON
	}

	public interface MenuItemsAdapterListener {

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onButtonClicked(int view);

		void onItemMoved(String id, int position);
	}

	public void setListener(MenuItemsAdapterListener listener) {
		this.listener = listener;
	}

	public RearrangeMenuAdapterItem getItem(int position) {
		return items.get(position);
	}

	public void updateItems(List<RearrangeMenuAdapterItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	private int getDescription(String id) {
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
			default:
				return R.string.app_name_osmand;
		}
	}

	public List<String> getMainActionsIds() {
		List<String> ids = new ArrayList<>();
		for (RearrangeMenuAdapterItem adapterItem : items) {
			Object value = adapterItem.getValue();
			if (value instanceof ContextMenuItem) {
				ids.add(((ContextMenuItem) value).getId());
			} else if (value instanceof HeaderItem
					&& (((HeaderItem) value).titleRes == R.string.additional_actions
					|| ((HeaderItem) value).titleRes == R.string.shared_string_hidden)) {
				break;
			}
		}
		return ids;
	}
}
