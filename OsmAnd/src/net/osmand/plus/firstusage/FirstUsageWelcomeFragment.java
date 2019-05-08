package net.osmand.plus.firstusage;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
		ImageView backgroundImage = (ImageView) view.findViewById(R.id.background_image);
		if (Build.VERSION.SDK_INT >= 14) {
			backgroundImage.setImageResource(R.drawable.bg_first_usage);
		} else {
			backgroundImage.setImageDrawable(null);
		}

		AppCompatButton skipButton = (AppCompatButton) view.findViewById(R.id.start_button);
		skipButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FirstUsageWizardFragment.startWizard(getActivity());
			}
		});
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
					.remove(FirstUsageWelcomeFragment.this).commit();
		}
	}
}
