package net.osmand.plus.firstusage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

public class FirstUsageActionsBottomSheet extends BottomSheetDialogFragment {
	private OsmandApplication app;
	private boolean nightMode;
	private OtherClickListener otherClickListener = null;
	private LocationClickListener locationClickListener = null;
	private WizardType wizardType;

	public FirstUsageActionsBottomSheet(OsmandApplication app, boolean nightMode, WizardType wizardType, OtherClickListener otherClickListener) {
		this.app = app;
		this.nightMode = nightMode;
		this.otherClickListener = otherClickListener;
		this.wizardType = wizardType;
	}

	public FirstUsageActionsBottomSheet(OsmandApplication app, boolean nightMode, LocationClickListener locationClickListener) {
		this.app = app;
		this.nightMode = nightMode;
		this.locationClickListener = locationClickListener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInflater layoutInflater = UiUtilities.getInflater(getContext(), nightMode);
		View dialogView = layoutInflater.inflate(R.layout.first_usage_bottom_sheet, container, false);
		TextViewEx bottomSheetTitle = dialogView.findViewById(R.id.title);
		LinearLayout layoutContainer = dialogView.findViewById(R.id.container);

		if (locationClickListener != null) {
			bottomSheetTitle.setText(getString(R.string.shared_string_location));

			layoutContainer.addView(createItemView(layoutInflater, getString(R.string.search_another_country), R.drawable.ic_show_on_map, view -> {
				dismiss();
				locationClickListener.onSelectCountry();
			}));

			layoutContainer.addView(createItemView(layoutInflater, getString(R.string.determine_location), R.drawable.ic_action_marker_dark, view -> {
				dismiss();
				locationClickListener.onDetermineLocation();
			}));
		} else if(otherClickListener != null) {
			bottomSheetTitle.setText(getString(R.string.shared_string_other));

			layoutContainer.addView(createItemView(layoutInflater, getString(R.string.restore_from_osmand_cloud), R.drawable.ic_action_restore, view -> {
				dismiss();
				otherClickListener.onRestoreFromCloud();
			}));

			if(wizardType != WizardType.MAP_DOWNLOAD){
				layoutContainer.addView(createItemView(layoutInflater, getString(R.string.application_dir), R.drawable.ic_action_folder, view -> {
					dismiss();
					otherClickListener.onSelectStorageFolder();
				}));
			}
		}
		return dialogView;
	}

	private View createItemView(LayoutInflater layoutInflater, String title, @DrawableRes int iconId, OnClickListener onClickListener){
		View item = layoutInflater.inflate(R.layout.item_with_left_icon, null);
		TextViewEx titleView = item.findViewById(R.id.title);
		AppCompatImageView iconView = item.findViewById(R.id.icon);
		View button = item.findViewById(R.id.button);

		titleView.setText(title);
		iconView.setImageDrawable(ContextCompat.getDrawable(app, iconId));
		button.setOnClickListener(onClickListener);
		return item;
	}
}

interface LocationClickListener{
	void onSelectCountry();
	void onDetermineLocation();
}

interface OtherClickListener{
	void onRestoreFromCloud();
	void onSelectStorageFolder();
}
