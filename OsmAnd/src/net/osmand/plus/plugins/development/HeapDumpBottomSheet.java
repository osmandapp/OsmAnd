package net.osmand.plus.plugins.development;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.feedback.HeapDump;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * Everything about heap dumps in one sheet: take one now and read the result, share the last
 * report, or let the app take one by itself while the heap is large.
 */
public class HeapDumpBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = HeapDumpBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(HeapDumpBottomSheet.class);

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.heap_dump)));

		// no icons here, so the rows line up with the switch below, which has no icon slot
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.heap_dump_collect_descr))
				.setIconHidden(true)
				.setTitle(getString(R.string.heap_dump_collect))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> collect())
				.create());

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.heap_dump_share_descr))
				.setIconHidden(true)
				.setTitle(getString(R.string.heap_dump_share))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					share();
					dismiss();
				})
				.create());

		items.add(new DividerHalfItem(getContext()));

		BottomSheetItemWithCompoundButton[] auto = new BottomSheetItemWithCompoundButton[1];
		auto[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(settings.AUTO_HEAP_HISTOGRAM.get())
				.setDescription(getString(R.string.heap_dump_auto_descr))
				.setTitle(getString(R.string.heap_dump_auto))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
				.setOnClickListener(v -> {
					boolean checked = !auto[0].isChecked();
					settings.AUTO_HEAP_HISTOGRAM.set(checked);
					auto[0].setChecked(checked);
				})
				.create();
		items.add(auto[0]);
	}

	private void collect() {
		app.showShortToastMessage(R.string.heap_dump_collect);
		new Thread(() -> {
			String summary;
			try {
				summary = HeapDump.collect(app);
			} catch (IOException | RuntimeException | OutOfMemoryError e) {
				LOG.error(e);
				summary = "Failed: " + e.getMessage();
			}
			String result = summary;
			app.runInUIThread(() -> showResult(result));
		}, "HeapHistogram").start();
	}

	// the numbers are the point of pressing the button, so they are shown straight away
	private void showResult(@NonNull String summary) {
		Activity activity = getActivity();
		if (activity == null) {
			app.showToastMessage(summary);
			return;
		}
		new AlertDialog.Builder(activity)
				.setTitle(R.string.heap_dump)
				.setMessage(summary)
				.setPositiveButton(R.string.shared_string_close, null)
				.show();
	}

	private void share() {
		app.showShortToastMessage(R.string.heap_dump_share);
		app.getFeedbackHelper().sendCrashReport(sent -> {
			app.showToastMessage(sent ? R.string.shared_string_ok : R.string.unexpected_error_occurred_warn);
			return true;
		});
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (!manager.isStateSaved() && manager.findFragmentByTag(TAG) == null) {
			HeapDumpBottomSheet fragment = new HeapDumpBottomSheet();
			fragment.show(manager, TAG);
		}
	}
}
