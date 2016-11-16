package net.osmand.core.samples.android.sample1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;

import java.util.HashMap;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;

public class OsmandResources {

	public static final String BIG_ICON_PREFIX = "mx_";

	private static Resources osmandResources;
	private static String packageName;
	private static Map<String, Integer> stringIds = new HashMap<>();
	private static Map<String, Integer> drawableIds = new HashMap<>();
	private static TLongObjectHashMap<Drawable> drawable = new TLongObjectHashMap<>();
	private static TLongObjectHashMap<String> string = new TLongObjectHashMap<>();

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
			OsmandResources.packageName = packageName;
		}
	}

	public static Resources getOsmandResources() {
		return osmandResources;
	}

	public static String getString(String id) {
		return getStringInternal(resolveStringId(id));
	}

	public static String getString(String id, Object... formatArgs) {
		int resolvedId = resolveStringId(id);
		if (resolvedId != 0) {
			return osmandResources.getString(resolvedId, formatArgs);
		}
		return null;
	}

	public static Drawable getDrawable(String id) {
		return getDrawableInternal(resolveDrawableId(id));
	}

	public static Drawable getBigDrawable(String id) {
		return getDrawableInternal(resolveDrawableId(BIG_ICON_PREFIX + id));
	}

	public static Drawable getDrawable(int id) {
		return getDrawableInternal(id);
	}

	public static Drawable getDrawableNonCached(int id) {
		if (osmandResources != null && id != 0) {
			Drawable d = osmandResources.getDrawable(id);
			if (d != null) {
				d = DrawableCompat.wrap(d);
				d.mutate();
			}
			return d;
		}
		return null;
	}

	public static int getDrawableId(String id) {
		return resolveDrawableId(id);
	}

	public static int getBigDrawableId(String id) {
		return resolveDrawableId(BIG_ICON_PREFIX + id);
	}

	private static Drawable getDrawableInternal(int resId) {
		if (osmandResources != null && resId != 0) {
			long hash = getResIdHash(resId);
			Drawable d = drawable.get(hash);
			if (d == null) {
				d = osmandResources.getDrawable(resId);
				if (d != null) {
					d = DrawableCompat.wrap(d);
					d.mutate();
					drawable.put(hash, d);
				}
			}
			return d;
		}
		return null;
	}

	private static String getStringInternal(int resId) {
		if (osmandResources != null && resId != 0) {
			long hash = getResIdHash(resId);
			String s = string.get(hash);
			if (s == null) {
				s = osmandResources.getString(resId);
				string.put(hash, s);
			}
			return s;
		}
		return null;
	}

	private static int resolveStringId(String id) {
		if (osmandResources != null) {
			Integer resolvedId = stringIds.get(id);
			if (resolvedId == null) {
				resolvedId = osmandResources.getIdentifier(id, "string", packageName);
				stringIds.put(id, resolvedId);
			}
			return resolvedId;
		}
		return 0;
	}

	private static int resolveDrawableId(String id) {
		if (osmandResources != null) {
			Integer resolvedId = drawableIds.get(id);
			if (resolvedId == null) {
				resolvedId = osmandResources.getIdentifier(id, "drawable", packageName);
				drawableIds.put(id, resolvedId);
			}
			return resolvedId;
		}
		return 0;
	}

	private static long getResIdHash(int resId) {
		return ((long)resId << 31L);
	}
}