package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;

public class WikivoyageWelcomeDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = WikivoyageWelcomeDialogFragment.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View mainView = inflate(R.layout.fragment_wikivoyage_welcome_dialog, container);

		ImageView backBtn = (ImageView) mainView.findViewById(R.id.back_button);
		backBtn.setImageDrawable(getContentIcon(R.drawable.ic_arrow_back));
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		int imgId = nightMode ? R.drawable.img_start_screen_travel_night : R.drawable.img_start_screen_travel_day;
		((ImageView) mainView.findViewById(R.id.main_image)).setImageResource(imgId);

		mainView.findViewById(R.id.continue_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					getMyApplication().getTravelDbHelper().initTravelBooks();
					WikivoyageExploreDialogFragment.showInstance(activity.getSupportFragmentManager());
					dismiss();
				}
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
