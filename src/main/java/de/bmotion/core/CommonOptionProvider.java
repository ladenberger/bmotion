package de.bmotion.core;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommonOptionProvider implements IBMotionOptionProvider {

	@Override
	public void installOptions(Options options) {
		options.addOption(Option.builder("workspace").hasArg().desc("Workspace").argName("workspace").build());
		options.addOption(Option.builder("host").hasArg().desc("Host").argName("host").build());
		options.addOption(Option.builder("port").hasArg().desc("Port").argName("port").build());
		options.addOption(Option.builder("local").hasArg().desc("Run on localhost").argName("local").build());

		options.addOption(Option.builder("socketHost").hasArg().desc("Socket Host").argName("socketHost").build());
		options.addOption(Option.builder("socketPort").hasArg().desc("Socket Port").argName("socketPort").build());
	}

}
