package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

public class OsmandTextViewFieldBoxes extends FrameLayout {
	public int DEFAULT_ERROR_COLOR;
	public int DEFAULT_PRIMARY_COLOR;
	public int DEFAULT_TEXT_COLOR;
	public int DEFAULT_DISABLED_TEXT_COLOR;
	public int DEFAULT_BG_COLOR;
	protected boolean enabled;
	protected String labelText;
	protected String helperText;
	protected int maxCharacters;
	protected int minCharacters;
	protected int helperTextColor;
	protected int errorColor;
	protected int primaryColor;
	protected int panelBackgroundColor;
	protected int iconSignifierResourceId;
	protected int endIconResourceId;
	protected boolean isResponsiveIconColor;
	protected boolean hasClearButton;
	protected boolean hasFocus;
	protected View panel;
	protected View bottomLine;
	protected ViewGroup editTextLayout;
	protected AppCompatTextView inputText;
	protected AppCompatTextView helperLabel;
	protected AppCompatTextView counterLabel;
	protected AppCompatTextView floatingLabel;
	protected AppCompatImageButton clearButton;
	protected AppCompatImageButton iconImageButton;
	protected AppCompatImageButton endIconImageButton;
	protected InputMethodManager inputMethodManager;
	protected RelativeLayout rightShell;
	protected RelativeLayout upperPanel;
	protected RelativeLayout bottomPart;
	protected RelativeLayout inputLayout;
	protected int labelColor = -1;
	protected int labelTopMargin = -1;
	protected int ANIMATION_DURATION = 100;
	protected boolean onError = false;
	protected boolean activated = false;
	protected boolean doNotRemoveError = false;
	private boolean useOsmandKeyboard;

	public OsmandTextViewFieldBoxes(Context context) {
		super(context);
		this.init();
	}

	public OsmandTextViewFieldBoxes(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.init();
		this.handleAttributes(context, attrs);
	}

	public OsmandTextViewFieldBoxes(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.init();
		this.handleAttributes(context, attrs);
	}

	public void setUseOsmandKeyboard(boolean useOsmandKeyboard) {
		this.useOsmandKeyboard = useOsmandKeyboard;
	}

