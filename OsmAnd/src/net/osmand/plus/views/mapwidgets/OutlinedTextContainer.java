package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.PaintedText;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.AutoScaleTextView;
import net.osmand.plus.widgets.FrameLayoutEx;
import net.osmand.plus.widgets.MultiTextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.List;

public class OutlinedTextContainer extends FrameLayoutEx {
	private static final Log LOG = PlatformUtil.getLog(OutlinedTextContainer.class);

	private TextView outlineTextView;
	private TextView mainTextView;

	public OutlinedTextContainer(Context context) {
		super(context);
		init(null);
	}

	public OutlinedTextContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public OutlinedTextContainer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	private void init(@Nullable AttributeSet attrs) {
		if (attrs != null) {
			AttrsHolder holder = new AttrsHolder(attrs);

			outlineTextView = createTextView(holder.textViewType);
			mainTextView = createTextView(holder.textViewType);

			setupTextView(outlineTextView, holder, true);
			setupTextView(mainTextView, holder, false);
		} else {
			outlineTextView = new TextView(getContext());
			mainTextView = new TextView(getContext());
		}

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		addView(outlineTextView, params);
		addView(mainTextView, params);

		setVerticalScrollBarEnabled(false);
		setHorizontalScrollBarEnabled(false);
	}

	private void setupTextView(@NonNull TextView textView, @NonNull AttrsHolder holder, boolean isOutline) {
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.textSize);
		textView.setTypeface(holder.typeface);
		textView.setGravity(holder.gravity);
		textView.setText(holder.initialText);

		if (holder.maxLines > 0) {
			textView.setMaxLines(holder.maxLines);
		}
		if (holder.ellipsize != null) {
			textView.setEllipsize(holder.ellipsize);
		}
		textView.setIncludeFontPadding(holder.includeFontPadding);
		textView.setLetterSpacing(holder.letterSpacing);
		textView.setLineSpacing(holder.lineSpacingExtra, holder.lineSpacingMultiplier);
		textView.setAllCaps(holder.textAllCaps);

		if (holder.autoSizeTextType != TextView.AUTO_SIZE_TEXT_TYPE_NONE && VERSION.SDK_INT >= VERSION_CODES.O) {
			textView.setAutoSizeTextTypeWithDefaults(holder.autoSizeTextType);
			if (holder.autoSizeMinTextSize > 0 && holder.autoSizeMaxTextSize > 0 && holder.autoSizeStepGranularity > 0) {
				textView.setAutoSizeTextTypeUniformWithConfiguration(
						holder.autoSizeMinTextSize,
						holder.autoSizeMaxTextSize,
						holder.autoSizeStepGranularity,
						TypedValue.COMPLEX_UNIT_PX
				);
			}
		}

		if (isOutline) {
			textView.setTextColor(holder.outlineColor);
			//textView.setLayerType(LAYER_TYPE_SOFTWARE, null);
			TextPaint paint = textView.getPaint();
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(holder.outlineWidth);
			paint.setStrokeJoin(Paint.Join.ROUND);
		} else {
			textView.setTextColor(holder.textColor);
		}

		if (textView instanceof AutoScaleTextView autoScaleTextView) {
			setupAutoScaleTextView(autoScaleTextView, holder);
		}

		if (textView instanceof MultiTextViewEx multiTextViewEx) {
			setupMultiTextView(multiTextViewEx, holder);
		}

