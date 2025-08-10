package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Calendar;
import java.util.Date;

public class XMasDialogFragment extends BaseAlertDialogFragment {

	public static final String TAG = XMasDialogFragment.class.getSimpleName();
	private static boolean XmasDialogWasProcessed;

	public static boolean shouldShowXmasDialog(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (XmasDialogWasProcessed || settings.DO_NOT_SHOW_STARTUP_MESSAGES.get()) {
			return false;
		}
		int numberOfStarts = app.getAppInitializer().getNumberOfStarts();
		if (numberOfStarts > 2) {
			Date now = new Date();
			Date start = createDateInCurrentYear(Calendar.DECEMBER, 5, 0, 0);
			Date end = createDateInCurrentYear(Calendar.DECEMBER, 25, 23, 59);
			int firstShownX = settings.NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.get();
			if (now.after(start) && now.before(end)) {
				if (firstShownX == 0 || numberOfStarts - firstShownX == 3 || numberOfStarts - firstShownX == 10) {
					if (firstShownX == 0) {
						settings.NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.set(numberOfStarts);
					}
				} else {
					return false;
				}
			} else {
				if (firstShownX != 0) {
					settings.NUMBER_OF_STARTS_FIRST_XMAS_SHOWN.set(0);
				}
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	private static Date createDateInCurrentYear(int month, int date, int hour, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DATE, date);
		calendar.set(Calendar.HOUR, hour);
		calendar.set(Calendar.MINUTE, minute);
		return calendar.getTime();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		XmasDialogWasProcessed = true;
		MapActivity mapActivity = requireMapActivity();

		AlertDialog.Builder builder = createDialogBuilder();
		View titleView = inflate(R.layout.xmas_dialog_title);
		builder.setCustomTitle(titleView);
		builder.setCancelable(true);
		builder.setNegativeButton(getString(R.string.shared_string_cancel), (dialog, which) -> dialog.dismiss());
		builder.setPositiveButton(getString(R.string.shared_string_show), (dialog, which) -> {
			dialog.dismiss();
			PoiCategory xmas = app.getPoiTypes().getPoiCategoryByName("xmas");
			if (xmas != null) {
				mapActivity.getFragmentsHelper().showQuickSearch(xmas);
			}
		});
		builder.setView(inflate(R.layout.xmas_dialog));

		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(d -> {
			// Customize POSITIVE, NEGATIVE and NEUTRAL buttons.
			Button positiveButton = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
			positiveButton.setTextColor(mapActivity.getColor(R.color.card_and_list_background_light));
			positiveButton.invalidate();

			Button negativeButton = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEGATIVE);
			negativeButton.setTextColor(mapActivity.getColor(R.color.card_and_list_background_light));
			negativeButton.invalidate();
		});
		return dialog;
	}

	@Override
	protected int getDialogThemeId() {
		return R.style.XmasDialogTheme;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			new XMasDialogFragment().show(fragmentManager, TAG);
		}
	}
}
