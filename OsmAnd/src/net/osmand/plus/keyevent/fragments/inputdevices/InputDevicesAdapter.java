package net.osmand.plus.keyevent.fragments.inputdevices;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

class InputDevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int CARD_DIVIDER = 1;
	public static final int DEVICE_ITEM = 2;
	public static final int CARD_BOTTOM_SHADOW = 3;
	public static final int SPACE = 4;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final UiUtilities iconsCache;
	private final InputDevicesController controller;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;

	public InputDevicesAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
	                           @NonNull InputDevicesController controller, boolean usedOnMap) {
		setHasStableIds(true);
		this.app = app;
		this.appMode = appMode;
		this.controller = controller;
		this.usedOnMap = usedOnMap;
		iconsCache = app.getUIUtilities();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		switch (viewType) {
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.list_item_divider));
			case DEVICE_ITEM:
				return new DeviceTypeViewHolder(inflate(R.layout.list_item_external_input_device_type));
			case CARD_BOTTOM_SHADOW:
				return new CardBottomShadowViewHolder(inflate(R.layout.card_bottom_divider));
			case SPACE:
				return new SpaceViewHolder(new View(context), getDimen(R.dimen.fab_margin_bottom_big));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		ScreenItem item = screenItems.get(position);
		if (item.getType() == DEVICE_ITEM) {
			DeviceTypeViewHolder h = (DeviceTypeViewHolder) holder;
			InputDeviceProfile device = (InputDeviceProfile) item.getValue();

			boolean nightMode = isNightMode();
			int color = appMode.getProfileColor(nightMode);
			setupSelectableBackground(h.buttonView, color);
			h.buttonView.setOnClickListener(v -> {
				controller.selectDevice(device);
			});
			h.title.setText(device.toHumanString(app));

			UiUtilities.setupCompoundButton(nightMode, color, h.compoundButton);
			h.compoundButton.setChecked(controller.isSelected(device));

			AndroidUiHelper.updateVisibility(h.overflowMenuButton, device.isCustom());
			h.overflowMenuButton.setOnClickListener(v -> {
				showOverflowMenu(h.overflowMenuButton, device);
			});

			ScreenItem nextItem = position < screenItems.size() - 1 ? screenItems.get(position + 1) : null;
			boolean dividerNeeded = nextItem != null && nextItem.getType() == DEVICE_ITEM;
			AndroidUiHelper.updateVisibility(h.divider, dividerNeeded);
		}
	}

	private void showOverflowMenu(@NonNull View anchorView, @NonNull InputDeviceProfile device) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(context)
				.setTitle(app.getString(R.string.shared_string_rename))
				.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
				.setOnClickListener(v -> {
					controller.askRenameDevice(device);
				}).create());

		items.add(new PopUpMenuItem.Builder(context)
				.setTitle(app.getString(R.string.shared_string_duplicate))
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					controller.duplicateDevice(device);
				}).create());

		items.add(new PopUpMenuItem.Builder(context)
				.setTitle(app.getString(R.string.shared_string_remove))
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> {
					controller.askRemoveDevice(device);
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = anchorView;
		displayData.menuItems = items;
		displayData.nightMode = isNightMode();
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	public void setScreenItems(@NonNull List<ScreenItem> screenItems) {
		this.screenItems = screenItems;
		notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		return screenItems.size();
	}

	@Override
	public int getItemViewType(int position) {
		return screenItems.get(position).getType();
	}

	@Override
	public long getItemId(int position) {
		return screenItems.get(position).getId();
	}

	private View inflate(@LayoutRes int layoutResId) {
		LayoutInflater inflater = UiUtilities.getInflater(context, isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private void setupSelectableBackground(@NonNull View view, @ColorInt int color) {
		setBackground(view, getColoredSelectableDrawable(view.getContext(), color, 0.3f));
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}

	@NonNull
	private Drawable getContentIcon(@DrawableRes int iconId) {
		return iconsCache.getThemedIcon(iconId);
	}

	public static class CardBottomShadowViewHolder extends RecyclerView.ViewHolder {

		public CardBottomShadowViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	public static class CardDividerViewHolder extends RecyclerView.ViewHolder {

		public CardDividerViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	public static class SpaceViewHolder extends RecyclerView.ViewHolder {

		public SpaceViewHolder(@NonNull View itemView, int hSpace) {
			super(itemView);
			itemView.setLayoutParams(new LayoutParams(MATCH_PARENT, hSpace));
		}
	}

	public static class DeviceTypeViewHolder extends RecyclerView.ViewHolder {

		public View buttonView;
		public TextView title;
		public CompoundButton compoundButton;
		public View overflowMenuButton;
		public View divider;

		public DeviceTypeViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonView = itemView.findViewById(R.id.selectable_list_item);
			title = itemView.findViewById(R.id.title);
			compoundButton = itemView.findViewById(R.id.compound_button);
			overflowMenuButton = itemView.findViewById(R.id.overflow_menu);
			divider = itemView.findViewById(R.id.bottom_divider);
		}

	}
}
