package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

public class RenameMarkerBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "RenameMarkerBottomSheetDialogFragment";

	private MapMarker marker;

	public void setMarker(MapMarker marker) {
		this.marker = marker;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_rename_marker_bottom_sheet_dialog, container);

		LinearLayout contentLayout = (LinearLayout) mainView.findViewById(R.id.content_linear_layout);
		int layoutRes = Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH
				? R.layout.markers_track_name_text_field_box
				: R.layout.markers_track_name_edit_text;
		contentLayout.addView(getLayoutInflater().inflate(layoutRes, contentLayout, false), 1);

		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		final EditText nameEditText = (EditText) mainView.findViewById(R.id.name_edit_text);
		if (nightMode) {
			nameEditText.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_white));
		}
		nameEditText.setText(marker.getName(mapActivity));
		nameEditText.requestFocus();
		final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
		View textBox = mainView.findViewById(R.id.name_text_box);
		if (textBox instanceof OsmandTextFieldBoxes) {
			((OsmandTextFieldBoxes) textBox).activate(true);
		}

		mainView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String name = nameEditText.getText().toString();
				if (name.replaceAll("\\s", "").length() > 0) {
					marker.setName(name);
					mapActivity.getMyApplication().getMapMarkersHelper().updateMapMarker(marker, true);
					dismiss();
				} else {
					nameEditText.setError(getString(R.string.wrong_input));
				}
			}
		});

		mainView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			boolean dimensSet;

			@Override
			public void onGlobalLayout() {
				if (!dimensSet) {
					final View scrollView = mainView.findViewById(R.id.rename_marker_scroll_view);
					int scrollViewHeight = scrollView.getHeight();
					int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
					int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
					int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
					if (scrollViewHeight > spaceForScrollView) {
						scrollView.getLayoutParams().height = spaceForScrollView;
						scrollView.requestLayout();
					}

					if (!portrait) {
						if (screenHeight - statusBarHeight - mainView.getHeight()
								>= AndroidUtils.dpToPx(getActivity(), 8)) {
							AndroidUtils.setBackground(getActivity(), mainView, nightMode,
									R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
						} else {
							AndroidUtils.setBackground(getActivity(), mainView, nightMode,
									R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
						}
					}
					dimensSet = true;
				}

				final Window window = getDialog().getWindow();
				WindowManager.LayoutParams params = window.getAttributes();
				params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				params.gravity = Gravity.BOTTOM;
				window.setAttributes(params);
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	public void onDestroyView() {
		View view = getView();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
		Dialog dialog = getDialog();
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	public static boolean showInstance(FragmentManager fm, MapMarker marker) {
		try {
			RenameMarkerBottomSheetDialogFragment fragment = new RenameMarkerBottomSheetDialogFragment();
			fragment.setMarker(marker);
			fragment.setRetainInstance(true);
			fragment.show(fm, RenameMarkerBottomSheetDialogFragment.TAG);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
