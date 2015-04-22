package de.bms

interface SocketServerListener {

    public void serverStarted(String clientApp);

    public void serverCloseRequest();

}