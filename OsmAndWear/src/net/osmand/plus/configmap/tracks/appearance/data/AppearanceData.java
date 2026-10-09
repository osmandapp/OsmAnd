package net.osmand.plus.configmap.tracks.appearance.data;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class AppearanceData {

	private final Map<GpxParameter, Pair<Boolean, Object>> map = new HashMap<>();

	private AppearanceChangedListener listener;

	public AppearanceData() {
	}

	public AppearanceData(@NonNull AppearanceData data) {
		this.map.putAll(data.map);
		this.listener = data.listener;
	}

	@NonNull
	public AppearanceData setListener(@NonNull AppearanceChangedListener listener) {
		this.listener = listener;
		return this;
	}

	@Nullable
	public <T> T getParameter(@NonNull GpxParameter parameter) {
		Pair<Boolean, Object> pair = map.get(parameter);
		return pair != null ? SharedUtil.castGpxParameter(parameter, pair.second) : null;
	}

	public boolean setParameter(@NonNull GpxParameter parameter, @Nullable Object value) {
		if (isValidValue(parameter, value)) {
			map.put(parameter, new Pair<>(false, value));
			notifyAppearanceModified();
			return true;
		}
		return false;
	}

	public boolean resetParameter(@NonNull GpxParameter parameter) {
		if (parameter.isAppearanceParameter()) {
			map.put(parameter, new Pair<>(true, null));
			notifyAppearanceModified();
			return true;
		}
		return false;
	}

	public boolean shouldResetParameter(@NonNull GpxParameter parameter) {
		if (parameter.isAppearanceParameter()) {
			Pair<Boolean, Object> pair = map.get(parameter);
			return pair != null && pair.first;
		}
		return false;
	}

	public boolean shouldResetAnything() {
		for (GpxParameter parameter : GpxParameter.Companion.getAppearanceParameters()) {
			if (shouldResetParameter(parameter)) {
				return true;
			}
		}
		return false;
	}

	public boolean isValidValue(@NonNull GpxParameter parameter, @Nullable Object value) {
		return parameter.isAppearanceParameter()
				&& (value == null || SharedUtil.isGpxParameterClass(parameter, value.getClass()));
	}

	private void notifyAppearanceModified() {
		if (listener != null) {
			listener.onAppearanceChanged();
		}
	}

	public interface AppearanceChangedListener {
		void onAppearanceChanged();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AppearanceData that = (AppearanceData) o;
		return Algorithms.objectEquals(map, that.map);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(map);
	}
}