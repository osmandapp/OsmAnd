package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
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
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;


import java.util.Collections;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_RENDERING_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.RENDERING_ITEMS_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_ITEMS_ID_SCHEME;

public class RearrangeMenuItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private boolean nightMode;
	private List<AdapterItem> items;
	private MenuItemsAdapterListener listener;
	private int activeColorRes;


	public RearrangeMenuItemsAdapter(OsmandApplication app,
	                                 List<AdapterItem> items) {
		this.app = app;
		this.items = items;
		uiUtilities = app.getUIUtilities();
		nightMode = !app.getSettings().isLightContent();
		activeColorRes = nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light;
	}

	@Override
	public int getItemViewType(int position) {
		AdapterItem item = items.get(position);
		return item.type.ordinal();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
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
	public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
		AdapterItem item = items.get(position);
		if (holder instanceof DescriptionHolder) {
			DescriptionHolder h = (DescriptionHolder) holder;
			ScreenType screenType = (ScreenType) item.value;
			int paddingStart = AndroidUtils.dpToPx(app, 56);
			int paddingTop = AndroidUtils.dpToPx(app, 16);
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
			final ItemHolder h = (ItemHolder) holder;
			ContextMenuItem menuItem = (ContextMenuItem) item.value;
			String id = menuItem.getId();
			if (DRAWER_DIVIDER_ID.equals(menuItem.getId())) {
				h.title.setText(R.string.shared_string_divider);
				h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_medium)));
				h.title.setTextColor(app.getResources().getColor(activeColorRes));
				h.title.setTextSize(15);
				h.description.setText(R.string.divider_descr);
				h.icon.setVisibility(View.GONE);
				h.actionIcon.setVisibility(View.GONE);
				h.moveIcon.setVisibility(View.VISIBLE);
				h.divider.setVisibility(View.VISIBLE);
			} else if (SHOW_CATEGORY_ID.equals(id)
					|| MAP_RENDERING_CATEGORY_ID.equals(id)) {
				h.title.setText(menuItem.getTitle());
				h.title.setTypeface(FontCache.getFont(app, app.getString(R.string.font_roboto_medium)));
				h.title.setTextSize(15);
				h.description.setText(R.string.move_inside_category);
				h.icon.setVisibility(View.GONE);
				h.actionIcon.setVisibility(View.GONE);
				h.moveIcon.setVisibility(View.GONE);
				h.divider.setVisibility(View.VISIBLE);
				h.movable = false;
			} else {
				if (menuItem.getIcon() != -1) {
					h.icon.setImageDrawable(uiUtilities.getIcon(menuItem.getIcon(), nightMode));
					h.icon.setVisibility(View.VISIBLE);
					h.actionIcon.setVisibility(View.VISIBLE);
				}
				h.title.setText(menuItem.getTitle());
				h.description.setText(String.valueOf(menuItem.getOrder()));
				h.divider.setVisibility(View.GONE);
				h.moveIcon.setVisibility(View.VISIBLE);
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
			h.moveIcon.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
						listener.onDragStarted(holder);
					}
					return false;
				}
			});
			if (!menuItem.isHidden()
					&& !id.equals(SHOW_CATEGORY_ID)
					&& !id.equals(MAP_RENDERING_CATEGORY_ID)) {
				h.moveIcon.setVisibility(View.VISIBLE);
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
			} else {
				h.moveIcon.setVisibility(View.GONE);
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_undo, R.color.color_osm_edit_create));
			}
			if (id.equals(MAP_CONTEXT_MENU_MORE_ID)) {
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, nightMode));
				h.actionIcon.setOnClickListener(null);
			}
		} else if (holder instanceof HeaderHolder) {
			HeaderHolder h = (HeaderHolder) holder;
			HeaderItem header = (HeaderItem) item.value;
			h.title.setText(header.titleRes);
			h.description.setText(header.descrRes);
			h.moveIcon.setVisibility(View.GONE);
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
				&& itemTo instanceof ContextMenuItem) {
			ContextMenuItem menuItemFrom = (ContextMenuItem) itemFrom;
			ContextMenuItem menuItemTo = (ContextMenuItem) itemTo;

			int orderFrom = menuItemFrom.getOrder();
			int orderTo = menuItemTo.getOrder();

			if (menuItemFrom.getId().startsWith(SHOW_ITEMS_ID_SCHEME) && menuItemTo.getId().startsWith(RENDERING_ITEMS_ID_SCHEME)
					|| menuItemFrom.getId().startsWith(RENDERING_ITEMS_ID_SCHEME) && menuItemTo.getId().startsWith(SHOW_ITEMS_ID_SCHEME)
					|| menuItemTo.isHidden()) {
				return false;
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
		private ImageView image;
		private ImageView deviceImage;
		private TextView description;
		private FrameLayout imageContainer;

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

		private TextView title;
		private TextView description;
		private ImageView icon;
		private ImageView actionIcon;
		private ImageView moveIcon;
		private View divider;
		private boolean movable = true;

		ItemHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			actionIcon = itemView.findViewById(R.id.action_icon);
			icon = itemView.findViewById(R.id.icon);
			moveIcon = itemView.findViewById(R.id.move_icon);
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
		private ImageView moveIcon;
		private TextView title;
		private TextView description;

		HeaderHolder(@NonNull View itemView) {
			super(itemView);
			moveIcon = itemView.findViewById(R.id.move_icon);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.summary);
		}

		@Override
		public boolean isMovingDisabled() {
			return true;
		}
	}

	private static class ButtonHolder extends RecyclerView.ViewHolder
			implements ReorderItemTouchHelperCallback.UnmovableItem {
		private View button;
		private ImageView icon;
		private TextView title;

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

	public static class AdapterItem {
		private AdapterItemType type;
		private Object value;

		public AdapterItem(AdapterItemType type, Object value) {
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
		private int titleRes;
		@DrawableRes
		private int iconRes;
		private View.OnClickListener listener;

		public ButtonItem(int titleRes, int iconRes, View.OnClickListener listener) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}

	public static class HeaderItem {
		@StringRes
		private int titleRes;
		@StringRes
		private int descrRes;

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

	public AdapterItem getItem(int position) {
		return items.get(position);
	}

	public void updateItems(List<AdapterItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}
}
