package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.coordinates.CoordinateFormat;
import net.osmand.plus.settings.coordinates.CoordinateFormatHelper;
import net.osmand.plus.settings.coordinates.CoordinateFormatSettingsStorage;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public abstract class BaseCoordinateFormatFragment extends BaseFullScreenFragment {

	protected CoordinateFormatSettingsStorage formatPreferences;
	protected CoordinateFormatHelper coordinateFormatHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		formatPreferences = settings.getCoordinateFormatSettingsStorage();
		coordinateFormatHelper = app.getCoordinateFormatHelper();
	}

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	protected Context getMaterialThemedContext() {
		return UiUtilities.getThemedContext(requireContext(), nightMode,
				R.style.OsmandMaterialLightTheme, R.style.OsmandMaterialDarkTheme);
	}

	protected int dp(float value) {
		return AndroidUtils.dpToPx(app, value);
	}

	@NonNull
	protected List<CoordinateFormat> resolveFormats(@NonNull List<String> ids) {
		return coordinateFormatHelper.resolveFormats(ids);
	}

	@NonNull
	protected String getFormatSummary(@NonNull CoordinateFormat format) {
		return coordinateFormatHelper.getFormatSummary(format);
	}

	@NonNull
	protected MaterialCardView createCard(int marginStart, int marginTop, int marginEnd, int marginBottom) {
		Context themedContext = getMaterialThemedContext();
		MaterialCardView card = new MaterialCardView(themedContext);
		card.setCardElevation(0);
		card.setRadius(dp(12));
		card.setStrokeWidth(0);
		card.setUseCompatPadding(false);
		card.setCardBackgroundColor(AndroidUtils.getColorFromAttr(themedContext, R.attr.list_background_color));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(dp(marginStart), dp(marginTop), dp(marginEnd), dp(marginBottom));
		card.setLayoutParams(params);
		return card;
	}

	@NonNull
	protected LinearLayout createVerticalContainer() {
		LinearLayout layout = new LinearLayout(getMaterialThemedContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return layout;
	}

	@NonNull
	protected TextView createTitleText(@NonNull String text) {
		Context themedContext = getMaterialThemedContext();
		TextView textView = new TextView(themedContext);
		textView.setText(text);
		textView.setTextColor(AndroidUtils.getColorFromAttr(themedContext, android.R.attr.textColorPrimary));
		textView.setTextSize(16);
		textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		return textView;
	}

	@NonNull
	protected View createFormatRow(@NonNull CoordinateFormat format, boolean addRow, boolean primary,
	                               boolean showDivider, @NonNull View.OnClickListener clickListener) {
		View row = LayoutInflater.from(getMaterialThemedContext())
				.inflate(R.layout.coordinate_format_settings_item, null, false);
		bindFormatRow(row, format, addRow, primary, showDivider, clickListener);
		return row;
	}

	protected void bindFormatRow(@NonNull View row, @NonNull CoordinateFormat format, boolean addRow, boolean primary,
	                             boolean showDivider, @NonNull View.OnClickListener clickListener) {
		TextView title = row.findViewById(android.R.id.title);
		TextView summary = row.findViewById(android.R.id.summary);
		View divider = row.findViewById(R.id.divider);
		View iconContainer = row.findViewById(R.id.iconContainer);
		ImageView icon = row.findViewById(R.id.icon);
		View selectable = row.findViewById(R.id.selectable_list_item);

		title.setText(format.getTitle());
		String description = getFormatSummary(format);
		if (primary) {
			description = description + " • " + getString(R.string.coordinate_format_primary);
		}
		summary.setText(description);
		divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
		if (addRow) {
			iconContainer.setVisibility(View.VISIBLE);
			iconContainer.setBackground(null);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_add, R.color.color_osm_edit_create));
			setDividerTextStartMargin(divider);
		} else {
			iconContainer.setVisibility(View.GONE);
			setDividerDefaultMargin(divider);
		}
		selectable.setOnClickListener(clickListener);
	}

	protected void setDividerTextStartMargin(@NonNull View divider) {
		setDividerStartMargin(divider, dp(72));
	}

	protected void setDividerDefaultMargin(@NonNull View divider) {
		setDividerStartMargin(divider, dp(16));
	}

	protected void setDividerStartMargin(@NonNull View divider, int marginStart) {
		ViewGroup.LayoutParams params = divider.getLayoutParams();
		if (params instanceof ViewGroup.MarginLayoutParams marginParams) {
			marginParams.setMarginStart(marginStart);
			divider.setLayoutParams(marginParams);
		}
	}
}
