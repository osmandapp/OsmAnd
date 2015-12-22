package net.osmand.plus;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;

public class FirstUsageFragment extends Fragment {
	public static final String TAG = "FirstUsageFragment";
	public static boolean SHOW = true;
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.first_usage_fragment, container, false);
		ImageView backgroundImage = (ImageView) view.findViewById(R.id.background_image);
		if (Build.VERSION.SDK_INT >= 14) {
			backgroundImage.setImageResource(R.drawable.bg_first_usage);
		} else {
			backgroundImage.setImageDrawable(null);
		}
		final EditText editText = (EditText) view.findViewById(R.id.searchEditText);
		Drawable searchIcon = ((MapActivity) getActivity()).getMyApplication().getIconsCache()
				.getIcon(R.drawable.ic_action_search_dark, true);
		editText.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null);
		Button skipButton = (Button) view.findViewById(R.id.skip_button);
		skipButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().getSupportFragmentManager().beginTransaction()
						.remove(FirstUsageFragment.this).commit();
			}
		});
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getActivity().getSupportFragmentManager().beginTransaction()
							.remove(FirstUsageFragment.this).commit();
					final Intent intent = new Intent(getActivity(), DownloadActivity.class);
					intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
					getActivity().startActivity(intent);
				}
			}
		});
		return view;
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getActivity().getWindow(); // in Activity's onCreate() for instance
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getActivity().getWindow(); // in Activity's onCreate() for instance
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}
}
