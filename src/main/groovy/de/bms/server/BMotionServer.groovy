package de.bms.server

import com.google.common.io.Resources
import de.bms.BMotionOptionProvider
import de.bms.BMotionSocketListenerProvider
import de.bms.BMotionVisualisationProvider
import de.bms.DesktopApi
import groovy.util.logging.Slf4j
import org.apache.commons.cli.*
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection

@Slf4j
public class BMotionServer {

    private String workspacePath

    private BMotionVisualisationProvider visualisationProvider

    private BMotionSocketListenerProvider socketListenerProvider

    private URL[] resourcePaths

    def BMotionSocketServer socketServer

    def CommandLine cmdLine

    def boolean standalone = false

    def ResourceResolver resourceResolver = new DefaultResourceResolver()

    def int port = 18080
    def boolean customPort = false
    def int socketPort = 19090
    def boolean customSocketPort = false

    def String host = "0.0.0.0"
    def String socketHost = "0.0.0.0"

    def String visualisation = ""

    public BMotionServer(String[] args, BMotionOptionProvider optionProvider) {

        Options options = new Options()
        options.addOption(OptionBuilder.withArgName("workspace").hasArg()
                .withDescription("Workspace").create("workspace"))
        options.addOption(OptionBuilder.withArgName("host").hasArg()
                .withDescription("Host").create("host"))
        options.addOption(OptionBuilder.withArgName("port").hasArg()
                .withDescription("Port").create("port"))
        options.addOption(OptionBuilder.withArgName("socketHost").hasArg()
                .withDescription("Socket Host").create("socketHost"))
        options.addOption(OptionBuilder.withArgName("socketPort").hasArg()
                .withDescription("Socket Port").create("socketPort"))
        options.addOption(OptionBuilder.withArgName("visualisation").hasArg()
                .withDescription("Open specific visualisation").create("visualisation"))
        options.addOption(new Option("standalone", "Run in standalone mode"));
        options.addOption(new Option("local", "Run on localhost"));

        if (optionProvider != null)
            optionProvider.installOptions(options)

        CommandLineParser parser = new BasicParser()
        cmdLine = parser.parse(options, args);
        if (cmdLine.hasOption("workspace")) {
            this.workspacePath = cmdLine.getOptionValue("workspace");
        }
        if (cmdLine.hasOption("local")) {
            this.host = "localhost"
        }
        if (cmdLine.hasOption("host")) {
            this.host = cmdLine.getOptionValue("host")
        }
        if (cmdLine.hasOption("port")) {
            this.port = Integer.parseInt(cmdLine.getOptionValue("port"))
            this.customPort = true
        }
        if (cmdLine.hasOption("socketHost")) {
            this.socketHost = cmdLine.getOptionValue("socketHost")
        }
        if (cmdLine.hasOption("socketPort")) {
            this.socketPort = Integer.parseInt(cmdLine.getOptionValue("socketPort"))
            this.customSocketPort = true
        }
        if (cmdLine.hasOption("standalone")) {
            this.standalone = true
        }
        if (cmdLine.hasOption("visualisation")) {
            this.visualisation = cmdLine.getOptionValue("visualisation")
        }

    }

    public void setResourcePaths(URL[] resourcePaths) {
        this.resourcePaths = resourcePaths
    }

    public void start() {
        startBMotionSocketServer()
        startBMotionJettyServer()
    }

    private Boolean connectBMotionJettyServer(Server server, int port) {
        try {
            Connector connector = new SelectChannelConnector();
            connector.setStatsOn(true);
            connector.setServer(server);
            connector.setHost(host);
            Connector[] connectors = [connector]
            server.setConnectors(connectors);
            connector.setPort(port);
            server.start();
            return true;
            log.info "Jetty server started on host " + host + " and port " + port
        } catch (BindException ex) {
            return false;
        }
    }

    private startBMotionJettyServer() {
        Server server = new Server();
        server.setHandler(setupWorkspaceHandler())
        boolean found = false;
        if (customPort) {
            if (connectBMotionJettyServer(server, port)) {
                found = true;
            }
        } else {
            while (!found && port < 18180) {
                if (connectBMotionJettyServer(server, port)) {
                    found = true;
                } else {
                    port++;
                }
            }
            if (!found) {
                log.error "No free port found between 18080 and 18179"
            }
        }
        if (found) {
            log.info "Jetty server started on host " + host + " and port " + port
        } else {
            log.error "Jetty server cannot be started on host " + host + " and port " + port + " (port is used)."
        }
    }

    private ContextHandler setupWorkspaceHandler() {
        ContextHandler context = new ContextHandler();
        context.setContextPath("/bms");
        ResourceHandler resHandler = new ResourceHandler()
        Resource[] s = [Resource.newResource(resourceResolver.resolve(Resources.getResource("content"))), Resource.
                newResource(workspacePath)]
        resourcePaths.each {
            s += Resource.newResource(resourceResolver.resolve(it))
        }
        ResourceCollection resources = new ResourceCollection(s);
        resHandler.setBaseResource(resources)
        resHandler.setCacheControl("no-cache")
        resHandler.setDirectoriesListed(true)
        context.setHandler(resHandler)
        return context
    }

    private void startBMotionSocketServer() {
        // Create socket server
        socketServer = new BMotionSocketServer(standalone, workspacePath, visualisationProvider,
                socketListenerProvider)
        socketServer.start(socketHost, socketPort, customSocketPort)
    }

    public int getPort() {
        return port
    }

    public String getHost() {
        return host
    }

    public void openBrowser() {
        java.net.URI uri = new java.net.URI("http://" + host + ":" + port + "/bms/" + visualisation)
        DesktopApi.browse(uri)
    }

    public void setSocketListenerProvider(BMotionSocketListenerProvider provider) {
        this.socketListenerProvider = provider
    }

    public void setVisualisationProvider(BMotionVisualisationProvider provider) {
        this.visualisationProvider = provider
    }

}
