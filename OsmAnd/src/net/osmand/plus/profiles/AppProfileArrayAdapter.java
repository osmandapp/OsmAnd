package net.osmand.plus.profiles;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.system.Os;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class AppProfileArrayAdapter extends ArrayAdapter<ProfileDataObject> {

	private Activity context;
	private List<ProfileDataObject> modes;
	private int layout;
	private int colorRes;

	public AppProfileArrayAdapter(@NonNull Activity context, int resource, @NonNull List<ProfileDataObject> objects) {
		super(context, resource, objects);
		this.context = context;
		this.modes = objects;
		this.layout = resource;
	}

	public long getItemId(int position) {
		return position;
	}

	static class ViewHolder {
		public TextView title;
		public TextView description;
		public ImageView icon;
		public CompoundButton compoundButton;
	}

	@Override
	public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
		ViewHolder viewHolder;
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater layoutInflater = context.getLayoutInflater();
			rowView = layoutInflater.inflate(layout, null, true);
			viewHolder = new ViewHolder();
			viewHolder.title = (TextView) rowView.findViewById(R.id.title);
			viewHolder.description = (TextView) rowView.findViewById(R.id.description);
			viewHolder.icon = (ImageView) rowView.findViewById(R.id.icon);
			viewHolder.compoundButton = (CompoundButton) rowView.findViewById(R.id.compound_button);
			rowView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) rowView.getTag();
		}

		ProfileDataObject mode = modes.get(position);

		Drawable iconDrawable;
		if  (getMyApp(context) != null) {
			if (mode.isSelected()) {
			iconDrawable = getMyApp(context).getUIUtilities().getIcon(mode.getIconRes(),
				mode.getIconColor(!getMyApp(context).getSettings().isLightContent())
			);
			} else {
				iconDrawable = getMyApp(context).getUIUtilities()
					.getIcon(mode.getIconRes(), R.color.profile_icon_color_inactive);
			}
		} else {
			iconDrawable = context.getDrawable(mode.getIconRes());
		}

		viewHolder.title.setText(mode.getName());
		viewHolder.description.setText(mode.getDescription());
		viewHolder.icon.setImageDrawable(iconDrawable);
		viewHolder.compoundButton.setChecked(mode.isSelected());

		return rowView;
	}

	private OsmandApplication getMyApp(Activity context) {
		Application app = context.getApplication();
		if (app instanceof OsmandApplication) {
			return (OsmandApplication) app;
		} else {
			return null;
		}
	}
}
