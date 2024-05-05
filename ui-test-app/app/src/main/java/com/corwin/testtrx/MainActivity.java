package com.corwin.testtrx;

import androidx.annotation.Dimension;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	Spinner spinner1;
	Spinner spinner2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		spinner1 = (Spinner) findViewById(R.id.gravity1);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
				this,
				R.array.gravity,
				R.layout.simple_spinner_item
		);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(adapter1);
		spinner1.setSelection(1);

		spinner2 = (Spinner) findViewById(R.id.gravity2);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
				this,
				R.array.gravity,
				R.layout.simple_spinner_item
		);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(adapter2);
		spinner2.setSelection(1);

		apply();
		findViewById(R.id.apply_btn).setOnClickListener(view -> {
			apply();

		});


	}

	private void apply() {
		boolean includeFontPadding = ((CheckBox) findViewById(R.id.include_font_padding)).isChecked();
		TextView txt1 = (TextView) findViewById(R.id.txt1);
		TextView txt2 = (TextView) findViewById(R.id.txt2);
		txt1.setIncludeFontPadding(includeFontPadding);
		txt2.setIncludeFontPadding(includeFontPadding);

		int fontSize = Integer.parseInt(((EditText) findViewById(R.id.font_size)).getText().toString());
		txt1.setTextSize(Dimension.SP, fontSize);
		txt2.setTextSize(Dimension.SP, fontSize);

		int minFontSize = Integer.parseInt(((EditText) findViewById(R.id.font_size_min)).getText().toString());
		int maxFontSize = Integer.parseInt(((EditText) findViewById(R.id.font_size_max)).getText().toString());

		if (maxFontSize <= minFontSize) {
			Toast.makeText(getBaseContext(), "Should be maxFontSize > minFontSize", Toast.LENGTH_LONG).show();
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				txt1.setAutoSizeTextTypeUniformWithConfiguration(minFontSize, maxFontSize, 1, Dimension.SP);
			}
		}

		String value = ((EditText) findViewById(R.id.value)).getText().toString();
		txt1.setText(value);
		txt2.setText(value);

		txt1.setGravity(getGravity(spinner1));
		txt2.setGravity(getGravity(spinner2));


		int height1 = Integer.parseInt(((EditText) findViewById(R.id.height1)).getText().toString());
		int height2 = Integer.parseInt(((EditText) findViewById(R.id.height2)).getText().toString());

		final float scale = this.getResources().getDisplayMetrics().density;

		ViewGroup.LayoutParams lp1 = txt1.getLayoutParams();
		lp1.height = (int) (height1 * scale + 0.5f);
		txt1.setLayoutParams(lp1);
		txt1.requestLayout();

		ViewGroup.LayoutParams lp2 = txt2.getLayoutParams();
		lp2.height = (int) (height2 * scale + 0.5f);
		txt2.setLayoutParams(lp2);
		txt2.requestLayout();
	}

	private int getGravity(Spinner spinner){
		String gravity = (String) spinner.getSelectedItem();
		if(gravity.equals("top")) {
			return Gravity.TOP;
		} else if(gravity.equals("center")) {
			return Gravity.CENTER_VERTICAL;
		} if(gravity.equals("bottom")) {
			return Gravity.BOTTOM;
		}
		return 0;
	}

}