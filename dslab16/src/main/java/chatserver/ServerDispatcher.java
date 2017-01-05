package chatserver;
import java.net.*;
import java.util.*;

import util.Config;
 
public class ServerDispatcher extends Thread
{
	private Config clientConfig;
	private Map<String,String> userList;
    private List<String> mMessageQueue;
    private List<ClientInfo> mClients;
    
    public ServerDispatcher(Config clientConfig)
    {
    	this.clientConfig = clientConfig;
    	userList = new HashMap<>();
    	mMessageQueue = new ArrayList<>();
    	mClients = new ArrayList<>();
    	readClientsFromConfig();
    }
 
    private void readClientsFromConfig()
    {
    	Set<String> keys = clientConfig.listKeys();
    	for(String name:keys)
    	{
    		String value = clientConfig.getString(name);
    		//remove "password" string from name    		
    		String username = name.substring(0,name.lastIndexOf("."));    		
    		userList.put(username, value);
    	}
    	System.out.println("read users!");
    }
    
    /**
     * Adds given client to the server's client list.
     */
    public synchronized void addClient(ClientInfo aClientInfo)
    {
        mClients.add(aClientInfo);
    }
 
    /**
     * Deletes given client from the server's client list
     * if the client is in the list.
     */
    public synchronized void deleteClient(ClientInfo aClientInfo)
    {
    	
    	mClients.remove(aClientInfo);
        
    }
 
    public synchronized void broadcast(ClientInfo from, String msg) {
    	//check if logged in -> only allow to send messages if logged in
    	if(from.loggedIn == false)
    	{
    		return;
    	}
		for (ClientInfo c : mClients) {
			if (c != from) {				
				//check if receiver is logged in as well -> dont send messages to clients not logged in
				if(c.loggedIn)
				{
					c.mClientSender.sendMessage(msg);
				}
			}
		}
	}
    public synchronized void exitCommand()
    {
    	for(ClientInfo c:mClients)
    	{
    		c.mClientSender.sendMessage("close");
    	}
    	for(ClientInfo c2:mClients)
    	{
    		c2.mClientListener.interrupt();
    	}
    }
    public synchronized boolean checkClientLogin(String userName,String password)
    {
    	if(alreadyLoggedIn(userName))
    	{
    		return false;
    	}
    	String pw = userList.get(userName);
    	if(pw != null)
    	{    		
    		if(pw.equals(password))
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    public synchronized boolean alreadyLoggedIn(String userName)
    {
    	//check if user with the given username is already loggedIn
		//dont allow double logged in users
		for(ClientInfo info:mClients)
		{
			if(info.userName.equals(userName))
			{
				if(info.loggedIn)
				{
					return true;
				}
			}
		}
		return false;
    }
    public String lookUpAddress(String userName)
    {
    	for(ClientInfo info: mClients)
    	{
    		if(info.userName.equals(userName))
    		{
    			if(info.privateAddress != "")
    			{
    				return info.privateAddress;
    			}
    		}
    	}
    	return null;
    }
    public Set<String> getUserList()
    {
    	return this.userList.keySet();
    }
    public synchronized List<ClientInfo> getClients()
    {
    	return this.mClients;
    }
    
    public void run()
    {
       
    }
 
}
