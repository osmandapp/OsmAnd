package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.ui.status.ChangesItemViewHolder;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.TextViewEx;


public class ChangeItemActionsBottomSheet extends BottomSheetDialogFragment {
	public static final String TAG = ChangeItemActionsBottomSheet.class.getSimpleName();
	public SettingsItem item;
	public FragmentManager fragmentManager;
	private boolean nightMode;
	private OsmandApplication app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		nightMode = !app.getSettings().isLightContent();
	}

	private void setupItems(@NonNull View view) {
		View headerItem = view.findViewById(R.id.item);
		ChangesItemViewHolder changesItemViewHolder = new ChangesItemViewHolder(headerItem);
		changesItemViewHolder.bindView(item, fragmentManager, false, false);
		View headerItemDivider = headerItem.findViewById(R.id.bottom_divider);
		AndroidUiHelper.updateVisibility(headerItemDivider, false);

		View downloadItem = view.findViewById(R.id.download_item);
		setupItem(downloadItem, R.string.download_cloud_version, R.drawable.ic_action_cloud_upload, item, true, view1 -> {

		});

		View uploadItem = view.findViewById(R.id.upload_item);
		setupItem(uploadItem, R.string.upload_local_version, R.drawable.ic_action_cloud_upload, item, false, view1 -> {

		});
	}

	private void setupItem(View itemView, int titleId, int iconId, SettingsItem item, boolean showDivider, OnClickListener onClickListener){
		AppCompatImageView icon = itemView.findViewById(R.id.icon);
		icon.setImageDrawable(getIcon(iconId, ColorUtilities.getActiveIconColorId(nightMode)));
		icon.getDrawable().setTint(ColorUtilities.getActiveIconColor(app, nightMode));

		TextViewEx title = itemView.findViewById(R.id.title);
		title.setText(getString(titleId));

		TextViewEx description = itemView.findViewById(R.id.description);
		String fileName = BackupHelper.getItemFileName(item);
		String summary = "";
		UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), fileName);
		if (info != null) {
			String time = OsmAndFormatter.getFormattedPassedTime(app, info.getUploadTime(), app.getString(R.string.shared_string_never));
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, time));
		} else {
			description.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, app.getString(R.string.shared_string_never)));
		}

		View divider = itemView.findViewById(R.id.bottom_divider);
		AndroidUiHelper.updateVisibility(divider, showDivider);

		View button = itemView.findViewById(R.id.selectable_list_item);
		button.setOnClickListener(onClickListener);

		View secondIcon = itemView.findViewById(R.id.second_icon);
		AndroidUiHelper.updateVisibility(secondIcon, false);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, SettingsItem item) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ChangeItemActionsBottomSheet fragment = new ChangeItemActionsBottomSheet();
			fragment.fragmentManager = fragmentManager;
			fragment.item = item;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.change_item_bottom_sheet, null);
		setupItems(view);

		return view;
	}
}