package net.osmand.plus.download.ui;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;

public class DeletedItemsCountViewHolder {

	protected final OsmandApplication app;
	protected final View view;
	protected final TextView tvName;
	protected final TextView tvDesc;
	protected final AppCompatImageView icon;

	protected final DownloadActivity context;

	public DeletedItemsCountViewHolder(@NonNull View view, @NonNull DownloadActivity context) {
		this.context = context;
		this.app = context.getApp();
		this.view = view;
		tvDesc = view.findViewById(R.id.description);
		tvName = view.findViewById(R.id.title);
		icon = view.findViewById(R.id.icon);
		icon.setImageTintList(null);

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		int textColorPrimary = typedValue.data;
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		int textColorSecondary = typedValue.data;
		tvName.setTextColor(textColorPrimary);
		tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		tvDesc.setTextColor(textColorSecondary);
	}

	public void bindItem(int mapsCount) {
		String name;
		name = app.getString(R.string.unsupported_maps);
		tvName.setText(name);
		tvDesc.setText(String.valueOf(mapsCount));
		icon.setImageDrawable(AppCompatResources.getDrawable(app, R.drawable.ic_action_warning_yellow_colored));
	}
}