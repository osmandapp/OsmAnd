/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.engine;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Displays the current engine revolutions per minute (RPM).
 */
public class EngineRPMObdCommand extends ObdCommand {
	
	private int _rpm = -1;
	
	/**
	 * Default ctor.
	 */
	public EngineRPMObdCommand() {
		super("01 0C");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public EngineRPMObdCommand(EngineRPMObdCommand other) {
		super(other);
	}

	/**
	 * @return the engine RPM per minute
	 */
	@Override
	public String getFormattedResult() {
	    String res = getResult();

	    if (!"NODATA".equals(res)) {
	        if (buffer.size() >= 4) {
	            // ignore first two bytes [41 0C] of the response
	            int a = buffer.get(2);
	            int b = buffer.get(3);
	            _rpm = (a * 256 + b) / 4;
	            res = String.format("%d%s", _rpm, " RPM");
	        } else {
	            res = "NODATA";
	        }
	    }

	    return res;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.ENGINE_RPM.getValue();
	}
	
	public int getRPM() {
		return _rpm;
	}
}