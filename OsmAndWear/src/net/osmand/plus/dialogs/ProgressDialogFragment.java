package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.ProgressImplementation;

/**
 * Created by GaidamakUA on 12/7/15.
 */
public class ProgressDialogFragment extends DialogFragment {

	public static final String TAG = "progress";
	private static final String TITLE_ID = "title_id";
	private static final String MESSAGE_ID = "message_id";
	private static final String STYLE = "style";
	private int mMax;
	private int accumulatedDelta;
	private DialogInterface.OnDismissListener onDismissListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int titleId = args.getInt(TITLE_ID);
		int messageId = args.getInt(MESSAGE_ID);
		int style = args.getInt(STYLE);
		ProgressDialog dialog = ProgressImplementation.createProgressDialog(getActivity(),
				getString(titleId), getString(messageId),
				style).getDialog();

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
	public void onDismiss(DialogInterface dialog) {
		if (onDismissListener != null) {
			onDismissListener.onDismiss(dialog);
		}
	}

	public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	public static ProgressDialogFragment createInstance(int titleId, int messageId, int style) {
		ProgressDialogFragment fragment = new ProgressDialogFragment();
		Bundle args = new Bundle();
		args.putInt(TITLE_ID, titleId);
		args.putInt(MESSAGE_ID, messageId);
		args.putInt(STYLE, style);
		fragment.setArguments(args);
		return fragment;
	}
}
