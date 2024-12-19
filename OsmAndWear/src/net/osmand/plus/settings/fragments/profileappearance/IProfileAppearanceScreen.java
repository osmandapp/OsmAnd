package net.osmand.plus.settings.fragments.profileappearance;

import android.content.DialogInterface;

import androidx.annotation.Nullable;

import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.settings.backend.ApplicationMode;

public interface IProfileAppearanceScreen extends IAskDismissDialog, IDialogNightModeInfoProvider {
	ApplicationMode getSelectedAppMode();
	void showNewProfileSavingDialog(@Nullable DialogInterface.OnShowListener showListener);
	void dismissProfileSavingDialog();
	void goBackWithoutSaving();
	void customProfileSaved();
	void updateColorItems();
	void updateOptionsCard();
	void updateApplyButtonEnable();
	void updateStatusBar();
	void hideKeyboard();
}
