package net.osmand.plus.views.mapwidgets.configure.buttons;

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
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.quickaction.ButtonSizeCard;
import net.osmand.plus.quickaction.CornerRadiusCard;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.OpacitySliderCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class DefaultButtonsAppearanceFragment extends BaseFullScreenFragment implements CardListener {

	public static final String TAG = DefaultButtonsAppearanceFragment.class.getSimpleName();

	private CommonPreference<Integer> defaultSizePref;
	private CommonPreference<Float> defaultOpacityPref;
	private CommonPreference<Integer> defaultCornerRadiusPref;

	private ButtonAppearanceParams appearanceParams;
	private ButtonAppearanceParams originalAppearanceParams;

	private MapHudCard mapHudCard;
	private List<BaseCard> cards;
	private DialogButton applyButton;

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

		MapButtonsHelper helper = app.getMapButtonsHelper();
		defaultSizePref = helper.getDefaultSizePref();
		defaultOpacityPref = helper.getDefaultOpacityPref();
		defaultCornerRadiusPref = helper.getDefaultCornerRadiusPref();

		appearanceParams = createAppearanceParams();
		originalAppearanceParams = createAppearanceParams();

		if (savedInstanceState != null) {
			appearanceParams.readBundle(savedInstanceState);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.map_button_appearance_fragment, container, false);
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
		title.setText(R.string.default_appearance);
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

		ViewGroup container = view.findViewById(R.id.cards_container);
		container.removeAllViews();

		MapActivity activity = requireMapActivity();
		mapHudCard = new MapHudCard(activity, appearanceParams);
		addCard(container, mapHudCard);
		addCard(container, new CornerRadiusCard(activity, appearanceParams, true));
		container.addView(inflate(R.layout.list_item_divider, container, false));
		addCard(container, new ButtonSizeCard(activity, appearanceParams, true));
		container.addView(inflate(R.layout.list_item_divider, container, false));
		addCard(container, new OpacitySliderCard(activity, appearanceParams, true));
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build());
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			saveChanges(false);
			showAllModesSnackbar();

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
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
			settings.setPreferenceForAllModes(defaultSizePref.getId(), appearanceParams.getSize());
			settings.setPreferenceForAllModes(defaultOpacityPref.getId(), appearanceParams.getOpacity());
			settings.setPreferenceForAllModes(defaultCornerRadiusPref.getId(), appearanceParams.getCornerRadius());
		} else {
			defaultSizePref.set(appearanceParams.getSize());
			defaultOpacityPref.set(appearanceParams.getOpacity());
			defaultCornerRadiusPref.set(appearanceParams.getCornerRadius());
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
		applyButton.setEnabled(!Algorithms.objectEquals(originalAppearanceParams, appearanceParams));
	}

	private void resetAppearance() {
		appearanceParams.setSize(defaultSizePref.getDefaultValue());
		appearanceParams.setOpacity(defaultOpacityPref.getDefaultValue());
		appearanceParams.setCornerRadius(defaultCornerRadiusPref.getDefaultValue());
		updateContent();
	}

	@NonNull
	private ButtonAppearanceParams createAppearanceParams() {
		return new ButtonAppearanceParams(null, defaultSizePref.get(),
				defaultOpacityPref.get(), defaultCornerRadiusPref.get());
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		updateButtons();

		if (mapHudCard != null) {
			mapHudCard.updateContent();
		}
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
	public void onDestroyView() {
		super.onDestroyView();

		if (mapHudCard != null) {
			mapHudCard.clearWidgets();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DefaultButtonsAppearanceFragment fragment = new DefaultButtonsAppearanceFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}