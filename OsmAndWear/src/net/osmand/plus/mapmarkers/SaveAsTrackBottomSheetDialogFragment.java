package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

import java.util.Date;

import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.ADDED_POINTS_NUMBER_KEY;

public class SaveAsTrackBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "SaveAsTrackBottomSheetDialogFragment";
	public static final String COORDINATE_INPUT_MODE_KEY = "coordinate_input_mode_key";

	private boolean portrait;
	private MarkerSaveAsTrackFragmentListener listener;

	public void setListener(MarkerSaveAsTrackFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		boolean openFromCoordinateInput = false;
		int number = 0;
		Bundle args = getArguments();
		if (args != null) {
			openFromCoordinateInput = args.getBoolean(COORDINATE_INPUT_MODE_KEY);
			if (openFromCoordinateInput) {
				number = args.getInt(ADDED_POINTS_NUMBER_KEY);
			}
		}
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		boolean nightMode = !app.getSettings().isLightContent();
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		int textPrimaryColor = ColorUtilities.getPrimaryTextColorId(nightMode);

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_save_as_track_bottom_sheet_dialog, container);
		LinearLayout contentLayout = mainView.findViewById(R.id.content_linear_layout);
		TextView titleTv = mainView.findViewById(R.id.save_as_track_title);
		titleTv.setText(openFromCoordinateInput ? R.string.coord_input_save_as_track : R.string.marker_save_as_track);
		titleTv.setTextColor(ContextCompat.getColor(getContext(), textPrimaryColor));
		TextView descriptionTv = mainView.findViewById(R.id.save_as_track_description);
		descriptionTv.setText(openFromCoordinateInput
				? getString(R.string.coord_input_save_as_track_descr, String.valueOf(number))
				: getString(R.string.marker_save_as_track_descr));
		contentLayout.addView(getLayoutInflater().inflate(R.layout.track_name_edit_text, contentLayout, false), 2);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}
		View textBox = mainView.findViewById(R.id.name_text_box);
		if (nightMode) {
			if (textBox instanceof TextInputLayout) {
				((TextInputLayout) textBox).setHintTextAppearance(R.style.TextAppearance_App_DarkTextInputLayout);
			} else if (textBox instanceof OsmandTextFieldBoxes) {
				((OsmandTextFieldBoxes) textBox).setPrimaryColor(ContextCompat.getColor(app, R.color.active_color_primary_dark));
			}
		}

		Date date = new Date();
		String dirName = IndexConstants.GPX_INDEX_DIR + IndexConstants.MAP_MARKERS_INDEX_DIR;
		String suggestedName = app.getString(R.string.markers) + "_" + DateFormat.format("yyyy-MM-dd", date).toString();
		String uniqueFileName = FileUtils.createUniqueFileName(app, suggestedName, dirName, IndexConstants.GPX_FILE_EXT);

		EditText nameEditText = mainView.findViewById(R.id.name_edit_text);
		nameEditText.setText(uniqueFileName);
		nameEditText.setTextColor(ContextCompat.getColor(getContext(), textPrimaryColor));

		mainView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.saveGpx(nameEditText.getText().toString());
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			boolean dimensSet;

			@Override
			public void onGlobalLayout() {
				if (!dimensSet) {
					View scrollView = mainView.findViewById(R.id.marker_save_as_track_scroll_view);
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
								>= getResources().getDimension(R.dimen.bottom_sheet_content_padding_small)) {
							AndroidUtils.setBackground(getActivity(), mainView, nightMode,
									R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
						} else {
							AndroidUtils.setBackground(getActivity(), mainView, nightMode,
									R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
						}
					}
					dimensSet = true;
				}

				Window window = getDialog().getWindow();
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
		if (!portrait) {
			Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	interface MarkerSaveAsTrackFragmentListener {
		void saveGpx(String fileName);
	}
}
