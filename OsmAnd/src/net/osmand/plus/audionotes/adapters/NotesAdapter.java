package net.osmand.plus.audionotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.audionotes.NotesFragment;

import java.util.List;

public class NotesAdapter extends ArrayAdapter<Recording> {

	private OsmandApplication app;
	private NotesAdapterListener listener;

	private boolean selectionMode;
	private List<Recording> selected;

	public void setListener(NotesAdapterListener listener) {
		this.listener = listener;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelected(List<Recording> selected) {
		this.selected = selected;
	}

	public NotesAdapter(OsmandApplication app, List<Recording> items) {
		super(app, R.layout.note, items);
		this.app = app;
	}

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row = convertView;
		if (row == null) {
			row = inflater.inflate(R.layout.note, parent, false);
		}

		final Recording recording = getItem(position);
		if (recording == NotesFragment.SHARE_LOCATION_FILE) {
			((TextView) row.findViewById(R.id.name)).setText(R.string.av_locations);
			((TextView) row.findViewById(R.id.description)).setText(R.string.av_locations_descr);
		} else {
			DashAudioVideoNotesFragment.getNoteView(recording, row, app);
		}
		row.findViewById(R.id.play).setVisibility(View.GONE);

		final CheckBox ch = (CheckBox) row.findViewById(R.id.check_local_index);
		ImageButton options = (ImageButton) row.findViewById(R.id.options);
		options.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
		if (selectionMode) {
			options.setVisibility(View.GONE);
			ch.setVisibility(View.VISIBLE);
			ch.setChecked(selected.contains(recording));
			row.findViewById(R.id.icon).setVisibility(View.GONE);
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onItemClick(recording, ch.isChecked());
					}
				}
			});
		} else {
			row.findViewById(R.id.icon).setVisibility(View.VISIBLE);
			options.setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
		}

		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onOptionsClick(recording);
				}
			}
		});
		row.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !ch.isChecked();
				ch.setChecked(checked);
				if (listener != null) {
					listener.onItemClick(recording, checked);
				}
			}
		});
		return row;
	}

	public interface NotesAdapterListener {

		void onItemClick(Recording rec, boolean checked);

		void onOptionsClick(Recording rec);
	}
}
