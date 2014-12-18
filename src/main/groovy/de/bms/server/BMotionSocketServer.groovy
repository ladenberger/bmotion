package de.bms.server

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import de.bms.BMotion
import de.bms.BMotionVisualisationProvider
import groovy.util.logging.Slf4j

@Slf4j
public class BMotionSocketServer {

    public final Map<String, BMotion> sessions = new HashMap<String, BMotion>();
    public final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();
    def SocketIOServer server

    public BMotionSocketServer(boolean standalone, String workspacePath,
                               BMotionVisualisationProvider visualisationProvider) {

        def config = new Configuration()
        config.setHostname("localhost")
        config.setPort(9090)
        server = new SocketIOServer(config)

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
        server.addEventListener("callMethod", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
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
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            def returnValue = bmotion.executeEvent(d.data)
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(returnValue);
                            }
                        }
                    }
                });

        server.addEventListener("transform", JsonObject.class,
                new DataListener<JsonObject>() {
                    @Override
                    public void onData(final SocketIOClient client, JsonObject d,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            def formulas = d.data.formulas
                            def returnValue = formulas.collect {
                                bmotion.eval(it)
                            }
                            if (ackRequest.isAckRequested()) {
                                ackRequest.sendAckData([values: returnValue]);
                            }
                        }
                    }
                });

        server.addEventListener("reloadModel", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String s,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            bmotion.reloadModel()
                        }
                    }
                });

        server.addEventListener("initSvgEditor", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String svgFile,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            def svg = bmotion.sessionConfiguration.bmsSvg
                            if (svg && ackRequest.isAckRequested()) {
                                ackRequest.sendAckData(svg.get(svgFile));
                            }
                        }
                    }
                });

        server.addEventListener("initialisation", String.class,
                new DataListener<String>() {
                    @Override
                    public void onData(final SocketIOClient client, String d,
                                       final AckRequest ackRequest) {
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            client.sendEvent("initialisation", bmotion.sessionConfiguration)
                            client.sendEvent("initSvg")
                            bmotion.refresh()
                        }
                    }
                });

        server.addEventListener("saveSvg", SvgEditorContent.class,
                new DataListener<SvgEditorContent>() {
                    @Override
                    public void onData(final SocketIOClient client, SvgEditorContent svg,
                                       final AckRequest ackRequest) {
                        client.sendEvent("initSvg")
                        String path = clients.get(client)
                        def BMotion bmotion = sessions.get(path) ?: null
                        if (bmotion != null) {
                            File svgFile = new File(bmotion.getTemplateFolder() + File.separator + svg.name)
                            svgFile.write(svg.content)
                            bmotion.sessionConfiguration.bmsSvg.put(svg.name, svg.content)
                            bmotion.refresh()
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

                sessionConfiguration.bmsSvg.keySet().each {
                    File svgFile = new File(templateFile.getParent() + File.separator + it)
                    sessionConfiguration.bmsSvg[it] = (svgFile.exists() ? svgFile.text :
                            '<svg width="325" height="430" xmlns="http://www.w3.org/2000/svg"></svg>')
                }
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
                    def data = [bmsSvg: bmotion.sessionConfiguration.bmsSvg, standalone: standalone]
                    ackRequest.sendAckData(data)
                }

            }
        });

    }

    public void start(String host, int port) {

        new Thread(new Runnable() {
            public void run() {
                boolean found = false
                while (!found && port < 9180) {
                    try {
                        found = true;
                        server.getConfiguration().setHostname(host)
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

    private BMotion createSession(String type, File templateFile, BMotionVisualisationProvider visualisationProvider) {
        if (type != null) {
            def visualisation = visualisationProvider.get(type, templateFile.getPath())
            visualisation != null ? visualisation : "BMotion Studio: No visualisation implementation found for " + type
        } else {
            log.error "BMotion Studio: Please enter a tool (e.g. BAnimation or CSPAnimation)"
        }
    }

}
