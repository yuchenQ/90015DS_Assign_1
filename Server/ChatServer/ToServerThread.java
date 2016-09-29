package Server.ChatServer;

import java.io.IOException;
import java.net.*;

public class ToServerThread implements Runnable {
	/*---------------------------------------------------------------*/
	protected ServerSocket listening_ServerSocket;
	protected ServerDef serverDef;
	protected int serverNum;

	/*---------------------------------------------------------------*/
	public ToServerThread(ServerSocket listening_ServerSocket, ServerDef serverDef, int serverNum) {
		this.listening_ServerSocket = listening_ServerSocket;
		this.serverDef = serverDef;
		this.serverNum = serverNum;
	}

	/*---------------------------------------------------------------*/
	@Override
	public void run() {

		try {
			// Listen for incoming connections forever
			while (true) {
				Socket serverSocket = listening_ServerSocket.accept();
				System.out.println(Thread.currentThread().getName() + serverNum
						+ " - Server conection accepted");

				String hostName = serverSocket.getInetAddress().getHostName();
				int port = serverSocket.getLocalPort();
				System.out.println("HostName: " + hostName + " Port: " + port);
				System.out.println();
				serverNum++;

				ServerConnection serverConnection =
						new ServerConnection(serverSocket, serverDef, serverNum);
						
				serverConnection.setName("ServerConnectionThread-" + serverNum);
				serverConnection.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (listening_ServerSocket != null) {
				try {
					listening_ServerSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/*---------------------------------------------------------------*/
}
