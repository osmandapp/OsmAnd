package net.osmand.core.samples.android.sample1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class OAResources {

	private static Resources osmandResources;
	private static String packageName;

	public static void init(Context ctx) {
		String packageName = "net.osmand";
		try {
			osmandResources = ctx.getPackageManager().getResourcesForApplication(packageName);
		} catch (PackageManager.NameNotFoundException e) {
			//ignore
		}
		if (osmandResources == null) {
			packageName = "net.osmand.plus";
			try {
				osmandResources = ctx.getPackageManager().getResourcesForApplication(packageName);
			} catch (PackageManager.NameNotFoundException e) {
				//ignore
			}
		}
		if (osmandResources == null) {
			packageName = "net.osmand.dev";
			try {
				osmandResources = ctx.getPackageManager().getResourcesForApplication(packageName);
			} catch (PackageManager.NameNotFoundException e) {
				//ignore
			}
		}
		if (osmandResources != null) {
			OAResources.packageName = packageName;
		}
	}

	public static Resources getOsmandResources() {
		return osmandResources;
	}

	public static String getString(String id) {
		if (osmandResources != null) {
			int resolvedId = osmandResources.getIdentifier(id, "string", packageName);
			return osmandResources.getString(resolvedId);
		}
		return null;
	}

	public static String getString(String id, Object... formatArgs) {
		if (osmandResources != null) {
			int resolvedId = osmandResources.getIdentifier(id, "string", packageName);
			return osmandResources.getString(resolvedId, formatArgs);
		}
		return null;
	}

	public static Drawable getDrawable(String id) {
		if (osmandResources != null) {
			int resolvedId = osmandResources.getIdentifier(id, "drawable", packageName);
			return osmandResources.getDrawable(resolvedId);
		}
		return null;
	}

	public static Drawable getDrawable(int id) {
		if (osmandResources != null) {
			return osmandResources.getDrawable(id);
		}
		return null;
	}

	public static int getDrawableId(String id) {
		if (osmandResources != null) {
			return osmandResources.getIdentifier(id, "drawable", packageName);
		}
		return 0;
	}
}