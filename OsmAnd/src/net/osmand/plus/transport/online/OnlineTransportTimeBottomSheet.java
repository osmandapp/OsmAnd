package net.osmand.plus.transport.online;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

import java.util.Calendar;

public class OnlineTransportTimeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = OnlineTransportTimeBottomSheet.class.getSimpleName();

	private Runnable onApplied;

	@NonNull
	public static String getLabel(@NonNull OsmandApplication app) {
		if (OnlineTransportState.isNow()) {
			return app.getString(R.string.transit_leave_now);
		}
		String time = OsmAndFormatter.getFormattedTimeShort(OnlineTransportState.getTimeMillis() / 1000, false);
		return OnlineTransportState.isArriveBy() ? app.getString(R.string.transit_arrive_prefix, time) : time;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);
		ColorStateList tint = AndroidUtils.createCheckedColorIntStateList(
				ColorUtilities.getDefaultIconColor(ctx, nightMode),
				app.getSettings().APPLICATION_MODE.get().getProfileColor(nightMode));

		boolean now = OnlineTransportState.isNow();
		boolean arriveBy = OnlineTransportState.isArriveBy();
		String picked = now ? null : formatDateTime(ctx, OnlineTransportState.getTimeMillis());

		items.add(new TitleItem(getString(R.string.transit_departure_time)));
		items.add(new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(now)
				.setButtonTintList(tint)
				.setTitle(getString(R.string.transit_leave_now))
				.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
				.setOnClickListener(v -> {
					OnlineTransportState.reset();
					apply();
				})
				.create());
		items.add(new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!now && !arriveBy)
				.setButtonTintList(tint)
				.setDescription(!now && !arriveBy ? picked : null)
				.setTitle(getString(R.string.transit_depart_at))
				.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
				.setOnClickListener(v -> pickDateTime(false))
				.create());
		items.add(new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!now && arriveBy)
				.setButtonTintList(tint)
				.setDescription(!now && arriveBy ? picked : null)
				.setTitle(getString(R.string.transit_arrive_by))
				.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
				.setOnClickListener(v -> pickDateTime(true))
				.create());
	}

	private void pickDateTime(boolean arriveBy) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		if (!OnlineTransportState.isNow()) {
			cal.setTimeInMillis(OnlineTransportState.getTimeMillis());
		}
		new DatePickerDialog(activity, (dateView, year, month, day) -> {
			Calendar picked = Calendar.getInstance();
			if (!OnlineTransportState.isNow()) {
				picked.setTimeInMillis(OnlineTransportState.getTimeMillis());
			}
			picked.set(Calendar.YEAR, year);
			picked.set(Calendar.MONTH, month);
			picked.set(Calendar.DAY_OF_MONTH, day);
			new TimePickerDialog(activity, (timeView, hour, minute) -> {
				picked.set(Calendar.HOUR_OF_DAY, hour);
				picked.set(Calendar.MINUTE, minute);
				picked.set(Calendar.SECOND, 0);
				OnlineTransportState.setTimeMillis(picked.getTimeInMillis());
				OnlineTransportState.setArriveBy(arriveBy);
				apply();
			}, picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity)).show();
		}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
	}

	private void apply() {
		app.getRoutingHelper().onSettingsChanged(true);
		if (onApplied != null) {
			onApplied.run();
		}
		dismiss();
	}

	@NonNull
	private static String formatDateTime(@NonNull Context ctx, long millis) {
		return DateFormat.getDateFormat(ctx).format(millis) + " " + DateFormat.getTimeFormat(ctx).format(millis);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Runnable onApplied) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			OnlineTransportTimeBottomSheet fragment = new OnlineTransportTimeBottomSheet();
			fragment.onApplied = onApplied;
			fragment.show(manager, TAG);
		}
	}
}
