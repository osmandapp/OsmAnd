package net.osmand.plus.profiles;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.List;

public class AppProfileArrayAdapter extends ArrayAdapter<ProfileDataObject> {

	private Activity context;
	private List<ProfileDataObject> modes;
	private int layout;
	private OsmandApplication app;
	private boolean isModeSelected;

	public AppProfileArrayAdapter(@NonNull Activity context, int resource, @NonNull List<ProfileDataObject> objects, boolean isModeSelected) {
		super(context, resource, objects);
		this.context = context;
		this.modes = objects;
		this.layout = resource;
		this.app = (OsmandApplication) context.getApplication();
		this.isModeSelected = isModeSelected;
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
		boolean lightContent = app.getSettings().isLightContent();
		if (mode.isSelected()) {
			iconDrawable = app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getIconColor(!lightContent));
		} else {
			iconDrawable = app.getUIUtilities().getIcon(mode.getIconRes(), lightContent);
		}

		viewHolder.title.setText(mode.getName());
		viewHolder.description.setText(mode.getDescription());
		viewHolder.icon.setImageDrawable(iconDrawable);
		if (isModeSelected) {
			viewHolder.compoundButton.setChecked(mode.isSelected());
		} else {
			viewHolder.compoundButton.setVisibility(View.GONE);
		}

		return rowView;
	}
}
