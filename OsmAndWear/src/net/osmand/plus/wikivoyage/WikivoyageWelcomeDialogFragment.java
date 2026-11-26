package net.osmand.plus.wikivoyage;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;

public class WikivoyageWelcomeDialogFragment extends WikiBaseDialogFragment {

	public static final String TAG = WikivoyageWelcomeDialogFragment.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		View mainView = inflate(R.layout.fragment_wikivoyage_welcome_dialog, container);

		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(getContext()));
		ImageView backBtn = mainView.findViewById(R.id.back_button);
		backBtn.setImageDrawable(icBack);
		backBtn.setOnClickListener(v -> dismiss());

		int imgId = nightMode ? R.drawable.img_start_screen_travel_night : R.drawable.img_start_screen_travel_day;
		ImageView mainImage = mainView.findViewById(R.id.main_image);
		mainImage.setScaleType(portrait ? ScaleType.CENTER_CROP : ScaleType.CENTER_INSIDE);
		mainImage.setImageResource(imgId);

		mainView.findViewById(R.id.continue_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				dismiss();
				Intent intent = new Intent(activity, WikivoyageExploreActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(intent);
			}
		});

		return mainView;
	}

	@Override
	protected int getStatusBarColor() {
		return nightMode ? R.color.wikivoyage_welcome_bg_dark : R.color.wikivoyage_welcome_bg_light;
	}

	public static boolean showInstance(FragmentManager fm) {
		try {
			new WikivoyageWelcomeDialogFragment().show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
