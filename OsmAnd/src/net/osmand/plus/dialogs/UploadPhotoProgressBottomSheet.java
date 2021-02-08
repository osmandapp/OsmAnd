package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.mapcontextmenu.UploadPhotosAsyncTask.UploadPhotosProgressListener;

public class UploadPhotoProgressBottomSheet extends MenuBottomSheetDialogFragment implements UploadPhotosProgressListener {

	public static final String TAG = UploadPhotoProgressBottomSheet.class.getSimpleName();

	private ProgressBar progressBar;
	private TextView uploadedPhotosTitle;
	private TextView uploadedPhotosCounter;

	private OnDismissListener onDismissListener;

	private int progress;
	private int maxProgress;
	private boolean uploadingFinished;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_with_progress_bar, null);

		uploadedPhotosTitle = view.findViewById(R.id.title);
		uploadedPhotosCounter = view.findViewById(R.id.description);
		progressBar = view.findViewById(R.id.progress_bar);
		progressBar.setMax(maxProgress);

		int descriptionId = uploadingFinished ? R.string.uploaded_count : R.string.uploading_count;

		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(descriptionId, progress, maxProgress))
				.setTitle(getString(uploadingFinished ? R.string.upload_photo_completed : R.string.upload_photo))
				.setCustomView(view)
				.create();
		items.add(descriptionItem);

		updateProgress(progress);

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(context, padding));
	}

	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	private void updateProgress(int progress) {
		int descriptionId = uploadingFinished ? R.string.uploaded_count : R.string.uploading_count;
		progressBar.setProgress(progress);
		uploadedPhotosCounter.setText(getString(descriptionId, progress, maxProgress));
		uploadedPhotosTitle.setText(uploadingFinished ? R.string.upload_photo_completed : R.string.upload_photo);
	}

	@Override
	public void uploadPhotosProgressUpdate(int progress) {
		this.progress = progress;
		updateProgress(progress);
	}

	@Override
	public void uploadPhotosFinished() {
		uploadingFinished = true;
		updateProgress(progress);
		UiUtilities.setupDialogButton(nightMode, dismissButton, getDismissButtonType(), getDismissButtonTextId());
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (onDismissListener != null && activity != null && !activity.isChangingConfigurations()) {
			onDismissListener.onDismiss(dialog);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return uploadingFinished ? R.string.shared_string_close : R.string.shared_string_cancel;
	}

	public static UploadPhotosProgressListener showInstance(@NonNull FragmentManager fragmentManager, int maxProgress, OnDismissListener listener) {
		UploadPhotoProgressBottomSheet fragment = new UploadPhotoProgressBottomSheet();
		fragment.setRetainInstance(true);
		fragment.setMaxProgress(maxProgress);
		fragment.setOnDismissListener(listener);
		fragmentManager.beginTransaction()
				.add(fragment, UploadPhotoProgressBottomSheet.TAG)
				.commitAllowingStateLoss();

		return fragment;
	}
}