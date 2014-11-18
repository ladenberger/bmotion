package de.bms.server

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import de.bms.BMotion
import de.bms.itool.ITool
import de.bms.itool.ToolRegistry
import groovy.util.logging.Slf4j

@Slf4j
public class BMotionSocketServer {

    public final Map<String, BMotion> sessions = new HashMap<String, BMotion>();
    public final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();
    def SocketIOServer server

    public void start(String host, int port, String workspacePath, BMotionScriptEngineProvider scriptEngineProvider,
                      BMotionIToolProvider iToolProvider) {

        Configuration config = new Configuration();
        config.setHostname(host)
        config.setPort(port)
        server = new SocketIOServer(config);

        ToolRegistry toolRegistry = new ToolRegistry()

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {}
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {

                String path = clients.get(client)
                clients.remove(client)
                def BMotion bmotion = sessions.get(path) ?: null
                if (bmotion != null) {
                    bmotion.clients.remove(client)
                    BMotionSocketServer.log.info "Removed client " + client.sessionId + " from BMotion session " + bmotion.sessionId
                }

            }
        });

        //TODO: Check if return value of groovy method is a json object
        server.addEventListener("callGroovyMethod", CallGroovyMethodObject.class,
                new DataListener<CallGroovyMethodObject>() {
                    @Override
                    public void onData(final SocketIOClient client, CallGroovyMethodObject data,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            Object returnValue = bmotion.callGroovyMethod(data.name, data.data)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });

        server.addEventListener("reloadModel", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String data,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            bmotion.loadModel()
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
                    bmotion = createSession(toolRegistry, sessionConfiguration, templateFile, scriptEngineProvider,
                            iToolProvider)
                    sessions.put(url.getPath(), bmotion)
                    BMotionSocketServer.log.info "Created new BMotion session " + bmotion.sessionId
                } else {
                    bmotion.refreshSession()
                }
                // Add client to session
                bmotion.clients.add(client)
                // Bound client to current visualisation
                clients.put(client, url.getPath())
                BMotionSocketServer.log.info "Refresh BMotion session " + bmotion.sessionId

            }
        });

        new Thread(new Runnable() {
            public void run() {
                boolean found = false
                while (!found && port < 9180) {
                    try {
                        found = true;
                        server.getConfiguration().setPort(port)
                        server.start();
                        log.info "Socket.io started on host " + host + " and port " + port
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

    //TODO: Make initialisation of ITool implementations generic
    private BMotion createSession(ToolRegistry toolRegistry, SessionConfiguration sessionConfiguration,
                                  File templateFile, BMotionScriptEngineProvider scriptEngineProvider,
                                  BMotionIToolProvider iToolProvider) {
        //BMotionSocketServer.log.info "Going to create new session for " + sessionId
        UUID sessionId = UUID.randomUUID()
        if (sessionConfiguration.tool != null) {
            def ITool tool = iToolProvider.get(sessionConfiguration.tool, toolRegistry)
            return new BMotion(sessionId, tool, toolRegistry, sessionConfiguration, templateFile.getPath(),
                    scriptEngineProvider)
        } else {
            log.error "BMotion Studio: Please enter a tool (e.g. BAnimation or CSPAnimation)"
        }
    }

}
