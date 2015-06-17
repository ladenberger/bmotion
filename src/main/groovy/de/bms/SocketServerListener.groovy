package de.bms

import com.corundumstudio.socketio.SocketIOServer

interface SocketServerListener {

    public void serverStarted(SocketIOServer socket);

}