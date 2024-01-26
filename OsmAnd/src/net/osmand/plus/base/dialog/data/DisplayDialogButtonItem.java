package net.osmand.plus.base.dialog.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.base.dialog.interfaces.other.IOnClickListener;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DisplayDialogButtonItem {

	@StringRes
	private int titleId;
	private DialogButtonType buttonType = DialogButtonType.SECONDARY;
	private IOnClickListener onClickListener;

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

	public void onButtonClick(@Nullable IOnClickListener alternative) {
		if (onClickListener != null) {
			onClickListener.onClick();
		} else if (alternative != null) {
			alternative.onClick();
		}
	}

	@NonNull
	public DisplayDialogButtonItem setOnClickListener(@Nullable IOnClickListener onClickListener) {
		this.onClickListener = onClickListener;
		return this;
	}
}
