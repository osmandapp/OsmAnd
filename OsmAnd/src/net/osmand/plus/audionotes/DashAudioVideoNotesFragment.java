package net.osmand.plus.audionotes;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.myplaces.FavoritesActivity;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_audio_video_notes_plugin, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.notes_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				Class<FavoritesActivity> fa = getMyApplication().getAppCustomization().getFavoritesActivity();
				final Intent favorites = new Intent(activity, fa);
				favorites.putExtra("TAB", "AUDIO");
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.NOTES_TAB);
				activity.startActivity(favorites);
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

		LinearLayout notesLayout = (LinearLayout) mainView.findViewById(R.id.notes);
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
