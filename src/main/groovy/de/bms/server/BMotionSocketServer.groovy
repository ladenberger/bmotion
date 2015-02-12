package de.bms.server

import com.corundumstudio.socketio.*
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import de.bms.BMotion
import de.bms.BMotionSocketListenerProvider
import de.bms.BMotionVisualisationProvider
import groovy.util.logging.Slf4j

@Slf4j
public class BMotionSocketServer {

    public final Map<String, BMotion> sessions = new HashMap<String, BMotion>();
    public final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();
    def SocketIOServer server
    def boolean standalone = true
    def String workspacePath
    def BMotionVisualisationProvider visualisationProvider
    def BMotionSocketListenerProvider socketListenerProvider

    public BMotionSocketServer(boolean standalone, String workspacePath,
                               BMotionVisualisationProvider visualisationProvider,
                               BMotionSocketListenerProvider socketListenerProvider) {
        this.standalone = standalone
        this.workspacePath = workspacePath
        this.visualisationProvider = visualisationProvider
        this.socketListenerProvider = socketListenerProvider
    }

    private SocketIOServer createSocketIOServer(int port) {

        def config = new Configuration()
        def socketConfig = new SocketConfig()
        socketConfig.setReuseAddress(true)
        //config.setHostname("localhost")
        config.setPort(port)
        config.setSocketConfig(socketConfig)
        server = new SocketIOServer(config)

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {}
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                def BMotion bmotion = getSession(client)
                if (bmotion != null) {
                    bmotion.clients.remove(client)
                    BMotionSocketServer.log.info "Removed client " + client.sessionId + " from BMotion session " + bmotion.sessionId
                }
            }
        });

        //TODO: Check if return value of groovy method is a json object
        server.addEventListener("callMethod", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def BMotion bmotion = getSession(client)
                        if (bmotion != null) {
                            Object returnValue = bmotion.callGroovyMethod(d.data.name, d)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });

        server.addEventListener("executeEvent", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def BMotion bmotion = getSession(client)
                        if (bmotion != null) {
                            def returnValue = bmotion.executeEvent(d.data)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });

        server.addEventListener("observe", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        def BMotion bmotion = getSession(client)
                        if (bmotion != null) {
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(bmotion.observe(d));
                            }
                        }
                    }
                });

        server.addEventListener("eval", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bmotion = getSession(client)
                if (bmotion != null) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData([bmotion.eval(d.data.formula)]);
                    }
                }
            }
        });

        server.addEventListener("reloadModel", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String s,
                                       final AckRequest ackRequest) {
                        def BMotion bmotion = getSession(client)
                        if (bmotion != null) {
                            bmotion.reloadModel()
                        }
                    }
                });

        server.addEventListener("initSession", SessionConfiguration.class, new DataListener<SessionConfiguration>() {
            @Override
            public void onData(final SocketIOClient client, SessionConfiguration sessionConfiguration,
                               final AckRequest ackRequest) {

                URL url = new URL(sessionConfiguration.templateUrl)
                File templateFile = new File(workspacePath + File.separator + url.getPath().replace("/bms/", ""))
                BMotionSocketServer.log.debug "Template: " + templateFile
                def BMotion bmotion = sessions.get(url.getPath()) ?: null
                if (bmotion == null) {
                    bmotion = createSession(sessionConfiguration.tool, templateFile, visualisationProvider)
                    sessions.put(url.getPath(), bmotion)
                    BMotionSocketServer.log.info "Created new BMotion session " + bmotion.sessionId
                }
                // Add client to session
                BMotionSocketServer.log.info "Add client " + client.sessionId + " for BMotion session " + bmotion.sessionId
                bmotion.clients.add(client)
                // Bound client to current visualisation
                clients.put(client, url.getPath())
                // Initialise session
                bmotion.initSession(sessionConfiguration)
                BMotionSocketServer.log.info "Refresh BMotion session " + bmotion.sessionId
                // Send content of linked SVG files to client
                if (ackRequest.isAckRequested()) {
                    def data = [standalone: standalone]
                    ackRequest.sendAckData(data)
                }
                bmotion.refresh()
                client.sendEvent("initialised")

            }
        });

        socketListenerProvider?.installListeners(this)

        return server;

    }

    public void start(String host, int port) {

        new Thread(new Runnable() {
            public void run() {
                boolean found = false
                while (!found && port < 9180) {
                    try {
                        createSocketIOServer(port).start()
                        log.info "Socket.io started on host " + host + " and port " + port
                        found = true;
                        Thread.sleep(Integer.MAX_VALUE);
                        server.stop();
                    } catch (BindException ex) {
                        port++;
                    }
                }
                if (!found) {
                    log.error "No free port found between 9090 and 9179"
                }
            }
        }).start();

    }

    public BMotion getSession(SocketIOClient client) {
        String path = clients.get(client)
        return sessions.get(path) ?: null
    }

    private BMotion createSession(String type, File templateFile, BMotionVisualisationProvider visualisationProvider) {
        if (type != null) {
            def visualisation = visualisationProvider.get(type, templateFile.getPath())
            visualisation != null ? visualisation : "BMotion Studio: No visualisation implementation found for " + type
        } else {
            log.error "BMotion Studio: Please enter a tool (e.g. BAnimation or CSPAnimation)"
        }
    }

}
