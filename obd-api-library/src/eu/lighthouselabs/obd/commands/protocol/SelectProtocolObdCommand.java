/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.protocol;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Select the protocol to use.
 */
public class SelectProtocolObdCommand extends ProtocolObdCommand {
	
	private final ObdProtocols _protocol;

	/**
	 * @param command
	 */
	public SelectProtocolObdCommand(ObdProtocols protocol) {
		super("AT SP " + protocol.getValue());
		_protocol = protocol;
	}

	@Override
	public String getName() {
		return "Select Protocol " + _protocol.name();
	}

}