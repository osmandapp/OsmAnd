package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomTitleBar {

	private Activity activity;
	private CustomTitleBarView vidw;

	public CustomTitleBar(final Activity activity, int titleStringRes, int titleImageRes) {
		this(activity,titleStringRes,titleImageRes,null);
	}
	
	
	public CustomTitleBar(final Activity activity, int titleStringRes, int titleImageRes, OnClickListener click) {
		this.activity = activity;
		int style = R.style.CustomTitleTheme;
		this.activity.setTheme(style);
		this.activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		vidw = new CustomTitleBarView(activity.getString(titleStringRes), titleImageRes, click) {
			@Override
			public void backPressed() {
				activity.finish();
			}
		};
	}
	
	public CustomTitleBarView getView(){
		return vidw;
	}
	
	public FontFitTextView getTitleView(){
		return getView().getTextView();
	}
	
	public void afterSetContentView(){
		activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, vidw.getTitleBarLayout());
		vidw.init(activity.getWindow());
	}
	
	public static class CustomTitleBarView {
		protected String titleString;
		protected int titleImageRes;
		protected OnClickListener click;
		private FontFitTextView title;
		public CustomTitleBarView(String text, int img, OnClickListener cl) {
			this.titleString = text;
			this.titleImageRes = img;
			this.click = cl;
		}
		
		protected int getTitleBarLayout() {
			return click == null  ? R.layout.titlebar : R.layout.titlebar_extrabutton;
		}
		
		protected int getTitleImageRes() {
			return titleImageRes;
		}
		
		public String getTitleString() {
			return titleString;
		}
		
		public void init(Window wnd) {
			init(wnd.getDecorView());
		}
		
		public FontFitTextView getTextView(){
			return title;
		}

		public void init(View wnd) {
			title = (FontFitTextView) wnd.findViewById(R.id.title_text);
			title.setText(titleString);
			Button backButton = (Button) wnd.findViewById(R.id.back_button);
			backButton.setContentDescription(wnd.getContext().getString(R.string.close));
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					backPressed();
				}
			});
			ImageView titleImg = (ImageView) wnd.findViewById(R.id.title_image);
			titleImg.setImageResource(titleImageRes);
			if(click != null) {
				titleImg.setOnClickListener(click);
			}
		}
		
		public void backPressed(){
		}
	}
}
