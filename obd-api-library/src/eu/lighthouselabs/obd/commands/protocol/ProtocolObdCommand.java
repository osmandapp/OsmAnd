/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.commands.protocol;

import eu.lighthouselabs.obd.commands.ObdCommand;

public abstract class ProtocolObdCommand extends ObdCommand {

    /**
     * Default ctor
     * 
     * @param cmd
     */
    public ProtocolObdCommand(String cmd) {
        super(cmd);
    }

    /**
     * Copy ctor.
     * 
     * @param cmd
     */
    public ProtocolObdCommand(ObdCommand other) {
        super(other);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }
}