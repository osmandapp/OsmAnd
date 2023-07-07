package net.osmand.plus.plugins.openplacereviews;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

public class AddPhotosBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = AddPhotosBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(AddPhotosBottomSheetDialogFragment.class);

	public static final String OPEN_PLACE_REVIEWS = "OpenPlaceReviews";
	public static final String MAPILLARY = "Mapillary";
	public static final String WEB_WIKIMEDIA = "Web / Wikimedia";
	public static final String OPEN_STREET_MAP = "OpenStreetMap";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		View view = View.inflate(UiUtilities.getThemedContext(app, nightMode),
				R.layout.opr_add_photo, null);
		setDescriptionSpan(view);
		items.add(new SimpleBottomSheetItem.Builder()
				.setCustomView(view)
				.create());
		items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.text_margin_small)));
	}

	private void setDescriptionSpan(View view) {
		String desc = requireContext().getString(R.string.add_photos_descr);
		BoldSpannableString ss = new BoldSpannableString(desc, view.getContext());
		ss.setBold(OPEN_PLACE_REVIEWS);
		ss.setBold(MAPILLARY);
		ss.setBold(WEB_WIKIMEDIA);
		ss.setBold(OPEN_STREET_MAP);
		view.<TextView>findViewById(R.id.add_photos_descr).setText(ss);
	}

	static class BoldSpannableString extends SpannableString {
		Context ctx;

		public BoldSpannableString(CharSequence source, Context ctx) {
			super(source);
			this.ctx = ctx;
		}

		public void setBold(String boldText) {
			String source = toString();
			setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(ctx)),
					source.indexOf(boldText), source.indexOf(boldText) + boldText.length(),
					Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		}
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.add_to_opr;
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_button_divider_height);
	}

	@Override
	protected void onThirdBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			OprStartFragment.showInstance(activity.getSupportFragmentManager());
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.add_to_mapillary;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			MapillaryPlugin.openMapillary(activity, null);
		}
		dismiss();
	}

	@Override
	protected int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.content_padding_small);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (!fm.isStateSaved()) {
				AddPhotosBottomSheetDialogFragment fragment = new AddPhotosBottomSheetDialogFragment();
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}