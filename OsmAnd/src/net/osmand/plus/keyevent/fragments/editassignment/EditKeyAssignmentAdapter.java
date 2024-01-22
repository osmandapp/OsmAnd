package net.osmand.plus.keyevent.fragments.editassignment;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.keyevent.KeySymbolMapper.getKeySymbol;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;
import static net.osmand.util.CollectionUtils.equalsToAny;

import android.content.Context;
import android.graphics.Typeface;
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
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

class EditKeyAssignmentAdapter extends RecyclerView.Adapter<ViewHolder> {

	static final int CARD_TOP_DIVIDER = 1;
	static final int NAME_ITEM = 2;
	static final int ACTION_ITEM = 3;
	static final int BUTTON_ITEM = 4;
	static final int CARD_BOTTOM_SHADOW = 5;
	static final int SPACE = 6;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;
	private final EditKeyAssignmentController controller;

	public EditKeyAssignmentAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
	                                @NonNull EditKeyAssignmentController controller, boolean usedOnMap) {
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
			case CARD_TOP_DIVIDER:
				return new CardTopDividerViewHolder(inflate(R.layout.list_item_divider));
			case NAME_ITEM:
			case ACTION_ITEM:
			case BUTTON_ITEM:
				return new ActionItemViewHolder(inflate(R.layout.list_item_with_right_text_56dp));
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
		int itemType = getItemViewType(position);
		if (!equalsToAny(itemType, NAME_ITEM, ACTION_ITEM, BUTTON_ITEM)) {
			return;
		}
		ActionItemViewHolder h = (ActionItemViewHolder) holder;
		if (itemType == NAME_ITEM) {
			h.title.setText(R.string.shared_string_name);
			h.summary.setText(controller.getCustomNameSummary());
			h.buttonView.setOnClickListener(v -> {
				controller.askRenameAssignment();
			});

		} else if (itemType == ACTION_ITEM) {
			h.title.setText(R.string.shared_string_action);
			h.summary.setText(controller.getActionNameSummary());

		} else if (itemType == BUTTON_ITEM) {
			ScreenItem item = screenItems.get(position);
			Integer keyCode = (Integer) item.getValue();
			h.title.setText(R.string.shared_string_button);
			h.summary.setText(getKeySymbol(app, keyCode));
			h.summary.setTextColor(getActiveColor(app, controller.isNightMode()));
			h.summary.setTypeface(h.summary.getTypeface(), Typeface.BOLD);
			h.buttonView.setOnClickListener(v -> {
				controller.askChangeKeyCode(keyCode);
			});

			ScreenItem nextItem = position < screenItems.size() - 1 ? screenItems.get(position + 1) : null;
			boolean dividerNeeded = nextItem != null && nextItem.getType() == BUTTON_ITEM;
			AndroidUiHelper.updateVisibility(h.divider, dividerNeeded);
		}
		int color = appMode.getProfileColor(controller.isNightMode());
		setupSelectableBackground(h.buttonView, color);
	}

	public void setScreenData(@NonNull List<ScreenItem> screenItems) {
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
		LayoutInflater inflater = UiUtilities.getInflater(context, controller.isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private void setupSelectableBackground(@NonNull View view, @ColorInt int color) {
		setBackground(view, getColoredSelectableDrawable(view.getContext(), color, 0.3f));
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}

	static class CardTopDividerViewHolder extends ViewHolder {
		public CardTopDividerViewHolder(@NonNull View itemView) {
			super(itemView);
			View shadowToHide = itemView.findViewById(R.id.bottomShadowView);
			if (shadowToHide != null) {
				shadowToHide.setVisibility(View.INVISIBLE);
			}
		}
	}

	static class CardBottomShadowViewHolder extends ViewHolder {
		public CardBottomShadowViewHolder(@NonNull View itemView) {
			super(itemView);
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
		public TextView title;
		public TextView summary;
		public View divider;

		public ActionItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonView = itemView.findViewById(R.id.selectable_list_item);
			title = itemView.findViewById(R.id.title);
			summary = itemView.findViewById(R.id.description);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}

}
