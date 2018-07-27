package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

public class TransportStopRouteAdapter extends ArrayAdapter<TransportStopRoute> {

	private boolean nightMode;
	private OnClickListener listener;
	private OsmandApplication app;

	public TransportStopRouteAdapter(@NonNull OsmandApplication application, @NonNull List<TransportStopRoute> objects, boolean nightMode) {
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

		TransportStopRoute transportStopRoute = getItem(position);
		if (transportStopRoute != null) {
			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			transportStopRouteTextView.setText(getAdjustedRouteRef(transportStopRoute.route.getRef()));
			GradientDrawable gradientDrawableBg = (GradientDrawable) transportStopRouteTextView.getBackground();
			int bgColor = transportStopRoute.getColor(app, nightMode);
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));
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

	private String getAdjustedRouteRef(String ref) {
		if (ref != null) {
			int charPos = ref.lastIndexOf(':');
			if (charPos != -1) {
			    ref = ref.substring(0, charPos);
			}
			if (ref.length() > 4) {
				ref = ref.substring(0, 4);
			}
		}
		return ref;
	}

	public interface OnClickListener {
		void onClick(int position);
	}
}
