package net.osmand.plus.myplaces.favorites.dialogs;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.util.Algorithms;

import java.util.List;

public class FavoritesFreeBackupCard extends BaseCard {

	public static final int GET_OSMAND_CLOUD_BUTTON_INDEX = 0;

	public FavoritesFreeBackupCard(@NonNull FragmentActivity activity) {
		super(activity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.free_backup_card;
	}

	@Override
	protected void updateContent() {
		ImageView closeButton = view.findViewById(R.id.btn_close);
		closeButton.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_cancel, nightMode));
		closeButton.setOnClickListener(v -> dismiss());

		view.findViewById(R.id.dismiss_button_container)
				.setOnClickListener(v -> notifyButtonPressed(GET_OSMAND_CLOUD_BUTTON_INDEX));
	}

	private void dismiss() {
		settings.FAVORITES_FREE_ACCOUNT_CARD_DISMISSED.set(true);
		notifyCardPressed();
	}

	public static boolean shouldShow(@NonNull OsmandApplication app, @NonNull List<FavoriteGroup> favoriteGroups) {
		boolean hasFavorites = !Algorithms.isEmpty(favoriteGroups);
		boolean backupAvailable = InAppPurchaseUtils.isBackupAvailable(app);
		boolean registered = app.getBackupHelper().isRegistered();
		boolean dismissed = app.getSettings().FAVORITES_FREE_ACCOUNT_CARD_DISMISSED.get();
		return hasFavorites && !backupAvailable && !registered && !dismissed;
	}
}
