package net.osmand.plus.myplaces.favorites.dialogs;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;

public class FavoriteFreeBackupCardViewHolder extends RecyclerView.ViewHolder {

	private final FavoritesFreeBackupCard card;

	public FavoriteFreeBackupCardViewHolder(@NonNull View itemView,
	                                        @NonNull FragmentActivity activity,
	                                        @Nullable CardListener cardListener) {
		super(itemView);
		card = new FavoritesFreeBackupCard(activity);
		card.setListener(cardListener);

		ViewGroup cardContainer = itemView.findViewById(R.id.card_container);
		cardContainer.addView(card.build(cardContainer.getContext()));
	}

	public void bindView() {
		card.update();
	}
}
