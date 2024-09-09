package net.osmand.plus.quickaction;

import static net.osmand.plus.R.id.imageView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.widget.GridLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.QuickAction.QuickActionSelectionListener;
import net.osmand.plus.quickaction.actions.NewAction;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.ArrayList;
import java.util.List;


public class QuickActionsWidget extends LinearLayout {

	public static final int ACTIONS_PER_PAGE = 6;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final UiUtilities uiUtilities;

	private QuickActionSelectionListener selectionListener;

	private List<QuickAction> actions;
	private QuickActionButton selectedButton;

	private ImageButton next;
	private ImageButton prev;

	private ViewPager viewPager;
	private LinearLayout dots;

	public QuickActionsWidget(Context context) {
		this(context, null);
	}

	public QuickActionsWidget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QuickActionsWidget(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
	}

	public void setSelectedButton(@NonNull QuickActionButton selectedButton) {
		this.selectedButton = selectedButton;
		updateActions();
	}

	public void updateActions() {
		QuickActionButtonState buttonState = selectedButton.getButtonState();
		if (buttonState != null) {
			setActions(new ArrayList<>(buttonState.getQuickActions()));
		}
	}

	public void setActions(@NonNull List<QuickAction> actions) {
		this.actions = actions;
		this.actions.add(new NewAction());

		removeAllViews();
		setupLayout(getContext(), countPage());
	}

