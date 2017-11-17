package net.osmand.plus.osmedit;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.List;

public class OsmEditsAdapter extends ArrayAdapter<OsmPoint> {

	private List<OsmPoint> osmEdits;
	private OsmandApplication app;

	private boolean selectionMode;
	private List<OsmPoint> selectedOsmEdits;

	private OsmEditsAdapterListener listener;

	public OsmEditsAdapter(OsmandApplication app, @NonNull List<OsmPoint> points) {
		super(app, R.layout.note, points);
		this.app = app;
		osmEdits = points;
	}

	public List<OsmPoint> getOsmEdits() {
		return osmEdits;
	}

	public void setOsmEdits(List<OsmPoint> osmEdits) {
		this.osmEdits = osmEdits;
		notifyDataSetChanged();
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelectedOsmEdits(List<OsmPoint> selectedOsmEdits) {
		this.selectedOsmEdits = selectedOsmEdits;
	}

	public void setAdapterListener(OsmEditsAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(getContext()).inflate(R.layout.note_list_item, parent, false);
			OsmEditViewHolder holder = new OsmEditViewHolder(view);
			view.setTag(holder);
		}
		final OsmPoint osmEdit = getItem(position);

		if (osmEdit != null) {
			final OsmEditViewHolder holder = (OsmEditViewHolder) view.getTag();

			holder.titleTextView.setText(getName(osmEdit));
			holder.descriptionTextView.setText(getDescription(osmEdit));
			Drawable icon = getIcon(osmEdit);
			if (icon != null) {
				holder.icon.setImageDrawable(icon);
			}
			if (selectionMode) {
				holder.optionsImageButton.setVisibility(View.GONE);
				holder.selectCheckBox.setVisibility(View.VISIBLE);
				holder.selectCheckBox.setChecked(selectedOsmEdits.contains(osmEdit));
				holder.icon.setVisibility(View.GONE);
				holder.selectCheckBox.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onItemSelect(osmEdit, holder.selectCheckBox.isChecked());
						}
					}
				});
			} else {
				holder.icon.setVisibility(View.VISIBLE);
				holder.optionsImageButton.setVisibility(View.VISIBLE);
				holder.selectCheckBox.setVisibility(View.GONE);
			}

			holder.optionsImageButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
			holder.optionsImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onOptionsClick(v, osmEdit);
					}
				}
			});
			holder.mainView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectionMode) {
						holder.selectCheckBox.performClick();
					} else {
						if (listener != null) {
							listener.onItemShowMap(osmEdit);
						}
					}

				}
			});
		}

		return view;
	}

	private Drawable getIcon(OsmPoint point) {
		if (point.getGroup() == OsmPoint.Group.POI) {
			return app.getIconsCache().getIcon(R.drawable.ic_type_info, R.color.color_distance);
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return app.getIconsCache().getIcon(R.drawable.ic_type_bug, R.color.color_distance);
		}
		return null;
	}

	private String getName(OsmPoint point) {
		if (point.getGroup() == OsmPoint.Group.POI) {
			return ((OpenstreetmapPoint) point).getName();
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return ((OsmNotesPoint) point).getText();
		} else {
			return "";
		}
	}

	private String getDescription(OsmPoint point) {
		String action = "";
		if (point.getAction() == OsmPoint.Action.CREATE) {
			action = getContext().getString(R.string.action_create);
		} else if (point.getAction() == OsmPoint.Action.MODIFY) {
			action = getContext().getString(R.string.action_modify);
		} else if (point.getAction() == OsmPoint.Action.DELETE) {
			action = getContext().getString(R.string.action_delete);
		} else if (point.getAction() == OsmPoint.Action.REOPEN) {
			action = getContext().getString(R.string.action_modify);
		}

		String subtype = "";
		if (point.getGroup() == OsmPoint.Group.POI && !Algorithms.isEmpty(((OpenstreetmapPoint) point).getSubtype())) {
			subtype = ((OpenstreetmapPoint) point).getSubtype();
		}

		String prefix = OsmEditingPlugin.getPrefix(point);

		String description = "";
		if (!Algorithms.isEmpty(action)) {
			description += action + " • ";
		}
		if (!Algorithms.isEmpty(subtype)) {
			description += subtype + " • ";
		}
		description += prefix;

		return description;
	}

	private class OsmEditViewHolder {
		View mainView;
		ImageView icon;
		CheckBox selectCheckBox;
		ImageButton optionsImageButton;
		TextView titleTextView;
		TextView descriptionTextView;
		View bottomDivider;

		OsmEditViewHolder(View view) {
			mainView = view;
			icon = (ImageView) view.findViewById(R.id.icon);
			selectCheckBox = (CheckBox) view.findViewById(R.id.check_box);
			optionsImageButton = (ImageButton) view.findViewById(R.id.options);
			titleTextView = (TextView) view.findViewById(R.id.title);
			descriptionTextView = (TextView) view.findViewById(R.id.description);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public interface OsmEditsAdapterListener {

		void onItemSelect(OsmPoint point, boolean checked);

		void onItemShowMap(OsmPoint point);

		void onOptionsClick(View view, OsmPoint note);
	}
}
