package net.osmand.plus.mapsource;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class TileStorageFormatBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TileStorageFormatBottomSheet.class.getName();
	private static final String SQLITE_DB_KEY = "sqlite_db_key";
	private static final String NEW_MAP_SOURCE_KEY = "new_map_source_key";
	private LinearLayout valuesContainer;
	private TileStorageFormat tileStorageFormat;
	private boolean newMapSource;

	public static void showInstance(@NonNull FragmentManager fm,
									@Nullable Fragment targetFragment,
									boolean sqliteDb,
									boolean newMapSource) {
		TileStorageFormatBottomSheet bottomSheet = new TileStorageFormatBottomSheet();
		bottomSheet.setTargetFragment(targetFragment, 0);
		bottomSheet.setTileStorageFormat(sqliteDb);
		bottomSheet.setNewMapSource(newMapSource);
		bottomSheet.show(fm, TAG);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			setTileStorageFormat(savedInstanceState.getBoolean(SQLITE_DB_KEY));
			newMapSource = savedInstanceState.getBoolean(NEW_MAP_SOURCE_KEY);
		}
		Context context = requireContext();
		TitleItem titleItem = new TitleItem(getString(R.string.mercator_projection));
		items.add(titleItem);
		NestedScrollView nestedScrollView = new NestedScrollView(context);
		valuesContainer = new LinearLayout(context);
		valuesContainer.setLayoutParams((new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)));
		valuesContainer.setOrientation(LinearLayout.VERTICAL);
		valuesContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);
		for (int i = 0; i < TileStorageFormat.values().length; i++) {
			LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, valuesContainer, true);
		}
		nestedScrollView.addView(valuesContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());
		populateValuesList();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(SQLITE_DB_KEY, tileStorageFormat == TileStorageFormat.SQLITE_DB);
		outState.putBoolean(NEW_MAP_SOURCE_KEY, newMapSource);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void populateValuesList() {
		for (int i = 0; i < TileStorageFormat.values().length; i++) {
			final TileStorageFormat m = TileStorageFormat.values()[i];
			boolean selected = tileStorageFormat == m;
			View view = valuesContainer.getChildAt(i);
			((CompoundButton) view.findViewById(R.id.compound_button)).setChecked(selected);
			((TextView) view.findViewById(R.id.title)).setText(m.titleRes);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (tileStorageFormat != m) {
						if (newMapSource) {
							applyTileStorageFormat(m);
						} else {
							InputZoomLevelsBottomSheet.showClearTilesWarningDialog(requireActivity(), nightMode, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									applyTileStorageFormat(m);
								}
							});
						}
					}
				}
			});
		}
	}

	private void applyTileStorageFormat(TileStorageFormat tileStorageFormat) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnTileStorageFormatSelectedListener) {
			((OnTileStorageFormatSelectedListener) fragment).onStorageFormatSelected(tileStorageFormat == TileStorageFormat.SQLITE_DB);
		}
		dismiss();
	}

	private void setTileStorageFormat(boolean sqliteDb) {
		tileStorageFormat = sqliteDb ? TileStorageFormat.SQLITE_DB : TileStorageFormat.ONE_IMAGE_PER_TILE;
	}

	public void setNewMapSource(boolean newMapSource) {
		this.newMapSource = newMapSource;
	}

	public enum TileStorageFormat {
		ONE_IMAGE_PER_TILE(R.string.one_image_per_tile),
		SQLITE_DB(R.string.sqlite_db_file);

		@StringRes
		public int titleRes;

		TileStorageFormat(@StringRes int titleRes) {
			this.titleRes = titleRes;
		}
	}

	public interface OnTileStorageFormatSelectedListener {
		void onStorageFormatSelected(boolean sqliteDb);
	}
}
