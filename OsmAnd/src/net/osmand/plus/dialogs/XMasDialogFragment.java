package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.Date;

public class XMasDialogFragment extends DialogFragment {

	public static final String TAG = "XMasDialogFragment";
	private static boolean XmasDialogWasProcessed = false;

	public static boolean shouldShowXmasDialog(OsmandApplication app) {
		if (XmasDialogWasProcessed || app.getSettings().DO_NOT_SHOW_STARTUP_MESSAGES.get()) {
			return false;
		}
		int numberOfStarts = app.getAppInitializer().getNumberOfStarts();
		if (numberOfStarts > 2) {
			Date now = new Date();
			Date start = new Date(now.getYear(), 11, 5, 0, 0);
			Date end = new Date(now.getYear(), 11, 25, 23, 59);
			int firstShownX = app.getSettings().NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.get();
			if (now.after(start) && now.before(end)) {
				if (firstShownX == 0 || numberOfStarts - firstShownX == 3 || numberOfStarts - firstShownX == 10) {
					if (firstShownX == 0) {
						app.getSettings().NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.set(numberOfStarts);
					}
				} else {
					return false;
				}
			} else {
				if (firstShownX != 0) {
					app.getSettings().NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.set(0);
				}
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		XmasDialogWasProcessed = true;
		final MapActivity mapActivity = (MapActivity) getActivity();

		final AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity, R.style.XmasDialogTheme);
		View titleView = mapActivity.getLayoutInflater().inflate(R.layout.xmas_dialog_title, null);
		builder.setCustomTitle(titleView);
		builder.setCancelable(true);
		builder.setNegativeButton(mapActivity.getString(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(mapActivity.getString(R.string.shared_string_show), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				PoiCategory xmas = mapActivity.getMyApplication().getPoiTypes().getPoiCategoryByName("xmas");
				if (xmas != null) {
					mapActivity.showQuickSearch(xmas);
				}
			}
		});

		builder.setView(mapActivity.getLayoutInflater().inflate(R.layout.xmas_dialog, null));

		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				// Customize POSITIVE, NEGATIVE and NEUTRAL buttons.
				Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
				positiveButton.setTextColor(mapActivity.getResources().getColor(R.color.color_white));
				positiveButton.invalidate();

				Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
				negativeButton.setTextColor(mapActivity.getResources().getColor(R.color.color_white));
				negativeButton.invalidate();
			}
		});
		return dialog;
	}
}