	public void setSelectionListener(QuickActionSelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	private void setupLayout(@NonNull Context context, int pageCount) {
		boolean light = settings.isLightContent() && !app.getDaynightHelper().isNightMode();

		inflate(new ContextThemeWrapper(context, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme), R.layout.quick_action_widget, this);

		View container = findViewById(R.id.container);
		container.setBackgroundResource(light ? R.drawable.bg_card_light : R.drawable.bg_card_dark);

		viewPager = findViewById(R.id.viewPager);
		viewPager.setAdapter(new ViewsPagerAdapter());
		viewPager.getLayoutParams().height = actions.size() > ACTIONS_PER_PAGE / 2
				? (int) getResources().getDimension(R.dimen.quick_action_widget_height_big)
				: (int) getResources().getDimension(R.dimen.quick_action_widget_height_small);

		viewPager.requestLayout();
		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateControls(position);
			}
		});
		next = findViewById(R.id.btnNext);
		next.setOnClickListener(view -> {
			if (viewPager.getAdapter().getCount() > viewPager.getCurrentItem() + 1) {
				viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
			}
		});
		prev = findViewById(R.id.btnPrev);
		prev.setOnClickListener(view -> {
			if (viewPager.getCurrentItem() - 1 >= 0) {
				viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
			}
		});
		dots = findViewById(R.id.dots);
		dots.removeAllViews();

		if (pageCount > 1) {
			int color = light ? R.color.icon_color_default_dark : R.color.white_50_transparent;

			LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), !light);
			for (int i = 0; i < pageCount; i++) {
				ImageView dot = (ImageView) inflater.inflate(R.layout.quick_action_widget_dot, dots, false);
				dot.setImageDrawable(i == 0
						? uiUtilities.getIcon(R.drawable.ic_dot_position, R.color.active_color_primary_light)
						: uiUtilities.getIcon(R.drawable.ic_dot_position, color));

				dots.addView(dot);
			}
		}
		LinearLayout controls = findViewById(R.id.controls);
		controls.setVisibility(pageCount > 1 ? VISIBLE : GONE);

		Drawable background = controls.getBackground();
		int backgroundColor = ColorUtilities.getDividerColor(context, !light);
		if (background instanceof ShapeDrawable) {
			((ShapeDrawable) background).getPaint().setColor(backgroundColor);
		} else if (background instanceof GradientDrawable) {
			((GradientDrawable) background).setColor(backgroundColor);
		} else if (background instanceof ColorDrawable) {
			((ColorDrawable) background).setColor(backgroundColor);
		}
		updateControls(viewPager.getCurrentItem());
	}

	private void updateControls(int position) {
		OsmandApplication application = ((OsmandApplication) getContext().getApplicationContext());
		boolean light = application.getSettings().isLightContent() && !application.getDaynightHelper().isNightMode();

		int colorEnabled = light ? R.color.icon_color_default_light : R.color.card_and_list_background_light;
		int colorDisabled = light ? R.color.icon_color_default_dark : R.color.white_50_transparent;

		next.setEnabled(viewPager.getAdapter().getCount() > position + 1);
		next.setImageDrawable(next.isEnabled()
				? uiUtilities.getIcon(R.drawable.ic_arrow_forward, colorEnabled)
				: uiUtilities.getIcon(R.drawable.ic_arrow_forward, colorDisabled));

		prev.setEnabled(position > 0);
		prev.setImageDrawable(prev.isEnabled()
				? uiUtilities.getIcon(R.drawable.ic_arrow_back, colorEnabled)
				: uiUtilities.getIcon(R.drawable.ic_arrow_back, colorDisabled));

		for (int i = 0; i < dots.getChildCount(); i++) {

			((ImageView) dots.getChildAt(i)).setImageDrawable(i == position
					? uiUtilities.getIcon(R.drawable.ic_dot_position, R.color.active_color_primary_light)
					: uiUtilities.getIcon(R.drawable.ic_dot_position, colorDisabled));
		}
	}

	private View createPageView(@NonNull ViewGroup container, int position) {
		boolean light = settings.isLightContent() && !app.getDaynightHelper().isNightMode();
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), !light);

		View page = inflater.inflate(R.layout.quick_action_widget_page, container, false);
		GridLayout gridLayout = page.findViewById(R.id.grid);

		QuickActionButtonState buttonState = selectedButton.getButtonState();
		boolean land = !AndroidUiHelper.isOrientationPortrait((Activity) getContext());
		int maxItems = actions.size() == 1 ? 1 : ACTIONS_PER_PAGE;

		for (int i = 0; i < maxItems; i++) {
			View view = inflater.inflate(R.layout.quick_action_widget_item, gridLayout, false);

			if (i + (position * ACTIONS_PER_PAGE) < actions.size()) {
				QuickAction action = MapButtonsHelper.produceAction(
						actions.get(i + (position * ACTIONS_PER_PAGE)));

				((ImageView) view.findViewById(imageView))
						.setImageResource(action.getIconRes(app));

				((TextView) view.findViewById(R.id.title))
						.setText(action.getActionText(app));

				if (action.isActionWithSlash(app)) {
					((ImageView) view.findViewById(R.id.imageSlash))
							.setImageResource(light
									? R.drawable.ic_action_icon_hide_white
									: R.drawable.ic_action_icon_hide_dark);
				}

				view.setOnClickListener(v -> {
					if (selectionListener != null) {
						selectionListener.onActionSelected(buttonState, action);
					}
				});
//				if (action.isActionEditable()) {
				view.setOnLongClickListener(v -> {
					FragmentActivity activity = (AppCompatActivity) getContext();
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					if (action instanceof NewAction) {
						QuickActionListFragment.showInstance(activity, buttonState);
					} else {
						AddQuickActionController.showCreateEditActionDialog(app, fragmentManager, buttonState, action);
					}
					return true;
				});
//				}
				if (!action.isActionEnable(app)) {
					view.setEnabled(false);
					view.setAlpha(0.5f);
				}
			}
			if (land) {
				view.findViewById(R.id.dividerBot).setVisibility(GONE);
				view.findViewById(R.id.dividerRight).setVisibility(VISIBLE);
			} else {
				view.findViewById(R.id.dividerBot).setVisibility(i < ACTIONS_PER_PAGE / 2 ? VISIBLE : GONE);
				view.findViewById(R.id.dividerRight).setVisibility(((i + 1) % 3) == 0 ? GONE : VISIBLE);
			}

			gridLayout.addView(view);
		}

		return gridLayout;
	}

	private class ViewsPagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return countPage();
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position) {
			View view = createPageView(container, position);
			container.addView(view, 0);

			return view;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
			return view == object;
		}
	}

	private int countPage() {
		return (int) Math.ceil((actions.size()) / (double) 6);
	}

	public void animateWidget(boolean show) {
		AnimatorSet set = new AnimatorSet();
		List<Animator> animators = new ArrayList<>();

		int[] coordinates = AndroidUtils.getCenterViewCoordinates(selectedButton);

		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		float initialValueX = show ? coordinates[0] - centerX : 0;
		float finalValueX = show ? 0 : coordinates[0] - centerX;
		float initialValueY = show ? coordinates[1] - centerY : 0;
		float finalValueY = show ? 0 : coordinates[1] - centerY;

		animators.add(ObjectAnimator.ofFloat(this, View.TRANSLATION_X, initialValueX, finalValueX));
		animators.add(ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, initialValueY, finalValueY));

		float radius = (float) Math.sqrt(Math.pow(getWidth() / 2f, 2) + Math.pow(getHeight() / 2f, 2));
		float finalRadius = show ? radius : 0;
		float initialRadius = show ? 0 : radius;
		animators.add(ViewAnimationUtils.createCircularReveal(this, centerX, centerY, initialRadius, finalRadius));

		float initialValueScale = show ? 0f : 1f;
		float finalValueScale = show ? 1f : 0f;
		animators.add(ObjectAnimator.ofFloat(this, View.SCALE_X, initialValueScale, finalValueScale));
		animators.add(ObjectAnimator.ofFloat(this, View.SCALE_Y, initialValueScale, finalValueScale));

		set.setDuration(300).playTogether(animators);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
				if (show) {
					setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (!show) {
					setVisibility(View.GONE);
					setTranslationX(0);
					setTranslationY(0);
				}
			}
		});
		set.start();
	}
}
