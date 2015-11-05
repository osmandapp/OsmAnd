package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.osmedit.OsmPoint;

import java.util.Map;

public class EditPOIMenuBuilder extends MenuBuilder {

	private final OsmPoint osmPoint;

	public EditPOIMenuBuilder(OsmandApplication app, final OsmPoint osmPoint) {
		super(app);
		this.osmPoint = osmPoint;
	}

	private void buildRow(View view, int iconId, String text) {
		buildRow(view, getRowIcon(iconId), text);
	}

	protected void buildRow(final View view, Drawable icon, String text) {
		boolean light = app.getSettings().isLightContent();

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setText(text);

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

	@Override
	public void build(View view) {
		super.build(view);

		if (osmPoint instanceof OsmNotesPoint) {
			OsmNotesPoint notes = (OsmNotesPoint) osmPoint;

			buildRow(view, R.drawable.ic_action_note_dark, notes.getText());
			buildRow(view, R.drawable.ic_group, notes.getAuthor());

		} else if (osmPoint instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint point = (OpenstreetmapPoint) osmPoint;

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, R.drawable.ic_action_info_dark, text);
			}
		}
	}
}
