package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetListener;
import net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetsFragment.GroupItem;

public class SearchWidgetViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView count;
	private final ImageView icon;
	private final AppCompatImageView proIcon;

	public SearchWidgetViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		count = itemView.findViewById(R.id.count);
		icon = itemView.findViewById(R.id.icon);
		proIcon = itemView.findViewById(R.id.pro_icon);
	}

	public void bind(@NonNull OsmandApplication app, @NonNull SearchWidgetListener listener, WidgetType widgetType, boolean nightMode) {
		title.setText(widgetType.titleId);
		icon.setImageResource(widgetType.getIconId(nightMode));
		AndroidUiHelper.updateVisibility(count, false);
		AndroidUiHelper.updateVisibility(proIcon, !widgetType.isPurchased(app));

		itemView.setOnClickListener(view -> listener.widgetSelected(widgetType));
	}

	public void bind(@NonNull SearchWidgetListener listener, GroupItem groupItem, boolean nightMode) {
		title.setText(groupItem.group().titleId);
		count.setText(String.valueOf(groupItem.count()));
		AndroidUiHelper.updateVisibility(count, true);
		AndroidUiHelper.updateVisibility(proIcon, false);
		icon.setImageResource(groupItem.group().getIconId(nightMode));

		itemView.setOnClickListener(view -> listener.groupSelected(groupItem.group()));
	}

	public void bind(@NonNull OsmandApplication app, @NonNull WidgetIconsHelper iconsHelper, @NonNull SearchWidgetListener listener, @NonNull MapWidgetInfo widgetInfo) {
		title.setText(widgetInfo.getTitle(app));
		AndroidUiHelper.updateVisibility(count, false);
		AndroidUiHelper.updateVisibility(proIcon, false);

		iconsHelper.updateWidgetIcon(icon, widgetInfo);

		itemView.setOnClickListener(view -> listener.externalWidgetSelected(widgetInfo));
	}
}