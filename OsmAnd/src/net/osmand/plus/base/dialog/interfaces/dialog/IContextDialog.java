package net.osmand.plus.base.dialog.interfaces.dialog;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

/**
 * Implement this interface in specific dialogs
 * to implement the function of retrieving context objects from the dialog.
 */
public interface IContextDialog extends IDialog {

	FragmentActivity getActivity();

	Context getContext();

}
