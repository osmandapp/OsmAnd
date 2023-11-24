package net.osmand.plus.backup.ui.status;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
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
		View button = view.findViewById(R.id.trash_button);

		TextView title = button.findViewById(R.id.title);
		ImageView icon = button.findViewById(R.id.icon);
		ImageView proIcon = view.findViewById(R.id.pro_icon);

		title.setText(R.string.shared_string_trash);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));
		proIcon.setImageDrawable(getIcon(nightMode ? R.drawable.img_button_pro_night : R.drawable.img_button_pro_day));

		button.setOnClickListener(v -> notifyButtonPressed(TRASH_BUTTON_INDEX));

		setupSelectableBackground(button);
		AndroidUiHelper.updateVisibility(proIcon, !InAppPurchaseUtils.isBackupAvailable(app));
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = ColorUtilities.getActiveColor(app, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));
	}
}