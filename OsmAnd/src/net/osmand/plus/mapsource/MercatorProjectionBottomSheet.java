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

public class MercatorProjectionBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = MercatorProjectionBottomSheet.class.getName();
	private static final String ELLIPTIC_KEY = "elliptic_key";
	private LinearLayout valuesContainer;
	private MercatorProjection mercatorProjection;

	public static void showInstance(@NonNull FragmentManager fm,
									@Nullable Fragment targetFragment,
									boolean elliptic) {
		MercatorProjectionBottomSheet bottomSheet = new MercatorProjectionBottomSheet();
		bottomSheet.setTargetFragment(targetFragment, 0);
		bottomSheet.setMercatorProjection(elliptic);
		bottomSheet.show(fm, TAG);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			setMercatorProjection(savedInstanceState.getBoolean(ELLIPTIC_KEY));
		}
		Context context = requireContext();
		TitleItem titleItem = new TitleItem(getString(R.string.mercator_projection));
		items.add(titleItem);
		NestedScrollView nestedScrollView = new NestedScrollView(context);
		valuesContainer = new LinearLayout(context);
		valuesContainer.setLayoutParams((new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)));
		valuesContainer.setOrientation(LinearLayout.VERTICAL);
		valuesContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);
		for (int i = 0; i < MercatorProjection.values().length; i++) {
			LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, valuesContainer, true);
		}
		nestedScrollView.addView(valuesContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());
		populateValuesList();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(ELLIPTIC_KEY, mercatorProjection == MercatorProjection.ELLIPTIC);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnMercatorSelectedListener) {
			((OnMercatorSelectedListener) fragment).onMercatorSelected(mercatorProjection == MercatorProjection.ELLIPTIC);
		}
		super.onDismiss(dialog);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void populateValuesList() {
		for (int i = 0; i < MercatorProjection.values().length; i++) {
			final MercatorProjection m = MercatorProjection.values()[i];
			boolean selected = mercatorProjection == m;
			View view = valuesContainer.getChildAt(i);
			((CompoundButton) view.findViewById(R.id.compound_button)).setChecked(selected);
			((TextView) view.findViewById(R.id.title)).setText(m.titleRes);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (mercatorProjection != m){
						mercatorProjection = m;
						dismiss();
					}
				}
			});
		}
	}

	private void setMercatorProjection(boolean elliptic) {
		mercatorProjection = elliptic ? MercatorProjection.ELLIPTIC : MercatorProjection.PSEUDO;
	}

	public enum MercatorProjection {
		ELLIPTIC(R.string.edit_tilesource_elliptic_tile),
		PSEUDO(R.string.pseudo_mercator_projection);

		@StringRes
		public int titleRes;

		MercatorProjection(@StringRes int titleRes) {
			this.titleRes = titleRes;
		}
	}

	public interface OnMercatorSelectedListener {
		void onMercatorSelected(boolean elliptic);
	}
}
