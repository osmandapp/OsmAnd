package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.helpers.FontCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on 15.12.2014.
 */
public class DashAudioVideoNotesFragment extends DashBaseFragment {
	AudioVideoNotesPlugin plugin;
	boolean allNotes;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_audio_video_notes_plugin, container, false);
		allNotes = getActivity() instanceof DashAudioVideoNotesActivity;
		if (allNotes) {
			view.findViewById(R.id.header).setVisibility(View.GONE);
		} else {
			Typeface typeface = FontCache.getRobotoMedium(getActivity());
			((TextView) view.findViewById(R.id.notes_text)).setTypeface(typeface);
			((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);

			(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Activity activity = getActivity();
					final Intent favorites = new Intent(activity, DashAudioVideoNotesActivity.class);
					favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					activity.startActivity(favorites);
				}
			});
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (plugin == null) {
			plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		}
		setupNotes();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void setupNotes() {
		View mainView = getView();
		if (mainView == null) {
			return;
		}

		if (plugin == null){
			(mainView.findViewById(R.id.main_notes)).setVisibility(View.GONE);
			return;
		}

		List<AudioVideoNotesPlugin.Recording> notes = new ArrayList<AudioVideoNotesPlugin.Recording>(plugin.getAllRecordings());
		if (notes.size() == 0){
			(mainView.findViewById(R.id.main_notes)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_notes)).setVisibility(View.VISIBLE);
		}

		LinearLayout notesLayout = (LinearLayout) mainView.findViewById(R.id.notes);
		notesLayout.removeAllViews();
		if (notes.size() > 3 && !allNotes){
			while (notes.size() != 3){
				notes.remove(3);
			}
		}

		for (final AudioVideoNotesPlugin.Recording recording : notes){
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_note_item, null, false);

			if (recording.name != null){
				((TextView) view.findViewById(R.id.name)).setText(recording.name);
				((TextView) view.findViewById(R.id.descr)).setText(recording.getDescription(getActivity()));
			} else {
				((TextView) view.findViewById(R.id.name)).setText(recording.getDescription(getActivity()));
				view.findViewById(R.id.descr).setVisibility(View.GONE);
			}

			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			if (recording.isAudio()){
				icon.setImageResource(R.drawable.ic_type_audio);
			} else if (recording.isVideo()){
				icon.setImageResource(R.drawable.ic_type_video);
			} else {
				icon.setImageResource(R.drawable.ic_type_img);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getMyApplication().getSettings().setMapLocationToShow(recording.getLatitude(), recording.getLongitude(), 15, null,
							recording.name != null ? recording.name : recording.getDescription(getActivity()),
							recording); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			view.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					plugin.playRecording(getActivity(), recording);
				}
			});

			//int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

			//LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			//view.setLayoutParams(lp);
			notesLayout.addView(view);

		}
	}

}