		textView.setVerticalScrollBarEnabled(false);
		textView.setHorizontalScrollBarEnabled(false);
	}

	private void setupAutoScaleTextView(@NonNull AutoScaleTextView autoScaleTextView, @NonNull AttrsHolder holder) {
		if (holder.minTextSize > 0) {
			autoScaleTextView.setMinTextSize(holder.minTextSize);
		}

		if (holder.maxTextSize > 0) {
			autoScaleTextView.setMaxTextSize(holder.maxTextSize);
		}
	}

	private void setupMultiTextView(@NonNull MultiTextViewEx multiTextViewEx, @NonNull AttrsHolder holder) {
		int textColor = holder.textColor;

		String primaryText = !Algorithms.isEmpty(holder.multiTextViewPrimaryText) ? holder.multiTextViewPrimaryText : "";
		String secondaryText = !Algorithms.isEmpty(holder.multiTextViewSecondaryText) ? holder.multiTextViewSecondaryText : "";
		String tertiaryText = !Algorithms.isEmpty(holder.multiTextViewTertiaryText) ? holder.multiTextViewTertiaryText : "";

		int primaryColor = holder.multiTextViewPrimaryColor != -1 ? holder.multiTextViewPrimaryColor : textColor;
		int secondaryColor = holder.multiTextViewSecondaryColor != -1 ? holder.multiTextViewSecondaryColor : textColor;
		int tertiaryColor = holder.multiTextViewTertiaryColor != -1 ? holder.multiTextViewTertiaryColor : textColor;

		String separator = holder.multiTextViewSeparator;
		separator = Algorithms.isEmpty(separator) ? " " : separator;
		int separatorColor = holder.multiTextViewSeparatorColor != -1 ? holder.multiTextViewSeparatorColor : textColor;


		multiTextViewEx.init(separator, separatorColor,
				primaryText, secondaryText, tertiaryText,
				primaryColor, secondaryColor, tertiaryColor);
	}

	@NonNull
	private TextView createTextView(int type) {
		Context context = getContext();
		switch (type) {
			case 1: // autoScale
				return new AutoScaleTextView(getContext());
			case 2: // multi
				return new MultiTextViewEx(getContext());
			default: // normal
				return new TextView(getContext());
		}
	}

	public void setText(@Nullable CharSequence text) {
		outlineTextView.setText(text);
		mainTextView.setText(text);

		outlineTextView.requestLayout();
		mainTextView.requestLayout();
	}

	public CharSequence getText() {
		return mainTextView.getText();
	}

	public void setTextColor(int color) {
		mainTextView.setTextColor(color);
	}

	public int getTextColor() {
		return mainTextView.getCurrentTextColor();
	}

	public void setTextSize(float size) {
		outlineTextView.setTextSize(size);
		mainTextView.setTextSize(size);
	}

	public void setTextSize(int unit, float size) {
		outlineTextView.setTextSize(unit, size);
		mainTextView.setTextSize(unit, size);
	}

	public void setTypeface(@Nullable Typeface tf, int style) {
		outlineTextView.setTypeface(tf, style);
		mainTextView.setTypeface(tf, style);
	}

	public void setTypeface(@Nullable Typeface tf) {
		outlineTextView.setTypeface(tf);
		mainTextView.setTypeface(tf);
	}

	public void setTextDirection(int textDirection) {
		outlineTextView.setTextDirection(textDirection);
		mainTextView.setTextDirection(textDirection);
	}

	public void setTextAlignment(int textAlignment) {
		outlineTextView.setTextAlignment(textAlignment);
		mainTextView.setTextAlignment(textAlignment);
	}

	public void setGravity(int gravity) {
		mainTextView.setGravity(gravity);
		outlineTextView.setGravity(gravity);
	}

	public void setStrokeWidth(int textShadowRadius) {
		outlineTextView.getPaint().setStrokeWidth(textShadowRadius);
	}

	public float getStrokeWidth() {
		return outlineTextView.getPaint().getStrokeWidth();
	}

	public void setStrokeColor(int textShadowColor) {
		outlineTextView.setTextColor(textShadowColor);
	}

	public int getStrokeColor() {
		return outlineTextView.getCurrentTextColor();
	}

	public void showOutline(boolean showOutline) {
		AndroidUiHelper.updateVisibility(outlineTextView, showOutline);
	}

	public boolean isShowingOutline() {
		return outlineTextView.getVisibility() == View.VISIBLE;
	}

	public Typeface getTypeface() {
		return mainTextView.getTypeface();
	}

	public float getTextSize() {
		return mainTextView.getTextSize();
	}

	public void copyFromTextContainer(OutlinedTextContainer sourceTextContainer) {
		setTextColor(sourceTextContainer.getTextColor());
		setTypeface(sourceTextContainer.getTypeface());

		setStrokeWidth((int) sourceTextContainer.getStrokeWidth());
		setStrokeColor(sourceTextContainer.getStrokeColor());
		showOutline(sourceTextContainer.isShowingOutline());

		setText(sourceTextContainer.getText());
	}

	public void setMultiText(List<PaintedText> primaryLineText) {
		if (outlineTextView instanceof MultiTextViewEx multiTextViewEx) {
			multiTextViewEx.setOutlineMultiText(primaryLineText, getStrokeColor());
		}
		if (mainTextView instanceof MultiTextViewEx multiTextViewEx) {
			multiTextViewEx.setMultiText(primaryLineText);
		}
	}

	public void invalidateTextViews() {
		outlineTextView.invalidate();
		mainTextView.invalidate();
	}

	public TextPaint getPaint() {
		return mainTextView.getPaint();
	}

	class AttrsHolder {
		int textColor = Color.BLACK;
		int outlineColor = Color.WHITE;
		float outlineWidth = 0f;
		float textSize = 14f;
		Typeface typeface = Typeface.DEFAULT;
		int gravity = 0;
		CharSequence initialText = "";
		int maxLines = -1;
		TextUtils.TruncateAt ellipsize = null;
		boolean includeFontPadding = true;
		float letterSpacing = 0f;
		boolean textAllCaps = false;
		float lineSpacingExtra = 0f;
		float lineSpacingMultiplier = 1.0f;
		int autoSizeTextType = 0;
		int textViewType = 0;
		int autoSizeMinTextSize = -1;
		int autoSizeMaxTextSize = -1;
		int autoSizeStepGranularity = -1;

		//AutoScaleTextView attributes
		float minTextSize = -1;
		float maxTextSize = -1;

		//MultiTextViewEx attributes
		String multiTextViewSeparator = "";
		@ColorInt
		int multiTextViewSeparatorColor = -1;
		int multiTextViewPrimaryColor = -1;
		int multiTextViewSecondaryColor = -1;
		int multiTextViewTertiaryColor = -1;
		String multiTextViewPrimaryText = "";
		String multiTextViewSecondaryText = "";
		String multiTextViewTertiaryText = "";

		public AttrsHolder(@NonNull AttributeSet attrs) {
			init(attrs);
		}

		private void init(@NonNull AttributeSet attrs) {
			try (TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OutlinedTextContainer)) {
				textColor = a.getColor(R.styleable.OutlinedTextContainer_android_textColor, textColor);
				textSize = a.getDimension(R.styleable.OutlinedTextContainer_android_textSize, textSize);
				gravity = a.getInt(R.styleable.OutlinedTextContainer_android_gravity, gravity);

				outlineColor = a.getColor(R.styleable.OutlinedTextContainer_outlineColor, outlineColor);
				outlineWidth = a.getDimension(R.styleable.OutlinedTextContainer_outlineWidth, outlineWidth);

				String fontFamily = a.getString(R.styleable.OutlinedTextContainer_android_fontFamily);
				if (fontFamily != null) {
					typeface = Typeface.create(fontFamily, Typeface.NORMAL);
				}

				CharSequence text = a.getText(R.styleable.OutlinedTextContainer_android_text);
				if (text != null) {
					initialText = text;
				}

				maxLines = a.getInt(R.styleable.OutlinedTextContainer_android_maxLines, maxLines);

				int ellipsizeValue = a.getInt(R.styleable.OutlinedTextContainer_android_ellipsize, -1);
				if (ellipsizeValue != -1) {
					ellipsize = switch (ellipsizeValue) {
						case 1 -> TextUtils.TruncateAt.START;
						case 2 -> TextUtils.TruncateAt.MIDDLE;
						case 3 -> TextUtils.TruncateAt.END;
						case 4 -> TextUtils.TruncateAt.MARQUEE;
						default -> ellipsize;
					};
				}

				includeFontPadding = a.getBoolean(R.styleable.OutlinedTextContainer_android_includeFontPadding, includeFontPadding);

				letterSpacing = a.getFloat(R.styleable.OutlinedTextContainer_android_letterSpacing, letterSpacing);
				textAllCaps = a.getBoolean(R.styleable.OutlinedTextContainer_android_textAllCaps, textAllCaps);

				lineSpacingExtra = a.getDimension(R.styleable.OutlinedTextContainer_android_lineSpacingExtra, lineSpacingExtra);
				lineSpacingMultiplier = a.getFloat(R.styleable.OutlinedTextContainer_android_lineSpacingMultiplier, lineSpacingMultiplier);

				textViewType = a.getInt(R.styleable.OutlinedTextContainer_textViewType, textViewType);
				autoSizeTextType = a.getInt(R.styleable.OutlinedTextContainer_android_autoSizeTextType, autoSizeTextType);
				autoSizeMinTextSize = a.getDimensionPixelSize(R.styleable.OutlinedTextContainer_android_autoSizeMinTextSize, autoSizeMinTextSize);
				autoSizeMaxTextSize = a.getDimensionPixelSize(R.styleable.OutlinedTextContainer_android_autoSizeMaxTextSize, autoSizeMaxTextSize);
				autoSizeStepGranularity = a.getDimensionPixelSize(R.styleable.OutlinedTextContainer_android_autoSizeStepGranularity, autoSizeStepGranularity);

				minTextSize = a.getDimension(R.styleable.OutlinedTextContainer_autoScale_minTextSize, minTextSize);
				maxTextSize = a.getDimension(R.styleable.OutlinedTextContainer_autoScale_maxTextSize, maxTextSize);

				multiTextViewPrimaryText = a.getString(R.styleable.OutlinedTextContainer_multiTextViewEx_primaryText);
				multiTextViewSecondaryText = a.getString(R.styleable.OutlinedTextContainer_multiTextViewEx_secondaryText);
				multiTextViewTertiaryText = a.getString(R.styleable.OutlinedTextContainer_multiTextViewEx_tertiaryText);

				multiTextViewPrimaryColor = a.getColor(R.styleable.OutlinedTextContainer_multiTextViewEx_primaryTextColor, -1);
				multiTextViewSecondaryColor = a.getColor(R.styleable.OutlinedTextContainer_multiTextViewEx_secondaryTextColor, -1);
				multiTextViewTertiaryColor = a.getColor(R.styleable.OutlinedTextContainer_multiTextViewEx_tertiaryTextColor, -1);

				multiTextViewSeparator = a.getString(R.styleable.OutlinedTextContainer_multiTextViewEx_separator);
				multiTextViewSeparator = Algorithms.isEmpty(multiTextViewSeparator) ? " " : multiTextViewSeparator;
				multiTextViewSeparatorColor = a.getColor(R.styleable.OutlinedTextContainer_multiTextViewEx_separatorColor, -1);
			}
		}
	}
}