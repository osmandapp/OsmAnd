package net.osmand.plus.views;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.NavigationService;
import net.osmand.plus.R;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

public class LockInfoControl {
	
	protected boolean isScreenLocked;
	private View transparentLockView;
	private Drawable lockEnabled;
	private Drawable lockDisabled;
	
	public ImageView createLockScreenWidget(final OsmandMapTileView view) {
		final ImageView lockView = new ImageView(view.getContext());
		lockEnabled = view.getResources().getDrawable(R.drawable.lock_enabled);
		lockDisabled = view.getResources().getDrawable(R.drawable.lock_disabled);
		updateLockIcon(view, lockView);
		lockView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showBgServiceQAction(lockView, view);
			}
		});
		return lockView;
	}

	private void updateLockIcon(final OsmandMapTileView view, final ImageView lockView) {
		if (isScreenLocked) {
			lockView.setBackgroundDrawable(lockEnabled);
		} else {
			lockView.setBackgroundDrawable(lockDisabled);
		}
	}

	private void showBgServiceQAction(final ImageView lockView, final OsmandMapTileView view) {	
		final QuickAction bgAction = new QuickAction(lockView);
		
		if (transparentLockView == null) {
			transparentLockView = new FrameLayout(view.getContext());
			FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT,
					Gravity.CENTER);
			transparentLockView.setLayoutParams(fparams);
		}
		final FrameLayout parent = (FrameLayout) view.getParent();
		final ActionItem lockScreenAction = new ActionItem();
		lockScreenAction.setTitle(view.getResources().getString(
				isScreenLocked ? R.string.bg_service_screen_unlock : R.string.bg_service_screen_lock));
		lockScreenAction.setIcon(view.getResources().getDrawable(isScreenLocked ? R.drawable.lock_enabled : R.drawable.lock_disabled));
		lockScreenAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isScreenLocked) {
					parent.addView(transparentLockView);
					transparentLockView.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								int[] locs = new int[2];
								lockView.getLocationOnScreen(locs);
								int x = (int) event.getX() - locs[0];
								int y = (int) event.getY() - locs[1];
								transparentLockView.getLocationOnScreen(locs);
								x += locs[0];
								y += locs[1];
								if(lockView.getWidth() >= x && x >= 0 && 
										lockView.getHeight() >= y && y >= 0) {
									showBgServiceQAction(lockView, view);
									return true;
								}
								blinkIcon();
								AccessibleToast.makeText(transparentLockView.getContext(), R.string.screen_is_locked, Toast.LENGTH_LONG)
										.show();
								return true;
							}
							return true;
						}

						private void blinkIcon() {
							lockView.setBackgroundDrawable(lockDisabled);
							view.postDelayed(new Runnable() {
								@Override
								public void run() {
									lockView.setBackgroundDrawable(lockEnabled);
								}
							}, 300);
						}
					});
				} else {
					parent.removeView(transparentLockView);
				}
				isScreenLocked = !isScreenLocked;
				bgAction.dismiss();
				updateLockIcon(view, lockView);
			}
		});
		bgAction.addActionItem(lockScreenAction);
		
		final ActionItem bgServiceAction = new ActionItem();
		final boolean off = view.getApplication().getNavigationService() == null;
		bgServiceAction.setTitle(view.getResources().getString(off? R.string.bg_service_sleep_mode_on : R.string.bg_service_sleep_mode_off));
//		bgServiceAction.setIcon(view.getResources().getDrawable(R.drawable.car_small)); //TODO icon
		bgServiceAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceIntent = new Intent(view.getContext(), NavigationService.class);
				if (view.getApplication().getNavigationService() == null) {
					view.getContext().startService(serviceIntent);
				} else {
					view.getContext().stopService(serviceIntent);
				}
				bgAction.dismiss();
			}
		});
		bgAction.addActionItem(bgServiceAction);
		bgAction.show();
		
	}
	/*
	private void createIntervalRadioGrp(final QuickAction mQuickAction, OsmandMapTileView view) {
		final OsmandSettings settings = view.getSettings();
		LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View bgServiceView = inflater.inflate(R.layout.background_service_int, null);
		final FrameLayout parent = (FrameLayout) view.getParent();
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		fparams.setMargins(0, 30, 0, 30);
		bgServiceView.setLayoutParams(fparams);
		bgServiceView.setPadding(20, 5, 20, 5);
		parent.addView(bgServiceView);
		
		RadioGroup intRadioGrp = (RadioGroup) bgServiceView.findViewById(R.id.wake_up_int_grp);
		final int secondsLength = OsmandBackgroundServicePlugin.SECONDS.length;
    	final int minutesLength = OsmandBackgroundServicePlugin.MINUTES.length;
    	final int[] SECONDS = OsmandBackgroundServicePlugin.SECONDS;
    	final int[] MINUTES = OsmandBackgroundServicePlugin.MINUTES;
    	
    	final RadioButton[] intButtons = new RadioButton[minutesLength + secondsLength];
		for (int i = 0; i < secondsLength; i++) {
			intButtons[i] = new RadioButton(view.getContext());
			intButtons[i].setText(SECONDS[i] + " " + view.getContext().getString(R.string.int_seconds));
			intButtons[i].setTextColor(Color.BLACK);
			intButtons[i].setId(SECONDS[i] * 1000);
			intRadioGrp.addView(intButtons[i]);
		}
		for (int i = secondsLength; i < secondsLength + minutesLength; i++) {
			intButtons[i] = new RadioButton(view.getContext());
			intButtons[i].setText(MINUTES[i-secondsLength] + " " + view.getContext().getString(R.string.int_min));
			intButtons[i].setTextColor(Color.BLACK);
			intButtons[i].setId(MINUTES[i-secondsLength] * 60 * 1000);
			intRadioGrp.addView(intButtons[i]);
		}
		
		intRadioGrp.check(settings.SERVICE_OFF_INTERVAL.get());
		intRadioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) { 
            	settings.SERVICE_OFF_INTERVAL.set(checkedId);
            	parent.removeView(bgServiceView);
            }
		});
	} */
}
