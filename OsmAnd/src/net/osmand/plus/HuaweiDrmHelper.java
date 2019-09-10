package net.osmand.plus;

import android.app.Activity;
import android.widget.Toast;

import com.huawei.android.sdk.drm.Drm;
import com.huawei.android.sdk.drm.DrmCheckCallback;

public class HuaweiDrmHelper {
	private Activity activity;
	private DrmCheckCallback callback;
	
	//Copyright protection id
	private static final String DRM_ID = "101048021";
	//Copyright protection public key
	private static final String DRM_PUBLIC_KEY = "e0a6c798fddfd0927bd509dfeafcef4b61c4408d7ea0ca9dfb4b7766b964f801";
	
	public HuaweiDrmHelper(final Activity activity) {
		this.activity = activity;
		callback = new DrmCheckCallback() {
			@Override
			public void onCheckSuccess() { }

			@Override
			public void onCheckFailed() {
				activity.finish();
			}
		};
	}
	
	public void check() {
		Drm.check(getActivity(), getActivity().getPackageName(), DRM_ID, DRM_PUBLIC_KEY, callback);
	}
	
	public Activity getActivity() {
		return this.activity;
	}
}
