/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.config;

import java.util.ArrayList;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.control.DtcNumberObdCommand;
import eu.lighthouselabs.obd.commands.control.TimingAdvanceObdCommand;
import eu.lighthouselabs.obd.commands.control.TroubleCodesObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineLoadObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRuntimeObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.engine.ThrottlePositionObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FindFuelTypeObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.pressure.BarometricPressureObdCommand;
import eu.lighthouselabs.obd.commands.pressure.FuelPressureObdCommand;
import eu.lighthouselabs.obd.commands.pressure.IntakeManifoldPressureObdCommand;
import eu.lighthouselabs.obd.commands.protocol.ObdResetCommand;
import eu.lighthouselabs.obd.commands.temperature.AirIntakeTemperatureObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.commands.temperature.EngineCoolantTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.FuelTrim;

/**
 * TODO put description
 */
public final class ObdConfig {

	public static ArrayList<ObdCommand> getCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		// Protocol
		cmds.add(new ObdResetCommand());

		// Control
		cmds.add(new CommandEquivRatioObdCommand());
		cmds.add(new DtcNumberObdCommand());
		cmds.add(new TimingAdvanceObdCommand());
		cmds.add(new TroubleCodesObdCommand(0));

		// Engine
		cmds.add(new EngineLoadObdCommand());
		cmds.add(new EngineRPMObdCommand());
		cmds.add(new EngineRuntimeObdCommand());
		cmds.add(new MassAirFlowObdCommand());

		// Fuel
		// cmds.add(new AverageFuelEconomyObdCommand());
		// cmds.add(new FuelEconomyObdCommand());
		// cmds.add(new FuelEconomyMAPObdCommand());
		// cmds.add(new FuelEconomyCommandedMAPObdCommand());
		cmds.add(new FindFuelTypeObdCommand());
		cmds.add(new FuelLevelObdCommand());
		cmds.add(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_1));
		cmds.add(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_2));
		cmds.add(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_1));
		cmds.add(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_2));

		// Pressure
		cmds.add(new BarometricPressureObdCommand());
		cmds.add(new FuelPressureObdCommand());
		cmds.add(new IntakeManifoldPressureObdCommand());

		// Temperature
		cmds.add(new AirIntakeTemperatureObdCommand());
		cmds.add(new AmbientAirTemperatureObdCommand());
		cmds.add(new EngineCoolantTemperatureObdCommand());

		// Misc
		cmds.add(new SpeedObdCommand());
		cmds.add(new ThrottlePositionObdCommand());

		return cmds;
	}

}