package net.osmand.plus.wikivoyage.explore.travelcards;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class HeaderTravelCard extends BaseTravelCard {

	public static final int TYPE = 4;

	private int articleItemCount;
	private final String title;

	public HeaderTravelCard(OsmandApplication app, boolean nightMode, String title) {
		super(app, nightMode);
		this.title = title;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof HeaderTravelVH) {
			HeaderTravelVH holder = (HeaderTravelVH) viewHolder;
			holder.title.setText(title);
			int primaryTextColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
			holder.title.setTextColor(primaryTextColor);
			if (articleItemCount > 0) {
				holder.description.setText(String.valueOf(articleItemCount));
				holder.description.setTextColor(primaryTextColor);
				holder.description.setVisibility(View.VISIBLE);
			} else {
				holder.description.setVisibility(View.INVISIBLE);
			}
		}
	}

	public static class HeaderTravelVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;

		public HeaderTravelVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
		}
	}

	public void setArticleItemCount(int articleItemCount) {
		this.articleItemCount = articleItemCount;
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}
