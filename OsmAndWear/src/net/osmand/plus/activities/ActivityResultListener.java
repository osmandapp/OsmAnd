package net.osmand.plus.activities;

import android.content.Intent;

public class ActivityResultListener {

	private final int requestCode;
	private final OnActivityResultListener listener;

	public interface OnActivityResultListener {
		void onResult(int resultCode, Intent resultData);
	}

	public ActivityResultListener(int requestCode, OnActivityResultListener listener) {
		this.requestCode = requestCode;
		this.listener = listener;
	}

	public boolean processResult(int requestCode, int resultCode, Intent resultData) {
		if (this.requestCode == requestCode && listener != null) {
			listener.onResult(resultCode, resultData);
			return true;
		}
		return false;
	}
}
