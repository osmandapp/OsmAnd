package net.osmand.plus.base;

import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Window;

public class ColoredStatusBarFragment extends Fragment {

	private int statusBarColor = -1;

	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= 21 && getStatusBarColor() != -1) {
			Window window = getActivity().getWindow();
			statusBarColor = window.getStatusBarColor();
			window.setStatusBarColor(ContextCompat.getColor(getActivity(), getStatusBarColor()));
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= 21 && statusBarColor != -1) {
			getActivity().getWindow().setStatusBarColor(statusBarColor);
		}
	}

	@ColorRes
	protected int getStatusBarColor() {
		return -1;
	}
}
