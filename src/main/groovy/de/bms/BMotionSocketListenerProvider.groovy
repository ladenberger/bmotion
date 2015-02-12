package de.bms

import de.bms.server.BMotionSocketServer

interface BMotionSocketListenerProvider {

    public void installListeners(BMotionSocketServer server)

}
