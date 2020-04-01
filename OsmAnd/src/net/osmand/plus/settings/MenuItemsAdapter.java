package net.osmand.plus.settings;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;


import java.util.Collections;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;

public class MenuItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private boolean nightMode;
	private List<AdapterItem> items;
	private MenuItemsAdapterListener listener;
	private int activeColorRes;


	MenuItemsAdapter(OsmandApplication app,
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
			h.description.setText(String.format(app.getString(R.string.reorder_or_hide_from), app.getString(screenType.titleRes)));
			h.image.setImageResource(nightMode ? screenType.imageNightRes : screenType.imageDayRes);
		} else if (holder instanceof ItemHolder) {
			final ItemHolder h = (ItemHolder) holder;
			MenuItemBase menuItem = (MenuItemBase) item.value;
			h.title.setText(menuItem.titleRes);
			h.description.setText(String.valueOf(menuItem.order));
			if (DRAWER_DIVIDER_ID.equals(menuItem.id)) {
				h.icon.setVisibility(View.GONE);
				h.actionIcon.setVisibility(View.GONE);
			} else {
				h.icon.setImageDrawable(uiUtilities.getIcon(menuItem.iconRes, nightMode));
				h.icon.setVisibility(View.VISIBLE);
				h.actionIcon.setVisibility(View.VISIBLE);
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
			if (menuItem.hidden) {
				h.moveIcon.setVisibility(View.GONE);
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_undo, R.color.color_osm_edit_create));
			} else {
				h.moveIcon.setVisibility(View.VISIBLE);
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, nightMode));
				h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
			}
		} else if (holder instanceof HeaderHolder) {
			HeaderHolder h = (HeaderHolder) holder;
			HeaderItem header = (HeaderItem) item.value;
			h.title.setText(header.titleRes);
			h.description.setText(header.descrRes);
			h.movable = header.movable;
			if (header.movable) {
				h.moveIcon.setVisibility(View.VISIBLE);
				h.moveIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_item_move, activeColorRes));
				h.moveIcon.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
							listener.onDragStarted(holder);
						}
						return false;
					}
				});
			} else {
				h.moveIcon.setVisibility(View.GONE);
			}
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
		if (itemFrom instanceof MenuItemBase
				&& itemTo instanceof MenuItemBase) {
			MenuItemBase menuItemFrom = (MenuItemBase) itemFrom;
			MenuItemBase menuItemTo = (MenuItemBase) itemTo;

			int orderFrom = menuItemFrom.order;
			int orderTo = menuItemTo.order;

			menuItemFrom.order = orderTo;
			menuItemTo.order = orderFrom;

			listener.onItemMoved(menuItemFrom.id, orderTo);
			listener.onItemMoved(menuItemTo.id, orderFrom);

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
		private TextView description;

		DescriptionHolder(@NonNull View itemView) {
			super(itemView);
			image = itemView.findViewById(R.id.image);
			description = itemView.findViewById(R.id.description);
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

		ItemHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			actionIcon = itemView.findViewById(R.id.action_icon);
			icon = itemView.findViewById(R.id.icon);
			moveIcon = itemView.findViewById(R.id.move_icon);
		}

		@Override
		public boolean isMovingDisabled() {
			return false;
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
		private boolean movable;

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

	static class AdapterItem {
		private AdapterItemType type;
		private Object value;

		AdapterItem(AdapterItemType type, Object value) {
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

	public static class MenuItemBase {
		private String id;
		@StringRes
		private int titleRes;
		@StringRes
		private int descrRes;
		@DrawableRes
		private int iconRes;

		public void setOrder(int order) {
			this.order = order;
		}

		public int getOrder() {
			return order;
		}

		private int order;

		public void setHidden(boolean hidden) {
			this.hidden = hidden;
		}

		private boolean hidden;

		public MenuItemBase(String id, int titleRes, int descrRes, int iconRes, int order, boolean hidden) {
			this.id = id;
			this.titleRes = titleRes;
			this.descrRes = descrRes;
			this.iconRes = iconRes;
			this.order = order;
			this.hidden = hidden;
		}

		public void toggleHidden() {
			hidden = !hidden;
		}

		public String getId() {
			return id;
		}

		public boolean isHidden() {
			return hidden;
		}
	}

	static class ButtonItem {
		@StringRes
		private int titleRes;
		@DrawableRes
		private int iconRes;
		private View.OnClickListener listener;

		ButtonItem(int titleRes, int iconRes, View.OnClickListener listener) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}

	static class HeaderItem {
		@StringRes
		private int titleRes;
		@StringRes
		private int descrRes;
		boolean movable;

		HeaderItem(int titleRes, int descrRes, boolean movable) {
			this.titleRes = titleRes;
			this.descrRes = descrRes;
			this.movable = movable;
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
