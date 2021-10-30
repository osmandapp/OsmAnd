package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.util.Algorithms;

import java.io.File;

public class SavedTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SavedTrackBottomSheetDialogFragment.class.getSimpleName();

	private static final String FILE_NAME_KEY = "file_name_key";
	private static final String SHOW_CREATE_NEW_ROUTE_BUTTON = "show_create_new_route_button";

	private String fileName;
	private boolean showCreateNewRouteButton;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		if (view != null && !showCreateNewRouteButton) {
			AndroidUiHelper.updateVisibility(rightButton, false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider_top), false);
		}
		return view;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			fileName = savedInstanceState.getString(FILE_NAME_KEY);
			showCreateNewRouteButton = savedInstanceState.getBoolean(SHOW_CREATE_NEW_ROUTE_BUTTON);
		}

		View mainView = View.inflate(UiUtilities.getThemedContext(getMyApplication(), nightMode),
				R.layout.measure_track_is_saved, null);
		TextView fileNameView = mainView.findViewById(R.id.file_name);
		fileNameView.setText(Algorithms.getFileWithoutDirs(fileName));
		items.add(new SimpleBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create());

		DividerItem divider = new DividerItem(getContext());
		int contextPadding = getResources().getDimensionPixelSize(R.dimen.content_padding);
		int contextPaddingSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		divider.setMargins(contextPadding, contextPadding, contextPadding, contextPaddingSmall);
		items.add(divider);

		items.add(new BottomSheetItemButton.Builder()
				.setTitle(getString(R.string.open_saved_track))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null && !Algorithms.isEmpty(fileName)) {
							TrackMenuFragment.openTrack(activity, new File(fileName), null);
						}
						dismiss();
					}
				})
				.create());
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.shared_string_share;
	}

	@Override
	protected void onThirdBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			GpxUiHelper.shareGpx(activity, new File(fileName));
		}
		dismiss();
	}

	@Override
	protected UiUtilities.DialogButtonType getThirdBottomButtonType() {
		return UiUtilities.DialogButtonType.SECONDARY;
	}

	@Override
	protected int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.content_padding_small);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.plan_route_create_new_route;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.SECONDARY;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			MeasurementToolFragment.showInstance(((MapActivity) activity).getSupportFragmentManager(),
					((MapActivity) activity).getMapLocation());
		}
		dismiss();
	}

	@Override
	protected int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.context_menu_sub_info_height);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_exit;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(FILE_NAME_KEY, fileName);
		outState.putBoolean(SHOW_CREATE_NEW_ROUTE_BUTTON, showCreateNewRouteButton);
		super.onSaveInstanceState(outState);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, String fileName,
	                                boolean showCreateNewRouteButton) {
		if (!fragmentManager.isStateSaved()) {
			SavedTrackBottomSheetDialogFragment fragment = new SavedTrackBottomSheetDialogFragment();
			fragment.fileName = fileName;
			fragment.showCreateNewRouteButton = showCreateNewRouteButton;
			fragment.show(fragmentManager, TAG);
		}
	}
}
