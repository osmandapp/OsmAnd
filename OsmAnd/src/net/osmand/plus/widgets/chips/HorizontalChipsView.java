package net.osmand.plus.widgets.chips;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.chips.ChipsAdapter.OnSelectChipListener;

import java.util.List;

public class HorizontalChipsView extends RecyclerView {

	final private ChipsAdapter adapter;
	final private ChipsDataHolder holder = new ChipsDataHolder();
	final private DefaultAttributes defAttrs = new DefaultAttributes();

	public HorizontalChipsView(@NonNull Context context) {
		this(context, null);
	}

	public HorizontalChipsView(@NonNull Context context,
	                           @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalChipsView(@NonNull Context context,
	                           @Nullable AttributeSet attrs,
	                           int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
		setClipToPadding(false);

		initDefaultAttrs(context, attrs, defStyleAttr);
		adapter = new ChipsAdapter(context, holder, defAttrs);
		setAdapter(adapter);

		invalidate();
	}

	public void setItems(List<ChipItem> chips) {
		holder.setItems(chips);
	}

	public void setSelected(ChipItem chip) {
		holder.setSelected(chip);
	}

	public void setOnSelectChipListener(OnSelectChipListener listener) {
		adapter.onSelectChipListener = listener;
	}

	public void notifyDataSetChanged() {
		adapter.notifyDataSetChanged();
	}

	public ChipItem getChipById(String id) {
		return holder.getItemById(id);
	}

	public void scrollTo(@NonNull ChipItem chip) {
		scrollToPosition(holder.indexOf(chip));
	}

	public void smoothScrollTo(@NonNull ChipItem chip) {
		smoothScrollToPosition(holder.indexOf(chip));
	}

	private void initDefaultAttrs(@NonNull Context context,
	                              @Nullable AttributeSet attrs,
	                              int defStyleAttr) {
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.HorizontalChipsView,
				defStyleAttr,
				0
		);

		defAttrs.titleColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleColor,
				getColorFromAttr(R.attr.chip_content_color));
		defAttrs.titleSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleSelectedColor,
				getColor(R.color.color_white));
		defAttrs.titleDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleDisabledColor,
				getColorFromAttr(R.attr.inactive_text_color));

		defAttrs.iconColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconColor,
				getColorFromAttr(R.attr.chip_content_color));
		defAttrs.iconSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconSelectedColor,
				getColor(R.color.color_white));
		defAttrs.iconDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconDisabledColor,
				getColorFromAttr(R.attr.inactive_text_color));
		defAttrs.useNaturalIconColor = a.getBoolean(
				R.styleable.HorizontalChipsView_chipUseNaturalIconColor,
				false);

		defAttrs.bgColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgColor,
				getColor(R.color.color_transparent));
		defAttrs.bgSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgSelectedColor,
				getColorFromAttr(R.attr.active_color_basic));
		defAttrs.bgDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgDisabledColor,
				getColor(R.color.color_transparent));
		defAttrs.bgRippleId = resolveAttribute(R.attr.chip_ripple);

		defAttrs.drawablePaddingPx = getDimension(R.dimen.content_padding_half);

		defAttrs.strokeColor = getColorFromAttr(R.attr.stroked_buttons_and_links_outline);
		defAttrs.strokeWidth = AndroidUtils.dpToPx(context, 1);
	}

	@ColorInt
	private int getColorFromAttr(int attr) {
		return getColor(resolveAttribute(attr));
	}

	private int resolveAttribute(int attr) {
		return AndroidUtils.resolveAttribute(getContext(), attr);
	}

	@ColorInt
	private int getColor(int colorId) {
		return ContextCompat.getColor(getContext(), colorId);
	}

	private int getDimension(int dimen) {
		return getResources().getDimensionPixelSize(dimen);
	}

}
