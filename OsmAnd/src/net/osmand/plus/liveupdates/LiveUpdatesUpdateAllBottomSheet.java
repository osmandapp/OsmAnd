package net.osmand.plus.liveupdates;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.LiveUpdateListener;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;

public class LiveUpdatesUpdateAllBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = LiveUpdatesUpdateAllBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesUpdateAllBottomSheet.class);

	private BaseBottomSheetItem itemTitle;
	private BaseBottomSheetItem itemDescription;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		updateBottomButtons();

		itemTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.update_all_maps_now))
				.setTitleColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create();
		items.add(itemTitle);

		String osmAndLive = "\"" + getString(R.string.osm_live) + "\"";
		itemDescription = new LongDescriptionItem.Builder()
				.setDescription(getString(R.string.update_all_maps_added, osmAndLive))
				.setDescriptionMaxLines(5)
				.setDescriptionColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(itemDescription);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		((TextViewEx) itemTitle.getView()).setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimensionPixelSize(R.dimen.default_list_text_size));
		TextView textDescription = (TextView) itemDescription.getView();
		textDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimensionPixelSize(R.dimen.default_list_text_size));
		textDescription.setMinHeight(getDimensionPixelSize(R.dimen.context_menu_sub_info_height));
		return view;
	}

	private void updateAll() {
		Fragment target = getTargetFragment();
		if (target instanceof LiveUpdateListener) {
			runLiveUpdate(getActivity(), false, (LiveUpdateListener) target);
		}
	}

	@Override
	protected void onRightBottomButtonClick() {
		updateAll();
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.update_now;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			LiveUpdatesUpdateAllBottomSheet fragment = new LiveUpdatesUpdateAllBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
