package Server.ChatServer;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Server.ChatRoom.LocalChatRoom;
import Server.ChatRoom.RemoteChatRoom;
import Server.ClientConnection.ClientInfo;
import Server.Message.Message;
import Server.Tool.CrazyitMap;
import Server.Tool.JsonOperation;
import java.net.*;
import java.io.*;

public class ServerConnection extends Thread {
	/*---------------------------------------------------------------*/
	protected Socket serverSocket;
	protected BufferedReader reader;
	protected BufferedWriter writer;
	// This queue holds messages sent by the client or messages
	// intended for the client from other threads
	protected BlockingQueue<Message> messageQueue;
	protected int serverNum;
	// 需要初始化的功能类：
	protected JsonOperation jsonDo;
	// 最重要的东西，改变其中的某些值，实现多个server的校准
	protected ServerDef serverDef;
	// 解析serverDef的变量，方便调用：
	protected CrazyitMap<String, ClientInfo> userList;
	protected CrazyitMap<String, LocalChatRoom> local_RoomList;
	protected CrazyitMap<String, RemoteChatRoom> remote_RoomList;
	protected Set<String> lockedClients;

	protected final Lock lock = new ReentrantLock();

	/*---------------------------------------------------------------*/
	public ServerConnection(Socket serverSocket, ServerDef serverDef, int serverNum) {
		try {
			this.serverSocket = serverSocket;
			this.reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), "UTF-8"));
			this.writer = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream(), "UTF-8"));
			this.messageQueue = new LinkedBlockingQueue<Message>();
			this.serverNum = serverNum;
			this.jsonDo = new JsonOperation();
			this.serverDef = serverDef;
			this.userList = serverDef.getUserList();
			this.local_RoomList = serverDef.getLocal_RoomList();
			this.remote_RoomList = serverDef.getRemote_RoomList();
			this.lockedClients = serverDef.getLockedClients();

			System.out.println("ServerConnection " + serverNum + " 建立于 " + new Date());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
	public void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
	@Override
	public void run() {
		try {
			Thread messageReader = new Thread(new ServerMessageReader());
			messageReader.setName(Thread.currentThread().getName() + "Reader");
			messageReader.start();

			while (true) {
				Message msg = messageQueue.take();
				String type = jsonDo.doUnmarshalling(msg.getMessage(), "type");
				/*---------------------------------------------------------------*/
				switch (type) {
				/*---------------------------------------------------------------*/
				case "lockidentity": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + serverNum + " [#lockidentity]");
					// 如果发来的确实是lock的请求而不是回复：
					if (!jsonDo.isContainsKey(msg.getMessage(), "locked")) {

						String newClient = jsonDo.doUnmarshalling(msg.getMessage(), "identity");
						System.out.println("-- a server send lockidentity: " + newClient);
						String serverid = serverDef.getServerId();
						// 找此名字是否在userlist,locklist里存在：
						if (!userList.map.containsKey(newClient) && !lockedClients.contains(newClient)) {
							// 如果不存在，可以新建：
							msg.setMessage(jsonDo.newId_LockIdentity(serverid, newClient, true));
							// 把名字加进本服务器的lock列表中：
							serverDef.getLockedClients().add(newClient);
						} else {
							// 否则不能：
							msg.setMessage(jsonDo.newId_LockIdentity(serverid, newClient, false));
						}
						System.out.println(msg.getMessage());
						write(msg.getMessage());
						break;
					} else {
						break; // 否则直接结束：
					}
				}
				/*---------------------------------------------------------------*/
				case "lockroomid": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + serverNum + " [#lockroomid]");
					// 如果发来的确实是lock的请求而不是回复：
					if (!jsonDo.isContainsKey(msg.getMessage(), "locked")) {

						String newRoom = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
						System.out.println("-- a server send lockroomid: " + newRoom);
						String serverid = serverDef.getServerId();
						// 找此房子是否在localroomlist,remote_RoomList里存在：
						if (!local_RoomList.map.containsKey(newRoom) && !remote_RoomList.map.containsKey(newRoom)) {
							// 如果不存在，可以新建：
							String remoteid = jsonDo.doUnmarshalling(msg.getMessage(), "serverid");
							// 把名字加进本服务器的Remote_RoomList中：
							serverDef.addRemote_RoomList(newRoom, new RemoteChatRoom(newRoom, remoteid));
							// 设置回复信息：
							msg.setMessage(jsonDo.CR_LockRoomId(serverid, newRoom, true));
						} else {
							// 否则不能：
							msg.setMessage(jsonDo.CR_LockRoomId(serverid, newRoom, false));
						}
						System.out.println(msg.getMessage());
						write(msg.getMessage());
						break;
					} else {
						break; // 否则直接结束：
					}
				}
				/*---------------------------------------------------------------*/
				case "deleteroom": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + serverNum + " [#deleteroom]");

					String roomid = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
					serverDef.removeRemoteRoom(roomid);
					break;
				}
				/*---------------------------------------------------------------*/
				case "quit": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + serverNum + " [#quit]");

					String name = jsonDo.doUnmarshalling(msg.getMessage(), "identity");
					// 如果是删除房主：
					if(jsonDo.isContainsKey(msg.getMessage(), "roomid")) {
						String roomid = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
						// 删除lock名字和远程列表：
						serverDef.removeLockedClients(name);
						serverDef.removeRemoteRoom(roomid);
					} else {
						serverDef.removeLockedClients(name);
					}
					break;
				}
				/*---------------------------------------------------------------*/
				default:
					System.out.println("-- An error occured in [ServerConnection]!");
				}

			}
			// serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
	public BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
	}

	/*---------------------------------------------------------------*/
	public class ServerMessageReader implements Runnable {
		private String serverMsg;

		public void run() {
			try {
				while ((serverMsg = reader.readLine()) != null) {
					Message msg = new Message(serverMsg);
					messageQueue.add(msg);
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/*---------------------------------------------------------------*/
}
