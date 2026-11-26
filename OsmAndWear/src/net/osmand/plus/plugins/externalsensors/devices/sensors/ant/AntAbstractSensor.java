package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import androidx.annotation.NonNull;

import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public abstract class AntAbstractSensor<T extends AntPluginPcc> extends AbstractSensor {

	protected static final Log LOG = PlatformUtil.getLog(AntAbstractSensor.class);

	public AntAbstractSensor(@NonNull AntAbstractDevice<T> device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@SuppressWarnings("unchecked")
	@NonNull
	protected AntAbstractDevice<T> getAntDevice() {
		return (AntAbstractDevice<T>) device;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AntAbstractSensor<?> that = (AntAbstractSensor<?>) o;
		return Algorithms.stringsEqual(sensorId, that.sensorId)
				&& Algorithms.objectEquals(device, that.device);
	}

	@Override
	public int hashCode() {
		return getClass().getSimpleName().hashCode();
	}

	public abstract void subscribeToEvents();
}
