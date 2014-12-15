package net.osmand.plus.dashboard;

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
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.helpers.FontCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on 15.12.2014.
 */
public class DashAudioVideoNotesFragment extends DashBaseFragment {
	AudioVideoNotesPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		if (plugin == null) {
			return super.onCreateView(inflater, container, savedInstanceState);
		}

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_audio_video_notes_plugin, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.notes_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		setupNotes();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void setupNotes() {
		View mainView = getView();

		List<AudioVideoNotesPlugin.Recording> notes = new ArrayList<AudioVideoNotesPlugin.Recording>(plugin.getAllRecordings());
		if (notes.size() == 0){
			(mainView.findViewById(R.id.main_notes)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_notes)).setVisibility(View.VISIBLE);
		}

		LinearLayout notesLayout = (LinearLayout) mainView.findViewById(R.id.notes);
		notesLayout.removeAllViews();
		if (notes.size() > 3){
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
			}

			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			if (recording.isAudio()){
				icon.setImageResource(R.drawable.ic_type_audio);
			} else if (recording.isVideo()){
				icon.setImageResource(R.drawable.ic_type_video);
			} else {
				icon.setImageResource(R.drawable.ic_type_img);
			}
			view.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					plugin.playRecording(getActivity(), recording);
				}
			});
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			notesLayout.addView(view);

		}
	}

}
