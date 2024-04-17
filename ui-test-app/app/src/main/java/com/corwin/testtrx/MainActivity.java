package com.corwin.testtrx;

import androidx.annotation.Dimension;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.apply_btn).setOnClickListener(view -> {
			boolean includeFontPadding = ((CheckBox)findViewById(R.id.include_font_padding)).isChecked();
			TextView txt1 = (TextView) findViewById(R.id.txt1);
			TextView txt2 = (TextView) findViewById(R.id.txt2);
			txt1.setIncludeFontPadding(includeFontPadding);
			txt2.setIncludeFontPadding(includeFontPadding);

			int fontSize = Integer.parseInt(((EditText)findViewById(R.id.font_size)).getText().toString());
			txt1.setTextSize(Dimension.SP, fontSize);
			txt2.setTextSize(Dimension.SP, fontSize);

			int minFontSize = Integer.parseInt(((EditText)findViewById(R.id.font_size_min)).getText().toString());
			int maxFontSize = Integer.parseInt(((EditText)findViewById(R.id.font_size_max)).getText().toString());

			if(maxFontSize <= minFontSize) {
				Toast.makeText(getBaseContext(), "Should be maxFontSize > minFontSize", Toast.LENGTH_LONG).show();
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					txt1.setAutoSizeTextTypeUniformWithConfiguration(minFontSize, maxFontSize, 1, Dimension.SP);
				}
			}

			String value = ((EditText)findViewById(R.id.value)).getText().toString();
			txt1.setText(value);
			txt2.setText(value);

		});



	}
}