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

        server.addEventListener("refresh", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String s,
                                       final AckRequest ackRequest) {
                        def BMotion bmotion = getSession(client)
                        if (bmotion != null) {
                            bmotion.refresh()
                        }
                    }
                });

        socketListenerProvider?.installListeners(this)

        return server;

    }

    private Boolean connectBMotionSocketServer(int port) {
        try {
            createSocketIOServer(port).start()
            return true;
        } catch (BindException ex) {
            return false;
        }
    }

    public void start(String host, int port, boolean customPort) {
        new Thread(new Runnable() {
            public void run() {
                boolean found = false;
                if (customPort) {
                    if (connectBMotionSocketServer(port)) {
                        found = true;
                    }
                } else {
                    while (!found && port < 19180) {
                        if (connectBMotionSocketServer(port)) {
                            found = true;
                        } else {
                            port++;
                        }
                    }
                    if (!found) {
                        log.error "No free port found between 19090 and 19179"
                    }
                }
                if (found) {
                    log.info "Socket.io started on host " + host + " and port " + port
                    Thread.sleep(Integer.MAX_VALUE);
                    server.stop();
                } else {
                    log.error "Socket.io cannot be started on host " + host + " and port " + port + " (port is used)."
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
