package Server.ClientConnection;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Server.ChatProtocol;
import Server.ChatRoom.LocalChatRoom;
import Server.ChatRoom.RemoteChatRoom;
import Server.ChatServer.ServerDef;
import Server.ChatServer.ServerInfo;
import Server.Message.Message;
import Server.Tool.JsonOperation;
import Server.ChatServer.Server;

public class ClientConnection extends Thread {

	protected Socket clientSocket;
	protected BufferedReader reader;
	protected BufferedWriter writer;
	protected BlockingQueue<Message> messageQueue;
	protected JsonOperation jsonDo;
	protected int clientNum;
	protected ChatProtocol chatprotocol;

	protected ClientInfo clientInfo;
	protected ServerDef serverDef;

	public ClientConnection(Socket clientSocket, int clientNum, ServerDef serverDef) {
		try {
			this.clientSocket = clientSocket;
			this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			this.writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			this.messageQueue = new LinkedBlockingQueue<Message>();
			this.clientNum = clientNum;
			this.jsonDo = new JsonOperation();
			this.clientInfo = new ClientInfo("", null, this, false);
			this.serverDef = serverDef;
			System.out.println("-- ClientConnection " + clientNum + " 建立于 " + new Date());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
	public class ClientMessageReader extends Thread {
		/*---------------------------------------------------------------*/
		public ClientMessageReader() {
			System.out.println("-- ClientMessageReader " + clientNum + " 建立于 " + new Date());
		}

		@Override
		// This thread reads messages from the client's socket input stream
		public void run() {
			try {
				System.out.println(Thread.currentThread().getName() +
						" - Reading messages from client connection");
				String clientMsg = null;
				/*---------------------------------------------------------------*/
				while ((clientMsg = reader.readLine()) != null) {
					String requestType = jsonDo.doUnmarshalling(clientMsg, "type");
					String client_id = clientInfo.getClient_id();

					if (requestType.equals("newidentity") && client_id.equals("")) {
						String newid = jsonDo.doUnmarshalling(clientMsg, "identity");
						clientInfo.setClient_id(newid);
					}
					chatprotocol = new ChatProtocol(clientMsg, clientInfo, serverDef);
					String processedMsg = chatprotocol.processMsg();

					System.out.println();
					System.out.println(Thread.currentThread().getName() + " - Message from client received ");
					System.out.println(Thread.currentThread().getName() + " - The return Msg is: ");
					System.out.println(processedMsg);
					System.out.println();

					Message msg = new Message(processedMsg);
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

	@Override
	public void run() {
		try {
			ClientMessageReader messageReader = new ClientMessageReader();
			messageReader.setName(this.getName() + " Reader");
			messageReader.start();

			System.out.println(Thread.currentThread().getName() +
					" - Processing client " + clientNum + " messages");

			while (true) {
				Message msg = messageQueue.take();
				String type = jsonDo.doUnmarshalling(msg.getMessage(), "type");
				System.out.println(" - The return type is: " + type);
				switch (type) {
				/*---------------------------------------------------------------*/
				// 4.1 new identity:
				/*---------------------------------------------------------------*/
				case "newidentity": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#newidentity]");

					String client_id = clientInfo.getClient_id();
					String isapproved = jsonDo.doUnmarshalling(msg.getMessage(), "approved");
					// 如果本地可用：
					if (isapproved.equals("true")) {
						boolean cando = true;
						// 给其他每个服务器都写一遍：
						for (String id : Server.getAll_ServerInfo().map.keySet()) {
							if (!id.equals(serverDef.getServerId())) {
								ServerInfo server = Server.getAll_ServerInfo().map.get(id);
								String address = server.getAddress();
								int port = server.getManagementPort();
								Socket socket = new Socket(address, port);

								BufferedReader sReader = getSreader(socket);
								BufferedWriter sWriter = getSwriter(socket);

								String sMsg =
										jsonDo.newId_LockIdentity(serverDef.getServerId(), client_id);
								writeToServer(sWriter, sMsg);

								String serverMsg = null;
								while ((serverMsg = sReader.readLine()) != null) {
									String returnType = jsonDo.doUnmarshalling(serverMsg, "type");
									if (returnType.equals("lockidentity")) {
										String lock = jsonDo.doUnmarshalling(serverMsg, "locked");
										if (lock.equals("false")) {
											cando = false;
										}
										break;
									}
								}
							}
						}
						// 远程也同意：
						if (cando) {
							// 把其加入Mainhall:
							LocalChatRoom mainHall = serverDef.getMainHall();
							mainHall.addMemeber(clientInfo.getClient_id(), this);
							// 更新clientInfo:
							clientInfo.setCurrentChatRoom(mainHall);
							// 更新serverDef:
							serverDef.setMainHall(mainHall);
							serverDef.addUserList(client_id, clientInfo);
							serverDef.addLockedClients(client_id);
							// 广播roomchange:
							String boardCastMsg =
									jsonDo.newId_roomchange(client_id, mainHall.getRoomId());
							mainHall.boardCastAll(boardCastMsg);
							write(msg.getMessage());
						} else {
							// 更新clientInfo:
							clientInfo.setClient_id("");
							// 修改回复信息为否定：
							msg.setMessage(jsonDo.doMarshall_Newid(false));
							write(msg.getMessage());
						}
					}
					// 本地不可建：
					else {
						// 更新clientInfo:
						clientInfo.setClient_id("");
						write(msg.getMessage());
					}
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.2 list:
				/*---------------------------------------------------------------*/
				case "roomlist": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#list]");

					write(msg.getMessage());
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.3 who:
				/*---------------------------------------------------------------*/
				case "roomcontents": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#who]");

					write(msg.getMessage());
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.4 createroom:
				/*---------------------------------------------------------------*/
				case "createroom": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#createroom]");

					String approve = jsonDo.doUnmarshalling(msg.getMessage(), "approved");
					String identity = clientInfo.getClient_id();
					String former = clientInfo.getCurrentChatRoom().getRoomId();
					String roomid = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
					boolean cando = true;
					// 本地创建失败了：
					if (approve.equals("false")) {
						write(msg.getMessage());
					} else {
						// 给其他每个服务器都写一遍：
						for (String id : Server.getAll_ServerInfo().map.keySet()) {
							if (!id.equals(serverDef.getServerId())) {
								ServerInfo server = Server.getAll_ServerInfo().map.get(id);
								String address = server.getAddress();
								int port = server.getManagementPort();
								Socket socket = new Socket(address, port);

								BufferedReader sReader = getSreader(socket);
								BufferedWriter sWriter = getSwriter(socket);

								String sMsg = jsonDo.CR_LockRoomId(roomid, serverDef.getServerId());
								writeToServer(sWriter, sMsg);

								String serverMsg = null;
								while ((serverMsg = sReader.readLine()) != null) {

									String returnType = jsonDo.doUnmarshalling(serverMsg, "type");
									if (returnType.equals("lockroomid")) {
										String lock = jsonDo.doUnmarshalling(serverMsg, "locked");
										if (lock.equals("false")) {
											cando = false;
										}
										break;
									}
								}
							}
						}
						// 远程也同意：
						if (cando) {
							write(msg.getMessage());
							// 给former广播：
							String bMsg = jsonDo.CR_BoardCastMsg(identity, former, roomid);
							clientInfo.getCurrentChatRoom().boardCastAll(bMsg);
							// 在旧房子删除此人：
							clientInfo.getCurrentChatRoom().removeMember(identity);
							// 新建一个room，新建房主，加入chatrooms list中：
							LocalChatRoom newRoom = new LocalChatRoom(roomid, identity);
							// 将owner加入members中去：
							newRoom.addMemeber(identity, this);
							// 更新serverDef：
							serverDef.addLocal_RoomList(roomid, newRoom);
							// 更新clientInfo:
							clientInfo.setCurrentChatRoom(newRoom);
							clientInfo.setIsOwner(true);
						} else {
							// 修改回复信息为否定：
							msg.setMessage(jsonDo.doMarshall_CreateRoom(roomid, false));
							write(msg.getMessage());
						}
					}
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.5 join room:
				/*---------------------------------------------------------------*/
				case "roomchange": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#joinroom]");

					String former = clientInfo.getCurrentChatRoom().getRoomId();
					String roomid = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
					// 如果失败：
					if (former.equals(roomid)) {
						write(msg.getMessage());
					} else {
						// 如果是本地room：
						if (serverDef.getLocal_RoomList().map.containsKey(roomid)) {
							System.out.println(" -join in LOCAL chatroom");
							write(msg.getMessage());
							LocalChatRoom newRoom = serverDef.getLocal_RoomList().map.get(roomid);
							LocalChatRoom currentRoom = clientInfo.getCurrentChatRoom();
							// 旧房子删除玩家：
							currentRoom.removeMember(clientInfo.getClient_id());
							// 给两个room广播：
							currentRoom.boardCastAll(msg.getMessage());
							newRoom.boardCastAll(msg.getMessage());
							// 新房子添加玩家：
							newRoom.addMemeber(clientInfo.getClient_id(), this);
							// 更新clientInfo:
							clientInfo.setCurrentChatRoom(newRoom);
						}
						// 如果是远程room:
						else if (serverDef.getRemote_RoomList().map.containsKey(roomid)) {
							System.out.println(" -join in REMOTE chatroom");
							System.out.println();

							RemoteChatRoom newRoom = serverDef.getRemote_RoomList().map.get(roomid);

							String remoteSId = newRoom.getServer();
							String sHost = Server.getAll_ServerInfo().map.get(remoteSId).getAddress();
							int sPort = Server.getAll_ServerInfo().map.get(remoteSId).getClientPort();
							// 写回给client：
							String reMsg =
									jsonDo.doMarshall_JoinRemote(roomid, sHost, Integer.toString(sPort));
							System.out.println(reMsg);
							write(reMsg);
							// 删除client：
							LocalChatRoom currentRoom = clientInfo.getCurrentChatRoom();
							currentRoom.removeMember(clientInfo.getClient_id());
							// 更新serverDef:
							serverDef.getUserList().map.remove(clientInfo.getClient_id());
							// former广播：
							currentRoom.boardCastAll(msg.getMessage());
						}
					}
					break;
				}
				/*---------------------------------------------------------------*/
				case "movejoin": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#movejoin]");

					String roomid = jsonDo.doUnmarshalling(msg.getMessage(), "roomid");
					String former = jsonDo.doUnmarshalling(msg.getMessage(), "former");
					String identity = jsonDo.doUnmarshalling(msg.getMessage(), "identity");
					System.out.println(identity + " " + former + " " + roomid);
					// 如果是要加进mainhall：
					if (roomid.equals(serverDef.getMainHall().getRoomId())) {
						LocalChatRoom mainHall = serverDef.getMainHall();
						mainHall.addMemeber(identity, this);
						// 更新clientInfo:
						clientInfo.setClient_id(identity);
						clientInfo.setCurrentChatRoom(mainHall);
						clientInfo.setCurrentCC(this);
						clientInfo.setIsOwner(false);
						// 更新serverDef:
						serverDef.addUserList(identity, clientInfo);
						// 写给client的serverchange信息：
						System.out.println(jsonDo.MJ_Serverchange(serverDef.getServerId()));
						write(jsonDo.MJ_Serverchange(serverDef.getServerId()));
						// 广播roomchange:
						String boardCastMsg =
								jsonDo.MJ_roomchange(identity, former, mainHall.getRoomId());
						mainHall.boardCastAll(boardCastMsg);
					} else {
						LocalChatRoom newRoom = serverDef.getLocal_RoomList().map.get(roomid);
						newRoom.addMemeber(identity, this);
						// 更新clientInfo:
						clientInfo.setClient_id(identity);
						clientInfo.setCurrentChatRoom(newRoom);
						clientInfo.setCurrentCC(this);
						clientInfo.setIsOwner(false);
						// 更新serverDef:
						serverDef.addUserList(identity, clientInfo);
						// 写给client的serverchange信息：
						System.out.println(jsonDo.MJ_Serverchange(serverDef.getServerId()));
						write(jsonDo.MJ_Serverchange(serverDef.getServerId()));
						// 广播roomchange:
						String boardCastMsg = jsonDo.MJ_roomchange(identity, former, roomid);
						newRoom.boardCastAll(boardCastMsg);
					}
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.6 delete room:
				/*---------------------------------------------------------------*/
				case "deleteroom": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#deleteroom]");

					String isapprove = jsonDo.doUnmarshalling(msg.getMessage(), "approved");
					if (isapprove.equals("true")) {
						LocalChatRoom former_room = clientInfo.getCurrentChatRoom();
						// 取得房间名，所有名字：
						String former_name = former_room.getRoomId();
						Set<String> membersName = former_room.getAll_MembersName();
						LocalChatRoom mainHall = serverDef.getMainHall();

						for (String name : membersName) {
							String reMsg =
									jsonDo.DR_BoardCastMsg(name, former_name, mainHall.getRoomId());
							ClientConnection cc = former_room.getAll_Members().map.get(name);
							// former房间广播：
							cc.write(reMsg);
							// 加入mainhall:
							mainHall.addMemeber(name, cc);
							// mainhall广播：
							mainHall.boardCastAll(reMsg);
						}
						// serverDef把房间删除：
						serverDef.getLocal_RoomList().map.remove(former_name);
						// 更新clientInfo:
						clientInfo.setCurrentChatRoom(mainHall);
						clientInfo.setIsOwner(false);
						// 给其他每个服务器都写一遍：
						for (String id : Server.getAll_ServerInfo().map.keySet()) {
							if (!id.equals(serverDef.getServerId())) {
								ServerInfo server = Server.getAll_ServerInfo().map.get(id);
								String address = server.getAddress();
								int port = server.getManagementPort();
								Socket socket = new Socket(address, port);

								String sMsg = jsonDo.DR_RemoteMsg(former_name);
								BufferedWriter sWriter = getSwriter(socket);
								writeToServer(sWriter, sMsg);
							}
						}
						write(msg.getMessage());
					} else {
						write(msg.getMessage());
					}
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.7 message:
				/*---------------------------------------------------------------*/
				case "message": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#message]");
					clientInfo.getCurrentChatRoom().boardCastAll(msg.getMessage());
					break;
				}
				/*---------------------------------------------------------------*/
				// 4.8 quit:
				/*---------------------------------------------------------------*/
				case "quit": {
					System.out.println(Thread.currentThread().getName() +
							" - Processing client " + clientNum + " [#quit]");

					String identity = jsonDo.doUnmarshalling(msg.getMessage(), "identity");
					String isowner = jsonDo.doUnmarshalling(msg.getMessage(), "isowner");
					// 更新serverDef：
					serverDef.getUserList().map.remove(identity);
					serverDef.getLockedClients().remove(identity);
					// 取得房间名，所有名字：
					LocalChatRoom room = clientInfo.getCurrentChatRoom();
					String roomid = room.getRoomId();
					Set<String> membersName = room.getAll_MembersName();
					LocalChatRoom mainHall = serverDef.getMainHall();
					//如果此人是房主：
					if (isowner.equals("true")) {
						// 先把房主删掉：
						room.removeMember(identity);
						// 给其他人广播：
						for (String name : membersName) {
							String mainhall = serverDef.getMainHall().getRoomId();
							String reMsg = jsonDo.Quit_RoomChangeMsg(name, roomid, mainhall);
							ClientConnection cc = room.getAll_Members().map.get(name);
							// former房间广播：
							cc.write(reMsg);
							// 加入mainhall:
							mainHall.addMemeber(name, cc);
							// mainhall广播：
							mainHall.boardCastAll(reMsg);
						}
						// 给其他每个服务器都写一遍：
						for (String id : Server.getAll_ServerInfo().map.keySet()) {
							if (!id.equals(serverDef.getServerId())) {
								ServerInfo server = Server.getAll_ServerInfo().map.get(id);

								String address = server.getAddress();
								int port = server.getManagementPort();
								Socket socket = new Socket(address, port);

								String sMsg = jsonDo.Quit_RemoteMsg(identity, roomid);
								BufferedWriter sWriter = getSwriter(socket);
								writeToServer(sWriter, sMsg);
							}
						}
						write(jsonDo.Quit_RoomChange(identity, roomid));
						break;
					}
					else {
						// 给其他每个服务器都写一遍：
						for (String id : Server.getAll_ServerInfo().map.keySet()) {
							if (!id.equals(serverDef.getServerId())) {
								ServerInfo server = Server.getAll_ServerInfo().map.get(id);

								String address = server.getAddress();
								int port = server.getManagementPort();
								Socket socket = new Socket(address, port);

								String sMsg = jsonDo.Quit_RemoteMsg(identity);
								BufferedWriter sWriter = getSwriter(socket);
								writeToServer(sWriter, sMsg);
							}
						}
						write(jsonDo.Quit_RoomChange(identity, roomid));
						break;
					}
				}
				/*---------------------------------------------------------------*/
				default:
					System.out.println("-- An error occured in [ClientConnection]!");
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	/*---------------------------------------------------------------*/
	public BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
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
	public void boardCastAll(String boardCastMsg) {
		Set<ClientInfo> all_ClientInfo = this.serverDef.getUserList().valueSet();
		for (ClientInfo clientInfo : all_ClientInfo) {
			clientInfo.getCurrentCC().write(boardCastMsg);
		}
	}

	/*---------------------------------------------------------------*/
	public BufferedReader getSreader(Socket socket) throws IOException {
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

		return reader;
	}

	/*---------------------------------------------------------------*/
	public BufferedWriter getSwriter(Socket socket) throws IOException {
		BufferedWriter writer =
				new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

		return writer;
	}

	/*---------------------------------------------------------------*/
	public void writeToServer(BufferedWriter writer, String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------*/
}
