package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.plus.utils.ColorUtilities.getDividerColor;
import static net.osmand.plus.utils.ColorUtilities.getListBgColorId;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryTextColor;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.LinkedList;

public class MultiSelectionArrayAdapter extends ArrayAdapter<MenuObject> {

	private final MapMultiSelectionMenu menu;
	private final UiUtilities iconsCache;
	private OnClickListener listener;
	private boolean nightMode;

	MultiSelectionArrayAdapter(@NonNull MapMultiSelectionMenu menu) {
		super(menu.getMapActivity(), R.layout.menu_obj_list_item, new LinkedList<>(menu.getObjects()));
		this.menu = menu;
		MapActivity mapActivity = menu.getMapActivity();
		OsmandApplication app = mapActivity.getMyApplication();
		this.iconsCache = app.getUIUtilities();
	}

	public void setListener(OnClickListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		this.nightMode = !menu.isLight();
		MapActivity mapActivity = menu.getMapActivity();
		if (convertView == null) {
			LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
			convertView = inflater.inflate(R.layout.menu_obj_list_item, parent, false);
		}

		MenuObject item = getItem(position);
		if (mapActivity == null || item == null) {
			return convertView;
		}

		Context context = convertView.getContext();
		if (!menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(context, convertView, getListBgColorId(nightMode));
		}
		convertView.setOnClickListener(view -> onItemClicked(position));

		// Setup icon
		View iconLayout = convertView.findViewById(R.id.context_menu_icon_layout);
		ImageView ivIcon = convertView.findViewById(R.id.context_menu_icon_view);
		if (item.getPointDescription().isFavorite() || item.getPointDescription().isWpt()) {
			int iconSize = getDimension(R.dimen.favorites_my_places_icon_size);
			ivIcon.getLayoutParams().height = iconSize;
			ivIcon.getLayoutParams().width = iconSize;
			ivIcon.requestLayout();
		}
		Drawable icon = getIcon(item);
		if (icon != null) {
			ivIcon.setImageDrawable(icon);
			iconLayout.setVisibility(View.VISIBLE);
		} else {
			iconLayout.setVisibility(View.GONE);
		}

		// Text line 1
		TextView line1 = convertView.findViewById(R.id.context_menu_line1);
		line1.setTextColor(getPrimaryTextColor(context, nightMode));
		line1.setText(item.getTitleStr());

		// Text line 2
		TextView line2 = convertView.findViewById(R.id.context_menu_line2);
		line2.setTextColor(getSecondaryTextColor(context, nightMode));
		line2.setText(MenuObjectUtils.getSecondLineText(item));
		Drawable slIcon = item.getTypeIcon();
		line2.setCompoundDrawablesWithIntrinsicBounds(slIcon, null, null, null);
		line2.setCompoundDrawablePadding(AndroidUtils.dpToPx(context, 5f));

		// Divider
		View divider = convertView.findViewById(R.id.divider);
		divider.setBackgroundColor(getDividerColor(context, nightMode));
		divider.setVisibility(position != getCount() - 1 ? View.VISIBLE : View.GONE);

		return convertView;
	}

	@Nullable
	private Drawable getIcon(@NonNull MenuObject item) {
		Drawable icon = item.getRightIcon();
		if (icon == null) {
			int iconColorId = nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange;
			int iconId = item.getRightIconId();
			icon = iconId != 0 ? iconsCache.getIcon(iconId, iconColorId) : null;
		}
		return icon;
	}

	private void onItemClicked(int position) {
		if (listener != null) {
			listener.onItemClicked(position);
		}
	}

	private int getDimension(@DimenRes int dimensionResId) {
		return getContext().getResources().getDimensionPixelSize(dimensionResId);
	}

	public interface OnClickListener {
		void onItemClicked(int position);
	}
}
