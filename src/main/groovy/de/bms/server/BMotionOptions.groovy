package de.bms.server

import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder
import org.apache.commons.cli.Options

public class BMotionOptions {

    def Options options

    public BMotionOptions() {

        options = new Options()
        Option workspaceOption = OptionBuilder.withArgName("workspace").hasArg()
                .withDescription("Workspace").create("workspace");
        Option hostOption = OptionBuilder.withArgName("host").hasArg()
                .withDescription("Host").create("host");
        Option portOption = OptionBuilder.withArgName("port").hasArg()
                .withDescription("Port").create("port");
        options.addOption(workspaceOption)
        options.addOption(hostOption)
        options.addOption(portOption)

    }

}
