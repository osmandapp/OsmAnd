package net.osmand.plus.keyevent.fragments.keybindings;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

class KeyBindingsAdapter extends RecyclerView.Adapter<ViewHolder> {

	public static final int CARD_DIVIDER = 1;
	public static final int HEADER = 2;
	public static final int ACTION_ITEM = 3;
	public static final int CARD_BOTTOM_SHADOW = 4;
	public static final int SPACE = 5;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private boolean editable;
	private final boolean usedOnMap;
	private final KeyBindingsController controller;

	public KeyBindingsAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
	                          @NonNull KeyBindingsController controller, boolean usedOnMap) {
		setHasStableIds(true);
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.controller = controller;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		switch (viewType) {
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.list_item_divider));
			case HEADER:
				return new HeaderViewHolder(inflate(R.layout.list_item_header_48dp));
			case ACTION_ITEM:
				return new ActionItemViewHolder(inflate(R.layout.list_item_external_input_device_keybinding_item));
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
		if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			String groupName = (String) item.value;
			h.title.setText(groupName);

		} else if (holder instanceof ActionItemViewHolder) {
			ActionItemViewHolder h = (ActionItemViewHolder) holder;
			KeyBinding keyBinding = (KeyBinding) item.value;

			if (isEditable()) {
				boolean nightMode = isNightMode();
				int color = appMode.getProfileColor(nightMode);
				setupSelectableBackground(h.buttonView, color);
			}
			h.buttonView.setClickable(isEditable());
			h.buttonView.setFocusable(isEditable());
			h.buttonView.setOnClickListener(isEditable()? v -> {
				controller.askEditKeyAction(keyBinding);
			}: null);
			h.actionName.setText(keyBinding.getCommandTitle(app));
			h.keyName.setText(keyBinding.getKeySymbol());

			ScreenItem nextItem = position < screenItems.size() - 1 ? screenItems.get(position + 1) : null;
			boolean dividerNeeded = nextItem != null && nextItem.type == ACTION_ITEM;
			AndroidUiHelper.updateVisibility(h.divider, dividerNeeded);
		}
	}

	public void setScreenData(@NonNull List<ScreenItem> screenItems, boolean isTypeEditable) {
		this.screenItems = screenItems;
		this.editable = isTypeEditable;
		notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		return screenItems.size();
	}

	@Override
	public int getItemViewType(int position) {
		return screenItems.get(position).type;
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

	private boolean isEditable() {
		return editable;
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}

	static class CardBottomShadowViewHolder extends ViewHolder {
		public CardBottomShadowViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	static class CardDividerViewHolder extends ViewHolder {
		public CardDividerViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	static class HeaderViewHolder extends RecyclerView.ViewHolder {

		public TextView title;

		public HeaderViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.count), false);
		}
	}

	static class SpaceViewHolder extends ViewHolder {
		public SpaceViewHolder(@NonNull View itemView, int hSpace) {
			super(itemView);
			itemView.setLayoutParams(new LayoutParams(MATCH_PARENT, hSpace));
		}
	}

	static class ActionItemViewHolder extends ViewHolder {

		public View buttonView;
		public TextView actionName;
		public TextView keyName;
		public View divider;

		public ActionItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonView = itemView.findViewById(R.id.selectable_list_item);
			actionName = itemView.findViewById(R.id.action_name);
			keyName = itemView.findViewById(R.id.key_name);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}
}
