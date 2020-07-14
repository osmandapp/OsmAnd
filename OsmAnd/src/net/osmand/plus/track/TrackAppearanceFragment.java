package net.osmand.plus.track;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.DirectionArrowsCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;


public class TrackAppearanceFragment extends ContextMenuFragment {

	public static final String SELECTED_TRACK_FILE_PATH = "selected_track_file_path";

	private SelectedGpxFile selectedGpxFile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = requireMyApplication();

		String gpxFilePath = null;
		Bundle arguments = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_TRACK_FILE_PATH)) {
			gpxFilePath = savedInstanceState.getString(SELECTED_TRACK_FILE_PATH);
		}
		if (arguments != null && arguments.containsKey(SELECTED_TRACK_FILE_PATH)) {
			gpxFilePath = arguments.getString(SELECTED_TRACK_FILE_PATH);
		}
		selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			if (isPortrait()) {
				updateCardsLayout();
			}
			updateCards();
			updateButtons(view);
			if (!isPortrait()) {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
			}
			runLayoutListener();
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(SELECTED_TRACK_FILE_PATH, selectedGpxFile.getGpxFile().path);
		super.onSaveInstanceState(outState);
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
			}
		}
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.route_info_bg));
		View saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(isNightMode(), saveButton, DialogButtonType.PRIMARY, R.string.shared_string_apply);

		AndroidUiHelper.updateVisibility(saveButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			BaseCard splitIntervalCard = new SplitIntervalCard(mapActivity, selectedGpxFile);
			cardsContainer.addView(splitIntervalCard.build(mapActivity));

			BaseCard arrowsCard = new DirectionArrowsCard(mapActivity, selectedGpxFile);
			cardsContainer.addView(arrowsCard.build(mapActivity));

			TrackColoringCard trackColoringCard = new TrackColoringCard(mapActivity, selectedGpxFile);
			cardsContainer.addView(trackColoringCard.build(mapActivity));

			BaseCard width = new TrackWidthCard(mapActivity, selectedGpxFile);
			cardsContainer.addView(width.build(mapActivity));
		}
	}

	@Override
	public int getMainLayoutId() {
		return R.layout.track_appearance;
	}

	@Override
	public int getHeaderViewHeight() {
		return 0;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, TrackAppearanceFragment fragment) {
		try {
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, fragment.getFragmentTag())
					.addToBackStack(fragment.getFragmentTag())
					.commitAllowingStateLoss();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}