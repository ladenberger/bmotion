package de.bms

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketConfig
import com.corundumstudio.socketio.SocketIOServer
import com.google.gson.stream.JsonWriter
import groovy.util.logging.Slf4j

@Slf4j
public class BMotionSocketServer {

    def SocketIOServer socket
    def BMotionServer server
    def boolean standalone = true

    public BMotionSocketServer(BMotionServer server) {
        this.server = server
        this.standalone = server.standalone
    }

    private SocketIOServer createSocketIOServer(int port) {

        def config = new Configuration()
        def socketConfig = new SocketConfig()
        socketConfig.setReuseAddress(true)
        //config.setHostname("localhost")
        config.setPort(port)
        config.setSocketConfig(socketConfig)
        config.setMaxFramePayloadLength(64 * 1024 * 100);
        socket = new SocketIOServer(config)
        server.socketListenerProvider?.installListeners(this)
        return socket;

    }

    private Boolean connectBMotionSocketServer(int port) {
        try {
            createSocketIOServer(port).start()
            return true;
        } catch (BindException ex) {
            log.error ex.getMessage()
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
                    log.info "Socket.io started on host " + host + ", port " + port
                    OutputStreamWriter writer = new OutputStreamWriter(System.out);
                    JsonWriter jsonWriter = new JsonWriter(writer);
                    jsonWriter.beginObject().name("host").value(host).name("port").value(port).endObject().flush();
                    server.serverStartedListener?.serverStarted(socket)
                    Thread.sleep(Integer.MAX_VALUE)
                    socket.stop()
                } else {
                    log.error "Socket.io cannot be started on host " + host + " and port " + port + " (port is used)."
                }

            }
        }).start();
    }

}
