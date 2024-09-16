package net.osmand.plus.quickaction;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.card.icon.OnIconsPaletteListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonCard;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapButtonAppearanceFragment extends BaseOsmAndFragment implements CardListener, OnIconsPaletteListener<String> {

	public static final String TAG = MapButtonAppearanceFragment.class.getSimpleName();

	public static final String MAP_BUTTON_KEY = "map_button_key";

	private List<BaseCard> cards;
	private DialogButton applyButton;
	private MapButtonState buttonState;
	private MapButtonCard mapButtonCard;
	private MapButtonIconController iconController;

	private ButtonAppearanceParams appearanceParams;
	private ButtonAppearanceParams originalAppearanceParams;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = requireArguments();
		String key = args.getString(MAP_BUTTON_KEY);
		if (key != null) {
			buttonState = app.getMapButtonsHelper().getMapButtonStateById(key);
		}
		if (buttonState != null) {
			appearanceParams = buttonState.createAppearanceParams();
			originalAppearanceParams = buttonState.createAppearanceParams();

			if (savedInstanceState != null) {
				appearanceParams.readBundle(savedInstanceState);
			}
			iconController = MapButtonIconController.getInstance(app, buttonState, appearanceParams, this);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.map_button_appearance_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupCards(view);
		setupApplyButton(view);

		updateContent();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarSecondaryColor(view.getContext(), nightMode));

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_appearance);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(view.getContext(), nightMode));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ImageView resetButton = toolbar.findViewById(R.id.action_button);
		resetButton.setOnClickListener(v -> resetAppearance());
		resetButton.setImageDrawable(getContentIcon(R.drawable.ic_action_reset));
		resetButton.setContentDescription(getString(R.string.shared_string_reset));
		AndroidUiHelper.updateVisibility(resetButton, true);
	}

	private void setupCards(@NonNull View view) {
		cards = new ArrayList<>();
		MapActivity activity = requireMapActivity();
		ViewGroup container = view.findViewById(R.id.cards_container);
		container.removeAllViews();

		mapButtonCard = new MapButtonCard(activity, buttonState, appearanceParams);
		addCard(container, mapButtonCard);
		addCard(container, new ButtonIconsCard(activity, iconController));
		addCard(container, new CornerRadiusCard(activity, appearanceParams));
		container.addView(themedInflater.inflate(R.layout.simple_divider_item, container, false));
		addCard(container, new ButtonSizeCard(activity, appearanceParams));
		container.addView(themedInflater.inflate(R.layout.simple_divider_item, container, false));
		addCard(container, new OpacitySliderCard(activity, appearanceParams));
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build());
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			applyChanges();

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void applyChanges() {
		saveChanges(false);
		showAllModesSnackbar();
	}

	private void showAllModesSnackbar() {
		View containerView = getView();
		if (containerView != null) {
			String name = settings.getApplicationMode().toHumanString();
			String text = app.getString(R.string.changes_applied_to_profile, name);
			SpannableString message = UiUtilities.createSpannableString(text, Typeface.BOLD, name);
			Snackbar snackbar = Snackbar.make(containerView, message, Snackbar.LENGTH_LONG)
					.setAction(R.string.apply_to_all_profiles, view -> saveChanges(true));
			UiUtilities.setupSnackbarVerticalLayout(snackbar);
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}

	private void saveChanges(boolean applyToAllProfiles) {
		if (applyToAllProfiles) {
			settings.setPreferenceForAllModes(buttonState.getSizePref().getId(), appearanceParams.getSize());
			settings.setPreferenceForAllModes(buttonState.getOpacityPref().getId(), appearanceParams.getOpacity());
			settings.setPreferenceForAllModes(buttonState.getCornerRadiusPref().getId(), appearanceParams.getCornerRadius());
			settings.setPreferenceForAllModes(buttonState.getIconPref().getId(), appearanceParams.getIconName());
		} else {
			buttonState.getSizePref().set(appearanceParams.getSize());
			buttonState.getOpacityPref().set(appearanceParams.getOpacity());
			buttonState.getCornerRadiusPref().set(appearanceParams.getCornerRadius());
			buttonState.getIconPref().set(appearanceParams.getIconName());
		}
	}

	private void updateContent() {
		updateCards();
		updateButtons();
	}

	private void updateCards() {
		for (BaseCard card : cards) {
			card.update();
		}
	}

	private void updateButtons() {
		mapButtonCard.updateButton();
		applyButton.setEnabled(!Algorithms.objectEquals(originalAppearanceParams, appearanceParams));
	}

	private void resetAppearance() {
		ButtonAppearanceParams defaultParams = buttonState.createDefaultAppearanceParams();
		appearanceParams.setIconName(defaultParams.getIconName());
		appearanceParams.setSize(defaultParams.getSize());
		appearanceParams.setOpacity(defaultParams.getOpacity());
		appearanceParams.setCornerRadius(defaultParams.getCornerRadius());
		iconController.update();
		updateContent();
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		updateButtons();
	}

	@Override
	public void onIconSelectedFromPalette(@NonNull String iconName) {
		appearanceParams.setIconName(iconName);
		updateButtons();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		appearanceParams.saveToBundle(outState);
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.enableDrawer();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			MapButtonIconController.onDestroy(app);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull MapButtonState buttonState) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(MAP_BUTTON_KEY, buttonState.getId());

			MapButtonAppearanceFragment fragment = new MapButtonAppearanceFragment();
			fragment.setArguments(args);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}