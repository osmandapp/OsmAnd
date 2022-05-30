package net.osmand.plus.track.fragments;

import static net.osmand.plus.utils.UiUtilities.DialogButtonType.SECONDARY_ACTIVE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class TrackAltitudeBottomSheet extends MenuBottomSheetDialogFragment implements InAppPurchaseListener {

	public static final String TAG = TrackAltitudeBottomSheet.class.getSimpleName();

	private static final String SEGMENT_INDEX_KEY = "segment_index_key";

	private OsmandApplication app;
	private int segmentIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();

		if (savedInstanceState != null) {
			segmentIndex = savedInstanceState.getInt(SEGMENT_INDEX_KEY, -1);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.get_altitude_data)));
		createAttachToRoadsItem();
		if (InAppPurchaseHelper.isOsmAndProAvailable(app)) {
			int margin = getResources().getDimensionPixelSize(R.dimen.settings_divider_margin_start);
			DividerItem dividerItem = new DividerItem(app);
			dividerItem.setMargins(margin, 0, 0, 0);
			items.add(dividerItem);

			createOnlineItem();
		} else {
			createOsmAndProItem();
		}
	}

	private void createAttachToRoadsItem() {
		BaseBottomSheetItem attachToRoadsItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.track_attach_to_the_roads_descr))
				.setTitle(getString(R.string.attach_to_the_roads))
				.setIcon(getActiveIcon(R.drawable.ic_action_attach_track))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
				.setOnClickListener(v -> {
					Fragment fragment = getTargetFragment();
					if (fragment instanceof CalculateAltitudeListener) {
						((CalculateAltitudeListener) fragment).attachToRoadsSelected(segmentIndex);
					}
					dismiss();
				})
				.create();
		items.add(attachToRoadsItem);
	}

	private void createOnlineItem() {
		BaseBottomSheetItem attachToRoadsItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.calculate_online_altitude_descr))
				.setTitle(getString(R.string.calculate_online))
				.setIcon(getActiveIcon(R.drawable.ic_action_world_globe))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
				.setOnClickListener(v -> {
					Fragment fragment = getTargetFragment();
					if (fragment instanceof CalculateAltitudeListener) {
						((CalculateAltitudeListener) fragment).calculateOnlineSelected(segmentIndex);
					}
					dismiss();
				})
				.create();
		items.add(attachToRoadsItem);
	}

	private void createOsmAndProItem() {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		View view = inflater.inflate(R.layout.online_srtm_promo_item, itemsContainer, false);

		UiUtilities utilities = app.getUIUtilities();
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.switch_button_active);
		view.setBackground(utilities.getPaintedIcon(R.drawable.promo_banner_bg, color));

		View button = view.findViewById(R.id.button_action);
		UiUtilities.setupDialogButton(nightMode, button, SECONDARY_ACTIVE, R.string.shared_string_get);
		button.findViewById(R.id.button_container).setBackground(null);

		Drawable icon = utilities.getIcon(R.drawable.ic_action_osmand_pro_logo_colored);
		TextView textView = button.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 12));
		textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);

		BaseBottomSheetItem item = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						OsmAndProPlanFragment.showInstance(activity);
					}
				})
				.create();
		items.add(item);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SEGMENT_INDEX_KEY, segmentIndex);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {

	}

	@Override
	public void onGetItems() {

	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updateMenuItems();
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {

	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {

	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target, int segmentIndex) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackAltitudeBottomSheet fragment = new TrackAltitudeBottomSheet();
			fragment.segmentIndex = segmentIndex;
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface CalculateAltitudeListener {

		void attachToRoadsSelected(int segmentIndex);

		void calculateOnlineSelected(int segmentIndex);
	}
}