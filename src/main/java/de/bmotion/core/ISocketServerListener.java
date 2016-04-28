package de.bmotion.core;

import com.corundumstudio.socketio.SocketIOServer;

public interface ISocketServerListener {

    public void serverStarted(SocketIOServer socket);
    
    public void serverStopped(SocketIOServer socket);

}