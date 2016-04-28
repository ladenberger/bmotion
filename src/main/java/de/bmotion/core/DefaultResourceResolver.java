package de.bmotion.core;

import java.net.URL;

public class DefaultResourceResolver implements IResourceResolver {

	@Override
	public URL resolve(URL url) {
		return url;
	}

}
