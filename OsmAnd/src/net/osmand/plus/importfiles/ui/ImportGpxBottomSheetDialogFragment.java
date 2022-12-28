package net.osmand.plus.importfiles.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class ImportGpxBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = ImportGpxBottomSheetDialogFragment.class.getSimpleName();

	private ImportHelper importHelper;

	private GPXFile gpxFile;
	private String fileName;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;

	public void setImportHelper(ImportHelper importHelper) {
		this.importHelper = importHelper;
	}

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	public void setUseImportDir(boolean useImportDir) {
		this.useImportDir = useImportDir;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.import_file)));

		Context ctx = requireContext();
		int nameColor = ColorUtilities.getActiveColor(ctx, nightMode);
		int descrColor = ColorUtilities.getSecondaryTextColor(ctx, nightMode);
		String descr = getString(R.string.import_gpx_file_description);
		if (!descr.contains("%s")) {
			descr = "%s " + descr;
		}

		CharSequence txt = AndroidUtils.getStyledString(descr, fileName, new ForegroundColorSpan(descrColor),
				new ForegroundColorSpan(nameColor));
		items.add(new ShortDescriptionItem(txt));

		BaseBottomSheetItem asFavoritesItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_favorite))
				.setTitle(getString(R.string.import_as_favorites))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						importHelper.importFavoritesFromGpx(gpxFile, fileName);
						dismiss();
					}
				})
				.create();
		items.add(asFavoritesItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem asGpxItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
				.setTitle(getString(R.string.import_as_gpx))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						importHelper.handleGpxImport(gpxFile, fileName, fileSize, save, useImportDir);
						dismiss();
					}
				})
				.create();
		items.add(asGpxItem);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ImportHelper importHelper,
	                                @NonNull GPXFile gpxFile,
	                                @NonNull String fileName,
	                                long fileSize,
	                                boolean save,
	                                boolean useImportDir) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ImportGpxBottomSheetDialogFragment fragment = new ImportGpxBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.setImportHelper(importHelper);
			fragment.setGpxFile(gpxFile);
			fragment.setFileName(fileName);
			fragment.setFileSize(fileSize);
			fragment.setSave(save);
			fragment.setUseImportDir(useImportDir);
			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}