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
	private OnClickListener listener;

	public TransportStopRouteAdapter(@NonNull Context context, @NonNull List<TransportStopRoute> objects, boolean nightMode) {
		super(context, 0, objects);
		this.nightMode = nightMode;
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

		TransportStopRoute transportStopRoute = getItem(position);
		if (transportStopRoute != null) {
			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			transportStopRouteTextView.setText(transportStopRoute.route.getRef());
			GradientDrawable gradientDrawableBg = (GradientDrawable) transportStopRouteTextView.getBackground();
			gradientDrawableBg.setColor(ContextCompat.getColor(getContext(), transportStopRoute.getColor(nightMode)));
		}

		convertView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onClick(position);
				}
			}
		});

		return convertView;
	}

	public interface OnClickListener {
		void onClick(int position);
	}
}
