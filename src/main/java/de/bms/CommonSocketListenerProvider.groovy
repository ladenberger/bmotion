package de.bms

import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import groovy.util.logging.Slf4j

@Slf4j
class CommonSocketListenerProvider implements BMotionSocketListenerProvider {

    public final long waitTime = 10000;
    public final long sessionWaitTime = 10000;
    private Thread exitThread;

    @Override
    void installListeners(BMotionSocketServer server) {

        server.getSocket().addEventListener("initSession", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {

                try {

                    def String tool = d.data.tool
                    def String manifest = d.data.manifest
                    def String modelPath
                    def String model = d.data.model
                    def options = d.data.options

                    def BMotion bms

                    def String templateFolder = ""
                    if (manifest != null) { // If manifest file exists, load visualization
                        templateFolder = new File(manifest).getParent().toString()
                    }

                    // Get correct path to model
                    if (BMotionServer.MODE_ONLINE.equals(server.getServer().getMode())) {
                        modelPath = server.getServer().getWorkspacePath() + File.separator + templateFolder + File.separator + model
                    } else {
                        if (new File(model).isAbsolute()) {
                            modelPath = model
                        } else {
                            modelPath = templateFolder + File.separator + model
                        }
                    }
                    bms = initSession(server, modelPath, tool, options)
                    bms.getClientData().put('templateFolder', templateFolder)

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(bms.getId());
                    }

                } catch (BMotionException e) {
                    ackRequest.sendAckData([errors: [e.getMessage()]])
                }

            }
        });

        server.getSocket().addEventListener("initView", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def String id = d.data.id
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null) {
                    def sessionId = bms.getId().toString()
                    bms.clients.add(client)
                    def sessionThread = server.getSessionThreads().get(sessionId)
                    if (sessionThread != null) {
                        sessionThread.interrupt()
                        server.getSessionThreads().remove(sessionId)
                    }
                    server.getClients().put(client, id)
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(bms.getClientData());
                    }
                } else {
                    ackRequest.sendAckData([errors: ["Session with id " + id + " does not exists!"]]);
                }
            }
        });

        server.getSocket().addEventListener("destroySession", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null) {
                    bms.disconnect();
                }
            }
        });

        server.getSocket().addEventListener("executeEvent", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {

                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null) {
                    def returnValue = bms.executeEvent(d.data)
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(returnValue)
                    }
                }
            }
        });

        server.getSocket().addEventListener("evaluateFormulas", JsonObject.class, new DataListener<JsonObject>() {
            @Override
            public void onData(final SocketIOClient client, JsonObject d,
                               final AckRequest ackRequest) {
                def BMotion bms = server.getSessions().get((String) d.data['id'])
                if (bms != null) {
                    try {
                        ackRequest.sendAckData(bms.evaluateFormulas(d));
                    } catch (BMotionException e) {
                        ackRequest.sendAckData([errors: [e.getMessage()]]);
                    }
                }
            }
        });

        server.getSocket().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                log.info("Client connected")
                if (exitThread) exitThread.interrupt();
            }
        });

        server.getSocket().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {

                def String id = server.getClients().get(client)
                def BMotion bms = server.getSessions().get(id)
                if (bms != null) {

                    //sessions.remove(id)
                    server.getClients().remove(client)
                    bms.getClients().remove(client)
                    //bms.disconnect();

                    if (bms.getClients().isEmpty()) {
                        startSessionTimer(server, bms)
                    }

                }

                // In standalone mode exit server when no client exists
                if (server.getServer().getMode() == BMotionServer.MODE_STANDALONE) {
                    def isEmptyClient = server.getSocket().getAllClients().isEmpty()
                    log.info("Check if no clients exist " + isEmptyClient)
                    if (server.getSocket().getAllClients().isEmpty()) {
                        startTimer(server);
                    }
                }

            }
        });

    }

    private void startTimer(BMotionSocketServer server) {

        log.info("Going to start timer thread")
        //ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        //singleThreadExecutor.execute();

        exitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Timer thread started")
                try {
                    Thread.sleep(waitTime);
                    log.info("Check if still no clients exist")
                    if (server.getSocket().getAllClients().isEmpty()) {
                        log.info("Close BMotion Studio for ProB server process")
                        System.exit(-1)
                    }
                } catch (InterruptedException e) {
                    log.info("Timer thread interrupted " + e.getMessage())
                } finally {
                    log.info("Exit timer thread")
                }
            }
        });
        exitThread.start();
        //log.info("Is alive? " + exitThread.isAlive().toString())

    }

    private void startSessionTimer(BMotionSocketServer server, BMotion bms) {

        log.info("Going to start session timer thread")
        //ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        //singleThreadExecutor.execute();

        def sessionId = bms.getId().toString()
        def sessionThread = server.getSessionThreads().get(sessionId)

        if (sessionThread == null) {

            sessionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sessionWaitTime);
                        if (bms.clients.isEmpty()) {
                            log.info("Remove session " + bms.getId())
                            bms.disconnect()
                            server.getSessionThreads().remove(sessionId)
                        }
                    } catch (InterruptedException e) {
                        log.info("Session timer thread interrupted " + e.getMessage())
                    } finally {
                        log.info("Exit session timer thread")
                    }
                }
            });
            server.getSessionThreads().put(sessionId, sessionThread)
            sessionThread.start()

        }

    }

    private
    static BMotion initSession(BMotionSocketServer server, String modelPath, String tool, options) throws BMotionException {

        def String sessionId = UUID.randomUUID() // The id should come from the client!
        def BMotion bms = createSession(sessionId, tool, server.getServer().getVisualisationProvider());
        if (bms != null) {
            bms.setMode(server.getServer().getMode())
            bms.startSession(modelPath, options)
            server.getSessions().put(sessionId, bms)
            log.info "Created new BMotion session " + sessionId
            return bms
        } else {
            throw new BMotionException("BMotion Studio session could not be initialised!")
        }

    }

    private
    static BMotion createSession(String id, String tool, BMotionVisualisationProvider visualisationProvider) throws BMotionException {
        if (tool != null) {
            def visualisation = visualisationProvider.get(id, tool)
            if (visualisation == null) {
                throw new BMotionException("No visualisation implementation found for " + tool)
            } else {
                return visualisation;
            }
        } else {
            throw new BMotionException("Please specify a tool in bmotion.json (e.g. BAnimation or CSPAnimation)")
        }
    }

}
