package net.osmand.plus.wikivoyage.explore.travelcards;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelNeededMapsCard.CardListener;

public class TravelButtonCard extends BaseTravelCard {

	public static final int TYPE = 5;
	private CardListener listener;

	public TravelButtonCard(OsmandApplication app, boolean nightMode) {
		super(app, nightMode);
	}

	public void setListener(CardListener listener) {
		this.listener = listener;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof TravelButtonVH) {
			final TravelButtonVH holder = (TravelButtonVH) viewHolder;
			UiUtilities.setupDialogButton(nightMode, holder.button, UiUtilities.DialogButtonType.SECONDARY, R.string.show_more);
			holder.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onPrimaryButtonClick();
					}
				}
			});
		}
	}

	public static class TravelButtonVH extends RecyclerView.ViewHolder {

		final View button;

		public TravelButtonVH(View itemView) {
			super(itemView);
			button = itemView.findViewById(R.id.button);
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}