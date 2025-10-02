package net.osmand.plus.configmap;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.HashMap;
import java.util.Map;

public class CategoryAnimator {

	private static final Map<ContextMenuItem, ValueAnimator> runningAnimations = new HashMap<>();

	private static final float MAX_ROTATION = 180;

	private final Context ctx;
	private final boolean isExpanding;
	private final ContextMenuItem category;

	private final View header;
	private final ImageView ivIndicator;
	private final TextView tvDescription;
	private final View itemsContainer;
	private final View divider;

	// common parameters
	private float descHeight;
	private float minHeaderHeight;
	private float maxHeaderHeight;
	private int maxListHeight;
	private int maxDuration;


	private CategoryAnimator(@NonNull ContextMenuItem category, @NonNull View view, boolean isExpanding) {
		this.category = category;
		this.ctx = view.getContext();
		this.isExpanding = isExpanding;

		header = view.findViewById(R.id.button_container);
		ivIndicator = view.findViewById(R.id.explicit_indicator);
		tvDescription = view.findViewById(R.id.description);
		itemsContainer = view.findViewById(R.id.items_container);
		divider = view.findViewById(R.id.divider);

		calculateCommonParameters();
	}

	private ValueAnimator startAnimation() {
		onBeforeAnimation();
		// Determine passed distance in current direction
		float currentHeaderHeight = header.getHeight();
		float wholeDistance = maxHeaderHeight - minHeaderHeight;
		float leftDistance = isExpanding ? (currentHeaderHeight - minHeaderHeight) : (maxHeaderHeight - currentHeaderHeight);
		float passedDistance = wholeDistance - leftDistance;

		// Determine restrictions
		int minValue = 0;
		int maxValue = 100;
		int startValue = (int) ((passedDistance / wholeDistance) * maxValue);

		// Determine correct animation duration
		int calculatedDuration = (int) ((float) maxDuration / maxValue * (maxValue - startValue));
		int duration = Math.max(calculatedDuration, 0);

		// Create animation
		ValueAnimator animation = ValueAnimator.ofInt(startValue, maxValue);
		animation.setDuration(duration);
		animation.addUpdateListener(animator -> {
			int val = (Integer) animator.getAnimatedValue();
			onMainAnimationUpdate(val, minValue, maxValue);
			if (val == maxValue) {
				onMainAnimationFinished();
				if (isExpanding) {
					runningAnimations.remove(category);
				} else {
					onShowDescriptionAnimation(minValue, maxValue, duration);
				}
			}
		});
		animation.start();
		return animation;
	}

	private void calculateCommonParameters() {
		descHeight = getDimen(R.dimen.default_desc_line_height);
		int titleHeight = getDimen(R.dimen.default_title_line_height);
		int verticalMargin = getDimen(R.dimen.content_padding_small) * 2;
		minHeaderHeight = titleHeight + verticalMargin - AndroidUtils.dpToPx(ctx, 3);
		maxHeaderHeight = minHeaderHeight + descHeight;

		itemsContainer.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		maxListHeight = itemsContainer.getMeasuredHeight();

		maxDuration = getInteger(isExpanding ? android.R.integer.config_mediumAnimTime : android.R.integer.config_shortAnimTime);
	}

	private void onBeforeAnimation() {
		// Make all views visible before animate them
		tvDescription.setVisibility(View.VISIBLE);
		itemsContainer.setVisibility(View.VISIBLE);
		divider.setVisibility(View.VISIBLE);

		// Description will be invisible until collapsing animation finished
		boolean hasDescription = category.getDescription() != null;
		tvDescription.setVisibility(hasDescription ? View.INVISIBLE : View.GONE);
	}

	private void onMainAnimationUpdate(int val, float minValue, float maxValue) {
		// Set indicator rotation
		float rotation = MAX_ROTATION / maxValue * val;
		ivIndicator.setRotation(isExpanding ? -rotation : rotation);

		// Set header height
		float headerIncrement = descHeight / maxValue * val;
		float headerHeight = isExpanding ? maxHeaderHeight - headerIncrement : minHeaderHeight + headerIncrement;
		ViewGroup.LayoutParams layoutParams = header.getLayoutParams();
		layoutParams.height = (int) headerHeight;
		header.setLayoutParams(layoutParams);

		// Set list alpha
		float listAlpha = ColorUtilities.getProportionalAlpha(minValue, maxValue, val);
		float listInverseAlpha = 1.0f - listAlpha;
		itemsContainer.setAlpha(isExpanding ? listInverseAlpha : listAlpha);

		// Set list height
		float increment = (float) maxListHeight / maxValue * val;
		float height = isExpanding ? increment : maxListHeight - increment;
		ViewGroup.LayoutParams layoutParams1 = itemsContainer.getLayoutParams();
		layoutParams1.height = (int) height;
		itemsContainer.setLayoutParams(layoutParams1);
	}

	private void onMainAnimationFinished() {
		// Set indicator icon and reset its rotation
		int indicatorRes = isExpanding ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
		ivIndicator.setRotation(0);
		ivIndicator.setImageResource(indicatorRes);

		boolean hasDescription = category.getDescription() != null;
		// Update views visibility
		AndroidUiHelper.updateVisibility(divider, isExpanding);
		AndroidUiHelper.updateVisibility(tvDescription, !isExpanding && hasDescription);
		AndroidUiHelper.updateVisibility(itemsContainer, isExpanding);

		// Set items container height as WRAP_CONTENT
		LayoutParams params = itemsContainer.getLayoutParams();
		params.height = LayoutParams.WRAP_CONTENT;
		itemsContainer.setLayoutParams(params);
	}

	private void onShowDescriptionAnimation(int minValue, int maxValue, int duration) {
		ValueAnimator animator = ValueAnimator.ofInt(minValue, maxValue);
		animator.setDuration(duration);
		animator.addUpdateListener(animation -> {
			int val = (Integer) animator.getAnimatedValue();
			float alpha = ColorUtilities.getProportionalAlpha(minValue, maxValue, val);
			tvDescription.setAlpha(1.0f - alpha);
			if (maxValue == val) {
				runningAnimations.remove(category);
			}
		});
		animator.start();
	}

	private int getDimen(int id) {
		return ctx.getResources().getDimensionPixelSize(id);
	}

	private int getInteger(int id) {
		return ctx.getResources().getInteger(id);
	}


	public static void startCollapsing(@NonNull ContextMenuItem category, @NonNull View view) {
		tryStartAnimation(category, view, false);
	}

	public static void startExpanding(@NonNull ContextMenuItem category, @NonNull View view) {
		tryStartAnimation(category, view, true);
	}

	private static void tryStartAnimation(@NonNull ContextMenuItem category, @NonNull View view, boolean isExpanding) {
		CategoryAnimator animator = new CategoryAnimator(category, view, isExpanding);
		// Stop previous animation for the category
		ValueAnimator runningAnimation = runningAnimations.remove(category);
		if (runningAnimation != null && runningAnimation.isRunning()) {
			runningAnimation.end();
		}
		// Create and run a new animation
		runningAnimations.put(category, animator.startAnimation());
	}

}
