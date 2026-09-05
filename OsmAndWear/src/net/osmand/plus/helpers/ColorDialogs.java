package net.osmand.plus.helpers;

import static net.osmand.plus.utils.AndroidUtils.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class ColorDialogs {
	public static int[] paletteColors = {
			R.string.rendering_value_darkyellow_name,
			R.string.rendering_value_red_name,
			R.string.rendering_value_orange_name,
			R.string.rendering_value_yellow_name,
			R.string.rendering_value_lightgreen_name,
			R.string.rendering_value_green_name,
			R.string.rendering_value_lightblue_name,
			R.string.rendering_value_blue_name,
			R.string.rendering_value_purple_name,
			R.string.rendering_value_pink_name,
			R.string.rendering_value_brown_name,
			R.string.rendering_value_black_name
	};

	public static int[] pallette = {
			0xffeecc22,
			0xffd00d0d,
			0xffff5020,
			0xffeeee10,
			0xff88e030,
			0xff00842b,
			0xff10c0f0,
			0xff1010a0,
			0xffa71de1,
			0xffe044bb,
			0xff8e2512,
			0xff000001
	};

	public static String[] paletteColorTags = {
			"darkyellow",
			"red",
			"orange",
			"yellow",
			"lightgreen",
			"green",
			"lightblue",
			"blue",
			"purple",
			"pink",
			"brown",
			"black"
	};

	private static double getDistanceBetweenColors(int color1, int color2) {
		double distance;

		double r1 = Color.red(color1);
		double g1 = Color.green(color1);
		double b1 = Color.blue(color1);
		double a1 = Color.alpha(color1);

		double r2 = Color.red(color2);
		double g2 = Color.green(color2);
		double b2 = Color.blue(color2);
		double a2 = Color.alpha(color2);

		distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

		if (distance == 0) {
			distance = Math.sqrt(Math.pow(a1 - a2, 2));
		}

		return distance;
	}

	public static int getNearestColor(int source, int[] colors) {
		double distance = Double.MAX_VALUE;

		int index = 0;
		for (int i = 0; i < colors.length; i++) {
			double newDistance = getDistanceBetweenColors(source, colors[i]);
			if (newDistance < distance) {
				index = i;
				distance = newDistance;
			}
		}

		return colors[index];
	}

	public static void setupColorSpinnerEx(Context ctx, int selectedColor, Spinner colorSpinner,
	                                       TIntArrayList colors, OnItemSelectedListener listener) {
		colors.add(pallette);
		List<String> colorNames = new ArrayList<String>();
		int selection = -1;
		for (int i = 0; i < pallette.length; i++) {
			colorNames.add(ctx.getString(paletteColors[i]));
			colors.add(pallette[i]);
			if (selectedColor == pallette[i]) {
				selection = i;
			}
		}
		if (selection == -1) {
			colors.insert(0, selectedColor);
			colorNames.add(0, colorToString(selectedColor));
			selection = 0;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, R.layout.color_spinner_item, colorNames) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if (v instanceof TextView) {
					TextView textView = (TextView) v;
					textView.setCompoundDrawablesWithIntrinsicBounds(getIcon(ctx, R.drawable.ic_action_folder, colors.get(position)), null, null, null);
					textView.setCompoundDrawablePadding(dpToPx(ctx, 15f));
				}
				return v;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if (v instanceof TextView) {
					TextView textView = (TextView) v;
					textView.setCompoundDrawablesWithIntrinsicBounds(getIcon(ctx, R.drawable.ic_action_circle, colors.get(position)), null, null, null);
					textView.setCompoundDrawablePadding(dpToPx(ctx, 15f));
				}
				return v;
			}
		};
		adapter.setDropDownViewResource(R.layout.color_spinner_dropdown_item);
		colorSpinner.setAdapter(adapter);
		colorSpinner.setOnItemSelectedListener(listener);
		colorSpinner.setSelection(selection);
	}

	public static String colorToString(int color) {
		String c = "";
		if ((0xFF000000 & color) == 0xFF000000) {
			c = Integer.toHexString(color & 0x00FFFFFF);
			c = c.length() <= 6 ? "000000".substring(c.length()) + c : c; //$NON-NLS-1$
		} else if ((0x00FFFFFF & color) == color) {
			//issue: if alpha=00 this is wrong
			c = Integer.toHexString(color);
			c = c.length() <= 6 ? "000000".substring(c.length()) + c : c; //$NON-NLS-1$
		} else {
			c = Integer.toHexString(color);
			c = c.length() <= 8 ? "00000000".substring(c.length()) + c : c; //$NON-NLS-1$
		}
		return "#" + c; //$NON-NLS-1$
	}

	private static Drawable getIcon(Context ctx, int resId, int color) {
		Drawable d = AppCompatResources.getDrawable(ctx, resId);
		if (d != null) {
			d = DrawableCompat.wrap(d).mutate();
			d.clearColorFilter();
			d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		return d;
	}
}
