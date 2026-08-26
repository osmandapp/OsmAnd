package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.myplaces.tracks.dialogs.TracksFreeBackupCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;

public class TrackFreeBackupCardViewHolder extends RecyclerView.ViewHolder {

	private final TracksFreeBackupCard card;

	public TrackFreeBackupCardViewHolder(@NonNull View itemView,
	                                     @NonNull FragmentActivity activity,
	                                     @Nullable CardListener cardListener) {
		super(itemView);
		card = new TracksFreeBackupCard(activity);
		card.setListener(cardListener);
		ViewGroup cardContainer = itemView.findViewById(R.id.card_container);
		cardContainer.addView(card.build(cardContainer.getContext()));
	}

	public void bindView() {
		card.update();
	}
}
