package net.osmand.plus.importfiles.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxFile;
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

	private GpxFile gpxFile;
	private String fileName;
	private long fileSize;

	private boolean save;
	private boolean useImportDir;
	private boolean showSnackbar;

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
				.setOnClickListener(v -> {
					importHelper.importFavoritesFromGpx(gpxFile, fileName);
					dismiss();
				})
				.create();
		items.add(asFavoritesItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem asGpxItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
				.setTitle(getString(R.string.import_as_gpx))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					importHelper.handleGpxImport(gpxFile, fileName, fileSize, save, useImportDir, showSnackbar);
					dismiss();
				})
				.create();
		items.add(asGpxItem);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ImportHelper importHelper,
	                                @NonNull GpxFile gpxFile,
	                                @NonNull String fileName,
	                                long fileSize,
	                                boolean save,
	                                boolean useImportDir,
	                                boolean showSnackbar) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ImportGpxBottomSheetDialogFragment fragment = new ImportGpxBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.importHelper = importHelper;
			fragment.gpxFile = gpxFile;
			fragment.fileName = fileName;
			fragment.fileSize = fileSize;
			fragment.save = save;
			fragment.useImportDir = useImportDir;
			fragment.showSnackbar = showSnackbar;
			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}