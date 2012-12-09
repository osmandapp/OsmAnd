/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.protocol;

import eu.lighthouselabs.obd.commands.ObdCommand;

/**
 * Turns off line-feed.
 */
public class LineFeedOffObdCommand extends ProtocolObdCommand {

	/**
	 * @param command
	 */
	public LineFeedOffObdCommand() {
		super("AT L0");
	}

	/**
	 * @param other
	 */
	public LineFeedOffObdCommand(ObdCommand other) {
		super(other);
	}

	@Override
	public String getName() {
		return "Line Feed Off";
	}

}