package net.osmand.plus;

import android.app.Activity;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HuaweiDrmHelper {
	private static final String TAG = HuaweiDrmHelper.class.getSimpleName();

	//Copyright protection id
	private static final String DRM_ID = "101117397";
	//Copyright protection public key
	private static final String DRM_PUBLIC_KEY = "9d6f861e7d46be167809a6a62302749a6753b3c1bd02c9729efb3973e268091d";

	public static void check(Activity activity) {
		boolean succeed = false;
		try {
			final WeakReference<Activity> activityRef = new WeakReference<>(activity);
			Class<?> drmCheckCallbackClass = Class.forName("com.huawei.android.sdk.drm.DrmCheckCallback");
			Object callback = java.lang.reflect.Proxy.newProxyInstance(
					drmCheckCallbackClass.getClassLoader(),
					new java.lang.Class[]{drmCheckCallbackClass},
					new java.lang.reflect.InvocationHandler() {

						@Override
						public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
							Activity a = activityRef.get();
							if (a != null && !a.isFinishing()) {
								String method_name = method.getName();
								if (method_name.equals("onCheckSuccess")) {
									// skip now
								} else if (method_name.equals("onCheckFailed")) {
									closeApplication(a);
								}
							}
							return null;
						}
					});

			Class<?> drmClass = Class.forName("com.huawei.android.sdk.drm.Drm");
			Class[] partypes = new Class[]{Activity.class, String.class, String.class, String.class, drmCheckCallbackClass};
			Method check = drmClass.getMethod("check", partypes);
			check.invoke(null, activity, activity.getPackageName(), DRM_ID, DRM_PUBLIC_KEY, callback);
			succeed = true;

		} catch (ClassNotFoundException e) {
			Log.e(TAG, "check: ", e);
		} catch (NoSuchMethodException e) {
			Log.e(TAG, "check: ", e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, "check: ", e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, "check: ", e);
		}
		if (!succeed) {
			closeApplication(activity);
		}
	}

	private static void closeApplication(Activity activity) {
		((OsmandApplication) activity.getApplication()).closeApplicationAnywayImpl(activity, true);
	}
}
