package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.widgets.TextViewEx;

public class UploadPhotoWithProgressBarBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = UploadPhotoWithProgressBarBottomSheet.class.getSimpleName();

	private ProgressBar progressBar;
	private TextView uploadedPhotosCounter;
	private TextViewEx uploadedPhotosTitle;
	private int maxProgress;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_with_progress_bar, null);

		progressBar = view.findViewById(R.id.progress_bar);
		progressBar.setMax(maxProgress);
		uploadedPhotosCounter = view.findViewById(R.id.description);

		uploadedPhotosTitle = view.findViewById(R.id.title);

		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.uploading_count, progressBar.getProgress(), maxProgress))
				.setTitle(getString(R.string.upload_photo))
				.setCustomView(view)
				.create();
		items.add(descriptionItem);

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(requireContext(), padding));
	}

	public void onProgressUpdate(int progress) {
		progressBar.setProgress(progress);
		uploadedPhotosCounter.setText((getString(R.string.uploading_count, progressBar.getProgress(), maxProgress)));
	}

	public void onUploadingFinished() {
		TextViewEx dismissButtonText = dismissButton.findViewById(R.id.button_text);
		setDismissButtonTextId(R.string.shared_string_close);
		dismissButtonText.setText(R.string.shared_string_close);
		uploadedPhotosTitle.setText(R.string.upload_photo_completed);
		uploadedPhotosCounter.setText((getString(R.string.uploaded_count, progressBar.getProgress(), maxProgress)));
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	public static UploadPhotoWithProgressBarBottomSheet showInstance(@NonNull FragmentManager fragmentManager, int maxProgress) {
		UploadPhotoWithProgressBarBottomSheet fragment = new UploadPhotoWithProgressBarBottomSheet();
		fragment.maxProgress = maxProgress;
		fragment.setRetainInstance(true);
		fragmentManager.beginTransaction().add(fragment, TAG).commitAllowingStateLoss();
		return fragment;
	}
}