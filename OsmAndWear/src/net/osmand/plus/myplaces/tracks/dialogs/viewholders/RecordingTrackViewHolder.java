package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.Collections;

public class RecordingTrackViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final UiUtilities uiUtilities;
	protected final SavingTrackHelper savingTrackHelper;

	protected final TrackSelectionListener selectionListener;
	protected final RecordingTrackListener recordingListener;

	protected final ImageView icon;
	protected final TextView title;
	protected final TextView description;
	protected final View menuButton;
	protected final DialogButton saveButton;
	protected final DialogButton actionButton;

	protected final boolean nightMode;

	public RecordingTrackViewHolder(@NonNull View view, @Nullable TrackSelectionListener selectionListener, @Nullable RecordingTrackListener recordingListener, boolean nightMode) {
		super(view);
		this.selectionListener = selectionListener;
		this.recordingListener = recordingListener;
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		savingTrackHelper = app.getSavingTrackHelper();

		saveButton = view.findViewById(R.id.save_button);
		actionButton = view.findViewById(R.id.action_button);

		View header = view.findViewById(R.id.container);
		icon = header.findViewById(R.id.icon);
		title = header.findViewById(R.id.title);
		description = header.findViewById(R.id.description);
		menuButton = header.findViewById(R.id.menu_button);

		AndroidUiHelper.updateVisibility(header.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(header.findViewById(R.id.direction_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.checkbox_container), false);
	}

	public void bindView(@NonNull TrackItem trackItem) {
		boolean selected = selectionListener != null && selectionListener.isTrackItemSelected(trackItem);
		itemView.setOnClickListener(v -> {
			if (selectionListener != null) {
				selectionListener.onTrackItemsSelected(Collections.singleton(trackItem), !selected);
			}
		});
		menuButton.setOnClickListener(v -> {
			if (selectionListener != null) {
				selectionListener.onTrackItemOptionsSelected(v, trackItem);
			}
		});
		boolean hasDataToSave = savingTrackHelper.hasDataToSave();
		setupActionButton(hasDataToSave);
		setupSaveButton(hasDataToSave);
		setupDescription(hasDataToSave);
	}

	private void setupActionButton(boolean hasDataToSave) {
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
			icon.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_track_recordable));
			setupButton(actionButton, R.string.shared_string_control_stop, R.string.gpx_monitoring_stop, R.drawable.ic_action_rec_stop);
		} else {
			icon.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_polygom_dark));
			setupButton(actionButton, R.string.shortcut_start_recording, R.string.gpx_monitoring_start, R.drawable.ic_action_rec_start);
		}
		title.setText(hasDataToSave ? R.string.shared_string_currently_recording_track : R.string.shared_string_new_track);
		actionButton.setOnClickListener(v -> {
			if (recordingListener != null) {
				recordingListener.toggleTrackRecording();
			}
		});
	}

	private void setupSaveButton(boolean hasDataToSave) {
		setupButton(saveButton, R.string.shared_string_save, R.string.save_current_track, R.drawable.ic_action_gsave_dark);
		saveButton.setOnClickListener(v -> {
			if (recordingListener != null) {
				recordingListener.saveTrackRecording();
			}
		});
		AndroidUiHelper.updateVisibility(saveButton, hasDataToSave);
	}

	private void setupDescription(boolean hasDataToSave) {
		if (hasDataToSave) {
			String distance = OsmAndFormatter.getFormattedDistance(savingTrackHelper.getDistance(), app);
			String duration = OsmAndFormatter.getFormattedDurationShort((int) (savingTrackHelper.getDuration() / 1000));
			String pointsCount = String.valueOf(savingTrackHelper.getPoints());
			description.setText(app.getString(R.string.ltr_or_rtl_triple_combine_via_bold_point, distance, duration, pointsCount));
		} else {
			description.setText(R.string.track_not_recorded);
		}
	}

	private void setupButton(@NonNull DialogButton button, @StringRes int titleId, @StringRes int descriptionId, @DrawableRes int iconId) {
		Context context = button.getContext();
		button.setTitleId(titleId);
		button.setIconId(iconId);
		button.setContentDescription(context.getString(descriptionId));

		int paddingHalf = context.getResources().getDimensionPixelSize(R.dimen.content_padding_half);
		int paddingSmall = context.getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		TextView title = button.findViewById(R.id.button_text);
		title.setCompoundDrawablePadding(paddingSmall);
		AndroidUtils.setPadding(title, paddingHalf, 0, paddingSmall, 0);
	}

	public interface RecordingTrackListener {

		void saveTrackRecording();

		void toggleTrackRecording();
	}
}
