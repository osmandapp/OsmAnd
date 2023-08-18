package net.osmand.plus.backup.ui.status;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class BackupStorageCard extends BaseCard {

	public static final int TRASH_BUTTON_INDEX = 1;

	public BackupStorageCard(@NonNull FragmentActivity activity) {
		super(activity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_cloud_backup_storage;
	}

	@Override
	protected void updateContent() {
		setupTrashButton();
	}

	private void setupTrashButton() {
		View btnTrash = view.findViewById(R.id.trash_button);
		btnTrash.setOnClickListener(v -> notifyButtonPressed(TRASH_BUTTON_INDEX));
		setupSelectableBackground(btnTrash);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Context ctx = view.getContext();
		int color = ColorUtilities.getActiveColor(ctx, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f));
	}

}
