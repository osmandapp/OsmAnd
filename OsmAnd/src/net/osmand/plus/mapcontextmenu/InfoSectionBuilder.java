package net.osmand.plus.mapcontextmenu;

import android.content.res.Resources;
import android.util.AttributeSet;
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

public class InfoSectionBuilder extends BottomSectionBuilder {

	private final Amenity amenity;

	public InfoSectionBuilder(OsmandApplication app, final Amenity amenity) {
		super(app);
		this.amenity = amenity;
	}

	private void buildRow(View view, int iconId, String text) {

				/*
		<LinearLayout
            android:id="@+id/context_menu_top_view"
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">


		  <LinearLayout
                android:id="@+id/context_menu_icon_layout"
                android:orientation="horizontal"
                android:layout_width="42dp"
                android:layout_height="match_parent">

                <ImageView
                    android:id="@+id/context_menu_icon_view"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:layout_gravity="center_vertical"
                    android:scaleType="center"
                    android:layout_marginStart="12dp"
                    android:layout_marginLeft="12dp"
                    android:src="@drawable/ic_action_building_number"/>

            </LinearLayout>

		    <LinearLayout
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="16dp"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/context_menu_line1"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginLeft="12dp"
                    android:layout_marginRight="12dp"
                    android:text="@string/search_address_building"
                    android:textSize="@dimen/default_list_text_size_large"
                    android:textColor="?android:textColorPrimary"
                    android:textStyle="bold"/>

			</LinearLayout>

			<View
                android:layout_width="match_parent"
                android:layout_height="1px"
                android:background="#c9c9c9"/>

		    */


		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(42f), ViewGroup.LayoutParams.MATCH_PARENT));
		ll.addView(llIcon);

		ImageView icon = new ImageView(view.getContext());
		ViewGroup.MarginLayoutParams llIconParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		llIconParams.setMargins(dpToPx(12f), 0, 0, 0);
		//llIconParams.setGravity(Gravity.CENTER_VERTICAL);
		icon.setLayoutParams(llIconParams);
		icon.setScaleType(ImageView.ScaleType.CENTER);
		icon.setImageDrawable(getRowIcon(iconId));
		llIcon.addView(icon);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ViewGroup.MarginLayoutParams llTextParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, dpToPx(4f), 0, dpToPx(4f));
		llText.setLayoutParams(llTextParams);
		ll.addView(llText);

		TextView textView  = new TextView(view.getContext());
		ViewGroup.MarginLayoutParams llTextViewParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(dpToPx(10f), 0, dpToPx(10f), 0);
		llText.setLayoutParams(llTextViewParams);
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
	public void buildSection(View view) {

		MapPoiTypes poiTypes = app.getPoiTypes();
		for(Map.Entry<String, String> e : amenity.getAdditionalInfo().entrySet()) {
			int iconId = 0;
			String key = e.getKey();
			String vl = e.getValue();
			if(key.startsWith("name:")) {
				continue;
			} else if(Amenity.OPENING_HOURS.equals(key)) {
				iconId = R.drawable.mm_clock; // todo: change icon
			} else if(Amenity.PHONE.equals(key)) {
				iconId = R.drawable.mm_amenity_telephone; // todo: change icon
			} else if(Amenity.WEBSITE.equals(key)) {
				iconId = R.drawable.mm_internet_access; // todo: change icon
			} else {
				iconId = R.drawable.ic_type_info; // todo: change icon
				AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(e.getKey());
				if (pt != null) {
					if(pt instanceof PoiType && !((PoiType) pt).isText()) {
						vl = pt.getTranslation();
					} else {
						vl = /*pt.getTranslation() + ": " + */amenity.unzipContent(e.getValue());
					}
				} else {
					vl = /*Algorithms.capitalizeFirstLetterAndLowercase(e.getKey()) +
							": " + */amenity.unzipContent(e.getValue());
				}
			}

			buildRow(view, iconId, vl);
		}
	}
}
