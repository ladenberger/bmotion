package de.bmotion.core;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitSessionService {

	private final static Logger log = LoggerFactory.getLogger(InitSessionService.class);

	public static BMotion initSession(BMotionSocketServer server, String sessionId, String modelPath, String groovyPath,
			Map<String, String> modelOptions) throws BMotionException {

		IBMotionVisualizationProvider visualisationProvider = server.getServer().getVisualisationProvider();
		if (visualisationProvider == null) {
			throw new BMotionException("No visualistaion provider installed.");
		}

		if (modelPath == null) {
			throw new BMotionException("Model path must be not null.");
		}

		if (modelOptions == null) {
			modelOptions = Collections.emptyMap();
		}

		String fModelPath = getCorrectPath(server, modelPath);

		BMotion bms = visualisationProvider.get(sessionId, fModelPath, modelOptions);
		if (bms == null) {
			throw new BMotionException("No visualisation implementation found for " + fModelPath);
		} else {
			bms.getSessionData().put("tool", bms.getClass().getSimpleName());
			bms.getSessionData().put("modelPath", fModelPath);
			server.getSessions().put(bms.getId(), bms);
			bms.initModel(fModelPath, modelOptions, server.getServer().getMode());

			if (groovyPath != null) {
				String fGroovyPath = getCorrectPath(server, groovyPath);
				bms.getSessionData().put("groovyPath", fGroovyPath);
				bms.initGroovyScript(fGroovyPath);
			}

			log.info("Created new BMotion session " + bms.getId());
			return bms;
		}

	}

	private static String getCorrectPath(BMotionSocketServer server, String path) {

		String fpath = path;
		if (BMotionServer.MODE_ONLINE.equals(server.getServer().getMode())) {
			fpath = server.getServer().getWorkspacePath() + File.separator + path;
		}

		// Workaround for windows paths
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			fpath = fpath.replace("\\", "\\\\");
		}

		return fpath;

	}

}
