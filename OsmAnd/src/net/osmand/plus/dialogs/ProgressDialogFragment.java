package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

/**
 * Created by GaidamakUA on 12/7/15.
 */
public class ProgressDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = "progress";
	private static final String TITLE_ID = "title_id";
	private static final String MESSAGE_ID = "message_id";
	private static final String STYLE = "style";
	private int mMax;
	private int accumulatedDelta;
	private DialogInterface.OnDismissListener onDismissListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Bundle args = requireArguments();
		int titleId = args.getInt(TITLE_ID);
		int messageId = args.getInt(MESSAGE_ID);
		int style = args.getInt(STYLE);

		ProgressDialog dialog = ProgressImplementation.createProgressDialog(
				getThemedContext(), getString(titleId), getString(messageId), style).getDialog();
		dialog.setIndeterminate(false);
		dialog.setMax(mMax);
		dialog.setProgress(0);
		return dialog;
	}

	public void setMax(int max) {
		mMax = max;
		if (getDialog() != null) {
			getProgressDialog().setMax(mMax);
		}
	}

	public void incrementProgressBy(int delta) {
		if (getDialog() == null) {
			accumulatedDelta += delta;
		} else {
			getProgressDialog().incrementProgressBy(delta + accumulatedDelta);
		}
	}

	private ProgressDialog getProgressDialog() {
		return (ProgressDialog) getDialog();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		if (onDismissListener != null) {
			onDismissListener.onDismiss(dialog);
		}
	}

	public void setOnDismissListener(@NonNull DialogInterface.OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	@NonNull
	public static ProgressDialogFragment showInstance(@NonNull FragmentManager fragmentManager,
	                                                  int titleId, int messageId, int style) {
		ProgressDialogFragment fragment = new ProgressDialogFragment();
		Bundle args = new Bundle();
		args.putInt(TITLE_ID, titleId);
		args.putInt(MESSAGE_ID, messageId);
		args.putInt(STYLE, style);
		fragment.setArguments(args);
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragment.show(fragmentManager, TAG);
		}
		return fragment;
	}
}
