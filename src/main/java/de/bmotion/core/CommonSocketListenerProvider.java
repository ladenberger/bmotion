package de.bmotion.core;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import de.bmotion.core.objects.ErrorObject;
import de.bmotion.core.objects.ExecuteEventObject;
import de.bmotion.core.objects.InitSessionObject;
import de.bmotion.core.objects.ObserverFormulaListObject;

public class CommonSocketListenerProvider implements IBMotionSocketListenerProvider {

	public final Logger log = LoggerFactory.getLogger(CommonSocketListenerProvider.class);

	private final Map<String, Thread> sessionThreads = new HashMap<String, Thread>();

	public final long waitTime = 10000;
	public final long sessionWaitTime = 10000;
	private Thread exitThread;

	@Override
	public void installListeners(BMotionSocketServer server) {

		server.getSocket().addEventListener("initSession", InitSessionObject.class,
				new DataListener<InitSessionObject>() {
					@Override
					public void onData(final SocketIOClient client, InitSessionObject initSesionObject,
							final AckRequest ackRequest) {

						try {

							// Destroy/disconnect old client session (if exists)
							BMotion oldBmsVisualization = server.getSessions().get(server.getClients().get(client));
							if (oldBmsVisualization != null) {
								oldBmsVisualization.disconnect();
							}

							// Initialize new session
							BMotion bms = InitSessionService.initSession(server, initSesionObject.getSessionId(),
									initSesionObject.getModelPath(), initSesionObject.getOptions());
							bms.getSessionData().put("manifestFilePath", initSesionObject.getManifestFilePath());
							bms.getClients().add(client);
							server.getClients().put(client, bms.getId());

							// Send session data to client
							if (ackRequest.isAckRequested()) {
								ackRequest.sendAckData(bms.getSessionData(), bms.getToolData());
							}

						} catch (BMotionException e) {
							ackRequest.sendAckData(new ErrorObject(e.getMessage()));
						}

					}
				});

		server.getSocket().addEventListener("loadSession", String.class, new DataListener<String>() {
			@Override
			public void onData(final SocketIOClient client, String sessionId, final AckRequest ackRequest) {
				BMotion bms = server.getSessions().get(sessionId);
				if (bms != null) {
					bms.getClients().add(client);
					Thread sessionThread = sessionThreads.get(sessionId.toString());
					if (sessionThread != null) {
						sessionThread.interrupt();
						sessionThreads.remove(sessionId);
					}
					server.getClients().put(client, sessionId);
					if (ackRequest.isAckRequested()) {
						ackRequest.sendAckData(bms.getSessionData(), bms.getToolData());
					}
				} else {
					ackRequest.sendAckData(new ErrorObject("Session with id " + sessionId + " does not exists!"));
				}
			}
		});

		server.getSocket().addEventListener("destroySession", String.class, new DataListener<String>() {
			@Override
			public void onData(final SocketIOClient client, String sessionId, final AckRequest ackRequest) {
				BMotion bms = server.getSessions().get(sessionId);
				if (bms != null) {
					bms.disconnect();
				} else {
					ackRequest.sendAckData(new ErrorObject("Session with id " + sessionId + " does not exists!"));
				}
			}
		});

		server.getSocket().addEventListener("executeEvent", ExecuteEventObject.class,
				new DataListener<ExecuteEventObject>() {
					@Override
					public void onData(final SocketIOClient client, ExecuteEventObject event,
							final AckRequest ackRequest) {

						BMotion bms = server.getSessions().get(event.getSessionId());
						if (bms != null) {
							try {
								Object returnObject = bms.executeEvent(event.getName(), event.getOptions());
								ackRequest.sendAckData(returnObject);
							} catch (BMotionException e) {
								ackRequest.sendAckData(new ErrorObject(e.getMessage()));
							}
						} else {
							ackRequest.sendAckData(
									new ErrorObject("Session with id " + event.getSessionId() + " does not exists!"));
						}

					}
				});

		server.getSocket().addEventListener("evaluateFormulas", ObserverFormulaListObject.class,
				new DataListener<ObserverFormulaListObject>() {
					@Override
					public void onData(final SocketIOClient client, ObserverFormulaListObject oList,
							final AckRequest ackRequest) {
						BMotion bms = server.getSessions().get(oList.getSessionId());
						if (bms != null) {
							try {
								Map<String, Map<String, Object>> evaluateFormulas = bms
										.evaluateFormulas(oList.getFormulas());
								ackRequest.sendAckData(evaluateFormulas);
							} catch (BMotionException e) {
								ackRequest.sendAckData(new ErrorObject(e.getMessage()));
							}
						} else {
							ackRequest.sendAckData(
									new ErrorObject("Session with id " + oList.getSessionId() + " does not exists!"));
						}
					}
				});

		server.getSocket().addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) {
				log.info("Client connected");
				if (exitThread != null)
					exitThread.interrupt();
			}
		});

		server.getSocket().addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {

				String id = server.getClients().get(client);
				BMotion bms = server.getSessions().get(id);
				if (bms != null) {

					// sessions.remove(id)
					server.getClients().remove(client);
					bms.getClients().remove(client);
					// bms.disconnect();

					if (bms.getClients().isEmpty()) {
						startSessionTimer(server, bms);
					}

				}

				// In standalone mode exit server when no client exists
				if (server.getServer().getMode() == BMotionServer.MODE_STANDALONE) {
					boolean isEmptyClient = server.getSocket().getAllClients().isEmpty();
					log.info("Check if no clients exist " + isEmptyClient);
					if (server.getSocket().getAllClients().isEmpty()) {
						startTimer(server);
					}
				}

			}
		});

	}

	private void startTimer(BMotionSocketServer server) {

		log.info("Going to start timer thread");
		// ExecutorService singleThreadExecutor =
		// Executors.newSingleThreadExecutor();
		// singleThreadExecutor.execute();

		exitThread = new Thread(new Runnable() {
			@Override
			public void run() {
				log.info("Timer thread started");
				try {
					Thread.sleep(waitTime);
					log.info("Check if still no clients exist");
					if (server.getSocket().getAllClients().isEmpty()) {
						log.info("Close BMotionWeb server process");
						System.exit(-1);
					}
				} catch (InterruptedException e) {
					log.info("Timer thread interrupted " + e.getMessage());
				} finally {
					log.info("Exit timer thread");
				}
			}
		});
		exitThread.start();
		// log.info("Is alive? " + exitThread.isAlive().toString())

	}

	private void startSessionTimer(BMotionSocketServer server, BMotion bms) {

		log.info("Going to start session timer thread");
		// ExecutorService singleThreadExecutor =
		// Executors.newSingleThreadExecutor();
		// singleThreadExecutor.execute();

		String sessionId = bms.getId().toString();
		Thread sessionThread = sessionThreads.get(sessionId);

		if (sessionThread == null) {

			sessionThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(sessionWaitTime);
						if (bms.getClients().isEmpty()) {
							log.info("Remove session " + bms.getId());
							bms.disconnect();
							sessionThreads.remove(sessionId);
						}
					} catch (InterruptedException e) {
						log.info("Session timer thread interrupted " + e.getMessage());
					} finally {
						log.info("Exit session timer thread");
					}
				}
			});
			sessionThreads.put(sessionId, sessionThread);
			sessionThread.start();

		}

	}

}
