/*
 * Copyright 2015 Shell Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * File created: 2015-01-17 10:39:13
 */

package com.software.shell.fab;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * This class represents a <b>Action Button</b>, which is used in 
 * <a href="http://www.google.com.ua/design/spec/components/buttons.html">Material Design</a>
 *
 * @author Vladislav
 * @version 1.0.3
 * @since 1.0.0
 */
public class ActionButton extends View {

	/**
	 * Logging tag
	 */
	private static final String LOG_TAG = "FAB";

	/**
	 * <b>Action Button</b> type
	 */
	private Type type = Type.DEFAULT;

	/**
	 * <b>Action Button</b> state
	 */
	private State state = State.NORMAL;

	/**
	 * <b>Action Button</b> color in {@link State#NORMAL} state 
	 */
	private int buttonColor = Color.LTGRAY;

	/**
	 * <b>Action Button</b> color in {@link State#PRESSED} state 
	 */
	private int buttonColorPressed = Color.DKGRAY;

	/**
	 * Shadow radius expressed in actual pixels
	 */
	private float shadowRadius = MetricsConverter.dpToPx(getContext(), 2.0f);

	/**
	 * Shadow X-axis offset expressed in actual pixels
	 */
	private float shadowXOffset = MetricsConverter.dpToPx(getContext(), 1.0f);

	/**
	 * Shadow Y-axis offset expressed in actual pixels 
	 */
	private float shadowYOffset = MetricsConverter.dpToPx(getContext(), 1.5f);

	/**
	 * Shadow color 
	 */
	private int shadowColor = Color.parseColor("#757575");

	/**
	 * Stroke width 
	 */
	private float strokeWidth = 0.0f;

	/**
	 * Stroke color 
	 */
	private int strokeColor = Color.BLACK;

	/**
	 * <b>Action Button</b> image drawable centered inside the view  
	 */
	private Drawable image;

	/**
	 * Size of the <b>Action Button</b> image inside the view
	 */
	private float imageSize = MetricsConverter.dpToPx(getContext(), 24.0f);

	/**
	 * Animation, which is used while showing <b>Action Button</b>
	 */
	private Animation showAnimation;

	/**
	 * Animation, which is used while hiding or dismissing <b>Action Button</b> 
	 */
	private Animation hideAnimation;

	/**
	 * {@link android.graphics.Paint}, which is used for drawing the elements of
	 * <b>Action Button</b>
	 */
	protected final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

	/**
	 * Creates an instance of the <b>Action Button</b>
	 * <p> 
	 * Used when instantiating <b>Action Button</b> programmatically
	 *  
	 * @param context context the view is running in
	 */
	public ActionButton(Context context) {
		super(context);
		initActionButton();
	}

