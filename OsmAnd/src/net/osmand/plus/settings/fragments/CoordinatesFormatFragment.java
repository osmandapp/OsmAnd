package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.osmand.plus.R;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.coordinates.CoordinateFormat;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.List;

public class CoordinatesFormatFragment extends BaseCoordinateFormatFragment
		implements SelectCopyAppModeBottomSheet.CopyAppModePrefsListener {

	public static final String TAG = CoordinatesFormatFragment.class.getSimpleName();
	public static final String SETTINGS_PREF_ID = "coordinate_formats";

	private LinearLayout contentContainer;
	private FloatingActionButton fab;
	private List<String> lastRenderedIds;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = LayoutInflater.from(getMaterialThemedContext())
				.inflate(R.layout.coordinate_format_fragment, container, false);
		contentContainer = view.findViewById(R.id.content_container);
		fab = view.findViewById(R.id.fab);
		setupToolbar(view);
		renderMainScreen();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (contentContainer != null && !formatPreferences.getPreferredIds(appMode).equals(lastRenderedIds)) {
			renderMainScreen();
		}
	}

	private void setupToolbar(@NonNull View view) {
		TextView title = view.findViewById(R.id.toolbar_title);
		title.setText(R.string.coordinates_format);
		ImageButton closeButton = view.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getDefaultIconColorId(nightMode)));
		closeButton.setOnClickListener(v -> dismiss());
		ImageButton editButton = view.findViewById(R.id.action_edit);
		editButton.setOnClickListener(v -> CoordinatesFormatEditFragment.show(requireMyActivity(), appMode));
		ImageButton overflowButton = view.findViewById(R.id.action_overflow);
		overflowButton.setOnClickListener(this::showOverflowMenu);
	}

	private void renderMainScreen() {
		lastRenderedIds = formatPreferences.getPreferredIds(appMode);
		contentContainer.removeAllViews();

		fab.setImageDrawable(getIcon(R.drawable.ic_action_add_no_bg, ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode)));
		fab.setOnClickListener(v -> AddCoordinateFormatFragment.show(requireMyActivity(), appMode, false, false, null));

		TextView description = createText(R.string.coordinate_format_description, 16, 18, 16, 18);
		description.setTextSize(16);
		description.setTextColor(AndroidUtils.getColorFromAttr(getMaterialThemedContext(), android.R.attr.textColorPrimary));
		contentContainer.addView(description);

		List<CoordinateFormat> formats = resolveFormats(formatPreferences.getPreferredIds(appMode));
		if (formats.isEmpty()) {
			return;
		}

		MaterialCardView card = createCard(0, 0, 0, 0);
		LinearLayout list = createVerticalContainer();
		card.addView(list);
		contentContainer.addView(card);

		for (int i = 0; i < formats.size(); i++) {
			CoordinateFormat format = formats.get(i);
			list.addView(createFormatRow(format, false, i == 0, i < formats.size() - 1, v -> {
			}));
		}
	}

	private TextView createText(int textRes, int start, int top, int end, int bottom) {
		Context themedContext = getMaterialThemedContext();
		TextView text = new TextView(themedContext);
		text.setText(textRes);
		text.setPadding(dp(start), dp(top), dp(end), dp(bottom));
		text.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return text;
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromMode) {
		formatPreferences.copyPreferredIds(fromMode, appMode);
		renderMainScreen();
	}

	private void resetToDefault() {
		formatPreferences.resetPreferredIds(appMode);
		renderMainScreen();
	}

	private void showOverflowMenu(@NonNull View anchor) {
		PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
		MenuBuilder menuBuilder = (MenuBuilder) popupMenu.getMenu();
		menuBuilder.setOptionalIconsVisible(true);
		popupMenu.getMenu().add(0, R.string.reset_to_default, 0, R.string.reset_to_default)
				.setIcon(getContentIcon(R.drawable.ic_action_reset));
		popupMenu.getMenu().add(0, R.string.copy_from_other_profile, 1, R.string.copy_from_other_profile)
				.setIcon(getContentIcon(R.drawable.ic_action_copy));
		popupMenu.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.string.reset_to_default) {
				resetToDefault();
				return true;
			} else if (item.getItemId() == R.string.copy_from_other_profile) {
				SelectCopyAppModeBottomSheet.showInstance(getParentFragmentManager(), this, appMode);
				return true;
			}
			return false;
		});
		popupMenu.show();
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack();
		}
	}

	public static void showAddFormat(@NonNull FragmentActivity activity, @NonNull ApplicationMode appMode) {
		AddCoordinateFormatFragment.show(activity, appMode, false, true, null);
	}
}