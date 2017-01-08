package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ListenerThread extends Thread {

	private ServerSocket serverSocket;

	public ListenerThread(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void run() {

		while (true) {
			Socket socket = null;
			try {
				// wait for Client to connect
				socket = serverSocket.accept();
				// prepare the input reader for the socket
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				// prepare the writer for responding to clients requests
				PrintWriter writer = new PrintWriter(socket.getOutputStream(),
						true);

				String request;
				// read client requests
				while ((request = reader.readLine()) != null) {

					System.out.println("Client sent the following request: "
							+ request);

					/*
					 * check if request has the correct format: !ping
					 * <client-name>
					 */
					String[] parts = request.split("\\s");

					String response = "!error provided message does not fit the expected format: "
							+ "!ping <client-name> or !stop <client-name>";

					if (parts.length == 2) {

						String clientName = parts[1];

						if (request.startsWith("!ping")) {
							response = "!pong " + clientName;
						} else if (request.startsWith("!stop")) {
							response = "!bye " + clientName;
						}
					}

					// print request
					writer.println(response);
				}

			} catch (IOException e) {
				System.err
						.println("Error occurred while waiting for/communicating with client: "
								+ e.getMessage());
				break;
			} finally {
				if (socket != null && !socket.isClosed())
					try {
						socket.close();
					} catch (IOException e) {
						// Ignored because we cannot handle it
					}

			}

		}
	}
	
	private boolean checkLogin(String userName,String password)
	{
		return false;
	}
}