	protected void init() {
		this.initDefaultColor();
		this.inputMethodManager = (InputMethodManager)this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	protected void initDefaultColor() {
		Resources.Theme theme = this.getContext().getTheme();
		this.DEFAULT_ERROR_COLOR = ContextCompat.getColor(this.getContext(), studio.carbonylgroup.textfieldboxes.R.color.A400red);
		TypedArray themeArray = theme.obtainStyledAttributes(new int[]{16842800});
		this.DEFAULT_BG_COLOR = adjustAlpha(themeArray.getColor(0, 0), 0.06F);
		themeArray = theme.obtainStyledAttributes(new int[]{studio.carbonylgroup.textfieldboxes.R.attr.colorPrimary});
		if(isLight(this.DEFAULT_BG_COLOR)) {
			this.DEFAULT_PRIMARY_COLOR = lighter(themeArray.getColor(0, 0), 0.2F);
		} else {
			this.DEFAULT_PRIMARY_COLOR = themeArray.getColor(0, 0);
		}

		themeArray = theme.obtainStyledAttributes(new int[]{16843282});
		this.DEFAULT_TEXT_COLOR = themeArray.getColor(0, 0);
		themeArray = theme.obtainStyledAttributes(new int[]{16842803});
		float disabledAlpha = themeArray.getFloat(0, 0.0F);
		themeArray = theme.obtainStyledAttributes(new int[]{16843282});
		this.DEFAULT_DISABLED_TEXT_COLOR = adjustAlpha(themeArray.getColor(0, 0), disabledAlpha);
		themeArray.recycle();
	}

	protected AppCompatTextView findTextViewChild() {
		return this.getChildCount() > 0 && this.getChildAt(0) instanceof AppCompatTextView?(AppCompatTextView) this.getChildAt(0):null;
	}

	protected void onFinishInflate() {
		super.onFinishInflate();
		this.inputText = this.findTextViewChild();
		if(this.inputText != null) {
			this.addView(LayoutInflater.from(this.getContext()).inflate(studio.carbonylgroup.textfieldboxes.R.layout.text_field_boxes_layout, this, false));
			this.removeView(this.inputText);
			this.inputText.setBackgroundColor(0);
			this.inputLayout = (RelativeLayout)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_input_layout);
			this.inputLayout.addView(this.inputText);
			this.inputLayout.setAlpha(0.0F);
			this.panel = this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_panel);
			this.floatingLabel = (AppCompatTextView)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_label);
			this.floatingLabel.setPivotX(0.0F);
			this.floatingLabel.setPivotY(0.0F);
			this.bottomLine = this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.bg_bottom_line);
			this.rightShell = (RelativeLayout)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_right_shell);
			this.upperPanel = (RelativeLayout)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_upper_panel);
			this.bottomPart = (RelativeLayout)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_bottom);
			this.labelColor = this.floatingLabel.getCurrentTextColor();
			this.clearButton = (AppCompatImageButton)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_clear_button);
			this.clearButton.setColorFilter(this.DEFAULT_TEXT_COLOR);
			this.clearButton.setAlpha(0.35F);
			this.endIconImageButton = (AppCompatImageButton)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_end_icon_button);
			this.endIconImageButton.setColorFilter(this.DEFAULT_TEXT_COLOR);
			this.endIconImageButton.setAlpha(0.54F);
			this.helperLabel = (AppCompatTextView)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_helper);
			this.counterLabel = (AppCompatTextView)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_counter);
			this.iconImageButton = (AppCompatImageButton)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_imageView);
			this.editTextLayout = (ViewGroup)this.findViewById(studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_editTextLayout);
			this.labelTopMargin = ((RelativeLayout.LayoutParams)RelativeLayout.LayoutParams.class.cast(this.floatingLabel.getLayoutParams())).topMargin;
			this.panel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					select();
				}
			});
			this.iconImageButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					select();
				}
			});
			this.inputText.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean b) {
					if(b) {
						setHasFocus(true);
					} else {
						setHasFocus(false);
					}

				}
			});
			this.inputText.addTextChangedListener(new TextWatcher() {
				public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				}

				public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				}

				public void afterTextChanged(Editable editable) {
					if(!doNotRemoveError) {
						removeError();
						updateCounterText();
					}

				}
			});
			this.clearButton.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					inputText.setText("");
				}
			});
			if(!this.inputText.getText().toString().isEmpty() || this.hasFocus) {
				this.activate(false);
			}

			this.setLabelText(this.labelText);
			this.setHelperText(this.helperText);
			this.setHelperTextColor(this.helperTextColor);
			this.setErrorColor(this.errorColor);
			this.setPrimaryColor(this.primaryColor);
			this.setPanelBackgroundColor(this.panelBackgroundColor);
			this.setMaxCharacters(this.maxCharacters);
			this.setMinCharacters(this.minCharacters);
			this.setEnabled(this.enabled);
			this.setIconSignifier(this.iconSignifierResourceId);
			this.setEndIcon(this.endIconResourceId);
			this.setIsResponsiveIconColor(this.isResponsiveIconColor);
			this.setHasClearButton(this.hasClearButton);
			this.setHasFocus(this.hasFocus);
			this.updateCounterText();
			this.updateBottomViewVisibility();
		}
	}

	public void select() {
		if(!isActivated()) {
			activate(true);
		}

		setHasFocus(true);
		if (!useOsmandKeyboard) {
			inputMethodManager.showSoftInput(inputText, 1);
		}
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if(widthMode == 1073741824) {
			this.inputLayout.getLayoutParams().width = -1;
			this.upperPanel.getLayoutParams().width = -1;
			this.editTextLayout.getLayoutParams().width = -1;
			if(this.endIconImageButton.getVisibility() == VISIBLE) {
				((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(1, 0);
				((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(0, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_end_icon_button);
				if(Build.VERSION.SDK_INT >= 17) {
					((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(17, 0);
					((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(16, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_end_icon_button);
				}

				((RelativeLayout.LayoutParams)this.endIconImageButton.getLayoutParams()).addRule(1, 0);
				((RelativeLayout.LayoutParams)this.endIconImageButton.getLayoutParams()).addRule(11);
				if(Build.VERSION.SDK_INT >= 17) {
					((RelativeLayout.LayoutParams)this.endIconImageButton.getLayoutParams()).addRule(17, 0);
					((RelativeLayout.LayoutParams)this.endIconImageButton.getLayoutParams()).addRule(21);
				}

				if(this.hasClearButton) {
					((RelativeLayout.LayoutParams)this.inputLayout.getLayoutParams()).addRule(0, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_clear_button);
				} else {
					((RelativeLayout.LayoutParams)this.inputLayout.getLayoutParams()).addRule(0, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_end_icon_button);
				}
			} else {
				((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(1, 0);
				((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(11);
				if(Build.VERSION.SDK_INT >= 17) {
					((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(17, 0);
					((RelativeLayout.LayoutParams)this.clearButton.getLayoutParams()).addRule(21);
				}

				((RelativeLayout.LayoutParams)this.inputLayout.getLayoutParams()).addRule(0, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_clear_button);
			}
		} else if(widthMode == -2147483648) {
			this.inputLayout.getLayoutParams().width = -2;
			this.upperPanel.getLayoutParams().width = -2;
			this.editTextLayout.getLayoutParams().width = -2;
		}

		if(heightMode == 1073741824) {
			this.panel.getLayoutParams().height = -1;
			this.rightShell.getLayoutParams().height = -1;
			this.upperPanel.getLayoutParams().height = -1;
			((RelativeLayout.LayoutParams)this.bottomPart.getLayoutParams()).addRule(3, 0);
			((RelativeLayout.LayoutParams)this.bottomLine.getLayoutParams()).addRule(3, 0);
			((RelativeLayout.LayoutParams)this.bottomPart.getLayoutParams()).addRule(12);
			((RelativeLayout.LayoutParams)this.bottomLine.getLayoutParams()).addRule(12);
			((RelativeLayout.LayoutParams)this.panel.getLayoutParams()).addRule(2, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_bottom);
		} else if(heightMode == -2147483648) {
			this.panel.getLayoutParams().height = -2;
			this.rightShell.getLayoutParams().height = -2;
			this.upperPanel.getLayoutParams().height = -2;
			((RelativeLayout.LayoutParams)this.bottomPart.getLayoutParams()).addRule(3, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_panel);
			((RelativeLayout.LayoutParams)this.bottomLine.getLayoutParams()).addRule(3, studio.carbonylgroup.textfieldboxes.R.id.text_field_boxes_upper_panel);
			((RelativeLayout.LayoutParams)this.bottomPart.getLayoutParams()).addRule(12, 0);
			((RelativeLayout.LayoutParams)this.bottomLine.getLayoutParams()).addRule(12, 0);
			((RelativeLayout.LayoutParams)this.panel.getLayoutParams()).addRule(2, 0);
		}

	}

	protected void handleAttributes(Context context, AttributeSet attrs) {
		try {
			TypedArray styledAttrs = context.obtainStyledAttributes(attrs, studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes);
			this.labelText = styledAttrs.getString(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_labelText) == null?"":styledAttrs.getString(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_labelText);
			this.helperText = styledAttrs.getString(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_helperText) == null?"":styledAttrs.getString(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_helperText);
			this.helperTextColor = styledAttrs.getInt(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_helperTextColor, this.DEFAULT_TEXT_COLOR);
			this.errorColor = styledAttrs.getInt(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_errorColor, this.DEFAULT_ERROR_COLOR);
			this.primaryColor = styledAttrs.getColor(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_primaryColor, this.DEFAULT_PRIMARY_COLOR);
			this.panelBackgroundColor = styledAttrs.getColor(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_panelBackgroundColor, this.DEFAULT_BG_COLOR);
			this.maxCharacters = styledAttrs.getInt(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_maxCharacters, 0);
			this.minCharacters = styledAttrs.getInt(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_minCharacters, 0);
			this.enabled = styledAttrs.getBoolean(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_enabled, true);
			this.iconSignifierResourceId = styledAttrs.getResourceId(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_iconSignifier, 0);
			this.endIconResourceId = styledAttrs.getResourceId(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_endIcon, 0);
			this.isResponsiveIconColor = styledAttrs.getBoolean(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_isResponsiveIconColor, true);
			this.hasClearButton = styledAttrs.getBoolean(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_hasClearButton, false);
			this.hasFocus = styledAttrs.getBoolean(studio.carbonylgroup.textfieldboxes.R.styleable.TextFieldBoxes_hasFocus, false);
			styledAttrs.recycle();
		} catch (Exception var4) {
			var4.printStackTrace();
		}

	}

	public void deactivate() {
		if(this.inputText.getText().toString().isEmpty()) {
			ViewCompat.animate(this.floatingLabel).alpha(1.0F).scaleX(1.0F).scaleY(1.0F).translationY(0.0F).setDuration((long)this.ANIMATION_DURATION);
			this.editTextLayout.setVisibility(INVISIBLE);
			if(this.inputText.hasFocus()) {
				if (!useOsmandKeyboard) {
					this.inputMethodManager.hideSoftInputFromWindow(this.inputText.getWindowToken(), 0);
				}
				this.inputText.clearFocus();
			}
		}

		this.activated = false;
	}

	protected void activate(boolean animated) {
		this.editTextLayout.setVisibility(VISIBLE);
		if(this.inputText.getText().toString().isEmpty() && !this.isActivated()) {
			this.inputLayout.setAlpha(0.0F);
			this.floatingLabel.setScaleX(1.0F);
			this.floatingLabel.setScaleY(1.0F);
			this.floatingLabel.setTranslationY(0.0F);
		}

		if(animated) {
			ViewCompat.animate(this.inputLayout).alpha(1.0F).setDuration((long)this.ANIMATION_DURATION);
			ViewCompat.animate(this.floatingLabel).scaleX(0.75F).scaleY(0.75F).translationY((float)(-this.labelTopMargin + this.getContext().getResources().getDimensionPixelOffset(studio.carbonylgroup.textfieldboxes.R.dimen.text_field_boxes_margin_top))).setDuration((long)this.ANIMATION_DURATION);
		} else {
			this.inputLayout.setAlpha(1.0F);
			this.floatingLabel.setScaleX(0.75F);
			this.floatingLabel.setScaleY(0.75F);
			this.floatingLabel.setTranslationY((float)(-this.labelTopMargin + this.getContext().getResources().getDimensionPixelOffset(studio.carbonylgroup.textfieldboxes.R.dimen.text_field_boxes_margin_top)));
		}

		this.activated = true;
	}

	protected void makeCursorBlink() {
		int cursorPos = this.inputText.getSelectionStart();
		if(cursorPos == 0) {
			if(this.inputText.getText().toString().isEmpty()) {
				if(this.onError) {
					this.doNotRemoveError = true;
					this.inputText.setText(" ");
					this.inputText.setText("");
					this.doNotRemoveError = false;
				}
			}
		}

	}

	protected void setHighlightColor(int colorRes) {
		this.floatingLabel.setTextColor(colorRes);
		setCursorDrawableColor(this.inputText, colorRes);
		if(this.getIsResponsiveIconColor()) {
			this.iconImageButton.setColorFilter(colorRes);
			if(colorRes == this.DEFAULT_TEXT_COLOR) {
				this.iconImageButton.setAlpha(0.54F);
			} else {
				this.iconImageButton.setAlpha(1.0F);
			}
		}

		if(colorRes == this.DEFAULT_DISABLED_TEXT_COLOR) {
			this.iconImageButton.setAlpha(0.35F);
		}

		this.bottomLine.setBackgroundColor(colorRes);
	}

	protected void updateCounterText() {
		if(this.hasClearButton) {
			if(this.inputText.getText().toString().length() == 0) {
				this.showClearButton(false);
			} else {
				this.showClearButton(true);
			}
		}

		int length = this.inputText.getText().toString().replaceAll(" ", "").replaceAll("\n", "").length();
		String lengthStr = Integer.toString(length) + " / ";
		if(this.maxCharacters > 0) {
			if(this.minCharacters > 0) {
				this.counterLabel.setText(lengthStr + Integer.toString(this.minCharacters) + "-" + Integer.toString(this.maxCharacters));
				if(length >= this.minCharacters && length <= this.maxCharacters) {
					this.removeCounterError();
				} else {
					this.setCounterError();
				}
			} else {
				this.counterLabel.setText(lengthStr + Integer.toString(this.maxCharacters) + "");
				if(length > this.maxCharacters) {
					this.setCounterError();
				} else {
					this.removeCounterError();
				}
			}
		} else if(this.minCharacters > 0) {
			this.counterLabel.setText(lengthStr + Integer.toString(this.minCharacters) + "+");
			if(length < this.minCharacters) {
				this.setCounterError();
			} else {
				this.removeCounterError();
			}
		} else {
			this.counterLabel.setText("");
		}

	}

	protected void updateBottomViewVisibility() {
		if(this.helperLabel.getText().toString().isEmpty() && this.counterLabel.getText().toString().isEmpty()) {
			this.bottomPart.setVisibility(GONE);
		} else {
			this.bottomPart.setVisibility(GONE);
		}

	}

	protected void setCounterError() {
		this.onError = true;
		this.setHighlightColor(this.errorColor);
		this.counterLabel.setTextColor(this.errorColor);
	}

	protected void removeCounterError() {
		this.onError = false;
		if(this.hasFocus) {
			this.setHighlightColor(this.primaryColor);
		} else {
			this.setHighlightColor(this.DEFAULT_TEXT_COLOR);
		}

		this.counterLabel.setTextColor(this.DEFAULT_TEXT_COLOR);
	}

	public void setError(String errorText, boolean giveFocus) {
		if(this.enabled) {
			this.onError = true;
			this.activate(true);
			this.setHighlightColor(this.errorColor);
			this.helperLabel.setTextColor(this.errorColor);
			this.helperLabel.setText(errorText);
			if(giveFocus) {
				this.setHasFocus(true);
			}

			this.makeCursorBlink();
		}

	}

	public void removeError() {
		this.onError = false;
		if(this.hasFocus) {
			this.setHighlightColor(this.primaryColor);
		} else {
			this.setHighlightColor(this.DEFAULT_TEXT_COLOR);
		}

		this.helperLabel.setTextColor(this.helperTextColor);
		this.helperLabel.setText(this.helperText);
	}

	protected void showClearButton(boolean show) {
		if(show) {
			this.clearButton.setVisibility(VISIBLE);
		} else {
			this.clearButton.setVisibility(GONE);
		}

	}

	public void setLabelText(String labelText) {
		this.labelText = labelText;
		this.floatingLabel.setText(this.labelText);
	}

	public void setHelperText(String helperText) {
		this.helperText = helperText;
		this.helperLabel.setText(this.helperText);
	}

	public void setHelperTextColor(int colorRes) {
		this.helperTextColor = colorRes;
		this.helperLabel.setTextColor(this.helperTextColor);
	}

	public void setErrorColor(int colorRes) {
		this.errorColor = colorRes;
	}

	public void setPrimaryColor(int colorRes) {
		this.primaryColor = colorRes;
		if(this.hasFocus) {
			this.setHighlightColor(this.primaryColor);
		}

	}

	public void setPanelBackgroundColor(int colorRes) {
		this.panelBackgroundColor = colorRes;
		this.panel.getBackground().setColorFilter(new PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_IN));
	}

	public void setMaxCharacters(int maxCharacters) {
		this.maxCharacters = maxCharacters;
	}

	public void removeMaxCharacters() {
		this.maxCharacters = 0;
	}

	public void setMinCharacters(int minCharacters) {
		this.minCharacters = minCharacters;
	}

	public void removeMinCharacters() {
		this.minCharacters = 0;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if(this.enabled) {
			this.inputText.setEnabled(true);
			this.inputText.setFocusableInTouchMode(true);
			this.inputText.setFocusable(true);
			this.helperLabel.setVisibility(VISIBLE);
			this.counterLabel.setVisibility(VISIBLE);
			this.bottomLine.setVisibility(VISIBLE);
			this.panel.setEnabled(true);
			this.iconImageButton.setEnabled(true);
			this.iconImageButton.setClickable(true);
			this.setHighlightColor(this.DEFAULT_TEXT_COLOR);
			this.updateCounterText();
		} else {
			this.removeError();
			this.setHasFocus(false);
			this.inputText.setEnabled(false);
			this.inputText.setFocusableInTouchMode(false);
			this.inputText.setFocusable(false);
			this.iconImageButton.setClickable(false);
			this.iconImageButton.setEnabled(false);
			this.helperLabel.setVisibility(INVISIBLE);
			this.counterLabel.setVisibility(INVISIBLE);
			this.bottomLine.setVisibility(INVISIBLE);
			this.panel.setEnabled(false);
			this.setHighlightColor(this.DEFAULT_DISABLED_TEXT_COLOR);
		}

	}

	public void setIconSignifier(int resourceID) {
		this.iconSignifierResourceId = resourceID;
		if(this.iconSignifierResourceId != 0) {
			this.iconImageButton.setImageResource(this.iconSignifierResourceId);
			this.iconImageButton.setVisibility(VISIBLE);
		} else {
			this.removeIconSignifier();
		}

	}

	public void setIconSignifier(Drawable drawable) {
		this.removeIconSignifier();
		this.iconImageButton.setImageDrawable(drawable);
		this.iconImageButton.setVisibility(VISIBLE);
	}

	public void removeIconSignifier() {
		this.iconSignifierResourceId = 0;
		this.iconImageButton.setVisibility(GONE);
	}

	public void setEndIcon(int resourceID) {
		this.endIconResourceId = resourceID;
		if(this.endIconResourceId != 0) {
			this.endIconImageButton.setImageResource(this.endIconResourceId);
			this.endIconImageButton.setVisibility(VISIBLE);
		} else {
			this.removeEndIcon();
		}

	}

	public void setEndIcon(Drawable drawable) {
		this.removeEndIcon();
		this.endIconImageButton.setImageDrawable(drawable);
		this.endIconImageButton.setVisibility(VISIBLE);
	}

	public void removeEndIcon() {
		this.endIconResourceId = 0;
		this.endIconImageButton.setVisibility(GONE);
	}

	public void setHasFocus(boolean hasFocus) {
		this.hasFocus = hasFocus;
		if(this.hasFocus) {
			this.activate(true);
			this.inputText.requestFocus();
			this.makeCursorBlink();
			if(!this.onError && this.enabled) {
				this.setHighlightColor(this.primaryColor);
			}
		} else {
			this.deactivate();
			if(!this.onError && this.enabled) {
				this.setHighlightColor(this.DEFAULT_TEXT_COLOR);
			}
		}

	}

	public void setIsResponsiveIconColor(boolean isResponsiveIconColor) {
		this.isResponsiveIconColor = isResponsiveIconColor;
		if(this.isResponsiveIconColor) {
			if(this.hasFocus) {
				this.iconImageButton.setColorFilter(this.primaryColor);
				this.iconImageButton.setAlpha(1.0F);
			} else {
				this.iconImageButton.setColorFilter(this.DEFAULT_TEXT_COLOR);
				this.iconImageButton.setAlpha(0.54F);
			}
		} else {
			this.iconImageButton.setColorFilter(this.primaryColor);
			this.iconImageButton.setAlpha(1.0F);
		}

	}

	public void setHasClearButton(boolean hasClearButton) {
		this.hasClearButton = hasClearButton;
	}

	public String getLabelText() {
		return this.labelText;
	}

	public String getHelperText() {
		return this.helperText;
	}

	public String getCounterText() {
		return this.counterLabel.getText().toString();
	}

	public int getHelperTextColor() {
		return this.helperTextColor;
	}

	public int getErrorColor() {
		return this.errorColor;
	}

	public int getPrimaryColor() {
		return this.primaryColor;
	}

	public int getPanelBackgroundColor() {
		return this.panelBackgroundColor;
	}

	public int getMaxCharacters() {
		return this.maxCharacters;
	}

	public int getMinCharacters() {
		return this.minCharacters;
	}

	public View getPanel() {
		return this.panel;
	}

	public View getBottomLine() {
		return this.bottomLine;
	}

	public AppCompatTextView getHelperLabel() {
		return this.helperLabel;
	}

	public AppCompatTextView getCounterLabel() {
		return this.counterLabel;
	}

	public AppCompatTextView getFloatingLabel() {
		return this.floatingLabel;
	}

	public AppCompatImageButton getIconImageButton() {
		return this.iconImageButton;
	}

	public AppCompatImageButton getEndIconImageButton() {
		return this.endIconImageButton;
	}

	public boolean isActivated() {
		return this.activated;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public int getIconSignifierResourceId() {
		return this.iconSignifierResourceId;
	}

	public int getEndIconResourceId() {
		return this.endIconResourceId;
	}

	public boolean getIsResponsiveIconColor() {
		return this.isResponsiveIconColor;
	}

	public boolean getHasClearButton() {
		return this.hasClearButton;
	}

	public boolean getHasFocus() {
		return this.hasFocus;
	}

	protected static void setCursorDrawableColor(AppCompatTextView _inputText, int _colorRes) {
		try {
			Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
			fCursorDrawableRes.setAccessible(true);
			int mCursorDrawableRes = fCursorDrawableRes.getInt(_inputText);
			Field fEditor = TextView.class.getDeclaredField("mEditor");
			fEditor.setAccessible(true);
			Object editor = fEditor.get(_inputText);
			Class<?> clazz = editor.getClass();
			Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
			fCursorDrawable.setAccessible(true);
			Drawable[] drawables = new Drawable[]{ContextCompat.getDrawable(_inputText.getContext(), mCursorDrawableRes), ContextCompat.getDrawable(_inputText.getContext(), mCursorDrawableRes)};
			drawables[0].setColorFilter(_colorRes, PorterDuff.Mode.SRC_IN);
			drawables[1].setColorFilter(_colorRes, PorterDuff.Mode.SRC_IN);
			fCursorDrawable.set(editor, drawables);
		} catch (Throwable var9) {
			;
		}

	}

	protected static int lighter(int color, float _factor) {
		int red = (int)(((float) Color.red(color) * (1.0F - _factor) / 255.0F + _factor) * 255.0F);
		int green = (int)(((float)Color.green(color) * (1.0F - _factor) / 255.0F + _factor) * 255.0F);
		int blue = (int)(((float)Color.blue(color) * (1.0F - _factor) / 255.0F + _factor) * 255.0F);
		return Color.argb(Color.alpha(color), red, green, blue);
	}

	protected static boolean isLight(int color) {
		return Math.sqrt((double)(Color.red(color) * Color.red(color)) * 0.241D + (double)(Color.green(color) * Color.green(color)) * 0.691D + (double)(Color.blue(color) * Color.blue(color)) * 0.068D) > 130.0D;
	}

	protected static int adjustAlpha(int color, float _toAlpha) {
		int alpha = Math.round(255.0F * _toAlpha);
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		return Color.argb(alpha, red, green, blue);
	}
}
