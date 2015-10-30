package net.osmand.plus.mapcontextmenu.builders;

import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class AudioVideoNoteMenuBuilder extends MenuBuilder {

	private final Recording recording;

	public AudioVideoNoteMenuBuilder(OsmandApplication app, final Recording recording) {
		super(app);
		this.recording = recording;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void build(View view) {
		super.build(view);

		File file = recording.getFile();
		if (file != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(recording.getFile().lastModified());
			buildRow(view, R.drawable.ic_action_data, dateFormat.format(date) + " — " + timeFormat.format(date), 0);
		}

		buildPlainMenuItems(view);

		buildButtonRow(view, null, view.getResources().getString(R.string.recording_context_menu_delete), new OnClickListener() {
			@Override
			public void onClick(View v) {
				AccessibleAlertBuilder bld = new AccessibleAlertBuilder(v.getContext());
				bld.setMessage(R.string.recording_delete_confirm);
				final View fView = v;
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
						if (plugin != null) {
							plugin.deleteRecording(recording);
							if (fView.getContext() instanceof MapActivity) {
								((MapActivity)fView.getContext()).getContextMenu().close();
							}
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.show();
			}
		});
	}
}
