package net.osmand.plus.mapcontextmenu;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;

import java.util.List;

public class TransportStopRouteAdapter extends ArrayAdapter<TransportStopRoute> {

	private boolean nightMode;

	public TransportStopRouteAdapter(@NonNull Context context, @NonNull List<TransportStopRoute> objects, boolean nightMode) {
		super(context, 0, objects);
		this.nightMode = nightMode;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.transport_stop_route_item, parent, false);
		}

		TransportStopRoute transportStopRoute = getItem(position);
		if (transportStopRoute != null) {
			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			transportStopRouteTextView.setText(transportStopRoute.route.getRef());
			GradientDrawable gradientDrawableBg = (GradientDrawable) transportStopRouteTextView.getBackground();
			gradientDrawableBg.setColor(ContextCompat.getColor(getContext(), getColor(transportStopRoute)));
		}

		return convertView;
	}

	private int getColor(TransportStopRoute route) {
		int color;
		switch (route.type) {
			case BUS:
				color = R.color.route_bus_color;
				break;
			case SHARE_TAXI:
				color = R.color.route_share_taxi_color;
				break;
			case TROLLEYBUS:
				color = R.color.route_trolleybus_color;
				break;
			case TRAM:
				color = R.color.route_tram_color;
				break;
			case TRAIN:
				color = nightMode ? R.color.route_train_color_dark : R.color.route_train_color_light;
				break;
			case LIGHT_RAIL:
				color = R.color.route_lightrail_color;
				break;
			case FUNICULAR:
				color = R.color.route_funicular_color;
				break;
			case FERRY:
				color = nightMode ? R.color.route_ferry_color_dark : R.color.route_ferry_color_light;
				break;
			default:
				color = R.color.nav_track;
				break;
		}
		return color;
	}
}
