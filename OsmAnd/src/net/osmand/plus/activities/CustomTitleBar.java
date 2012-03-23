package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomTitleBar {

	private Activity activity;
	private int titleStringRes;
	private int titleImageRes;

	public CustomTitleBar(final Activity activity, int titleStringRes, int titleImageRes) {
		this(activity,titleStringRes,titleImageRes,R.style.CustomTitleTheme);
	}
	
	public CustomTitleBar(final Activity activity, int titleStringRes, int titleImageRes, int style) {
		this.activity = activity;
		this.titleStringRes = titleStringRes;
		this.titleImageRes = titleImageRes;
		
		this.activity.setTheme(style);
		this.activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	}
	
	public void afterSetContentView() {
		activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
		
		Button backButton = (Button) activity.findViewById(R.id.back_button);
		backButton.setContentDescription(activity.getString(R.string.close));
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.finish();
			}
		});
		TextView title = (TextView) activity.findViewById(R.id.title_text);
		title.setText(titleStringRes);
		ImageView titleImg = (ImageView) activity.findViewById(R.id.title_image);
		titleImg.setImageResource(titleImageRes);
	}
	
}
