package net.osmand.plus.helpers;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.osmand.plus.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class ColorDialogs {
	public static int[] paletteColors = new int[] {
			R.string.color_red,
			R.string.color_pink,
			R.string.color_orange,
			R.string.color_brown,
			R.string.color_yellow,
			R.string.color_lightblue,
			R.string.color_blue,
			R.string.color_green
	};
	
	public static int[] pallette = new int[] {
			0xffd00d0d,
			0xffe044bb,
			0xffff5020,
			0xff8e2512,
			0xffeeee10,
			0xff10c0f0,
			0xff1010a0,
			0xff88e030
	};

	
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
        	colorNames.add(0, colorToString(selectedColor));
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
	
	public static int getRandomColor() {
		return pallette[new Random().nextInt(pallette.length)];
	}
	
	public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}
}
