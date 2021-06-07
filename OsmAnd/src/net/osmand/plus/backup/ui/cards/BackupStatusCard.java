package net.osmand.plus.backup.ui.cards;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.util.Algorithms;

import static net.osmand.plus.backup.ui.cards.LocalBackupCard.adjustIndicator;

public class BackupStatusCard extends MapBaseCard {

	public static final int RETRY_BUTTON_INDEX = 0;
	public static final int BACKUP_BUTTON_INDEX = 1;

	private final String error;
	private final BackupInfo backupInfo;

	private BackupStatus status;
	private View itemsContainer;

	private boolean buttonsVisible = true;

	public BackupStatusCard(@NonNull MapActivity mapActivity, BackupInfo backupInfo, String error) {
		super(mapActivity, false);
		this.error = error;
		this.backupInfo = backupInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.backup_status_card;
	}

	@Override
	protected void updateContent() {
		itemsContainer = view.findViewById(R.id.items_container);

		if (backupInfo != null) {
			if (!Algorithms.isEmpty(backupInfo.filesToMerge)) {
				status = BackupStatus.CONFLICTS;
			} else if (!Algorithms.isEmpty(backupInfo.filesToUpload)) {
				status = BackupStatus.MAKE_BACKUP;
			}
		} else if (!app.getSettings().isInternetConnectionAvailable()) {
			status = BackupStatus.NO_INTERNET_CONNECTION;
		} else if (error != null) {
			status = BackupStatus.ERROR;
		}
		if (status == null) {
			status = BackupStatus.BACKUP_COMPLETE;
		}
		setupActionButton();
		setupStatusContainer();
		setupWarningContainer();
		AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
	}

	private void setupActionButton() {
		View button = view.findViewById(R.id.action_button);
		UiUtilities.setupDialogButton(nightMode, button, DialogButtonType.SECONDARY, status.actionTitleRes);
		AndroidUtils.setBackground(app, button, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					if (status == BackupStatus.CONFLICTS || status == BackupStatus.BACKUP_COMPLETE || status == BackupStatus.MAKE_BACKUP) {
						listener.onCardButtonPressed(BackupStatusCard.this, BACKUP_BUTTON_INDEX);
					} else {
						listener.onCardButtonPressed(BackupStatusCard.this, RETRY_BUTTON_INDEX);
					}
				}
			}
		});
	}

	private void setupWarningContainer() {
		View container = view.findViewById(R.id.warning_container);
		ImageView icon = container.findViewById(R.id.warning_icon);
		TextView title = container.findViewById(R.id.warning_title);
		TextView description = container.findViewById(R.id.warning_description);

		if (status.warningTitleRes != -1 || !Algorithms.isEmpty(error)) {
			if (status.warningTitleRes != -1) {
				title.setText(status.warningTitleRes);
				description.setText(status.warningDescriptionRes);
			} else {
				title.setText(R.string.subscribe_email_error);
				description.setText(error);
			}
			setupWarningRoundedBg(container);
			AndroidUiHelper.updateVisibility(container, true);
			icon.setImageDrawable(getContentIcon(status.warningIconRes));
		} else {
			AndroidUiHelper.updateVisibility(container, false);
		}
	}

	private void setupStatusContainer() {
		TextView title = view.findViewById(R.id.status_title);
		title.setText(status.statusTitleRes);

		TextView description = view.findViewById(R.id.status_description);
		String backupTime = MainSettingsFragment.getLastBackupTimeDescription(app);
		if (Algorithms.isEmpty(backupTime)) {
			description.setText(R.string.shared_string_never);
		} else {
			description.setText(backupTime);
		}

		ImageView icon = view.findViewById(R.id.status_icon);
		icon.setImageDrawable(getContentIcon(status.statusIconRes));

		View container = view.findViewById(R.id.status_container);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonsVisible = !buttonsVisible;
				adjustIndicator(app, buttonsVisible, view, nightMode);
				AndroidUiHelper.updateVisibility(itemsContainer, buttonsVisible);
			}
		});
		adjustIndicator(app, buttonsVisible, view, nightMode);
		setupSelectableBackground(container);
	}

	private void setupSelectableBackground(View view) {
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(view, drawable);
	}

	public void setupWarningRoundedBg(View selectableView) {
		int color = AndroidUtils.getColorFromAttr(selectableView.getContext(), R.attr.activity_background_color);
		int selectedColor = UiUtilities.getColorWithAlpha(getActiveColor(), 0.3f);

		Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, color);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			Drawable selectable = getPaintedIcon(R.drawable.ripple_rectangle_rounded, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(selectableView, new LayerDrawable(layers));
		} else {
			AndroidUtils.setBackground(selectableView, bgDrawable);
		}
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) selectableView.getLayoutParams();
		params.setMargins(params.leftMargin, AndroidUtils.dpToPx(app, 6), params.rightMargin, params.bottomMargin);
	}

	protected enum BackupStatus {
		BACKUP_COMPLETE(R.string.backup_complete, R.drawable.ic_action_cloud_done, -1, -1, -1, R.string.backup_now),
		MAKE_BACKUP(R.string.last_backup, R.drawable.ic_action_cloud, R.drawable.ic_action_alert_circle, R.string.make_backup, R.string.make_backup_descr, R.string.backup_now),
		CONFLICTS(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_alert, R.string.backup_conflicts, R.string.backup_confilcts_descr, R.string.backup_view_conflicts),
		NO_INTERNET_CONNECTION(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_wifi_off, R.string.no_inet_connection, R.string.backup_no_internet_descr, R.string.retry),
		ERROR(R.string.last_backup, R.drawable.ic_action_cloud_alert, R.drawable.ic_action_alert, -1, -1, R.string.retry);

		@StringRes
		public int statusTitleRes;
		@DrawableRes
		public int statusIconRes;
		@DrawableRes
		public int warningIconRes;
		@StringRes
		public int warningTitleRes;
		@StringRes
		public int warningDescriptionRes;
		@StringRes
		public int actionTitleRes;

		BackupStatus(int statusTitleRes, int statusIconRes, int warningIconRes, int warningTitleRes,
					 int warningDescriptionRes, int actionTitleRes) {
			this.statusTitleRes = statusTitleRes;
			this.statusIconRes = statusIconRes;
			this.warningIconRes = warningIconRes;
			this.warningTitleRes = warningTitleRes;
			this.warningDescriptionRes = warningDescriptionRes;
			this.actionTitleRes = actionTitleRes;
		}
	}
}