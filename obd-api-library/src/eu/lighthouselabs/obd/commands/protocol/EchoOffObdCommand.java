/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.protocol;

import eu.lighthouselabs.obd.commands.ObdCommand;

/**
 * This command will turn-off echo.
 */
public class EchoOffObdCommand extends ProtocolObdCommand {

	/**
	 * @param command
	 */
	public EchoOffObdCommand() {
		super("AT E0");
	}

	/**
	 * @param other
	 */
	public EchoOffObdCommand(ObdCommand other) {
	    super(other);
	}

	@Override
	public String getName() {
		return "Echo Off";
	}

}