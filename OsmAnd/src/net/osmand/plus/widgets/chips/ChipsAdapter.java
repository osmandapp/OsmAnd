package net.osmand.plus.widgets.chips;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public class ChipsAdapter extends RecyclerView.Adapter<ChipViewHolder> {

	private final Context ctx;
	private final LayoutInflater inflater;
	OnSelectChipListener onSelectChipListener;

	private final ChipsDataHolder dataHolder;
	private final DefaultAttributes defAttrs;

	public ChipsAdapter(Context ctx, ChipsDataHolder chipsHolder, DefaultAttributes defAttrs) {
		this.ctx = ctx;
		this.dataHolder = chipsHolder;
		this.defAttrs = defAttrs;
		inflater = LayoutInflater.from(ctx);
	}

	@NonNull
	@Override
	public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = inflater.inflate(R.layout.point_editor_icon_category_item, parent, false);
		return new ChipViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ChipViewHolder holder, final int position) {
		ChipItem item = dataHolder.getItem(holder.getAdapterPosition());

		if (item.onBeforeBindCallback != null) {
			item.onBeforeBindCallback.onBeforeViewBound(item);
		}

		bindBackground(item, holder.button, holder.container);
		bindTitle(item, holder.title);
		bindIcon(item, holder.image);
		bindClickListener(item, holder.button); // TODO may be use container

		if (item.onAfterBindCallback != null) {
			item.onAfterBindCallback.onAfterViewBound(item, holder);
		}
	}

	private void bindBackground(@NonNull ChipItem chip,
	                            @NonNull View button,
	                            @NonNull View wrapper) {
		GradientDrawable background = (GradientDrawable) getDrawable(R.drawable.bg_selected_chip).mutate();
		int color = getBgColor(chip);
		background.setColor(color);
		int strokeWidth = getStrokeWidth();
		if (strokeWidth > 0) {
			int strokeColor = getStrokeColor();
			background.setStroke(strokeWidth, strokeColor);
		}
		Drawable ripple = getDrawable(getRippleId());
		AndroidUtils.setBackground(button, background);
		AndroidUtils.setBackground(wrapper, ripple);
	}

	private void bindTitle(@NonNull ChipItem chip,
	                       @NonNull TextView tvTitle) {
		if (chip.title == null) {
			tvTitle.setVisibility(View.GONE);
			return;
		}
		int color = getTitleColor(chip);
		tvTitle.setVisibility(View.VISIBLE);
		tvTitle.setText(capitalizeFirstLetter(chip.title));
		tvTitle.setTextColor(color);
		tvTitle.requestLayout();
	}

	private void bindIcon(@NonNull ChipItem chip,
	                      @NonNull ImageView ivImage) {
		if (chip.icon == null) {
			ivImage.setVisibility(View.GONE);
			return;
		}
		ivImage.setVisibility(View.VISIBLE);
		ivImage.setImageDrawable(chip.icon);
		if (chip.useNaturalIconColor) {
			ivImage.clearColorFilter();
		} else {
			int color = getIconColor(chip);
			ivImage.setColorFilter(color);
		}
		MarginLayoutParams lp = (MarginLayoutParams) ivImage.getLayoutParams();
		AndroidUtils.setMargins(lp, 0, 0, getDrawablePadding(chip), 0);
	}

	private void bindClickListener(@NonNull ChipItem chip,
	                               @NonNull View button) {
		button.setOnClickListener(v -> {
			if (onSelectChipListener != null && onSelectChipListener.onSelectChip(chip)) {
				dataHolder.setSelected(chip);
				notifyDataSetChanged();
			}
		});
	}

	@ColorInt
	private int getBgColor(ChipItem chip) {
		if (chip.isEnabled && chip.isSelected) {
			return chip.bgSelectedColor != null ? chip.bgSelectedColor : defAttrs.bgSelectedColor;
		} else if (chip.isEnabled) {
			return chip.bgColor != null ? chip.bgColor : defAttrs.bgColor;
		} else {
			return chip.bgDisabledColor != null ? chip.bgDisabledColor : defAttrs.bgDisabledColor;
		}
	}

	@ColorInt
	private int getTitleColor(ChipItem chip) {
		if (chip.isEnabled && chip.isSelected) {
			return chip.titleSelectedColor != null ? chip.titleSelectedColor : defAttrs.titleSelectedColor;
		} else if (chip.isEnabled) {
			return chip.titleColor != null ? chip.titleColor : defAttrs.titleColor;
		} else {
			return chip.titleDisabledColor != null ? chip.titleDisabledColor : defAttrs.titleDisabledColor;
		}
	}

	@ColorInt
	private int getIconColor(ChipItem chip) {
		if (chip.isEnabled && chip.isSelected) {
			return chip.iconSelectedColor != null ? chip.iconSelectedColor : defAttrs.iconSelectedColor;
		} else if (chip.isEnabled) {
			return chip.iconColor != null ? chip.iconColor : defAttrs.iconColor;
		} else {
			return chip.iconDisabledColor != null ? chip.iconDisabledColor : defAttrs.iconDisabledColor;
		}
	}

	@ColorInt
	private int getStrokeColor() {
		return defAttrs.strokeColor;
	}

	private int getStrokeWidth() {
		return defAttrs.strokeWidth;
	}

	private int getRippleId() {
		return defAttrs.bgRippleId;
	}

	/**
	 * Drawable padding applies only when title available
	 */
	private int getDrawablePadding(ChipItem item) {
		if (item.title != null) {
			return item.drawablePaddingPx != null ? item.drawablePaddingPx : defAttrs.drawablePaddingPx;
		} else {
			return 0;
		}
	}

	@Override
	public int getItemCount() {
		return dataHolder.getItems().size();
	}

	private Drawable getDrawable(int drawableId) {
		return AppCompatResources.getDrawable(ctx, drawableId);
	}

	public interface OnSelectChipListener {
		boolean onSelectChip(ChipItem chip);
	}

}