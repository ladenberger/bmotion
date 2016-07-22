package de.bmotion.core;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitSessionService {

	private final static Logger log = LoggerFactory.getLogger(InitSessionService.class);

	public static BMotion initSession(BMotionSocketServer server, String sessionId, String modelPath,
			Map<String, String> options) throws BMotionException {

		IBMotionVisualizationProvider visualisationProvider = server.getServer().getVisualisationProvider();
		if (visualisationProvider == null) {
			throw new BMotionException("No visualistaion provider installed.");
		}

		// String templateFolder = initSesionObject.getManifest() != null
		// ? new File(initSesionObject.getManifest()).getParent().toString() :
		// "";

		// Get correct path to model
		String fModelPath;
		if (BMotionServer.MODE_ONLINE.equals(server.getServer().getMode())) {
			// modelPath = server.getServer().getWorkspacePath() +
			// File.separator + templateFolder + File.separator
			// + initSesionObject.getModel();
			fModelPath = server.getServer().getWorkspacePath() + File.separator + modelPath;
		} else {
			// if (new File(initSesionObject.getModelPath()).isAbsolute()) {
			fModelPath = modelPath;
			// } else {
			// modelPath = templateFolder + File.separator +
			// initSesionObject.getModel();
			// }
		}

		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			modelPath = modelPath.replace("\\", "\\\\");
		}

		if (options == null) {
			options = Collections.emptyMap();
		}

		BMotion bms = visualisationProvider.get(sessionId, modelPath, options);
		if (bms == null) {
			throw new BMotionException("No visualisation implementation found for " + modelPath);
		} else {
			// bms.setMode(server.getServer().getMode());
			bms.getSessionData().put("tool", bms.getClass().getSimpleName());
			// bms.getClientData().put("templateFolder", templateFolder);
			bms.getSessionData().put("modelPath", fModelPath);
			server.getSessions().put(bms.getId(), bms);
			bms.initModel(fModelPath, options, server.getServer().getMode());
			log.info("Created new BMotion session " + bms.getId());
			return bms;
		}

	}

}
