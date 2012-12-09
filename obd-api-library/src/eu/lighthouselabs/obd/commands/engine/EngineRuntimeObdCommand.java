/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.engine;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * TODO put description
 */
public class EngineRuntimeObdCommand extends ObdCommand {

	/**
	 * Default ctor.
	 */
	public EngineRuntimeObdCommand() {
		super("01 1F");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public EngineRuntimeObdCommand(EngineRuntimeObdCommand other) {
		super(other);
	}

	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
		    if (buffer.size() >= 4) {
			// ignore first two bytes [01 0C] of the response
			int a = buffer.get(2);
			int b = buffer.get(3);
			int value = a * 256 + b;
			
			// determine time
			String hh = String.format("%02d", value / 3600);
			String mm = String.format("%02d", (value % 3600) / 60);
			String ss = String.format("%02d", value % 60);
			res = String.format("%s:%s:%s", hh, mm, ss);
		    } else {
		        res = "NODATA";
		    }
		}

		return res;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.ENGINE_RUNTIME.getValue();
	}
}