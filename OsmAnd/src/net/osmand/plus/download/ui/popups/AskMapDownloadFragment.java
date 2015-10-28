package net.osmand.plus.download.ui.popups;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;

public class AskMapDownloadFragment extends DialogFragment {
	public static final String TAG = "AskMapDownloadFragment";

	private static final String KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME = "key_ask_map_download_item_filename";
	private IndexItem indexItem;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		boolean isLightTheme = getMyApplication()
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme_BottomSheet
				: R.style.OsmandDarkTheme_BottomSheet;
		final Dialog dialog = new Dialog(getActivity(), themeId);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			String itemFileName = savedInstanceState.getString(KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME);
			if (itemFileName != null) {
				indexItem = getMyApplication().getDownloadThread().getIndexes().getIndexItem(itemFileName);
			}
		}

		View view = inflater.inflate(R.layout.ask_map_download_fragment, container, false);
		((ImageView) view.findViewById(R.id.titleIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));

		Button actionButtonOk = (Button) view.findViewById(R.id.actionButtonOk);

		String titleText = null;
		String descriptionText = null;

		if (indexItem != null) {
			if (indexItem.getBasename().equalsIgnoreCase(WorldRegion.WORLD_BASEMAP)) {
				titleText = getString(R.string.index_item_world_basemap);
				descriptionText = getString(R.string.world_map_download_descr);
			}

			actionButtonOk.setText(getString(R.string.shared_string_download) + " (" + indexItem.getSizeDescription(getActivity()) + ")");
		}

		if (titleText != null) {
			((TextView) view.findViewById(R.id.titleTextView))
					.setText(titleText);
		}
		if (descriptionText != null) {
			((TextView) view.findViewById(R.id.descriptionTextView))
					.setText(descriptionText);
		}

		final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		actionButtonOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (indexItem != null) {
					((DownloadActivity) getActivity()).startDownload(indexItem);
					dismiss();
				}
			}
		});

		view.findViewById(R.id.actionButtonCancel)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				});

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		final Window window = getDialog().getWindow();
		WindowManager.LayoutParams params = window.getAttributes();
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.gravity = Gravity.BOTTOM;
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		window.setAttributes(params);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (indexItem != null) {
			outState.putString(KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME, indexItem.getFileName());
		}
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return getMyApplication().getIconsCache().getIcon(drawableRes, color);
	}

	private Drawable getContentIcon(@DrawableRes int drawableRes) {
		return getMyApplication().getIconsCache().getContentIcon(drawableRes);
	}

	public static void showInstance(IndexItem indexItem, DownloadActivity activity) {
		AskMapDownloadFragment fragment = new AskMapDownloadFragment();
		fragment.indexItem = indexItem;
		fragment.show(activity.getFragmentManager(), TAG);
	}
}
