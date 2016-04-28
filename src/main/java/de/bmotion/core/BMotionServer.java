package de.bmotion.core;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMotionServer {

	private final Logger log = LoggerFactory.getLogger(BMotionServer.class);

	public final static String MODE_INTEGRATED = "ModeIntegrated";
	public final static String MODE_STANDALONE = "ModeStandalone";
	public final static String MODE_ONLINE = "ModeOnline";

	private String mode = MODE_INTEGRATED;

	private String workspacePath;

	private IBMotionVisualizationProvider visualisationProvider;

	private final List<IBMotionSocketListenerProvider> socketListenerProvider = new ArrayList<IBMotionSocketListenerProvider>();

	private final List<ISocketServerListener> serverStartedListener = new ArrayList<ISocketServerListener>();

	// private URL[] resourcePaths;

	private BMotionSocketServer socketServer;

	private CommandLine cmdLine;

	// private IResourceResolver resourceResolver = new
	// DefaultResourceResolver();

	private final Options options = new Options();

	private int port = 18080;
	private boolean customPort = false;
	private int socketPort = 19090;
	private boolean customSocketPort = false;

	private String host = "0.0.0.0";
	private String socketHost = "0.0.0.0";

	public BMotionServer(String[] args, IBMotionVisualizationProvider visualizationProvider) throws BMotionException {
		this(args, new DefaultOptionProvider(), visualizationProvider);
	}

	public BMotionServer(String[] args, IBMotionOptionProvider optionProvider,
			IBMotionVisualizationProvider visualizationProvider) throws BMotionException {

		if (visualizationProvider == null) {
			throw new BMotionException("Please provide a visualisation provider.");
		}

		if (optionProvider == null) {
			throw new BMotionException("Options provider must not be null.");
		}

		this.visualisationProvider = visualizationProvider;

		// Install common options
		new CommonOptionProvider().installOptions(options);
		// Install custom options
		optionProvider.installOptions(options);

		// Parse and handle command line arguments
		CommandLineParser parser = new DefaultParser();
		try {
			cmdLine = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (cmdLine.hasOption("workspace")) {
			this.workspacePath = cmdLine.getOptionValue("workspace");
		}
		if (cmdLine.hasOption("local")) {
			this.host = "localhost";
		}
		if (cmdLine.hasOption("host")) {
			this.host = cmdLine.getOptionValue("host");
		}
		if (cmdLine.hasOption("port")) {
			this.port = Integer.parseInt(cmdLine.getOptionValue("port"));
			this.customPort = true;
		}
		if (cmdLine.hasOption("socketHost")) {
			this.socketHost = cmdLine.getOptionValue("socketHost");
		}
		if (cmdLine.hasOption("socketPort")) {
			this.socketPort = Integer.parseInt(cmdLine.getOptionValue("socketPort"));
			this.customSocketPort = true;
		}

	}

	/*
	 * public void setResourcePaths(URL[] resourcePaths) { this.resourcePaths =
	 * resourcePaths; }
	 */

	public void start() throws BMotionException {
		startBMotionSocketServer();
	}

	public void stop() {
		if (socketServer != null) {
			socketServer.stop();
		}
	}

	public void startWithJetty() throws Exception {
		start();
		startBMotionJettyServer();
	}

	private Boolean connectBMotionJettyServer(Server server, int port) throws Exception {
		try {
			Connector connector = new SelectChannelConnector();
			connector.setStatsOn(true);
			connector.setServer(server);
			connector.setHost(host);
			Connector[] connectors = new Connector[] { connector };
			server.setConnectors(connectors);
			connector.setPort(port);
			server.start();
			log.info("Jetty server started on host " + host + " and port " + port);
			return true;
		} catch (BindException ex) {
			log.error(ex.getMessage());
			return false;
		}
	}

	private void startBMotionJettyServer() throws Exception {
		Server server = new Server();
		server.setHandler(setupWorkspaceHandler());
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
				log.error("No free port found between 18080 and 18179");
			}
		}
		if (found) {
			log.info("Jetty server started on host " + host + " and port " + port);
		} else {
			log.error("Jetty server cannot be started on host " + host + " and port " + port + " (port is used).");
		}
	}

	private ContextHandler setupWorkspaceHandler() {
		ContextHandler context = new ContextHandler();
		// context.setContextPath("/");
		ResourceHandler resHandler = new ResourceHandler();
		// Resource[] s = new Resource[] { Resource.newResource(workspacePath)
		// };
		// Resource[] s = resourcePaths.map( r ->
		// Resource.newResource(resourceResolver.resolve(r)));
		// ResourceCollection resources = new ResourceCollection(s);
		// resHandler.setBaseResource(resources);
		resHandler.setCacheControl("no-cache");
		resHandler.setDirectoriesListed(true);
		context.setHandler(resHandler);
		return context;
	}

	private void startBMotionSocketServer() throws BMotionException {
		// Create socket server
		socketServer = new BMotionSocketServer(this);
		socketServer.start(socketHost, socketPort, customSocketPort);
	}

	public List<ISocketServerListener> getServerStartedListener() {
		return serverStartedListener;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getWorkspacePath() {
		return workspacePath;
	}

	public IBMotionVisualizationProvider getVisualisationProvider() {
		return visualisationProvider;
	}

	public CommandLine getCmdLine() {
		return cmdLine;
	}

	public List<IBMotionSocketListenerProvider> getSocketListenerProvider() {
		return socketListenerProvider;
	}

	public BMotionSocketServer getSocketServer() {
		return socketServer;
	}

}
