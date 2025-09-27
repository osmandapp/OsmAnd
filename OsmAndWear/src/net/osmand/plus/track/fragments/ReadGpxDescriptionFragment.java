package net.osmand.plus.track.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.fragments.controller.EditGpxDescriptionController;

public class ReadGpxDescriptionFragment extends ReadDescriptionFragment {

	private static final String TITLE_KEY = "title_key";
	private static final String IMAGE_URL_KEY = "image_url_key";

	private EditGpxDescriptionController controller;

	private String mTitle;
	private String mImageUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = new EditGpxDescriptionController((MapActivity) requireActivity());
	}

	@Override
	public void setupWebViewClient(@NonNull View view) {
		controller.setupWebViewController(mWebView, view, this);
	}

	@Override
	public boolean onSaveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity == null || mapActivity.getFragmentsHelper().getTrackMenuFragment() == null) {
			return false;
		}
		controller.saveEditedDescription(editedText, () -> {
			updateContent(editedText);
			callback.onDescriptionSaved();
		});
		return true;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(TITLE_KEY, mTitle);
		outState.putString(IMAGE_URL_KEY, mImageUrl);
	}

	@Override
	protected void readBundle(@NonNull Bundle bundle) {
		super.readBundle(bundle);
		mTitle = bundle.getString(TITLE_KEY);
		mImageUrl = bundle.getString(IMAGE_URL_KEY);
	}

	@NonNull
	@Override
	protected String getTitle() {
		return mTitle;
	}

	@Nullable
	@Override
	protected String getImageUrl() {
		return mImageUrl;
	}

	private void writeBundle(@NonNull Bundle bundle, @NonNull String title,
	                         @Nullable String imageUrl, @NonNull String description) {
		bundle.putString(TITLE_KEY, title);
		bundle.putString(IMAGE_URL_KEY, imageUrl);
		bundle.putString(CONTENT_KEY, description);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String title,
	                                @Nullable String imageUrl,
	                                @NonNull String description,
	                                @NonNull Fragment targetFragment) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!fm.isStateSaved()) {
			ReadGpxDescriptionFragment fragment = new ReadGpxDescriptionFragment();
			Bundle args = new Bundle();
			fragment.writeBundle(args, title, imageUrl, description);
			fragment.setArguments(args);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fm, ReadGpxDescriptionFragment.TAG);
		}
	}
}
