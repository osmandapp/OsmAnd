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

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.List;

public class PopUpMenuArrayAdapter extends ArrayAdapter<PopUpMenuItem> {

	private List<PopUpMenuItem> items;
	private boolean nightMode;

	public PopUpMenuArrayAdapter(@NonNull Context context,
	                             int resource,
	                             List<PopUpMenuItem> items,
	                             boolean nightMode) {
		super(context, resource);
		this.items = items;
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
			convertView = inflater.inflate(R.layout.popup_menu_item, parent, false);
		}
		PopUpMenuItem item = getItem(position);
		if (item != null) {
			TextView tvTitle = convertView.findViewById(R.id.title);
			tvTitle.setText(item.getTitle());
			ImageView ivIcon = convertView.findViewById(R.id.icon);
			Drawable icon = item.getIcon();
			if (icon != null) {
				ivIcon.setImageDrawable(icon);
			} else {
				ivIcon.setVisibility(View.GONE);
			}
			CompoundButton radio = convertView.findViewById(R.id.radio);
			if (item.isShowCompoundBtn()) {
				UiUtilities.setupCompoundButton(nightMode, item.getCompoundBtnColor(), radio);
				radio.setVisibility(View.VISIBLE);
			} else {
				radio.setVisibility(View.GONE);
			}
			if (item.isSelected()) {
				if (item.isShowCompoundBtn()) {
					radio.setChecked(true);
				} else {
					convertView.setBackgroundColor(UiUtilities.getColorWithAlpha(
							AndroidUtils.getColorFromAttr(getContext(), R.attr.active_color_basic), 0.1f));
				}
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
