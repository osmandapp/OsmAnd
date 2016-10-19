package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import net.osmand.plus.R;

public class OtherDialogs {

	public static void showXMasDialog(final Activity activity) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.XmasDialogTheme);
		View titleView = activity.getLayoutInflater().inflate(R.layout.xmas_dialog_title, null);
		builder.setCustomTitle(titleView);
		builder.setCancelable(true);
		builder.setNegativeButton(activity.getString(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(activity.getString(R.string.shared_string_show), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				//showQuickSearch();
			}
		});

		builder.setView(activity.getLayoutInflater().inflate(R.layout.xmas_dialog, null));

		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				// Customize POSITIVE, NEGATIVE and NEUTRAL buttons.
				Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
				positiveButton.setTextColor(activity.getResources().getColor(R.color.color_white));
				positiveButton.invalidate();

				Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
				negativeButton.setTextColor(activity.getResources().getColor(R.color.color_white));
				negativeButton.invalidate();
			}
		});
		dialog.show();
	}
}
