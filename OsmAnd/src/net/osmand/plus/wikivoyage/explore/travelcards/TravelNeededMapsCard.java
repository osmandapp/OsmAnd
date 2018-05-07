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
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;

import java.util.List;

public class TravelNeededMapsCard extends BaseTravelCard {

	public static final int TYPE = 70;

	private DownloadIndexesThread downloadThread;
	private List<IndexItem> items;

	private Drawable downloadIcon;

	private CardListener listener;

	public void setListener(CardListener listener) {
		this.listener = listener;
	}

	public TravelNeededMapsCard(OsmandApplication app, boolean nightMode, List<IndexItem> items) {
		super(app, nightMode);
		downloadThread = app.getDownloadThread();
		this.items = items;
		downloadIcon = getActiveIcon(R.drawable.ic_action_import);
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof NeededMapsVH) {
			NeededMapsVH holder = (NeededMapsVH) viewHolder;
			holder.description.setText(isInternetAvailable()
					? R.string.maps_you_need_descr : R.string.no_index_file_to_download);
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
			boolean primaryBtnVisible = updatePrimaryButton(holder);
			boolean secondaryBtnVisible = updateSecondaryButton(holder);
			holder.buttonsDivider.setVisibility(primaryBtnVisible && secondaryBtnVisible ? View.VISIBLE : View.GONE);
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

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updateSecondaryButton(NeededMapsVH vh) {
		vh.secondaryBtnContainer.setVisibility(View.VISIBLE);
		vh.secondaryBtn.setText(isDownloading() ? R.string.shared_string_cancel : R.string.later);
		vh.secondaryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onSecondaryButtonClick();
				}
			}
		});
		return true;
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updatePrimaryButton(NeededMapsVH vh) {
		if (!isDownloadingAll()) {
			boolean enabled = isInternetAvailable();
			vh.primaryBtnContainer.setVisibility(View.VISIBLE);
			vh.primaryBtnContainer.setBackgroundResource(getPrimaryBtnBgRes(enabled));
			vh.primaryButton.setTextColor(getResolvedColor(getPrimaryBtnTextColorRes(enabled)));
			vh.primaryButton.setEnabled(enabled);
			vh.primaryButton.setText(R.string.download_all);
			vh.primaryButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onPrimaryButtonClick();
					}
				}
			});
			return true;
		}
		vh.primaryBtnContainer.setVisibility(View.GONE);
		return false;
	}

	public boolean isDownloading() {
		for (IndexItem item : items) {
			if (downloadThread.isDownloading(item)) {
				return true;
			}
		}
		return false;
	}

	public boolean isDownloadingAll() {
		for (IndexItem item : items) {
			if (!downloadThread.isDownloading(item)) {
				return false;
			}
		}
		return true;
	}

	private String getItemDescription(IndexItem item) {
		return app.getString(R.string.file_size_in_mb, item.getArchiveSizeMB()) + " • " + item.getType().getString(app);
	}

	public interface CardListener {

		void onPrimaryButtonClick();

		void onSecondaryButtonClick();
	}

	public static class NeededMapsVH extends RecyclerView.ViewHolder {

		final TextView description;
		final LinearLayout itemsContainer;
		final View secondaryBtnContainer;
		final TextView secondaryBtn;
		final View buttonsDivider;
		final View primaryBtnContainer;
		final TextView primaryButton;

		@SuppressWarnings("RedundantCast")
		public NeededMapsVH(View itemView) {
			super(itemView);
			description = (TextView) itemView.findViewById(R.id.description);
			itemsContainer = (LinearLayout) itemView.findViewById(R.id.items_container);
			secondaryBtnContainer = itemView.findViewById(R.id.secondary_btn_container);
			secondaryBtn = (TextView) itemView.findViewById(R.id.secondary_button);
			buttonsDivider = itemView.findViewById(R.id.buttons_divider);
			primaryBtnContainer = itemView.findViewById(R.id.primary_btn_container);
			primaryButton = (TextView) itemView.findViewById(R.id.primary_button);
		}
	}
}
