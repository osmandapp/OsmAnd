package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.app.Activity;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class CustomTitleBarWithExtraButton extends CustomTitleBar {

	private final OnClickListener listener;

	public CustomTitleBarWithExtraButton(Activity activity, int titleStringRes,
			int titleImageRes, OnClickListener listener) {
		super(activity, titleStringRes, titleImageRes);
		this.listener = listener;
	}

	@Override
	protected int getTitleBarLayout() {
		return R.layout.titlebar_extrabutton;
	}
	
	@Override
	protected void initImage() {
		//this titlebar has no image, it has an extra button instead!
		ImageView titleImg = (ImageView)getActivity().findViewById(R.id.title_image);
		titleImg.setImageResource(getTitleImageRes());
//		titleImg.setVisibility(View.GONE);
		
//		titleImg.setPadding(0, 0, 2, 0);
//		titleImg.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
//		titleImg.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//		titleImg.setBackgroundResource(R.drawable.tab_back_button_background);
		titleImg.setOnClickListener(listener);
//		titleImg.invalidate();
//		
//		Button extraButton = (Button)getActivity().findViewById(R.id.extra_button);
//		extraButton.setVisibility(View.VISIBLE);
//		extraButton.set
	}
}
