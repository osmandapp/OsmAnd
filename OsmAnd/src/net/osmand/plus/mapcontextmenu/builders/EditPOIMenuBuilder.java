package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.util.Algorithms;

import java.util.Map;

public class EditPOIMenuBuilder extends MenuBuilder {

	private final OsmPoint osmPoint;

	public EditPOIMenuBuilder(OsmandApplication app, final OsmPoint osmPoint) {
		super(app);
		this.osmPoint = osmPoint;
	}

	private void buildRow(View view, int iconId, String text, int textColor, boolean needLinks) {
		buildRow(view, getRowIcon(iconId), text, textColor, needLinks);
	}

	protected void buildRow(final View view, Drawable icon, String text, int textColor, boolean needLinks) {
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

		if (needLinks) {
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setLinksClickable(true);
		}
		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setText(text);
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}

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

			buildRow(view, R.drawable.ic_action_note_dark, notes.getText(), 0, false);
			buildRow(view, R.drawable.ic_group, notes.getAuthor(), 0, false);

		} else if (osmPoint instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint point = (OpenstreetmapPoint) osmPoint;

			MapPoiTypes poiTypes = app.getPoiTypes();

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				int iconId;
				Drawable icon = null;
				int textColor = 0;
				String key = e.getKey();
				String vl = e.getValue();

				boolean needLinks = !"population".equals(key);

				if (key.startsWith("name:")) {
					continue;
				} else if (Amenity.OPENING_HOURS.equals(key)) {
					iconId = R.drawable.ic_action_time;
				} else if (Amenity.PHONE.equals(key)) {
					iconId = R.drawable.ic_action_call_dark;
				} else if (Amenity.WEBSITE.equals(key)) {
					iconId = R.drawable.ic_world_globe_dark;
					vl = vl.replace(' ', '_');
				} else {
					if (Amenity.DESCRIPTION.equals(key)) {
						iconId = R.drawable.ic_action_note_dark;
					} else {
						iconId = R.drawable.ic_action_info_dark;
					}
					AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
					if (pt != null) {
						PoiType pType = (PoiType) pt;
						if (pType.getParentType() != null && pType.getParentType() instanceof PoiType) {
							icon = getRowIcon(view.getContext(), ((PoiType) pType.getParentType()).getOsmTag() + "_" + pType.getOsmTag().replace(':', '_') + "_" + pType.getOsmValue());
						}
						if (!((PoiType) pt).isText()) {
							vl = pt.getTranslation();
						} else {
							vl = pt.getTranslation() + ": " + e.getValue();
						}
					} else {
						vl = Algorithms.capitalizeFirstLetterAndLowercase(e.getKey()) + ": " + e.getValue();
					}
				}

				if (icon != null) {
					buildRow(view, icon, vl, textColor, needLinks);
				} else {
					buildRow(view, iconId, vl, textColor, needLinks);
				}
			}
		}

		buildButtonRow(view, null, view.getResources().getString(R.string.shared_string_delete), new OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
				if (plugin != null) {
					boolean deleted = false;
					if (osmPoint instanceof OsmNotesPoint) {
						deleted = plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) osmPoint);
					} else if (osmPoint instanceof OpenstreetmapPoint) {
						deleted = plugin.getDBPOI().deletePOI((OpenstreetmapPoint) osmPoint);
					}
					if (deleted && v.getContext() instanceof MapActivity) {
						((MapActivity)v.getContext()).getContextMenu().close();
					}
				}
			}
		});
	}
}
