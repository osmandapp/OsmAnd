package net.osmand.plus.mapcontextmenu;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;

import java.util.List;

public class TransportStopRouteAdapter extends ArrayAdapter<TransportStopRoute> {

	public TransportStopRouteAdapter(@NonNull Context context, @NonNull List<TransportStopRoute> objects) {
		super(context, 0, objects);
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.transport_stop_route_item, parent, false);
		}

		((TextView) convertView.findViewById(R.id.transport_stop_route_text)).setText(getItem(position).route.getRef());

		return convertView;
	}
}
