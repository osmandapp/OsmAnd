package net.osmand.plus;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;

public class BusyIndicator {
	
	private View bar;
	private Handler uiHandler;
	private int status;
	private final Context ctx;
	
	public static final int STATUS_INVISIBLE = 0;
	public static final int STATUS_GREEN = 1;
	public static final int STATUS_BLUE = 2;
	public static final int STATUS_BLACK = 3;
	
	public BusyIndicator(Context ctx, View bar){
		this.ctx = ctx;
		this.bar = bar;
		bar.setVisibility(View.INVISIBLE);
		uiHandler = new Handler();
	}
	
	public boolean isVisible(){
		return status != 0;
	}
	
	public int getStatus(){
		return status;
	}

	/**
	 * @param status - 0 invisible
	 * 1 
	 */
	public void updateStatus(int status){
		if(this.status != status){
			this.status = status;
			final Drawable drawable;
			if(this.status == STATUS_BLACK){
				drawable =  ctx.getResources().getDrawable(R.drawable.progress_grey);
			} else if(this.status == STATUS_BLUE){
				drawable =  ctx.getResources().getDrawable(R.drawable.progress_blue);
			} else if(this.status == STATUS_GREEN){
				drawable =  ctx.getResources().getDrawable(R.drawable.progress_green);
			} else {
				drawable = null;
			}
			final RotateAnimation animation; 
			if(drawable != null){
				animation = new RotateAnimation(0, 360, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
				animation.setRepeatCount(Animation.INFINITE);
				final int cycles = 12;
				animation.setInterpolator(new Interpolator(){
					@Override
					public float getInterpolation(float input) {
						return ((int)(input * cycles)) / (float) cycles;
					}
				});
				animation.setDuration(1200);
				animation.setStartTime(RotateAnimation.START_ON_FIRST_FRAME);
				animation.setStartOffset(0);
			} else {
				animation = null;
			}
			uiHandler.post(new Runnable(){
				@Override
				public void run() {
					bar.setVisibility(drawable != null ? View.VISIBLE : View.INVISIBLE);
					if(bar.getAnimation() != null){
						bar.clearAnimation();
					}
					if(drawable != null){
						bar.setBackgroundDrawable(drawable);
						bar.startAnimation(animation);
					}
				}
			});
		}
	}
	
	

}
