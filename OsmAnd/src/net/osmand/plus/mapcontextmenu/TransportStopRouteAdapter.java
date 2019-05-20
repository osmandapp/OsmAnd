package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.transport.TransportStopRoute;

import java.util.List;

public class TransportStopRouteAdapter extends ArrayAdapter<Object> {

	private boolean nightMode;
	private OnClickListener listener;
	private OsmandApplication app;

	public TransportStopRouteAdapter(@NonNull OsmandApplication application, @NonNull List<Object> objects, boolean nightMode) {
		super(application, 0, objects);
		this.nightMode = nightMode;
		this.app = application;
	}

	public void setListener(OnClickListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.transport_stop_route_item, parent, false);
		}
		Object object = getItem(position);
		if (object != null) {
			String routeRef = "";
			int bgColor = 0;
			if (object instanceof TransportStopRoute) {
				TransportStopRoute transportStopRoute = (TransportStopRoute) object;
				routeRef = transportStopRoute.route.getAdjustedRouteRef(false);
				bgColor = transportStopRoute.getColor(app, nightMode);
			} else if (object instanceof String) {
				routeRef = (String) object;
				bgColor = ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);
			}
			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			transportStopRouteTextView.setText(routeRef);
			GradientDrawable gradientDrawableBg = (GradientDrawable) transportStopRouteTextView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onClick(position);
					}
				}
			});
		}

		return convertView;
	}

	public interface OnClickListener {
		void onClick(int position);
	}
}
