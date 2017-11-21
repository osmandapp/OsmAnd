package net.osmand.plus.audionotes;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public class ItemMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ItemMenuBottomSheetDialogFragment";

	private ItemMenuFragmentListener listener;
	private Recording recording;

	public void setListener(ItemMenuFragmentListener listener) {
		this.listener = listener;
	}

	public void setRecording(Recording recording) {
		this.recording = recording;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_notes_item_menu_bottom_sheet_dialog, null);

		if (recording != null) {
			NumberFormat f = new DecimalFormat("#0.00000");
			((TextView) mainView.findViewById(R.id.title_text_view))
					.setText(recording.getName(getActivity(), true));
			((TextView) mainView.findViewById(R.id.play_text_view))
					.setText(recording.isPhoto() ? R.string.watch : R.string.recording_context_menu_play);
			((TextView) mainView.findViewById(R.id.show_on_map_descr_text_view))
					.setText(f.format(recording.getLatitude()) + ", " + f.format(recording.getLongitude()));
			((ImageView) mainView.findViewById(R.id.play_icon))
					.setImageDrawable(getContentIcon(recording.isPhoto() ? R.drawable.ic_action_view : R.drawable.ic_play_dark));
		}

		((ImageView) mainView.findViewById(R.id.share_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_gshare_dark));
		((ImageView) mainView.findViewById(R.id.show_on_map_icon)).setImageDrawable(getContentIcon(R.drawable.ic_show_on_map));
		((ImageView) mainView.findViewById(R.id.rename_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
		((ImageView) mainView.findViewById(R.id.delete_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));

		mainView.findViewById(R.id.play_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null && recording != null) {
					listener.playOnClick(recording);
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.share_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null && recording != null) {
					listener.shareOnClick(recording);
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.show_on_map_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null && recording != null) {
					listener.showOnMapOnClick(recording);
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.rename_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null && recording != null) {
					listener.renameOnClick(recording);
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.delete_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null && recording != null) {
					listener.deleteOnClick(recording);
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	interface ItemMenuFragmentListener {

		void playOnClick(Recording recording);

		void shareOnClick(Recording recording);

		void showOnMapOnClick(Recording recording);

		void renameOnClick(Recording recording);

		void deleteOnClick(Recording recording);
	}
}
