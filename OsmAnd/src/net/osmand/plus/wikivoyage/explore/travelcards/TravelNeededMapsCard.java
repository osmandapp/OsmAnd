package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.IndexItem;

import java.util.List;

public class TravelNeededMapsCard extends BaseTravelCard {

	public static final int TYPE = 70;

	private List<IndexItem> items;

	private Drawable downloadIcon;

	public TravelNeededMapsCard(OsmandApplication app, boolean nightMode, List<IndexItem> items) {
		super(app, nightMode);
		this.items = items;
		downloadIcon = getActiveIcon(R.drawable.ic_action_import);
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof NeededMapsVH) {
			NeededMapsVH holder = (NeededMapsVH) viewHolder;
			adjustChildCount(holder.itemsContainer);
			for (int i = 0; i < items.size(); i++) {
				boolean lastItem = i == items.size() - 1;
				IndexItem item = items.get(i);
				View view = holder.itemsContainer.getChildAt(i);
				((ImageView) view.findViewById(R.id.icon))
						.setImageDrawable(getActiveIcon(item.getType().getIconResource()));
				((TextView) view.findViewById(R.id.title)).setText(item.getVisibleName(app, app.getRegions(), false));
				((TextView) view.findViewById(R.id.description)).setText(getItemDescription(item));
				((ImageView) view.findViewById(R.id.icon_action)).setImageDrawable(downloadIcon);
				view.findViewById(R.id.divider).setVisibility(lastItem ? View.GONE : View.VISIBLE);
			}
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}

	private void adjustChildCount(LinearLayout itemsContainer) {
		int itemsCount = items.size();
		int childCount = itemsContainer.getChildCount();
		if (itemsCount == childCount) {
			return;
		}
		if (itemsCount > childCount) {
			LayoutInflater inflater = LayoutInflater.from(itemsContainer.getContext());
			for (int i = childCount; i < itemsCount; i++) {
				inflater.inflate(R.layout.travel_needed_map_item, itemsContainer);
			}
		} else if (itemsCount < childCount) {
			itemsContainer.removeViews(0, childCount - itemsCount);
		}
	}

	private String getItemDescription(IndexItem item) {
		return app.getString(R.string.file_size_in_mb, item.getArchiveSizeMB()) + " • " + item.getType().getString(app);
	}

	public static class NeededMapsVH extends RecyclerView.ViewHolder {

		final LinearLayout itemsContainer;
		final View secondaryBtnContainer;
		final TextView secondaryBtn;
		final View buttonsDivider;
		final View primaryBtnContainer;
		final TextView primaryButton;

		@SuppressWarnings("RedundantCast")
		public NeededMapsVH(View itemView) {
			super(itemView);
			itemsContainer = (LinearLayout) itemView.findViewById(R.id.items_container);
			secondaryBtnContainer = itemView.findViewById(R.id.secondary_btn_container);
			secondaryBtn = (TextView) itemView.findViewById(R.id.secondary_button);
			buttonsDivider = itemView.findViewById(R.id.buttons_divider);
			primaryBtnContainer = itemView.findViewById(R.id.primary_btn_container);
			primaryButton = (TextView) itemView.findViewById(R.id.primary_button);
		}
	}
}
