package net.osmand.plus.mapcontextmenu.details;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private final FavouritePoint fav;

	public FavouritePointMenuBuilder(OsmandApplication app, final FavouritePoint fav) {
		super(app);
		this.fav = fav;
	}

	private void buildRow(View view, int iconId, String text, int textColor, boolean isDescription) {
		buildRow(view, getRowIcon(iconId), text, textColor, isDescription);
	}

	private void buildRow(final View view, Drawable icon, String text, int textColor, final boolean isDescription) {
		boolean light = app.getSettings().isLightContent();

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		//llParams.setMargins(0, dpToPx(14f), 0, dpToPx(14f));
		ll.setLayoutParams(llParams);

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), isFirstRow() ? dpToPx(48f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP) : dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView iconView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		llIconParams.setMargins(dpToPx(16f), isFirstRow() ? dpToPx(12f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP / 2f) : dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(llIconParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER);
		iconView.setImageDrawable(icon);
		llIcon.addView(iconView);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, isFirstRow() ? dpToPx(8f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP) : dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16); // todo: create constant
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		textView.setAutoLinkMask(Linkify.ALL);
		textView.setLinksClickable(true);
//		if (isDescription) {
//			textView.setMinLines(1);
//			textView.setMaxLines(5);
//		}
		textView.setText(text);
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}
//		if (isDescription) {
//			textView.setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					//todo: implement edit fav description dialog
//					//POIMapLayer.showDescriptionDialog(view.getContext(), app, fav);
//				}
//			});
//		}

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		((LinearLayout) view).addView(ll);

		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		horizontalLine.setLayoutParams(llHorLineParams);

		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_info_divider_light : R.color.ctx_menu_info_divider_dark));

		((LinearLayout) view).addView(horizontalLine);

		rowBuilt();
	}

	public int dpToPx(float dp) {
		Resources r = app.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	@Override
	public void build(View view) {
		super.build(view);

		if (!Algorithms.isEmpty(fav.getDescription())) {
			buildRow(view, R.drawable.ic_action_note_dark, fav.getDescription(), 0, true);
		}

		for (PlainMenuItem item : plainMenuItems) {
			buildRow(view, item.getIconId(), item.getText(), 0, false);
		}
	}
}
