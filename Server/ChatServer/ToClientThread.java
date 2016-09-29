package Server.ChatServer;

import java.io.IOException;
import java.net.*;

import Server.ClientConnection.ClientConnection;

public class ToClientThread implements Runnable {
	/*---------------------------------------------------------------*/
	protected ServerSocket listening_Socket;
	protected ServerDef serverDef;
	protected int clientNum;

	/*---------------------------------------------------------------*/
	public ToClientThread(ServerSocket listening_Socket, ServerDef serverDef, int clientNum) {
		this.listening_Socket = listening_Socket;
		this.serverDef = serverDef;
		this.clientNum = clientNum;
	}

	/*---------------------------------------------------------------*/
	@Override
	public void run() {

		try {
			// Listen for incoming connections forever
			while (true) {
				Socket clientSocket = listening_Socket.accept();
				System.out.println(Thread.currentThread().getName() + clientNum
						+ " Client conection accepted");

				String hostName = clientSocket.getInetAddress().getHostName();
				int port = clientSocket.getLocalPort();
				System.out.println("HostName: " + hostName + " Port: " + port);
				System.out.println();
				clientNum++;

				ClientConnection clientConnection =
						new ClientConnection(clientSocket, clientNum, serverDef);
						
				clientConnection.setName("ClientConnectionThread-" + clientNum);
				clientConnection.start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (listening_Socket != null) {
				try {
					listening_Socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/*---------------------------------------------------------------*/
}
