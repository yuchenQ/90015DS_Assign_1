package Server.ChatServer;

import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.*;
import Server.ChatRoom.RemoteChatRoom;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import Server.Tool.CrazyitMap;
import Server.Tool.CmdLineArgs;

public class Server {
	/*---------------------------------------------------------------*/
	// 存所有server信息的map,格式为“名字” + “ServerInfo”:
	// 一旦开启server，all_ServerInfo会存储所有server的info:
	private static CrazyitMap<String, ServerInfo> all_ServerInfo = new CrazyitMap<>();

	private static int client_SocketPort = 0;
	private static int server_SocketPort = 0;

	private static Runnable client_Connection;
	private static Runnable server_Connection;

	/*---------------------------------------------------------------*/
	public static CrazyitMap<String, ServerInfo> getAll_ServerInfo() {
		return Server.all_ServerInfo;
	}

	/*---------------------------------------------------------------*/
	public static ServerInfo getTheServerInfo(String serverid) {
		return Server.all_ServerInfo.map.get(serverid);
	}

	/*---------------------------------------------------------------*/
	public static Set<String> getAllServerId() {
		return Server.all_ServerInfo.map.keySet();
	}
	
	/*---------------------------------------------------------------*/
	public static void main(String[] args) throws IOException {

		ServerSocket listening_Socket = null;
		ServerSocket listening_ServerSocket = null;

		CmdLineArgs argsBean = new CmdLineArgs();
		CmdLineParser parser = new CmdLineParser(argsBean);
		try {
			parser.parseArgument(args);

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
		}
		// 控制台输入有两项，第一是server名字，第二是config文件的位置：
		String server_id = argsBean.getServerid();
		String path = argsBean.getserversconf();
		// 通过path读取文件：
		FileReader file = new FileReader(path);
		String line = null;

		try {
			BufferedReader reader = new BufferedReader(file);
			// config文件会有多行，每一行是一个server的信息，在此初始化所有的server：
			while ((line = reader.readLine()) != null) {
				String[] info = line.split("\t");

				String serverID = info[0];
				String serverAddress = info[1];
				int clientPort = Integer.valueOf(info[2]);
				int managementPort = Integer.valueOf(info[3]);

				/*---------------------------------------------------------------*/
				// 如果此行的server名字匹配输入的server名字，我要开启此server：
				if (serverID.equals(server_id)) {
					ServerInfo thisServer =
							new ServerInfo(serverID, serverAddress, clientPort, managementPort);
					client_SocketPort = thisServer.getClientPort();
					server_SocketPort = thisServer.getManagementPort();
					all_ServerInfo.put(serverID, thisServer);
				} else {
					// 否则，仅仅记录下其ServerInfo：
					ServerInfo server =
							new ServerInfo(serverID, serverAddress, clientPort, managementPort);
					all_ServerInfo.put(serverID, server);
				}
			}
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// Create a server socket listening on port
			listening_Socket = new ServerSocket(client_SocketPort);
			System.out.println(Thread.currentThread().getName() + " - Server " + server_id
					+ " listening on port " + client_SocketPort + " - for a ClientConnection");
			// Create a server socket listening on port
			listening_ServerSocket = new ServerSocket(server_SocketPort);
			System.out.println(Thread.currentThread().getName() + " - Server " + server_id
					+ " listening on port " + server_SocketPort + " - for a ServerConnection");
			// 建立ServerDef对象：
			ServerDef this_serverDef = new ServerDef(server_id);
			// 设置MainHall的名字：
			this_serverDef.getMainHall().setRoomId("MainHall-" + server_id);
			// 扔进local_RoomList(map)里面：
			this_serverDef.addLocal_RoomList(this_serverDef.getMainHall().getRoomId(), this_serverDef.getMainHall());

			// 遍历所有server:
			for (String remote_ServerId : all_ServerInfo.map.keySet()) {
				// 把其他所有服务器的MainHall
				// 加入到这个this_serverDef的remote_RoomList中：
				if (!remote_ServerId.equals(server_id)) {
					this_serverDef.getRemote_RoomList().put("MainHall-" + remote_ServerId,
							new RemoteChatRoom("MainHall-" + remote_ServerId, remote_ServerId));
				}
			}
			client_Connection = new ToClientThread(listening_Socket, this_serverDef, 0);
			Thread toClientThread = new Thread(client_Connection, "toClientThread");
			toClientThread.start();

			server_Connection = new ToServerThread(listening_ServerSocket, this_serverDef, 0);
			Thread ToServerThread = new Thread(server_Connection, "toClientThread");
			ToServerThread.start();

			System.out.println();
			System.out.println(Thread.currentThread().getName() + " - toClientThread is started ");
			System.out.println(Thread.currentThread().getName() + " - toServerThread is started ");
			System.out.println();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
