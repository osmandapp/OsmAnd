package net.osmand.plus.settings.fragments.configureitems;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ACTIONS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.settings.fragments.configureitems.RearrangeItemsHelper.MAIN_BUTTONS_QUANTITY;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeButtonHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeCategoryHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeDescriptionHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeDividerHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeHeaderHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.RearrangeItemHolder;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.Collections;
import java.util.List;

public class RearrangeMenuItemsAdapter extends RecyclerView.Adapter<ViewHolder> implements OnItemMoveCallback {

	public static final int HEADER_TYPE = 0;
	public static final int MENU_ITEM_TYPE = 1;
	public static final int CATEGORY_TYPE = 2;
	public static final int DESCRIPTION_TYPE = 3;
	public static final int DIVIDER_TYPE = 4;
	public static final int BUTTON_TYPE = 5;

	private List<Object> items;
	private MenuItemsAdapterListener listener;
	private boolean nightMode;

	public RearrangeMenuItemsAdapter(@NonNull List<Object> items, boolean nightMode) {
		this.items = items;
		this.nightMode = nightMode;
	}

	public void setListener(@Nullable MenuItemsAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	public void updateItems(@NonNull List<Object> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view;
		switch (viewType) {
			case DESCRIPTION_TYPE:
				view = inflater.inflate(R.layout.list_item_description_with_image, parent, false);
				return new RearrangeDescriptionHolder(view);
			case MENU_ITEM_TYPE:
				view = inflater.inflate(R.layout.profile_edit_list_item, parent, false);
				return new RearrangeItemHolder(view, nightMode);
			case CATEGORY_TYPE:
				view = inflater.inflate(R.layout.profile_edit_list_item, parent, false);
				return new RearrangeCategoryHolder(view);
			case DIVIDER_TYPE:
				view = inflater.inflate(R.layout.divider, parent, false);
				return new RearrangeDividerHolder(view);
			case HEADER_TYPE:
				view = inflater.inflate(R.layout.list_item_move_header, parent, false);
				return new RearrangeHeaderHolder(view);
			case BUTTON_TYPE:
				view = inflater.inflate(R.layout.preference_button, parent, false);
				return new RearrangeButtonHolder(view, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		Object object = getItem(position);
		if (viewHolder instanceof RearrangeDescriptionHolder) {
			RearrangeDescriptionHolder holder = (RearrangeDescriptionHolder) viewHolder;
			holder.bindView((ScreenType) object, nightMode);
		} else if (viewHolder instanceof RearrangeItemHolder) {
			RearrangeItemHolder holder = (RearrangeItemHolder) viewHolder;
			holder.bindView((ContextMenuItem) object, listener);
		} else if (viewHolder instanceof RearrangeCategoryHolder) {
			RearrangeCategoryHolder holder = (RearrangeCategoryHolder) viewHolder;
			holder.bindView((ContextMenuItem) object);
		} else if (viewHolder instanceof RearrangeHeaderHolder) {
			RearrangeHeaderHolder holder = (RearrangeHeaderHolder) viewHolder;
			holder.bindView((RearrangeHeaderItem) object);
		} else if (viewHolder instanceof RearrangeButtonHolder) {
			RearrangeButtonHolder holder = (RearrangeButtonHolder) viewHolder;
			holder.bindView((RearrangeButtonItem) object);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = getItem(position);
		if (object instanceof ScreenType) {
			return DESCRIPTION_TYPE;
		} else if (object instanceof RearrangeHeaderItem) {
			return HEADER_TYPE;
		} else if (object instanceof RearrangeButtonItem) {
			return BUTTON_TYPE;
		} else if (object instanceof ContextMenuItem) {
			String id = ((ContextMenuItem) object).getId();
			if (ContextMenuUtils.isCategoryItem(id)) {
				return CATEGORY_TYPE;
			}
			return MENU_ITEM_TYPE;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (DIVIDER_TYPE == item) {
				return DIVIDER_TYPE;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@NonNull
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public void onItemDismiss(@NonNull ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Object itemFrom = items.get(from);
		Object itemTo = items.get(to);

		if (itemFrom instanceof ContextMenuItem
				&& ((ContextMenuItem) itemFrom).getId().startsWith(MAP_CONTEXT_MENU_ACTIONS)
				&& itemTo instanceof RearrangeHeaderItem
				&& ((RearrangeHeaderItem) itemTo).titleId == R.string.additional_actions) {
			ContextMenuItem menuItemFrom = (ContextMenuItem) itemFrom;
			int headerMaxIndex = MAIN_BUTTONS_QUANTITY + 2;
			if (to >= headerMaxIndex || menuItemFrom.getId().equals(MAP_CONTEXT_MENU_MORE_ID)) {
				return false;
			}
			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}

		if (itemFrom instanceof ContextMenuItem && itemTo instanceof ContextMenuItem) {
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
					Object value = items.get(i);
					if (value instanceof RearrangeHeaderItem && ((RearrangeHeaderItem) value).titleId == R.string.additional_actions) {
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
}