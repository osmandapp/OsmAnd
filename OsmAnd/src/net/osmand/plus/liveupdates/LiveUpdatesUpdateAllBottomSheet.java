package net.osmand.plus.liveupdates;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.liveupdates.PerformLiveUpdateAsyncTask.LiveUpdateListener;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.AndroidUtils.getPrimaryTextColorId;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;

public class LiveUpdatesUpdateAllBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = LiveUpdatesUpdateAllBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesUpdateAllBottomSheet.class);
	private static final String MAPS_TO_UPDATE = "maps_to_update";
	private static final String LIVE_UPDATE_LISTENER = "live_update_listener";

	private BaseBottomSheetItem itemTitle;
	private BaseBottomSheetItem itemDescription;

	private List<LocalIndexInfo> mapsList;
	private LiveUpdateListener listener;

	public void setMapsList(List<LocalIndexInfo> mapsList) {
		this.mapsList = mapsList;
	}

	public void setListener(LiveUpdateListener listener) {
		this.listener = listener;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
									List<LocalIndexInfo> mapsList, LiveUpdateListener listener) {
		if (!fragmentManager.isStateSaved()) {
			LiveUpdatesUpdateAllBottomSheet fragment = new LiveUpdatesUpdateAllBottomSheet();
			fragment.setMapsList(mapsList);
			fragment.setListener(listener);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mapsList = savedInstanceState.getParcelableArrayList(MAPS_TO_UPDATE);
			listener = (LiveUpdateListener) savedInstanceState.getSerializable(LIVE_UPDATE_LISTENER);
		}

		updateBottomButtons();

		itemTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.update_all_maps_now))
				.setTitleColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create();
		items.add(itemTitle);

		String osmAndLive = "\"" + getString(R.string.osm_live) + "\"";
		itemDescription = new LongDescriptionItem.Builder()
				.setDescription(getString(R.string.update_all_maps_added, osmAndLive))
				.setDescriptionMaxLines(5)
				.setDescriptionColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(itemDescription);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		((TextViewEx) itemTitle.getView()).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
		TextView textDescription = (TextView) itemDescription.getView();
		textDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
		textDescription.setMinHeight(getResources().getDimensionPixelSize(R.dimen.context_menu_sub_info_height));
		return view;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(MAPS_TO_UPDATE, (ArrayList<? extends Parcelable>) mapsList);
		outState.putSerializable(LIVE_UPDATE_LISTENER, listener);
	}

	private void updateAll() {
		for (LocalIndexInfo li : mapsList) {
			runLiveUpdate(getActivity(), li.getFileName(), false, listener);
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

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}
}
