package net.osmand.plus.track.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class TrackAltitudeBottomSheet extends MenuBottomSheetDialogFragment implements InAppPurchaseListener {

	public static final String TAG = TrackAltitudeBottomSheet.class.getSimpleName();

	private static final String SEGMENT_INDEX_KEY = "segment_index_key";

	private int segmentIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			segmentIndex = savedInstanceState.getInt(SEGMENT_INDEX_KEY, -1);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.get_altitude_data)));
		createUseNearbyRoadsItem();
		if (InAppPurchaseUtils.is3dMapsAvailable(app)) {
			int margin = getDimensionPixelSize(R.dimen.divider_color_light_margin_start);
			DividerItem dividerItem = new DividerItem(app);
			dividerItem.setMargins(margin, 0, 0, 0);
			items.add(dividerItem);

			SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
			if (plugin != null && plugin.is3DReliefAllowed()) {
				createOfflineItem();
			} else {
				createOnlineItem();
			}
		} else {
			createOsmAndProItem();
		}
	}

	private void createUseNearbyRoadsItem() {
		BaseBottomSheetItem attachToRoadsItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.use_nearby_roads_summary))
				.setTitle(getString(R.string.use_nearby_roads))
				.setIcon(getActiveIcon(R.drawable.ic_action_attach_track))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof CalculateAltitudeListener listener) {
						listener.attachToRoadsSelected(segmentIndex);
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
				.setTitleColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setIcon(getContentIcon(R.drawable.ic_action_world_globe))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
				.setDisabled(true)
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

	private void createOfflineItem() {
		BaseBottomSheetItem attachToRoadsItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.use_terrain_maps_summary))
				.setTitle(getString(R.string.use_terrain_maps))
				.setIcon(getActiveIcon(R.drawable.ic_action_terrain))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof CalculateAltitudeListener listener) {
						listener.calculateOfflineSelected(segmentIndex);
					}
					dismiss();
				})
				.create();
		items.add(attachToRoadsItem);
	}

	private void createOsmAndProItem() {
		View view = inflate(R.layout.online_srtm_promo_item, itemsContainer, false);

		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.switch_button_active);
		view.setBackground(getPaintedIcon(R.drawable.promo_banner_bg, color));

		DialogButton button = view.findViewById(R.id.button_action);
		button.findViewById(R.id.button_container).setBackground(null);

		Drawable icon = getIcon(R.drawable.ic_action_osmand_pro_logo_colored);
		TextView textView = button.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(dpToPx(12));
		textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);

		BaseBottomSheetItem item = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.setOnClickListener(v -> callActivity(OsmAndProPlanFragment::showInstance))
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
	public void onItemPurchased(String sku, boolean active) {
		updateMenuItems();
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

		void calculateOfflineSelected(int segmentIndex);
	}
}