package net.osmand.plus.audionotes;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 15.12.2014.
 */
public class DashAudioVideoNotesFragment extends DashBaseFragment {

	public static final String TAG = "DASH_NOTES_FRAGMENT";
	public static final int TITLE_ID = R.string.map_widget_av_notes;
	private static final String ROW_NUMBER_TAG = TAG + "_row_number";
	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			TAG, DashAudioVideoNotesFragment.class, SHOULD_SHOW_FUNCTION, 100, ROW_NUMBER_TAG);


	AudioVideoNotesPlugin plugin;

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startFavoritesActivity(AudioVideoNotesPlugin.NOTES_TAB);
				closeDashboard();
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
		if (plugin == null) {
			mainView.setVisibility(View.GONE);
			return;
		}

		List<AudioVideoNotesPlugin.Recording> notes = new ArrayList<AudioVideoNotesPlugin.Recording>(plugin.getAllRecordings());
		if (notes.size() == 0) {
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		LinearLayout notesLayout = (LinearLayout) mainView.findViewById(R.id.items);
		notesLayout.removeAllViews();
		DashboardOnMap.handleNumberOfRows(notes, getMyApplication().getSettings(), ROW_NUMBER_TAG);

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
							new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity(), true)),
							true, recording); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			notesLayout.addView(view);
		}
	}

	public static Drawable getNoteView(final AudioVideoNotesPlugin.Recording recording, View view,
									   final OsmandApplication ctx) {
		String name = recording.getName(ctx, true);
		TextView nameText = ((TextView) view.findViewById(R.id.name));
		nameText.setText(name);
		((TextView) view.findViewById(R.id.description)).setText(recording.getSmallDescription(ctx));

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
