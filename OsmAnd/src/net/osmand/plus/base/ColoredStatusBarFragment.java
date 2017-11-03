package net.osmand.plus.base;

import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

public class ColoredStatusBarFragment extends Fragment {

	private int statusBarColor = -1;

	@Override
	public void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= 21) {
			statusBarColor = getActivity().getWindow().getStatusBarColor();
		}
		setupStatusBarColor();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= 21 && statusBarColor != -1) {
			getActivity().getWindow().setStatusBarColor(statusBarColor);
		}
	}

	protected void setupStatusBarColor() {
		if (Build.VERSION.SDK_INT >= 21 && getStatusBarColor() != -1) {
			getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), getStatusBarColor()));
		}
	}

	@ColorRes
	protected int getStatusBarColor() {
		return -1;
	}
}
