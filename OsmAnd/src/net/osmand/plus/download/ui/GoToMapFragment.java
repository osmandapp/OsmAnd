package net.osmand.plus.download.ui;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;

public class GoToMapFragment extends DialogFragment {
	public static final String TAG = "GoToMapFragment";

	private static final String KEY_GOTO_MAP_REGION_CENTER = "key_goto_map_region_center";
	private static final String KEY_GOTO_MAP_REGION_NAME = "key_goto_map_region_name";
	private LatLon regionCenter;
	private String regionName;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		boolean isLightTheme = getMyApplication()
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme_BottomSheet
				: R.style.OsmandDarkTheme_BottomSheet;
		final Dialog dialog = new Dialog(getActivity(), themeId);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dialog.getWindow().setDimAmount(0.3f);
		dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			regionName = savedInstanceState.getString(KEY_GOTO_MAP_REGION_NAME, "");
			Object rCenterObj = savedInstanceState.getSerializable(KEY_GOTO_MAP_REGION_CENTER);
			if (rCenterObj != null) {
				regionCenter = (LatLon) rCenterObj;
			} else {
				regionCenter = new LatLon(0, 0);
			}
		}

		View view = inflater.inflate(R.layout.go_to_map_fragment, container, false);
		((ImageView) view.findViewById(R.id.titleIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));
		((TextView) view.findViewById(R.id.descriptionTextView))
				.setText(getActivity().getString(R.string.map_downloaded_descr, regionName));

		final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		view.findViewById(R.id.actionButton)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = (OsmandApplication) getActivity().getApplication();
						app.getSettings().setMapLocationToShow(regionCenter.getLatitude(), regionCenter.getLongitude(), 5, null);
						dismiss();
						MapActivity.launchMapActivityMoveToTop(getActivity());
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
		outState.putString(KEY_GOTO_MAP_REGION_NAME, regionName);
		outState.putSerializable(KEY_GOTO_MAP_REGION_CENTER, regionCenter);
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

	public static void showInstance(WorldRegion region, DownloadActivity activity) {
		GoToMapFragment fragment = new GoToMapFragment();
		fragment.regionCenter = region.getRegionCenter();
		fragment.regionName = region.getLocaleName();
		fragment.show(activity.getFragmentManager(), TAG);
	}
}
