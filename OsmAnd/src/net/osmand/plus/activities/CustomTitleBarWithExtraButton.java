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
		return R.layout.titlebar;
	}
	
	@Override
	protected void initImage() {
		//this titlebar has no image, it has an extra button instead!
		ImageView titleImg = (ImageView)getActivity().findViewById(R.id.title_image);
		titleImg.setImageResource(getTitleImageRes());
//		titleImg.setVisibility(View.GONE);
		
		titleImg.setBackgroundResource(R.drawable.tab_back_button_background);
		titleImg.setOnClickListener(listener);
//		
//		Button extraButton = (Button)getActivity().findViewById(R.id.extra_button);
//		extraButton.setVisibility(View.VISIBLE);
//		extraButton.set
	}
}
