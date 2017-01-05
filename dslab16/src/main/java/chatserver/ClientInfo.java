package chatserver;

import java.net.Socket;

public class ClientInfo
{
    public Socket mSocket = null;
    public ClientListener mClientListener = null;
    public ClientSender mClientSender = null;
    
    public boolean loggedIn = false;
    public String userName = "";
    public String privateAddress = "";
}
