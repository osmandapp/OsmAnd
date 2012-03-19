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
	
	protected int getTitleBarLayout() {
		return R.layout.titlebar;
	}
	
	public void afterSetContentView() {
		activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, getTitleBarLayout());
		
		initBackButton();
		initText();
		initImage();
	}

	protected void initImage() {
		ImageView titleImg = (ImageView) activity.findViewById(R.id.title_image);
		titleImg.setImageResource(titleImageRes);
	}

	protected void initText() {
		TextView title = (TextView) activity.findViewById(R.id.title_text);
		title.setText(titleStringRes);
	}

	protected void initBackButton() {
		Button backButton = (Button) activity.findViewById(R.id.back_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.finish();
			}
		});
	}
	
	protected Activity getActivity() {
		return activity;
	}
	
	protected int getTitleImageRes() {
		return titleImageRes;
	}
}
