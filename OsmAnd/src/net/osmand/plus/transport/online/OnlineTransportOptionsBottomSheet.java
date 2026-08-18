package net.osmand.plus.transport.online;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class OnlineTransportOptionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = OnlineTransportOptionsBottomSheet.class.getSimpleName();

	private Runnable onApplied;

	private OnlineTransportOptimize optimize;
	private boolean wheelchair;
	private final List<BottomSheetItemWithCompoundButton> optimizeItems = new ArrayList<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		OsmandSettings settings = app.getSettings();
		optimize = settings.ONLINE_TRANSPORT_OPTIMIZE.get();
		wheelchair = settings.ONLINE_TRANSPORT_WHEELCHAIR.get();
		optimizeItems.clear();

		ctx = UiUtilities.getThemedContext(ctx, nightMode);
		ApplicationMode appMode = settings.APPLICATION_MODE.get();
		ColorStateList tint = AndroidUtils.createCheckedColorIntStateList(
				ColorUtilities.getDefaultIconColor(ctx, nightMode), appMode.getProfileColor(nightMode));

		items.add(new TitleItem(getString(R.string.transit_optimize)));
		items.add(createRadio(OnlineTransportOptimize.BEST, R.string.transit_opt_best, tint));
		items.add(createRadio(OnlineTransportOptimize.FEWEST_TRANSFERS, R.string.transit_opt_fewest_changes, tint));
		items.add(createRadio(OnlineTransportOptimize.LEAST_WALKING, R.string.transit_opt_least_walking, tint));

		items.add(new DividerItem(ctx));
		items.add(createWheelchairSwitch());
	}

	@NonNull
	private BottomSheetItemWithCompoundButton createRadio(@NonNull OnlineTransportOptimize opt, int titleId, @NonNull ColorStateList tint) {
		BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(optimize == opt)
				.setButtonTintList(tint)
				.setTitle(getString(titleId))
				.setTag(opt)
				.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
				.setOnClickListener(v -> selectOptimize(opt))
				.create();
		optimizeItems.add(item);
		return item;
	}

	private void selectOptimize(@NonNull OnlineTransportOptimize opt) {
		optimize = opt;
		for (BottomSheetItemWithCompoundButton item : optimizeItems) {
			item.setChecked(item.getTag() == opt);
		}
	}

	@NonNull
	private BottomSheetItemWithCompoundButton createWheelchairSwitch() {
		BottomSheetItemWithCompoundButton[] ref = new BottomSheetItemWithCompoundButton[1];
		ref[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(wheelchair)
				.setTitle(getString(R.string.transit_wheelchair))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
				.setOnClickListener(v -> {
					wheelchair = !wheelchair;
					ref[0].setChecked(wheelchair);
				})
				.create();
		return ref[0];
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.APPLICATION_MODE.get();
		settings.ONLINE_TRANSPORT_OPTIMIZE.setModeValue(appMode, optimize);
		settings.ONLINE_TRANSPORT_WHEELCHAIR.setModeValue(appMode, wheelchair);
		app.getRoutingHelper().onSettingsChanged(true);
		if (onApplied != null) {
			onApplied.run();
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Runnable onApplied) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			OnlineTransportOptionsBottomSheet fragment = new OnlineTransportOptionsBottomSheet();
			fragment.onApplied = onApplied;
			fragment.show(manager, TAG);
		}
	}
}
