package net.osmand.plus.firstusage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class FirstUsageWelcomeFragment extends Fragment {

	public static final String TAG = "FirstUsageWelcomeFragment";
	public static final String SHOW_OSMAND_WELCOME_SCREEN = "show_osmand_welcome_screen";

	public static boolean SHOW = true;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.first_usage_welcome_fragment, container, false);
		ImageView backgroundImage = view.findViewById(R.id.background_image);
		backgroundImage.setImageResource(R.drawable.bg_first_usage);

		AppCompatButton skipButton = view.findViewById(R.id.start_button);
		skipButton.setOnClickListener(v -> FirstUsageWizardFragment.startWizard(getActivity()));

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		((MapActivity)getActivity()).disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		((MapActivity)getActivity()).enableDrawer();
	}

	public void closeWelcomeFragment() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().beginTransaction()
					.remove(FirstUsageWelcomeFragment.this)
					.commitAllowingStateLoss();
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, new FirstUsageWelcomeFragment(), TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}