	/**
	 * Creates an instance of the <b>Action Button</b>
	 * <p> 
	 * Used when inflating the declared <b>Action Button</b> 
	 * within XML resource
	 *
	 * @param context context the view is running in
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	public ActionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initActionButton(context, attrs, 0, 0);
	}

	/**
	 * Creates an instance of the <b>Action Button</b>
	 * <p> 
	 * Used when inflating the declared <b>Action Button</b> 
	 * within XML resource
	 *
	 * @param context context the view is running in
	 * @param attrs attributes of the XML tag that is inflating the view
	 * @param defStyleAttr attribute in the current theme that contains a
	 *        reference to a style resource that supplies default values for
	 *        the view. Can be 0 to not look for defaults
	 */
	public ActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initActionButton(context, attrs, defStyleAttr, 0);
	}

	/**
	 * Creates an instance of the <b>Action Button</b>
	 * <p>
	 * Used when inflating the declared <b>Action Button</b> 
	 * within XML resource
	 * <p>
	 * Might be called if target API is LOLLIPOP (21) and higher
	 *  
	 * @param context context the view is running in
	 * @param attrs attributes of the XML tag that is inflating the view
	 * @param defStyleAttr attribute in the current theme that contains a
	 *        reference to a style resource that supplies default values for
	 *        the view. Can be 0 to not look for defaults
	 * @param defStyleRes resource identifier of a style resource that
	 *        supplies default values for the view, used only if
	 *        defStyleAttr is 0 or can not be found in the theme. Can be 0
	 *        to not look for defaults
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initActionButton(context, attrs, defStyleAttr, defStyleRes);
	}

	/**
	 * Initializes the <b>Action Button</b>, which is created programmatically 
	 */
	private void initActionButton() {
		initLayerType();
		Log.v(LOG_TAG, "Action Button initialized");
	}

	/**
	 * Initializes the <b>Action Button</b>, which is declared within XML resource
	 * <p>
	 * Makes calls to different initialization methods for parameters initialization.
	 * For those parameters, which are not declared in the XML resource, 
	 * the default value will be used 
	 *
	 * @param context context the view is running in
	 * @param attrs attributes of the XML tag that is inflating the view
	 * @param defStyleAttr attribute in the current theme that contains a
	 *        reference to a style resource that supplies default values for
	 *        the view. Can be 0 to not look for defaults
	 * @param defStyleRes resource identifier of a style resource that
	 *        supplies default values for the view, used only if
	 *        defStyleAttr is 0 or can not be found in the theme. Can be 0
	 *        to not look for defaults
	 */
	private void initActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		initLayerType();
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ActionButton,
				defStyleAttr, defStyleRes);
		try {
			initType(attributes);
			initButtonColor(attributes);
			initButtonColorPressed(attributes);
			initShadowRadius(attributes);
			initShadowXOffset(attributes);
			initShadowYOffset(attributes);
			initShadowColor(attributes);
			initStrokeWidth(attributes);
			initStrokeColor(attributes);
			initImage(attributes);
			initImageSize(attributes);
			initShowAnimation(attributes);
			initHideAnimation(attributes);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Unable to read attr", e);
		} finally {
			attributes.recycle();
		}
		Log.v(LOG_TAG, "Action Button initialized");
	}

	/**
	 * Initializes the layer type needed for shadows drawing
	 * <p>
	 * Might be called if target API is HONEYCOMB (11) and higher
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initLayerType() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerType(LAYER_TYPE_SOFTWARE, paint);
			Log.v(LOG_TAG, "Layer type initialized");
		}
	}

	/**
	 * Initializes the {@link Type} of <b>Action Button</b>
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initType(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_type)) {
			final int id = attrs.getInteger(R.styleable.ActionButton_type, type.getId());
			type = Type.forId(id);
			Log.v(LOG_TAG, "Initialized type: " + getType());
		}
	}

	/**
	 * Initializes the <b>Action Button</b> color for 
	 * {@link #state} set to {@link State#NORMAL} 
	 *  
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initButtonColor(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_button_color)) {
			buttonColor = attrs.getColor(R.styleable.ActionButton_button_color, buttonColor);
			Log.v(LOG_TAG, "Initialized button color: " + getButtonColor());
		}
	}

	/**
	 * Initializes the <b>Action Button</b> color for
	 * {@link #state} set to {@link State#PRESSED}
	 * 
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initButtonColorPressed(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_button_colorPressed)) {
			buttonColorPressed = attrs.getColor(R.styleable.ActionButton_button_colorPressed,
					buttonColorPressed);
			Log.v(LOG_TAG, "Initialized button color pressed: " + getButtonColorPressed());
		}
	}

	/**
	 * Initializes the shadow radius
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initShadowRadius(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_shadow_radius)) {
			shadowRadius = attrs.getDimension(R.styleable.ActionButton_shadow_radius, shadowRadius);
			Log.v(LOG_TAG, "Initialized shadow radius: " + getShadowRadius());
		}
	}

	/**
	 * Initializes the shadow X-axis offset
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initShadowXOffset(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_shadow_xOffset)) {
			shadowXOffset = attrs.getDimension(R.styleable.ActionButton_shadow_xOffset, shadowXOffset);
			Log.v(LOG_TAG, "Initialized shadow X-axis offset: " + getShadowXOffset());
		}
	}

	/**
	 * Initializes the shadow Y-axis offset
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initShadowYOffset(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_shadow_yOffset)) {
			shadowYOffset = attrs.getDimension(R.styleable.ActionButton_shadow_yOffset, shadowYOffset);
			Log.v(LOG_TAG, "Initialized shadow Y-axis offset: " + getShadowYOffset());
		}
	}

	/**
	 * Initializes the shadow color
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initShadowColor(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_shadow_color)) {
			shadowColor = attrs.getColor(R.styleable.ActionButton_shadow_color, shadowColor);
			Log.v(LOG_TAG, "Initialized shadow color: " + getShadowColor());
		}
	}

	/**
	 * Initializes the stroke width
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initStrokeWidth(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_stroke_width)) {
			strokeWidth = attrs.getDimension(R.styleable.ActionButton_stroke_width, strokeWidth);
			Log.v(LOG_TAG, "Initialized stroke width: " + getStrokeWidth());
		}
	}

	/**
	 * Initializes the stroke color
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initStrokeColor(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_stroke_color)) {
			strokeColor = attrs.getColor(R.styleable.ActionButton_stroke_color, strokeColor);
			Log.v(LOG_TAG, "Initialized stroke color: " + getStrokeColor());
		}
	}

	/**
	 * Initializes the animation, which is used while showing 
	 * <b>Action Button</b>
	 *  
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initShowAnimation(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_show_animation)) {
			final int animResId = attrs.getResourceId(R.styleable.ActionButton_show_animation,
					Animations.NONE.animResId);
			showAnimation = Animations.load(getContext(), animResId);
			Log.v(LOG_TAG, "Initialized animation on show");
		}
	}

	/**
	 * Initializes the animation, which is used while hiding or dismissing
	 * <b>Action Button</b>
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initHideAnimation(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_hide_animation)) {
			final int animResId = attrs.getResourceId(R.styleable.ActionButton_hide_animation,
					Animations.NONE.animResId);
			hideAnimation = Animations.load(getContext(), animResId);
			Log.v(LOG_TAG, "Initialized animation on hide");
		}
	}

	/**
	 * Initializes the image inside <b>Action Button</b>
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initImage(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_image)) {
			image = attrs.getDrawable(R.styleable.ActionButton_image);
			Log.v(LOG_TAG, "Initialized image");
		}
	}

	/**
	 * Initializes the image size inside <b>Action Button</b>
	 * <p>
	 * Changing the default size of the image breaks the rules of 
	 * <a href="http://www.google.com/design/spec/components/buttons.html">Material Design</a>
	 *
	 * @param attrs attributes of the XML tag that is inflating the view
	 */
	private void initImageSize(TypedArray attrs) {
		if (attrs.hasValue(R.styleable.ActionButton_image_size)) {
			imageSize = attrs.getDimension(R.styleable.ActionButton_image_size, imageSize);
			Log.v(LOG_TAG, "Initialized image size: " + getImageSize());
		}
	}

	/**
	 * Plays the {@link #showAnimation} if set
	 */
	public void playShowAnimation() {
		startAnimation(getShowAnimation());
	}

	/**
	 * Plays the {@link #hideAnimation} if set
	 */
	public void playHideAnimation() {
		startAnimation(getHideAnimation());
	}

	/**
	 * Makes the <b>Action Button</b> to appear and 
	 * sets its visibility to {@link #VISIBLE}
	 * <p>
	 * {@link #showAnimation} is played if set
	 */
	public void show() {
		if (isHidden()) {
			playShowAnimation();
			setVisibility(VISIBLE);
			Log.v(LOG_TAG, "Action Button shown");
		}
	}

	/**
	 * Makes the <b>Action Button</b> to disappear and
	 * sets its visibility to {@link #INVISIBLE}
	 * <p>
	 * {@link #hideAnimation} is played if set
	 */
	public void hide() {
		if (!isHidden() && !isDismissed()) {
			playHideAnimation();
			setVisibility(INVISIBLE);
			Log.v(LOG_TAG, "Action Button hidden");
		}
	}

	/**
	 * Completely dismisses the <b>Action Button</b>,
	 * sets its visibility to {@link #GONE} and removes it from the parent view
	 * <p>
	 * After calling this method any calls to {@link #show()} won't result in showing
	 * the <b>Action Button</b> so far as it is removed from the parent View
	 * <p> 
	 * {@link #hideAnimation} is played if set
	 */
	public void dismiss() {
		if (!isDismissed()) {
			if (!isHidden()) {
				playHideAnimation();
			}
			setVisibility(GONE);
			ViewGroup parent = (ViewGroup) getParent();
			parent.removeView(this);
			Log.v(LOG_TAG, "Action Button dismissed");
		}
	}

	/**
	 * Checks whether <b>Action Button</b> is hidden
	 *  
	 * @return true if <b>Action Button</b> is hidden, otherwise false
	 */
	public boolean isHidden() {
		return getVisibility() == INVISIBLE;
	}

	/**
	 * Checks whether <b>Action Button</b> is dismissed
	 *
	 * @return true if <b>Action Button</b> is dismissed, otherwise false
	 */
	public boolean isDismissed() {
		ViewGroup parent = (ViewGroup) getParent();
		return parent == null;
	}

	/**
	 * Returns the size of the <b>Action Button</b> in actual pixels (px).
	 * Size of the <b>Action Button</b> is the diameter of the main circle
	 *  
	 * @return size of the <b>Action Button</b> in actual pixels (px)
	 */
	public int getButtonSize() {
		final int buttonSize = (int) type.getSize(getContext());
		Log.v(LOG_TAG, "Button size is: " + buttonSize);
		return buttonSize;
	}

	/**
	 * Returns the type of the <b>Action Button</b>
	 *  
	 * @return type of the <b>Action Button</b>
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Sets the type of the <b>Action Button</b> and 
	 * invalidates the layout of the view
	 *
	 * @param type type of the <b>Action Button</b>
	 */
	public void setType(Type type) {
		this.type = type;
		requestLayout();
		Log.v(LOG_TAG, "Type changed to: " + getType());
	}

	/**
	 * Returns the current state of the <b>Action Button</b> 
	 *  
	 * @return current state of the <b>Action Button</b>
	 */
	public State getState() {
		return state;
	}

	/**
	 * Sets the current state of the <b>Action Button</b> and 
	 * invalidates the view
	 *  
	 * @param state new state of the <b>Action Button</b>
	 */
	public void setState(State state) {
		this.state = state;
		invalidate();
		Log.v(LOG_TAG, "State changed to: " + getState());
	}

	/**
	 * Returns the <b>Action Button</b> color when in 
	 * {@link State#NORMAL} state
	 * 
	 * @return <b>Action Button</b> color when in 
	 * {@link State#NORMAL} state 
	 */
	public int getButtonColor() {
		return buttonColor;
	}

	/**
	 * Sets the <b>Action Button</b> color when in 
	 * {@link State#NORMAL} state and invalidates the view
	 *  
	 * @param buttonColor <b>Action Button</b> color 
	 *                    when in {@link State#NORMAL} state
	 */
	public void setButtonColor(int buttonColor) {
		this.buttonColor = buttonColor;
		invalidate();
		Log.v(LOG_TAG, "Color changed to: " + getButtonColor());
	}

	/**
	 * Sets the <b>Action Button</b> color when in
	 * {@link State#PRESSED} state
	 *  
	 * @return <b>Action Button</b> color when in
	 * {@link State#PRESSED} state 
	 */
	public int getButtonColorPressed() {
		return buttonColorPressed;
	}

	/**
	 * Sets the <b>Action Button</b> color when in
	 * {@link State#PRESSED} state and invalidates the view
	 * 
	 * @param buttonColorPressed <b>Action Button</b> color
	 *                           when in {@link State#PRESSED} state                              
	 */
	public void setButtonColorPressed(int buttonColorPressed) {
		this.buttonColorPressed = buttonColorPressed;
		invalidate();
		Log.v(LOG_TAG, "Pressed color changed to: " + getButtonColorPressed());
	}

	/**
	 * Checks whether <b>Action Button</b> has shadow by determining shadow radius
	 * <p>
	 * Shadow is disabled if elevation is set API level is {@code 21 Lollipop} and higher     
	 *  
	 * @return true if <b>Action Button</b> has radius, otherwise false
	 */
	public boolean hasShadow() {
		return !hasElevation() && getShadowRadius() > 0.0f;
	}

	/**
	 * Returns the <b>Action Button</b> shadow radius in actual 
	 * pixels (px)
	 *  
	 * @return <b>Action Button</b> shadow radius in actual pixels (px)
	 */
	public float getShadowRadius() {
		return shadowRadius;
	}

	/**
	 * Sets the <b>Action Button</b> shadow radius and 
	 * invalidates the layout of the view
	 * <p>
	 * Must be specified in density-independent (dp) pixels, which are
	 * then converted into actual pixels (px). If shadow radius is set to 0, 
	 * shadow is removed
	 *
	 * @param shadowRadius shadow radius specified in density-independent 
	 *                     (dp) pixels
	 */
	public void setShadowRadius(float shadowRadius) {
		this.shadowRadius = MetricsConverter.dpToPx(getContext(), shadowRadius);
		requestLayout();
		Log.v(LOG_TAG, "Shadow radius changed to:" + getShadowRadius());
	}

	/**
	 * Removes the <b>Action Button</b> shadow by setting its radius to 0
	 */
	public void removeShadow() {
		if (hasShadow()) {
			setShadowRadius(0.0f);
		}
	}

	/**
	 * Returns the <b>Action Button</b> shadow X-axis offset 
	 * in actual pixels (px)
	 * <p>
	 * If X-axis offset is greater than 0 shadow is shifted right. 
	 * If X-axis offset is lesser than 0 shadow is shifted left.
	 * 0 X-axis offset means that shadow is not X-axis shifted at all
	 *  
	 * @return <b>Action Button</b> shadow X-axis offset 
	 * in actual pixels (px)
	 */
	public float getShadowXOffset() {
		return shadowXOffset;
	}

	/**
	 * Sets the <b>Action Button</b> shadow X-axis offset and 
	 * invalidates the layout of the view
	 * <p>
	 * If X-axis offset is greater than 0 shadow is shifted right. 
	 * If X-axis offset is lesser than 0 shadow is shifted left.
	 * 0 X-axis offset means that shadow is not shifted at all
	 * <p>
	 * Must be specified in density-independent (dp) pixels, which are
	 * then converted into actual pixels (px)
	 *      
	 * @param shadowXOffset shadow X-axis offset specified in density-independent
	 *                      (dp) pixels                         
	 */
	public void setShadowXOffset(float shadowXOffset) {
		this.shadowXOffset = MetricsConverter.dpToPx(getContext(), shadowXOffset);
		requestLayout();
		Log.v(LOG_TAG, "Shadow X offset changed to: " + getShadowXOffset());
	}

	/**
	 * Returns the <b>Action Button</b> shadow Y-axis offset 
	 * in actual pixels (px)
	 * <p>
	 * If Y-axis offset is greater than 0 shadow is shifted down.
	 * If Y-axis offset is lesser than 0 shadow is shifted up.
	 * 0 Y-axis offset means that shadow is not Y-axis shifted at all
	 *  
	 * @return <b>Action Button</b> shadow Y-axis offset 
	 * in actual pixels (px)
	 */
	public float getShadowYOffset() {
		return shadowYOffset;
	}

	/**
	 * Sets the <b>Action Button</b> shadow Y-axis offset and
	 * invalidates the layout of the view
	 * <p>
	 * If Y-axis offset is greater than 0 shadow is shifted down.
	 * If Y-axis offset is lesser than 0 shadow is shifted up.
	 * 0 Y-axis offset means that shadow is not Y-axis shifted at all
	 * <p>
	 * Must be specified in density-independent (dp) pixels, which are
	 * then converted into actual pixels (px)
	 *  
	 * @param shadowYOffset shadow Y-axis offset specified in density-independent
	 *                      (dp) pixels                         
	 */
	public void setShadowYOffset(float shadowYOffset) {
		this.shadowYOffset = MetricsConverter.dpToPx(getContext(), shadowYOffset);
		requestLayout();
		Log.v(LOG_TAG, "Shadow Y offset changed to:" + getShadowYOffset());
	}

	/**
	 * Returns <b>Action Button</b> shadow color
	 *  
	 * @return <b>Action Button</b> shadow color
	 */
	public int getShadowColor() {
		return shadowColor;
	}

	/**
	 * Sets the <b>Action Button</b> shadow color and
	 * invalidates the view
	 *  
	 * @param shadowColor <b>Action Button</b> color
	 */
	public void setShadowColor(int shadowColor) {
		this.shadowColor = shadowColor;
		invalidate();
		Log.v(LOG_TAG, "Shadow color changed to: " + getShadowColor());
	}

	/**
	 * Returns the <b>Action Button</b> stroke width in actual 
	 * pixels (px)
	 *  
	 * @return <b>Action Button</b> stroke width in actual 
	 * pixels (px)
	 */
	public float getStrokeWidth() {
		return strokeWidth;
	}

	/**
	 * Checks whether <b>Action Button</b> has stroke by checking 
	 * stroke width
	 *  
	 * @return true if <b>Action Button</b> has stroke, otherwise false
	 */
	public boolean hasStroke() {
		return getStrokeWidth() > 0.0f;		
	}

	/**
	 * Sets the <b>Action Button</b> stroke width and
	 * invalidates the layout of the view
	 * <p>
	 * Stroke width value must be greater than 0. If stroke width is 
	 * set to 0 stroke is removed     
	 * <p>
	 * Must be specified in density-independent (dp) pixels, which are
	 * then converted into actual pixels (px)
	 *  
	 * @param strokeWidth stroke width specified in density-independent
	 *                    (dp) pixels                       
	 */
	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = MetricsConverter.dpToPx(getContext(), strokeWidth);
		requestLayout();
		Log.v(LOG_TAG, "Stroke width changed to: " + getStrokeWidth());
	}

	/**
	 * Removes the <b>Action Button</b> stroke by setting its width to 0 
	 */
	public void removeStroke() {
		if (hasStroke()) {
			setStrokeWidth(0.0f);
		}
	}

	/**
	 * Returns the <b>Action Button</b> stroke color
	 *
	 * @return <b>Action Button</b> stroke color
	 */
	public int getStrokeColor() {
		return strokeColor;
	}

	/**
	 * Sets the <b>Action Button</b> stroke color and 
	 * invalidates the view
	 *  
	 * @param strokeColor <b>Action Button</b> stroke color
	 */
	public void setStrokeColor(int strokeColor) {
		this.strokeColor = strokeColor;
		invalidate();
		Log.v(LOG_TAG, "Stroke color changed to: " + getStrokeColor());
	}

	/**
	 * Returns the <b>Action Button</b> image drawable centered 
	 * inside the view
	 *  
	 * @return <b>Action Button</b> image drawable centered 
	 * inside the view
	 */
	public Drawable getImage() {
		return image;
	}

	/**
	 * Checks whether <b>Action Button</b> has an image centered 
	 * inside the view
	 *  
	 * @return true if <b>Action Button</b> has an image centered 
	 * inside the view, otherwise false 
	 */
	public boolean hasImage() {
		return getImage() != null;
	}

	/**
	 * Places the image drawable centered inside the view and
	 * invalidates the view 
	 * <p>
	 * Size of the image while drawing is fit to {@link #imageSize}     
	 *     
	 * @param image image drawable, which will be placed centered 
	 *              inside the view                 
	 */
	public void setImageDrawable(Drawable image) {
		this.image = image;
		invalidate();
		Log.v(LOG_TAG, "Image drawable set");
	}

	/**
	 * Resolves the drawable resource id and places the resolved image drawable
	 * centered inside the view
	 * 
	 * @param resId drawable resource id, which is to be resolved to 
	 *              image drawable and used as parameter when calling
	 *              {@link #setImageDrawable(android.graphics.drawable.Drawable)}              
	 */
	public void setImageResource(int resId) {
		setImageDrawable(getResources().getDrawable(resId));
	}

	/**
	 * Creates the {@link android.graphics.drawable.BitmapDrawable} from the given
	 * {@link android.graphics.Bitmap} and places it centered inside the view 
	 *  
	 * @param bitmap bitmap, from which {@link android.graphics.drawable.BitmapDrawable}
	 *               is created and used as parameter when calling
	 *               {@link #setImageDrawable(android.graphics.drawable.Drawable)}               
	 */
	public void setImageBitmap(Bitmap bitmap) {
		setImageDrawable(new BitmapDrawable(getResources(), bitmap));
	}

	/**
	 * Removes the <b>Action Button</b> image by setting its value to null 
	 */
	public void removeImage() {
		if (hasImage()) {
			setImageDrawable(null);
		}
	}

	/**
	 * Returns the <b>Action Button</b> image size in actual pixels (px).
	 * If <b>Action Button</b> image is not set returns 0 
	 *   
	 * @return <b>Action Button</b> image size in actual pixels (px), 
	 * 0 if image is not set
	 */
	public float getImageSize() {
		return getImage() != null ? imageSize : 0.0f;
	}

	/**
	 * Sets the size of the <b>Action Button</b> image
	 * <p>
	 * Changing the default size of the image breaks the rules of 
	 * <a href="http://www.google.com/design/spec/components/buttons.html">Material Design</a>
	 * <p>
	 * Must be specified in density-independent (dp) pixels, which are
	 * then converted into actual pixels (px)     
	 *
	 * @param size size of the <b>Action Button</b> image
	 *             specified in density-independent (dp) pixels                
	 */
	public void setImageSize(float size) {
		this.imageSize = MetricsConverter.dpToPx(getContext(), size);
		Log.v(LOG_TAG, "Image size changed to: " + getImageSize());
	}

	/**
	 * Returns an animation, which is used while showing <b>Action Button</b>
	 *
	 * @return animation, which is used while showing <b>Action Button</b>
	 */
	public Animation getShowAnimation() {
		return showAnimation;
	}

	/**
	 * Sets the animation, which is used while showing <b>Action Button</b>
	 *
	 * @param animation animation, which is to be used while showing 
	 *                  <b>Action Button</b>
	 */
	public void setShowAnimation(Animation animation) {
		this.showAnimation = animation;
		Log.v(LOG_TAG, "Show animation set");
	}

	/**
	 * Sets one of the {@link Animations} as animation, which is used while showing
	 * <b>Action Button</b>
	 *
	 * @param animation one of the {@link Animations}, which is to be used while
	 *                  showing <b>Action Button</b>                     
	 */
	public void setShowAnimation(Animations animation) {
		setShowAnimation(Animations.load(getContext(), animation.animResId));
	}

	/**
	 * Removes the animation, which is used while showing <b>Action Button</b> 
	 */
	public void removeShowAnimation() {
		setShowAnimation(Animations.NONE);
		Log.v(LOG_TAG, "Show animation removed");
	}

	/**
	 * Returns an animation, which is used while hiding <b>Action Button</b>
	 *
	 * @return animation, which is used while hiding <b>Action Button</b>
	 */
	public Animation getHideAnimation() {
		return hideAnimation;
	}

	/**
	 * Sets the animation, which is used while hiding <b>Action Button</b>
	 *
	 * @param animation animation, which is to be used while hiding 
	 *                  <b>Action Button</b>
	 */
	public void setHideAnimation(Animation animation) {
		this.hideAnimation = animation;
		Log.v(LOG_TAG, "Hide animation set");
	}

	/**
	 * Sets one of the {@link Animations} as animation, which is used while hiding
	 * <b>Action Button</b>
	 *
	 * @param animation one of the {@link Animations}, which is to be used while
	 *                  hiding <b>Action Button</b>                     
	 */
	public void setHideAnimation(Animations animation) {
		setHideAnimation(Animations.load(getContext(), animation.animResId));
	}
	
	public void removeHideAnimation() {
		setHideAnimation(Animations.NONE);
		Log.v(LOG_TAG, "Hide animation removed");
	}
	
	/**
	 * Adds additional actions on motion events:
	 * 1. Changes the <b>Action Button</b> {@link #state} to {@link State#PRESSED}
	 *    on {@link android.view.MotionEvent#ACTION_DOWN}
	 * 2. Changes the <b>Action Button</b> {@link #state} to {@link State#NORMAL}
	 *    on {@link android.view.MotionEvent#ACTION_UP}
	 *        
	 * @param event motion event
	 * @return true if event was handled, otherwise false
	 */
	@SuppressWarnings("all")
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		final int action = event.getAction();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				Log.v(LOG_TAG, "Motion event action down detected");
				setState(State.PRESSED);
				return true;
			case MotionEvent.ACTION_UP:
				Log.v(LOG_TAG, "Motion event action up detected");
				setState(State.NORMAL);
				return true;
			default:
				Log.v(LOG_TAG, "Unrecognized motion event detected");
				return false;
		}
	}

	/**
	 * Adds additional checking whether animation is null before starting to play it
	 *  
	 * @param animation animation to play
	 */
	@SuppressWarnings("all")
	@Override
	public void startAnimation(Animation animation) {
		if (animation != null) {
			super.startAnimation(animation);
		}
	}

	/**
	 * Resets the paint to its default values and sets initial flags to it
	 * <p>
	 * Use this method before drawing the new element of the view     
	 */
	protected final void resetPaint() {
		paint.reset();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		Log.v(LOG_TAG, "Paint reset");
	}

	/**
	 * Draws the elements of the <b>Action Button</b>
	 *  
	 * @param canvas canvas, on which the drawing is to be performed
	 */
	@SuppressWarnings("all")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.v(LOG_TAG, "Action Button onDraw called");
		drawCircle(canvas);
		if (hasElevation()) {
			drawElevation();
		}
		if (hasStroke()) {
			drawStroke(canvas);
		}
		if (hasImage()) {
			drawImage(canvas);
		}
	}

	/**
	 * Draws the main circle of the <b>Action Button</b> and calls
	 * {@link #drawShadow()} to draw the shadow if present
	 *  
	 * @param canvas canvas, on which circle is to be drawn
	 */
	protected void drawCircle(Canvas canvas) {
		resetPaint();
		if (hasShadow()) {
			drawShadow();
		}
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(getState() == State.PRESSED ? getButtonColorPressed() : getButtonColor());
		canvas.drawCircle(calculateCenterX(), calculateCenterY(), calculateCircleRadius(), paint);
		Log.v(LOG_TAG, "Circle drawn");
	}

	/**
	 * Calculates the X-axis center coordinate of the entire view
	 *
	 * @return X-axis center coordinate of the entire view
	 */
	protected float calculateCenterX() {
		final float centerX = getMeasuredWidth() / 2;
		Log.v(LOG_TAG, "Calculated center X = " + centerX);
		return centerX;
	}

	/**
	 * Calculates the Y-axis center coordinate of the entire view
	 *
	 * @return Y-axis center coordinate of the entire view
	 */
	protected float calculateCenterY() {
		final float centerY = getMeasuredHeight() / 2;
		Log.v(LOG_TAG, "Calculated center Y = " + centerY);
		return centerY;
	}

	/**
	 * Calculates the radius of the main circle
	 *
	 * @return radius of the main circle
	 */
	protected final float calculateCircleRadius() {
		final float circleRadius = getButtonSize() / 2;
		Log.v(LOG_TAG, "Calculated circle circleRadius = " + circleRadius);
		return circleRadius;
	}	

	/**
	 * Draws the shadow if view elevation is not enabled
	 */
	protected void drawShadow() {
		paint.setShadowLayer(getShadowRadius(), getShadowXOffset(), getShadowYOffset(), getShadowColor());
		Log.v(LOG_TAG, "Shadow drawn");
	}
	
	/**
	 * Draws the elevation around the main circle
	 * <p>
	 * Uses the stroke corrective, which helps to avoid the elevation overlapping issue     
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected void drawElevation() {
		final int strokeWeightCorrective = (int) (getStrokeWidth() / 1.5f);
		final int width = getWidth() - strokeWeightCorrective;
		final int height = getHeight() - strokeWeightCorrective;
		final ViewOutlineProvider outlineProvider = new ActionButtonOutlineProvider(width, height);
		setOutlineProvider(outlineProvider);
		Log.v(LOG_TAG, "Elevation drawn");
	}

	/**
	 * Checks whether view elevation is enabled
	 *  
	 * @return true if view elevation enabled, otherwise false
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private boolean hasElevation() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getElevation() > 0.0f;
	}

	/**
	 * Draws stroke around the main circle
	 *
	 * @param canvas canvas, on which circle is to be drawn
	 */
	protected void drawStroke(Canvas canvas) {
		resetPaint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(getStrokeWidth());
		paint.setColor(getStrokeColor());
		canvas.drawCircle(calculateCenterX(), calculateCenterY(), calculateCircleRadius(), paint);
		Log.v(LOG_TAG, "Stroke drawn");
	}

	/**
	 * Draws the image centered inside the view
	 *
	 * @param canvas canvas, on which circle is to be drawn
	 */
	protected void drawImage(Canvas canvas) {
		final int startPointX = (int) (calculateCenterX() - getImageSize() / 2);
		final int startPointY = (int) (calculateCenterY() - getImageSize() / 2);
		final int endPointX = (int) (startPointX + getImageSize());
		final int endPointY = (int) (startPointY + getImageSize());
		getImage().setBounds(startPointX, startPointY, endPointX, endPointY);
		getImage().draw(canvas);
		Log.v(LOG_TAG, String.format("Image drawn on canvas with coordinates: startPointX = %s, startPointY = %s, " +
				"endPointX = %s, endPointY = %s", startPointX, startPointY, endPointX, endPointY));
	}

	/**
	 * Sets the measured dimension for the entire view
	 *
	 * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
	 *                         The requirements are encoded with
	 *                         {@link android.view.View.MeasureSpec}
	 * @param heightMeasureSpec vertical space requirements as imposed by the parent.
	 *                         The requirements are encoded with
	 *                         {@link android.view.View.MeasureSpec}
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.v(LOG_TAG, "Action Button onMeasure called");
		setMeasuredDimension(calculateMeasuredWidth(), calculateMeasuredHeight());
		Log.v(LOG_TAG, String.format("View size measured with: height = %s, width = %s", getHeight(), getWidth()));
	}

	/**
	 * Calculates the measured width in actual pixels for the entire view
	 *  
	 * @return measured width in actual pixels for the entire view
	 */
	private int calculateMeasuredWidth() {
		final int measuredWidth = getButtonSize() + calculateShadowWidth() + calculateStrokeWeight();
		Log.v(LOG_TAG, "Calculated measured width = " + measuredWidth);
		return measuredWidth;
	}

	/**
	 * Calculates the measured height in actual pixels for the entire view
	 *  
	 * @return measured width in actual pixels for the entire view
	 */
	private int calculateMeasuredHeight() {
		final int measuredHeight = getButtonSize() + calculateShadowHeight() + calculateStrokeWeight();
		Log.v(LOG_TAG, "Calculated measured height = " + measuredHeight);
		return measuredHeight;
	}

	/**
	 * Calculates shadow width in actual pixels
	 *  
	 * @return shadow width in actual pixels
	 */
	private int calculateShadowWidth() {
		final int shadowWidth = hasShadow() ? (int) ((getShadowRadius() + Math.abs(getShadowXOffset())) * 2) : 0;
		Log.v(LOG_TAG, "Calculated shadow width = " + shadowWidth);
		return shadowWidth;
	}

	/**
	 * Calculates shadow height in actual pixels
	 *  
	 * @return shadow height in actual pixels
	 */
	private int calculateShadowHeight() {
		final int shadowHeight = hasShadow() ? (int) ((getShadowRadius() + Math.abs(getShadowYOffset())) * 2) : 0;
		Log.v(LOG_TAG, "Calculated shadow height = " + shadowHeight);
		return shadowHeight;
	}

	/**
	 * Calculates the stroke weight in actual pixels
	 * *
	 * @return stroke weight in actual pixels
	 */
	private int calculateStrokeWeight() {
		final int strokeWeight = (int) (getStrokeWidth() * 2.0f);
		Log.v(LOG_TAG, "Calculated stroke weight is: " + strokeWeight);
		return strokeWeight;
	}

	/**
	 * Determines the <b>Action Button</b> types 
	 */
	public enum Type {

		/**
		 * <b>Action Button</b> default (56dp) type
		 */
		DEFAULT {
			@Override
			int getId() {
				return 0;
			}
		
			@Override
			float getSize(Context context) {
				return MetricsConverter.dpToPx(context, 56.0f);
			}
		},

		/**
		 * <b>Action Button</b> mini (40dp) type 
		 */
		MINI {
			@Override
			int getId() {
				return 1;
			}

			@Override
			float getSize(Context context) {
				return MetricsConverter.dpToPx(context, 40.0f);
			}
		};

		/**
		 * Returns an {@code id} for specific <b>Action Button</b> 
		 * type, which is defined in attributes  
		 *  
		 * @return {@code id} for particular <b>Action Button</b> type,
		 * which is defined in attributes 
		 */
		abstract int getId();

		/**
		 * Returns the size of the specific type of the <b>Action Button</b>
		 *
		 * @param context context the view is running in
		 * @return size of the particular type of the <b>Action Button</b>
		 */
		abstract float getSize(Context context);

		/**
		 * Returns the <b>Action Button</b> type for a specific {@code id}
		 *  
		 * @param id an {@code id}, for which <b>Action Button</b> type required
		 * @return <b>Action Button</b> type
		 */
		static Type forId(int id) {
			for (Type type : values()) {
				if (type.getId() == id) {
					return type;
				}
			}
			return DEFAULT;
		}
		
	}

	/**
	 * Determines the <b>Action Button</b> states 
	 */
	public enum State {

		/**
		 * <b>Action Button</b> normal state  
		 */
		NORMAL,

		/**
		 * <b>Action Button</b> pressed state 
		 */
		PRESSED
		
	}
	
	public enum Animations {

		/**
		 * None. Animation absent 
		 */
		NONE                (0),

		/**
		 * Fade in animation 
		 */
		FADE_IN             (R.anim.fab_fade_in),

		/**
		 * Fade out animation 
		 */
		FADE_OUT            (R.anim.fab_fade_out),

		/**
		 * Scale up animation 
		 */
		SCALE_UP            (R.anim.fab_scale_up),

		/**
		 * Scale down animation 
		 */
		SCALE_DOWN          (R.anim.fab_scale_down),

		/**
		 * Roll from down animation 
		 */
		ROLL_FROM_DOWN      (R.anim.fab_roll_from_down),

		/**
		 * Roll to down animation 
		 */
		ROLL_TO_DOWN        (R.anim.fab_roll_to_down),

		/**
		 * Roll from right animation 
		 */
		ROLL_FROM_RIGHT     (R.anim.fab_roll_from_right),

		/**
		 * Roll to right animation 
		 */
		ROLL_TO_RIGHT       (R.anim.fab_roll_to_right),

		/**
		 * Jump from down animation 
		 */
		JUMP_FROM_DOWN      (R.anim.fab_jump_from_down),

		/**
		 * Jump to down animation 
		 */
		JUMP_TO_DOWN        (R.anim.fab_jump_to_down),

		/**
		 * Jump from right animation 
		 */
		JUMP_FROM_RIGHT     (R.anim.fab_jump_from_right),

		/**
		 * Jump to right animation 
		 */
		JUMP_TO_RIGHT       (R.anim.fab_jump_to_right);

		/**
		 * Correspondent animation resource id 
		 */
		final int animResId;
		
		private Animations(int animResId) {
			this.animResId = animResId;
		}

		/**
		 * Loads an animation from animation resource id
		 *
		 * @param context context the view is running in
		 * @param animResId resource id of the animation, which is to be loaded
		 * @return loaded animation
		 */
		protected static Animation load(Context context, int animResId) {
			return animResId == NONE.animResId ? null : AnimationUtils.loadAnimation(context, animResId);
		}

	}

}
