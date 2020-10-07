package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public class MultiSelectionArrayAdapter extends ArrayAdapter<MapMultiSelectionMenu.MenuObject> {

	private MapMultiSelectionMenu menu;
	private OnClickListener listener;

	MultiSelectionArrayAdapter(@NonNull MapMultiSelectionMenu menu, int resource, @NonNull List<MapMultiSelectionMenu.MenuObject> objects) {
		super(menu.getMapActivity(), resource, objects);
		this.menu = menu;
	}

	public void setListener(OnClickListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = menu.getMapActivity().getLayoutInflater().inflate(R.layout.menu_obj_list_item, parent, false);
		}
		final MapMultiSelectionMenu.MenuObject item = getItem(position);
		if (item != null) {
			if (!menu.isLandscapeLayout()) {
				AndroidUtils.setBackground(convertView.getContext(), convertView, !menu.isLight(),
						R.color.list_background_color_light, R.color.list_background_color_dark);
			}
			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onClick(position);
					}
				}
			});
			UiUtilities iconsCache = menu.getMapActivity().getMyApplication().getUIUtilities();
			final View iconLayout = convertView.findViewById(R.id.context_menu_icon_layout);
			final ImageView iconView = (ImageView) convertView.findViewById(R.id.context_menu_icon_view);
			if (item.getPointDescription().isFavorite() || item.getPointDescription().isWpt()) {
				int iconSize = getContext().getResources().getDimensionPixelSize(R.dimen.favorites_my_places_icon_size);
				iconView.getLayoutParams().height = iconSize;
				iconView.getLayoutParams().width = iconSize;
				iconView.requestLayout();
			}
			Drawable icon = item.getRightIcon();
			int iconId = item.getRightIconId();
			if (icon != null) {
				iconView.setImageDrawable(icon);
				iconLayout.setVisibility(View.VISIBLE);
			} else if (iconId != 0) {
				iconView.setImageDrawable(iconsCache.getIcon(iconId,
						menu.isLight() ? R.color.osmand_orange : R.color.osmand_orange_dark));
				iconLayout.setVisibility(View.VISIBLE);
			} else {
				iconLayout.setVisibility(View.GONE);
			}

			// Text line 1
			TextView line1 = (TextView) convertView.findViewById(R.id.context_menu_line1);
			line1.setTextColor(ContextCompat.getColor(getContext(),
					!menu.isLight() ? R.color.text_color_primary_dark : R.color.text_color_primary_light));
			line1.setText(item.getTitleStr());

			// Text line 2
			TextView line2 = (TextView) convertView.findViewById(R.id.context_menu_line2);
			((TextView) line2).setTextColor(ContextCompat.getColor(getContext(), R.color.ctx_menu_subtitle_color));
			StringBuilder line2Str = new StringBuilder(item.getTypeStr());
			String streetStr = item.getStreetStr();
			if (!Algorithms.isEmpty(streetStr) && !item.displayStreetNameInTitle()) {
				if (line2Str.length() > 0) {
					line2Str.append(", ");
				}
				line2Str.append(streetStr);
			}
			line2.setText(line2Str);
			Drawable slIcon = item.getTypeIcon();
			line2.setCompoundDrawablesWithIntrinsicBounds(slIcon, null, null, null);
			line2.setCompoundDrawablePadding(AndroidUtils.dpToPx(menu.getMapActivity(), 5f));
			// Divider
			View divider = convertView.findViewById(R.id.divider);
			divider.setBackgroundColor(ContextCompat.getColor(getContext(), menu.isLight() ? R.color.multi_selection_menu_divider_light : R.color.multi_selection_menu_divider_dark));
			divider.setVisibility(position != getCount() - 1 ? View.VISIBLE : View.GONE);
		}

		return convertView;
	}

	public interface OnClickListener {
		void onClick(int position);
	}
}
