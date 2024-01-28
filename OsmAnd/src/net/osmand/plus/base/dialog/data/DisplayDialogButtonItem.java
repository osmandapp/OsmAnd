package net.osmand.plus.base.dialog.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.base.dialog.interfaces.other.IOnButtonClick;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DisplayDialogButtonItem {

	@StringRes
	private int titleId;
	private DialogButtonType buttonType = DialogButtonType.SECONDARY;
	private IOnButtonClick onClickListener;

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public DisplayDialogButtonItem setTitleId(int titleId) {
		this.titleId = titleId;
		return this;
	}

	@NonNull
	public DialogButtonType getButtonType() {
		return buttonType;
	}

	@NonNull
	public DisplayDialogButtonItem setButtonType(@NonNull DialogButtonType buttonType) {
		this.buttonType = buttonType;
		return this;
	}

	public void onButtonClick(@Nullable IOnButtonClick alternative) {
		if (onClickListener != null) {
			onClickListener.onButtonClicked();
		} else if (alternative != null) {
			alternative.onButtonClicked();
		}
	}

	@NonNull
	public DisplayDialogButtonItem setOnClickListener(@Nullable IOnButtonClick onClickListener) {
		this.onClickListener = onClickListener;
		return this;
	}
}
