package de.bmotion.core;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.stream.JsonWriter;

public class BMotionSocketServer {

	private final Logger log = LoggerFactory.getLogger(BMotionSocketServer.class);

	private SocketIOServer socket;
	private BMotionServer server;
	private final Map<String, BMotion> sessions = new HashMap<String, BMotion>();
	private final Map<SocketIOClient, String> clients = new HashMap<SocketIOClient, String>();

	public BMotionSocketServer(BMotionServer server) throws BMotionException {
		if (server == null) {
			throw new BMotionException("You must provide an instance of BMotionServer.");
		}
		this.server = server;
	}

	private SocketIOServer createSocketIOServer(int port) {

		Configuration config = new Configuration();
		SocketConfig socketConfig = new SocketConfig();
		socketConfig.setReuseAddress(true);
		// config.setHostname("localhost")
		config.setPort(port);
		config.setSocketConfig(socketConfig);
		config.setMaxFramePayloadLength(64 * 1024 * 100);
		config.setPingTimeout(900000); // 15 min
		socket = new SocketIOServer(config);
		server.getSocketListenerProvider().add(new CommonSocketListenerProvider());
		server.getSocketListenerProvider().forEach(listener -> listener.installListeners(this));
		return socket;

	}

	private Boolean connectBMotionSocketServer(int port) {
		createSocketIOServer(port).start();
		return true;
	}

	public void start(String host, int port, boolean customPort) {
		new Thread(new Runnable() {
			public void run() {
				boolean found = false;
				int fport = port;
				if (customPort) {
					if (connectBMotionSocketServer(fport)) {
						found = true;
					}
				} else {
					while (!found && port < 19180) {
						if (connectBMotionSocketServer(fport)) {
							found = true;
						} else {
							fport++;
						}
					}
					if (!found) {
						log.error("No free port found between 19090 and 19179");
					}
				}
				if (found) {
					try {
						log.info("Socket.io started on host " + host + ", port " + fport);
						OutputStreamWriter writer = new OutputStreamWriter(System.out);
						JsonWriter jsonWriter = new JsonWriter(writer);
						jsonWriter.beginObject().name("host").value(host).name("port").value(fport).endObject().flush();
						jsonWriter.close();
						server.getServerStartedListener().forEach(listener -> listener.serverStarted(socket));
						Thread.sleep(Integer.MAX_VALUE);
						socket.stop();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					log.error(
							"Socket.io cannot be started on host " + host + " and port " + fport + " (port is used).");
				}

			}
		}).start();
	}

	public void stop() {
		if (socket != null) {
			socket.stop();
			server.getServerStartedListener().forEach(listener -> listener.serverStopped(socket));
		}
	}

	public SocketIOServer getSocket() {
		return socket;
	}

	public BMotionServer getServer() {
		return server;
	}

	public Map<String, BMotion> getSessions() {
		return sessions;
	}

	public Map<SocketIOClient, String> getClients() {
		return clients;
	}

}
