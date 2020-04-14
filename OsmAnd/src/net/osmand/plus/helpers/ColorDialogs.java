package net.osmand.plus.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gnu.trove.list.array.TIntArrayList;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class ColorDialogs {
	public static int[] paletteColors = new int[] {
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

	public static int[] pallette = new int[] {
			0xb4eecc22,
			0xb4d00d0d,
			0xb4ff5020,
			0xb4eeee10,
			0xb488e030,
			0xb400842b,
			0xb410c0f0,
			0xb41010a0,
			0xb4a71de1,
			0xb4e044bb,
			0xb48e2512,
			0xff000001
	};

	public static String[] paletteColorTags = new String[] {
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

	public static int getColorByTag(String tag) {
		String t = tag.toLowerCase();
		for (int i = 0; i < paletteColorTags.length; i++) {
			String colorTag = paletteColorTags[i];
			if (colorTag.equals(t)) {
				return pallette[i];
			}
		}
		return 0;
	}

	public static void setupColorSpinner(Context ctx, int selectedColor, final Spinner colorSpinner,
			final TIntArrayList colors) {
		 OnItemSelectedListener listener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				View v = parent.getChildAt(0);
				if(v instanceof TextView) {
				   ((TextView) v).setTextColor(colors.get(position));
				}
				colorSpinner.invalidate();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}

		};
		colors.add(pallette);
        List<String> colorNames= new ArrayList<String>();
        int selection = -1;
        for(int i = 0; i < pallette.length; i++) {
        	colorNames.add(ctx.getString(paletteColors[i]));
        	colors.add(pallette[i]);
        	if(selectedColor == pallette[i]) {
        		selection = i;
        	}
        }
        if(selection == -1) {
        	colors.insert(0, selectedColor);
        	colorNames.add(0, ((OsmandApplication)ctx.getApplicationContext()).accessibilityEnabled() ? ctx.getString(R.string.access_default_color) : colorToString(selectedColor));
        	selection = 0;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, colorNames) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View v = super.getView(position, convertView, parent);
        		if(v instanceof TextView) {
 				   ((TextView) v).setTextColor(colors.get(position));
 				}
        		return v;
        	}
        };
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		colorSpinner.setAdapter(adapter);
		colorSpinner.setOnItemSelectedListener(listener);
		colorSpinner.setSelection(selection);
	}

	public static void setupColorSpinnerEx(final Context ctx, int selectedColor, final Spinner colorSpinner,
	                                       final TIntArrayList colors, OnItemSelectedListener listener) {
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

	public static int getRandomColor() {
		return pallette[new Random().nextInt(pallette.length)];
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

	private static Drawable getIcon(final Context ctx, int resId, int color) {
		Drawable d = ctx.getResources().getDrawable(resId).mutate();
		d.clearColorFilter();
		d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		return d;
	}

	public static int getColorName(@ColorRes int color) {
		int colorName = R.string.rendering_value_darkyellow_name;
		for (int i = 0; i < ColorDialogs.pallette.length; i++) {
			if (ColorDialogs.pallette[i] == color) {
				colorName = ColorDialogs.paletteColors[i];
				break;
			}
		}
		return colorName;
	}

	private static int dpToPx(final Context activity, float dp) {
		Resources r = activity.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}
