package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
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
			Integer titleColor = item.getTitleColor();
			if (titleColor != null) {
				tvTitle.setTextColor(titleColor);
			}
			ImageView ivIcon = convertView.findViewById(R.id.icon);
			Drawable icon = item.getIcon();
			if (icon != null) {
				ivIcon.setImageDrawable(icon);
			} else {
				ivIcon.setVisibility(View.GONE);
			}
			CompoundButton radio = convertView.findViewById(R.id.compound_button);
			if (item.isShowCompoundBtn()) {
				UiUtilities.setupCompoundButton(nightMode, item.getCompoundBtnColor(), radio);
			}
			AndroidUiHelper.updateVisibility(radio, item.isShowCompoundBtn());
			if (item.isSelected()) {
				if (item.isShowCompoundBtn()) {
					radio.setChecked(true);
				} else {
					convertView.setBackgroundColor(ColorUtilities.getColorWithAlpha(
							AndroidUtils.getColorFromAttr(getContext(), R.attr.active_color_basic), 0.1f));
				}
			}
			AndroidUiHelper.updateVisibility(convertView.findViewById(R.id.divider), item.shouldShowTopDivider());
		}
		return convertView;
	}

	@Nullable
	@Override
	public PopUpMenuItem getItem(int position) {
		return items.get(position);
	}
}
