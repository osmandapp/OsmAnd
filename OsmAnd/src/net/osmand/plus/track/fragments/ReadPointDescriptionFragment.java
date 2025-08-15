package net.osmand.plus.track.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.fragments.controller.EditPointDescriptionController;
import net.osmand.plus.utils.AndroidUtils;

public class ReadPointDescriptionFragment extends ReadDescriptionFragment {

	private EditPointDescriptionController controller;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = EditPointDescriptionController.getInstance((MapActivity) requireActivity());
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupDependentViews(view);
	}

	@Override
	public boolean onSaveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		controller.saveEditedDescription(editedText, () -> {
			updateContent(editedText);
			callback.onDescriptionSaved();
		});
		return true;
	}

	@NonNull
	@Override
	protected String getTitle() {
		return controller.getTitle();
	}

	@Nullable
	@Override
	protected String getImageUrl() {
		return controller.getImageUrl();
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String description) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ReadPointDescriptionFragment fragment = new ReadPointDescriptionFragment();
			Bundle args = new Bundle();
			args.putString(CONTENT_KEY, description);
			fragment.setArguments(args);
			fragment.show(manager, ReadDescriptionFragment.TAG);
		}
	}

}