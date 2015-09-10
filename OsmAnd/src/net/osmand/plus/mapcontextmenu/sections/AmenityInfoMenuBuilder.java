package net.osmand.plus.mapcontextmenu.sections;

import android.content.res.Resources;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.Map;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class AmenityInfoMenuBuilder extends MenuBuilder {

	private final Amenity amenity;

	public AmenityInfoMenuBuilder(OsmandApplication app, final Amenity amenity) {
		super(app);
		this.amenity = amenity;
	}

	private void buildRow(View view, int iconId, String text) {
		boolean light = app.getSettings().isLightContent();

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
		//llParams.setMargins(0, dpToPx(14f), 0, dpToPx(14f));
		ll.setLayoutParams(llParams);

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView icon = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) ;
		llIconParams.setMargins(dpToPx(16f), dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		icon.setLayoutParams(llIconParams);
		icon.setScaleType(ImageView.ScaleType.CENTER);
		icon.setImageDrawable(getRowIcon(iconId));
		llIcon.addView(icon);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView  = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(18); // todo: create constant
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		SpannableString spannable = new SpannableString(text);
		Linkify.addLinks(spannable, Linkify.ALL);
		textView.setClickable(true);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setLinksClickable(true);

		textView.setText(spannable);
		//textView.setText("sdf dsaf fsdasdfg adsf asdsfd asdf sdf adsfg asdf sdfa sdf dsf agsfdgd fgsfd sdf asdf adg adf sdf asdf dfgdfsg sdfg adsf asdf asdf sdf SDF ASDF ADSF ASDF ASDF DAF SDAF dfg dsfg dfg sdfg rg rth sfghs dfgs dfgsdfg adfg dfg sdfg dfs ");

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		((LinearLayout)view).addView(ll);

		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		horizontalLine.setLayoutParams(llHorLineParams);

		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_info_divider_light : R.color.ctx_menu_info_divider_dark));

		((LinearLayout)view).addView(horizontalLine);
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

		MapPoiTypes poiTypes = app.getPoiTypes();
		for(Map.Entry<String, String> e : amenity.getAdditionalInfo().entrySet()) {
			int iconId;
			String key = e.getKey();
			String vl = e.getValue();
			if(key.startsWith("name:")) {
				continue;
			} else if(Amenity.OPENING_HOURS.equals(key)) {
				iconId = R.drawable.ic_action_time;
			} else if(Amenity.PHONE.equals(key)) {
				iconId = R.drawable.ic_action_call_dark;
			} else if(Amenity.WEBSITE.equals(key)) {
				iconId = R.drawable.ic_world_globe_dark;
			} else {
				if (Amenity.DESCRIPTION.equals(key)) {
					iconId = R.drawable.ic_action_note_dark;
				} else {
					iconId = R.drawable.ic_action_info_dark;
				}
				AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(e.getKey());
				if (pt != null) {
					if(pt instanceof PoiType && !((PoiType) pt).isText()) {
						vl = pt.getTranslation();
					} else {
						vl = pt.getTranslation() + ": " + amenity.unzipContent(e.getValue());
					}
				} else {
					vl = Algorithms.capitalizeFirstLetterAndLowercase(e.getKey()) +
							": " + amenity.unzipContent(e.getValue());
				}
			}

			buildRow(view, iconId, vl);
		}
	}
}
