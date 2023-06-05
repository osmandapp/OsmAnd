package net.osmand.plus.charts;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum GPXDataSetType {

	ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
	SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed),
	SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	GPXDataSetType(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	public String getName(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public Drawable getImageDrawable(@NonNull OsmandApplication app) {
		return app.getUIUtilities().getThemedIcon(iconId);
	}

	public static String getName(@NonNull Context ctx, @NonNull GPXDataSetType[] types) {
		List<String> list = new ArrayList<>();
		for (GPXDataSetType type : types) {
			list.add(type.getName(ctx));
		}
		Collections.sort(list);
		StringBuilder builder = new StringBuilder();
		for (String s : list) {
			if (builder.length() > 0) {
				builder.append("/");
			}
			builder.append(s);
		}
		return builder.toString();
	}

	public static Drawable getImageDrawable(@NonNull OsmandApplication app, @NonNull GPXDataSetType[] types) {
		if (types.length > 0) {
			return types[0].getImageDrawable(app);
		} else {
			return null;
		}
	}
}
