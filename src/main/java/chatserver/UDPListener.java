package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cli.Shell;

public class UDPListener extends Thread {
	
	private DatagramSocket serverSocket;
	
	private ServerDispatcher serverDispatcher; 
	private Shell shell;
	private ExecutorService executor;
	
	private final int receiveLength = 1024;
	
	public UDPListener(DatagramSocket socket, ServerDispatcher serverDispatcher, Shell shell)
	{		
		this.serverSocket = socket;   
		this.serverDispatcher = serverDispatcher;
		this.shell = shell;
		this.executor = Executors.newCachedThreadPool();
	}
	
	@Override
	public synchronized void run()
	{
		Thread.currentThread().setName("udplistener");

		byte[] receiveData = new byte[receiveLength];
       
		while (!isInterrupted() && !serverSocket.isClosed()) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				serverSocket.receive(receivePacket);
				executor.execute(new UDPHandler(serverSocket,receivePacket,serverDispatcher));				
				
			}
			catch(SocketException e)
			{
				try {
					shell.writeLine(e.getMessage());
					return;
				} catch (IOException e1) {					
					e1.printStackTrace();
					return;
				}
			}
			catch (IOException e) {
				try {
					shell.writeLine(e.getMessage());
					return;
				} catch (IOException e1) {					
					e1.printStackTrace();
					return;
				}
			}
			

		}
	}
	public void close()
	{
		Thread.currentThread().interrupt();
		executor.shutdown();
		try {
		    if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
		    	executor.shutdownNow();
		    } 
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
		this.serverSocket.close();
	}
	
}
