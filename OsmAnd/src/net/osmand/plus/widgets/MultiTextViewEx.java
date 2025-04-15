package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.PaintedText;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiTextViewEx extends TextViewEx {

	private String separator;
	@ColorInt
	private int separatorColor;

	public MultiTextViewEx(@NonNull Context context) {
		super(context);
		init(context, null);
	}

	public MultiTextViewEx(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public MultiTextViewEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
		if (attrs == null) return;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiTextViewEx);

		String primaryText = a.getString(R.styleable.MultiTextViewEx_primaryText);
		String secondaryText = a.getString(R.styleable.MultiTextViewEx_secondaryText);
		String tertiaryText = a.getString(R.styleable.MultiTextViewEx_tertiaryText);

		int currentColor = getCurrentTextColor();
		int primaryColor = a.getColor(R.styleable.MultiTextViewEx_primaryTextColor, currentColor);
		int secondaryColor = a.getColor(R.styleable.MultiTextViewEx_secondaryTextColor, currentColor);
		int tertiaryColor = a.getColor(R.styleable.MultiTextViewEx_tertiaryTextColor, currentColor);

		separator = a.getString(R.styleable.MultiTextViewEx_separator);
		separator = Algorithms.isEmpty(separator) ? " " : separator;
		separatorColor = a.getColor(R.styleable.MultiTextViewEx_separatorColor, currentColor);
		a.recycle();

		List<PaintedText> values = new ArrayList<>();
		if (!TextUtils.isEmpty(primaryText)) {
			values.add(new PaintedText(primaryText, primaryColor));
		}
		if (!TextUtils.isEmpty(secondaryText)) {
			values.add(new PaintedText(secondaryText, secondaryColor));
		}
		if (!TextUtils.isEmpty(tertiaryText)) {
			values.add(new PaintedText(tertiaryText, tertiaryColor));
		}
		setMultiText(values);
	}

	public void setMultiText(@NonNull Collection<PaintedText> texts) {
		if (texts.isEmpty()) {
			setText("");
			return;
		}
		SpannableStringBuilder builder = new SpannableStringBuilder();
		boolean first = true;
		for (PaintedText pt : texts) {
			if (!first) {
				builder.append(createColorSpannable(separator, separatorColor));
			}
			builder.append(createColorSpannable(pt));
			first = false;
		}
		setText(builder);
	}

	@NonNull
	private CharSequence createColorSpannable(@NonNull PaintedText paintedText) {
		return createColorSpannable(paintedText.getText().toString(), paintedText.getColor());
	}

	@NonNull
	private CharSequence createColorSpannable(@NonNull String text, @ColorInt int color) {
		return UiUtilities.createColorSpannable(text, color, text);
	}
}
