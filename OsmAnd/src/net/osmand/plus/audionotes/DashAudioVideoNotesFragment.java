package net.osmand.plus.audionotes;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.myplaces.FavoritesActivity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis
 * on 15.12.2014.
 */
public class DashAudioVideoNotesFragment extends DashBaseFragment {

	public static final String TAG = "DASH_NOTES_FRAGMENT";

	AudioVideoNotesPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(R.string.map_widget_av_notes);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startFavoritesActivity(AudioVideoNotesPlugin.NOTES_TAB);
			}
		});
		return view;
	}
	
	@Override
	public void onOpenDash() {
		if (plugin == null) {
			plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		}
		setupNotes();		
	}
	
	public void setupNotes() {
		View mainView = getView();
		if (plugin == null){
			mainView.setVisibility(View.GONE);
			return;
		}

		List<AudioVideoNotesPlugin.Recording> notes = new ArrayList<AudioVideoNotesPlugin.Recording>(plugin.getAllRecordings());
		if (notes.size() == 0){
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		LinearLayout notesLayout = (LinearLayout) mainView.findViewById(R.id.items);
		notesLayout.removeAllViews();
		if (notes.size() > 3){
			while (notes.size() != 3){
				notes.remove(3);
			}
		}

		for (final AudioVideoNotesPlugin.Recording recording : notes) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.note, null, false);

			getNoteView(recording, view, getMyApplication());
			((ImageView) view.findViewById(R.id.play)).setImageDrawable(getMyApplication().getIconsCache()
					.getContentIcon(R.drawable.ic_play_dark));
			view.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					plugin.playRecording(getActivity(), recording);
				}
			});
			view.findViewById(R.id.options).setVisibility(View.GONE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getMyApplication().getSettings().setMapLocationToShow(recording.getLatitude(),
							recording.getLongitude(), 15,
							new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity())),
							true, recording); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			notesLayout.addView(view);
		}
	}
	
	public static Drawable getNoteView(final AudioVideoNotesPlugin.Recording recording, View view,
								   final OsmandApplication ctx) {
		String name = recording.getName(ctx);
		TextView nameText = ((TextView) view.findViewById(R.id.name));
		nameText.setText(name);
		((TextView) view.findViewById(R.id.descr)).setText(recording.getSmallDescription(ctx));

		ImageView icon = (ImageView) view.findViewById(R.id.icon);
		Drawable iconDrawable;
		
		if (recording.isAudio()) {
			iconDrawable = ctx.getIconsCache().getIcon(R.drawable.ic_type_audio, R.color.color_distance);
		} else if (recording.isVideo()) {
			iconDrawable = ctx.getIconsCache().getIcon(R.drawable.ic_type_video, R.color.color_distance);
		} else {
			iconDrawable = ctx.getIconsCache().getIcon(R.drawable.ic_type_img, R.color.color_distance);
		}
		icon.setImageDrawable(iconDrawable);
		return iconDrawable;
	}

}
