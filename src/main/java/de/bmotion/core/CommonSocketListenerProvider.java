package de.bmotion.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import de.bmotion.core.objects.CallMethodObject;
import de.bmotion.core.objects.ErrorObject;
import de.bmotion.core.objects.ExecuteEventObject;
import de.bmotion.core.objects.InitSessionObject;
import de.bmotion.core.objects.ObserverFormulaListObject;

public class CommonSocketListenerProvider implements IBMotionSocketListenerProvider {

	public final Logger log = LoggerFactory.getLogger(CommonSocketListenerProvider.class);

	private final Map<String, Thread> sessionThreads = new HashMap<String, Thread>();

	public final long waitTime = 10000;
	public final long sessionWaitTime = 10000;

	@Override
	public void installListeners(BMotionSocketServer server) {

		server.getSocket().addEventListener("initSession", InitSessionObject.class,
				new DataListener<InitSessionObject>() {
					@Override
					public void onData(final SocketIOClient client, InitSessionObject initSesionObject,
							final AckRequest ackRequest) {

						try {

							File manifestFile = new File(initSesionObject.getManifestFilePath());
							String templateFolder = manifestFile.getParent();
							String modelPath = templateFolder + File.separator + initSesionObject.getModelPath();
							String groovyPath = null;

							if (initSesionObject.getGroovyPath() != null) {
								groovyPath = templateFolder + File.separator + initSesionObject.getGroovyPath();
							}

							// Initialize new session
							BMotion bms = InitSessionService.initSession(server, initSesionObject.getSessionId(),
									modelPath, groovyPath, initSesionObject.getOptions());
							bms.getSessionData().put("manifestFilePath", initSesionObject.getManifestFilePath());
							bms.getSessionData().put("templateFolder", templateFolder);
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
					try {
						// Add client only if not exists
						if (!bms.getClients().contains(client)) {
							bms.getClients().add(client);
						}
						Thread sessionThread = sessionThreads.get(sessionId.toString());
						if (sessionThread != null) {
							sessionThread.interrupt();
							sessionThreads.remove(sessionId);
						}
						Object groovyPath = bms.getSessionData().get("groovyPath");
						if (groovyPath != null) {
							bms.initGroovyScript(String.valueOf(groovyPath));
						}
						bms.sessionLoaded();
						server.getClients().put(client, sessionId);
						server.getSessions().put(sessionId, bms);
						if (ackRequest.isAckRequested()) {
							ackRequest.sendAckData(bms.getSessionData(), bms.getToolData());
						}

					} catch (BMotionException e) {
						ackRequest.sendAckData(new ErrorObject(e.getMessage()));
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

						Object returnObject;
						BMotion bms = server.getSessions().get(event.getSessionId());
						if (bms != null) {
							try {
								returnObject = bms.executeEvent(event.getOptions());

							} catch (BMotionException e) {
								returnObject = new ErrorObject(e.getMessage());
							}
						} else {
							returnObject = new ErrorObject(
									"Session with id " + event.getSessionId() + " does not exists!");
						}

						if (ackRequest.isAckRequested()) {
							ackRequest.sendAckData(returnObject);
						}

					}
				});

		server.getSocket().addEventListener("callMethod", CallMethodObject.class, new DataListener<CallMethodObject>() {
			@Override
			public void onData(final SocketIOClient client, CallMethodObject method, final AckRequest ackRequest) {

				BMotion bms = server.getSessions().get(method.getSessionId());
				if (bms != null) {

					Object returnObject;

					try {
						returnObject = bms.callMethod(method.getName(), method.getArguments());
					} catch (BMotionException e) {
						returnObject = new ErrorObject(e.getMessage());
					}
				
					if (ackRequest.isAckRequested()) {
						ackRequest.sendAckData(returnObject);
					}

				} else {
					ackRequest.sendAckData(
							new ErrorObject("Session with id " + method.getSessionId() + " does not exists!"));
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
			}
		});

		server.getSocket().addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {

				String id = server.getClients().get(client);
				BMotion bms = server.getSessions().get(id);
				if (bms != null) {
					log.info("Client disconnected");
					server.getClients().remove(client);
					bms.getClients().remove(client);
					if (bms.getClients().isEmpty()) {
						startSessionTimer(server, bms);
					}
				}

			}
		});

	}

	private void startSessionTimer(BMotionSocketServer server, BMotion bms) {

		log.info("Going to start session timer thread");

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
