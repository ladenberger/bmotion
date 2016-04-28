package de.bmotion.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMotionScriptException {

	private final Logger log = LoggerFactory.getLogger(BMotionScriptException.class);

	public static void checkForScriptErrors(Throwable e, String[] scriptPaths) {

		/*
		 * Arrays.asList(e.getStackTrace()).forEach(error -> {
		 * 
		 * if (scriptPaths.contains(error.getFileName())) {
		 * if(error.getLineNumber() != -1) { log.error(e.getMessage() + " (" +
		 * e.getClass(). getName() + ") (Line: " + (error.getLineNumber() - 9) +
		 * ")" + " (File: " + error.getFileName() + ")"); } } });
		 */

	}

}
