package net.osmand.plus.download.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.AnyRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public class DataStoragePlaceDialogFragment extends DialogFragment {

	public DataStoragePlaceDialogFragment() {
		// Required empty public constructor
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Dialog dialog = new Dialog(getActivity(), R.style.BottomSheet_Dialog);
		Window window = dialog.getWindow();
		window.setGravity(Gravity.CENTER);
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_data_storage_place_dialog, container,
				false);
		((ImageView) view.findViewById(R.id.folderIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_action_folder, R.color.map_widget_blue));
		return view;
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public Drawable getContentIcon(@DrawableRes int drawableRes) {
		return getMyApplication().getIconsCache().getContentIcon(drawableRes);
	}


	public Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return getMyApplication().getIconsCache().getIcon(drawableRes, color);
	}
}
