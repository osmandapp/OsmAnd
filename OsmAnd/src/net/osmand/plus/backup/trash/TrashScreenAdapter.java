package net.osmand.plus.backup.trash;

import static net.osmand.plus.backup.trash.ScreenItemType.*;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.trash.controller.TrashScreenController;
import net.osmand.plus.backup.trash.data.TrashGroup;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.plus.backup.trash.viewholder.AlertCardViewHolder;
import net.osmand.plus.backup.trash.viewholder.CardBottomShadowViewHolder;
import net.osmand.plus.backup.trash.viewholder.CardDividerViewHolder;
import net.osmand.plus.backup.trash.viewholder.DividerViewHolder;
import net.osmand.plus.backup.trash.viewholder.EmptyBannerViewHolder;
import net.osmand.plus.backup.trash.viewholder.HeaderViewHolder;
import net.osmand.plus.backup.trash.viewholder.SpaceViewHolder;
import net.osmand.plus.backup.trash.viewholder.TrashItemViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class TrashScreenAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final OsmandApplication app;
	private final UiUtilities iconsCache;
	private ViewGroup parent;
	private Context context;

	private final TrashScreenController controller;
	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;

	public TrashScreenAdapter(@NonNull OsmandApplication app,
	                          @NonNull TrashScreenController controller, boolean usedOnMap) {
		this.app = app;
		this.controller = controller;
		this.usedOnMap = usedOnMap;
		iconsCache = app.getUIUtilities();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		ScreenItemType type = viewType < ScreenItemType.values().length ? ScreenItemType.values()[viewType] : ScreenItemType.UNKNOWN;
		switch (type) {
			case ALERT_CARD:
				return new AlertCardViewHolder(inflate(R.layout.card_trash_alert));
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.list_item_divider));
			case HEADER:
				return new HeaderViewHolder(inflate(R.layout.list_item_header_56dp));
			case TRASH_ITEM:
				return new TrashItemViewHolder(inflate(R.layout.list_item_trash_item));
			case DIVIDER:
				return new DividerViewHolder(inflate(R.layout.divider));
			case EMPTY_TRASH_BANNER:
				return new EmptyBannerViewHolder(inflate(R.layout.card_cloud_trash_empty_banner));
			case CARD_BOTTOM_SHADOW:
				return new CardBottomShadowViewHolder(inflate(R.layout.card_bottom_divider));
			case SPACE:
				return new SpaceViewHolder(new View(context), getDimen(R.dimen.content_padding));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}


	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		ScreenItem item = screenItems.get(position);

		if (holder instanceof AlertCardViewHolder) {
			AlertCardViewHolder h = (AlertCardViewHolder) holder;
			h.actionButton.setOnClickListener(v -> {
				controller.askEmptyTrash();
			});

		} else if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			TrashGroup trashGroup = (TrashGroup) item.value;
			h.title.setText(trashGroup.getFormattedDate());

		} else if (holder instanceof TrashItemViewHolder) {
			TrashItemViewHolder h = (TrashItemViewHolder) holder;
			TrashItem trashItem = (TrashItem) item.value;
			setupSelectableBackground(h.buttonView);
			h.buttonView.setOnClickListener(v -> {
				controller.onTrashItemClicked(trashItem);
			});
			h.icon.setImageDrawable(getContentIcon(trashItem.getIconId()));
			h.title.setText(trashItem.getName());
			h.description.setText(trashItem.getDescription());
			AndroidUiHelper.updateVisibility(h.cloudLabel, !trashItem.isLocalItem());

			ScreenItem nextItem = position < screenItems.size() - 1 ? screenItems.get(position + 1) : null;
			boolean dividerNeeded = nextItem != null && nextItem.type == TRASH_ITEM;
			AndroidUiHelper.updateVisibility(h.divider, dividerNeeded);
		}
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
		return screenItems.get(position).type.ordinal();
	}

	private View inflate(@LayoutRes int layoutResId) {
		LayoutInflater inflater = UiUtilities.getInflater(context, isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Context ctx = view.getContext();
		int color = getActiveColor(ctx, isNightMode());
		AndroidUtils.setBackground(view, getColoredSelectableDrawable(ctx, color, 0.3f));
	}

	@NonNull
	private Drawable getContentIcon(@DrawableRes int iconResId) {
		return iconsCache.getThemedIcon(iconResId);
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}
}
