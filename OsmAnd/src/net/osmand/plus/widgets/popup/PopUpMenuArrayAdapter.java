package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class PopUpMenuArrayAdapter extends ArrayAdapter<PopUpMenuItem> {

	private final List<PopUpMenuItem> items;
	private final boolean nightMode;
	private final int layoutId;

	public PopUpMenuArrayAdapter(@NonNull Context context,
	                             int layoutId,
	                             List<PopUpMenuItem> items,
	                             boolean nightMode) {
		super(context, layoutId);
		this.items = items;
		this.layoutId = layoutId;
		this.nightMode = nightMode;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		if (convertView == null) {
			convertView = inflater.inflate(layoutId, parent, false);
		}
		PopUpMenuItem item = getItem(position);
		if (item != null) {
			TextView tvTitle = convertView.findViewById(R.id.title);
			tvTitle.setText(item.getTitle());
			tvTitle.setTypeface(tvTitle.getTypeface(), item.isTitleBold() ? Typeface.BOLD : Typeface.NORMAL);
			Integer titleColor = item.getTitleColor();
			if (titleColor != null) {
				tvTitle.setTextColor(titleColor);
			} else {
				tvTitle.setTextColor(ColorUtilities.getPrimaryTextColor(getContext(), nightMode));
			}
			Integer titleSize = item.getTitleSize();
			if (titleSize != null) {
				tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);
			}
			ImageView ivIcon = convertView.findViewById(R.id.icon);
			Drawable icon = item.getIcon();
			if (icon != null) {
				ivIcon.setImageDrawable(icon);
			} else {
				ivIcon.setVisibility(View.GONE);
			}
			CompoundButton singleCompound = convertView.findViewById(R.id.compound_button);
			CompoundButton radioCompound = convertView.findViewById(R.id.compound_button_radio);
			CompoundButton checkCompound = convertView.findViewById(R.id.compound_button_checkbox);
			FrameLayout compoundContainer = convertView.findViewById(R.id.compound_button_container);
			if (singleCompound != null) {
				boolean showSingleCompound = item.isShowCompoundBtn();
				if (showSingleCompound) {
					UiUtilities.setupCompoundButton(nightMode, item.getCompoundBtnColor(), singleCompound);
					singleCompound.setChecked(item.isSelected());
				}
				AndroidUiHelper.updateVisibility(singleCompound, showSingleCompound);
				AndroidUiHelper.updateVisibility(compoundContainer, showSingleCompound);
			} else {
				boolean showRadio = item.isShowCompoundBtn()
						&& item.getCompoundButtonType() == PopUpMenuItem.CompoundButtonType.RADIO;
				boolean showCheckbox = item.isShowCompoundBtn()
						&& item.getCompoundButtonType() == PopUpMenuItem.CompoundButtonType.CHECKBOX;
				if (showRadio && radioCompound != null) {
					UiUtilities.setupCompoundButton(nightMode, item.getCompoundBtnColor(), radioCompound);
					radioCompound.setChecked(item.isSelected());
				}
				if (showCheckbox && checkCompound != null) {
					UiUtilities.setupCompoundButton(nightMode, item.getCompoundBtnColor(), checkCompound);
					checkCompound.setChecked(item.isSelected());
				}
				AndroidUiHelper.updateVisibility(radioCompound, showRadio);
				AndroidUiHelper.updateVisibility(checkCompound, showCheckbox);
				AndroidUiHelper.updateVisibility(compoundContainer, showRadio || showCheckbox);
			}
			if (item.isSelected()) {
				if (!item.isShowCompoundBtn()) {
					convertView.setBackgroundColor(ColorUtilities.getColorWithAlpha(
							AndroidUtils.getColorFromAttr(getContext(), R.attr.active_color_basic), 0.1f));
				}
			} else {
				convertView.setBackground(null);
			}
			AndroidUiHelper.updateVisibility(convertView.findViewById(R.id.divider), item.shouldShowTopDivider());
			View contentRow = convertView.findViewById(R.id.content_row);
			if (contentRow != null) {
				contentRow.setMinimumHeight(item.isShowCompoundBtn()
						? getContext().getResources().getDimensionPixelSize(R.dimen.card_row_min_height)
						: 0);
			}
		}
		return convertView;
	}

	@Nullable
	@Override
	public PopUpMenuItem getItem(int position) {
		return items.get(position);
	}
}
