package de.bms.server

import com.google.common.io.Resources
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

    private URL[] resourcePaths

    def BMotionSocketServer socketServer

    def boolean standalone = false

    def ResourceResolver resourceResolver = new DefaultResourceResolver()

    def int port = 8080
    def int socketPort = 9090

    def String host = "localhost"
    def String socketHost = "localhost"

    def String visualisation = ""

    public BMotionServer(String[] args, BMotionVisualisationProvider visualisationProvider) {

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

        CommandLineParser parser = new BasicParser()
        CommandLine line = parser.parse(options, args);
        if (line.hasOption("workspace")) {
            this.workspacePath = line.getOptionValue("workspace");
        }
        if (line.hasOption("host")) {
            this.host = line.getOptionValue("host")
        }
        if (line.hasOption("port")) {
            this.port = Integer.parseInt(line.getOptionValue("port"))
        }
        if (line.hasOption("socketHost")) {
            this.socketHost = line.getOptionValue("socketHost")
        }
        if (line.hasOption("socketPort")) {
            this.socketPort = Integer.parseInt(line.getOptionValue("socketPort"))
        }
        if (line.hasOption("standalone")) {
            this.standalone = true
        }
        if (line.hasOption("visualisation")) {
            this.visualisation = line.getOptionValue("visualisation")
        }

        // Create socket server
        socketServer = new BMotionSocketServer(standalone, workspacePath, visualisationProvider)

    }

    public void setResourcePaths(URL[] resourcePaths) {
        this.resourcePaths = resourcePaths
    }

    public void start() {
        startBMotionSocketServer()
        startBMotionJettyServer()
        openBrowser()
    }

    private startBMotionJettyServer() {
        Server server = new Server();
        server.setHandler(setupWorkspaceHandler())
        boolean found = false
        while (!found && port < 8180) {
            try {
                Connector connector = new SelectChannelConnector();
                connector.setStatsOn(true);
                connector.setServer(server);
                connector.setHost(host);
                Connector[] connectors = [connector]
                server.setConnectors(connectors);
                connector.setPort(port);
                server.start();
                found = true;
                log.info "Jetty server started on host " + host + " and port " + port
            } catch (BindException ex) {
                port++;
            }
        }
        if (!found) {
            log.error "No free port found between 8080 and 8179"
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
        socketServer.start(socketHost, socketPort)
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

}
