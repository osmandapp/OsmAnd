package net.osmand.plus.osmedit;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

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
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		final OsmPoint child = getItem(position);
		if (v == null) {
			v = LayoutInflater.from(getContext()).inflate(R.layout.note, parent, false);
		}
		OsmEditsFragment.getOsmEditView(v, child, app);

		v.findViewById(R.id.play).setVisibility(View.GONE);

		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_local_index);
		View options = v.findViewById(R.id.options);
		if (selectionMode) {
			options.setVisibility(View.GONE);
			ch.setVisibility(View.VISIBLE);
			ch.setChecked(selectedOsmEdits.contains(child));
			v.findViewById(R.id.icon).setVisibility(View.GONE);
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectItem(ch, child);
				}
			});
		} else {
			v.findViewById(R.id.icon).setVisibility(View.VISIBLE);
			options.setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
		}

		((ImageView) options).setImageDrawable(app.getIconsCache()
				.getThemedIcon(R.drawable.ic_overflow_menu_white));
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onOptionsClick(v, child);
				}
			}
		});
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectionMode) {
					ch.setChecked(!ch.isChecked());
					selectItem(ch, child);
				} else {
					if (listener != null) {
						listener.onItemShowMap(child);
					}
				}

			}
		});
		return v;
	}

	private void selectItem(CheckBox checkBox, OsmPoint note) {
		if (checkBox.isChecked()) {
			selectedOsmEdits.add(note);
		} else {
			selectedOsmEdits.remove(note);
		}
		if (listener != null) {
			listener.onItemSelect(note);
		}
	}

	public interface OsmEditsAdapterListener {

		void onItemSelect(OsmPoint point);

		void onItemShowMap(OsmPoint point);

		void onOptionsClick(View view, OsmPoint note);
	}
}
