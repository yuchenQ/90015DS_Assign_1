package Server;

import java.util.*;
import Server.ChatRoom.LocalChatRoom;
import Server.ChatRoom.RemoteChatRoom;
import Server.ChatServer.ServerDef;
import Server.ClientConnection.ClientConnection;
import Server.ClientConnection.ClientInfo;
import Server.Tool.CrazyitMap;
import Server.Tool.JsonOperation;

public class ChatProtocol {
	/*---------------------------------------------------------------*/
	protected String message;
	protected String returnMsg;

	protected ClientInfo clientInfo;
	protected String client_id;
	protected LocalChatRoom currentRoom;
	protected ClientConnection currentCC;
	protected boolean isOwner;

	protected ServerDef serverDef;
	protected CrazyitMap<String, ClientInfo> userList;
	protected CrazyitMap<String, LocalChatRoom> local_RoomList;
	protected CrazyitMap<String, RemoteChatRoom> remote_RoomList;
	protected Set<String> lockedClients;
	protected LocalChatRoom mainHall;

	protected JsonOperation jsonDo;

	/*---------------------------------------------------------------*/
	// 初始化会输入是哪个家伙调用了chatprotocol：
	// 还会从sever那里搞到所有的聊天室：
	public ChatProtocol(String message, ClientInfo clientInfo, ServerDef serverDef) {
		this.message = message;
		this.returnMsg = null;

		this.clientInfo = clientInfo;
		this.client_id = clientInfo.getClient_id();
		this.currentRoom = clientInfo.getCurrentChatRoom();
		this.currentCC = clientInfo.getCurrentCC();
		this.isOwner = clientInfo.getIsOwner();

		this.serverDef = serverDef;
		this.userList = serverDef.getUserList();
		this.local_RoomList = serverDef.getLocal_RoomList();
		this.remote_RoomList = serverDef.getRemote_RoomList();
		this.lockedClients = serverDef.getLockedClients();
		this.mainHall = serverDef.getMainHall();

		this.jsonDo = new JsonOperation();
	}

	/*---------------------------------------------------------------*/
	public String processMsg() {
		String type = jsonDo.doUnmarshalling(message, "type");

		switch (type) {
		/*---------------------------------------------------------------*/
		case "newidentity": {
			String newid = jsonDo.doUnmarshalling(message, "identity");
			this.returnMsg = isNewIdentity(newid);
			break;
		}
		/*---------------------------------------------------------------*/
		case "list": {
			this.returnMsg = isList();
			break;
		}
		/*---------------------------------------------------------------*/
		case "who": {
			this.returnMsg = isWho(client_id);
			break;
		}
		/*---------------------------------------------------------------*/
		case "createroom": {
			String new_roomid = jsonDo.doUnmarshalling(message, "roomid");
			this.returnMsg = isCreateRoom(new_roomid);
			break;
		}
		/*---------------------------------------------------------------*/
		case "join": {
			String target_room = jsonDo.doUnmarshalling(message, "roomid");
			this.returnMsg = isJoinRoom(target_room);
			break;
		}

		case "movejoin": {
			String former = jsonDo.doUnmarshalling(message, "former");
			String roomid = jsonDo.doUnmarshalling(message, "roomid");
			String identity = jsonDo.doUnmarshalling(message, "identity");
			this.returnMsg = isMoveJoin(former, roomid, identity);
			break;
		}
		/*---------------------------------------------------------------*/
		case "deleteroom": {
			String delete_target = jsonDo.doUnmarshalling(message, "roomid");
			this.returnMsg = isDeleteRoom(delete_target);
			break;
		}
		/*---------------------------------------------------------------*/
		case "message": {
			String content = jsonDo.doUnmarshalling(message, "content");
			this.returnMsg = isMessage(content);
			break;
		}
		/*---------------------------------------------------------------*/
		case "quit": {
			this.returnMsg = isQuit(client_id);
			break;
		}
		/*---------------------------------------------------------------*/
		default:
			System.out.println("-- An error occured in [ChatProtocol]!");
			this.returnMsg = null;
		}
		return this.returnMsg;
	}

	/*---------------------------------------------------------------*/
	protected String isNewIdentity(String newid) {
		// 如果输入的是字母，符合长度标准，且userlist和lockedlist里没有重名，则符合：
		boolean isWord = newid.matches("[a-zA-Z0-9]+");
		if (isWord && (newid.length() >= 3 && newid.length() <= 16)) {
			// 看本地server两个用户表,如果有一个包含此名字，则失败：
			if (serverDef.getLockedClients().contains(newid) ||
					serverDef.getUserList().map.keySet().contains(newid)) {
				return jsonDo.doMarshall_Newid(false);
			} else {
				return jsonDo.doMarshall_Newid(true);
			}
		} else {
			return jsonDo.doMarshall_Newid(false);
		}
	}

	/*---------------------------------------------------------------*/
	protected String isList() {
		ArrayList<String> roomlist = new ArrayList<>();
		// 遍历所有本地聊天室，搞到名字，封装入List：
		for (String name : serverDef.getLocal_RoomList().map.keySet()) {
			roomlist.add(name);
		}
		// 遍历所有远程聊天室，搞到名字，封装入List：
		for (String name : serverDef.getRemote_RoomList().map.keySet()) {
			roomlist.add(name);
		}
		return jsonDo.doMarshall_List(roomlist);
	}

	/*---------------------------------------------------------------*/
	protected String isWho(String client_id) {
		// 找到其所在聊天室的名字，所有组员，群主：
		LocalChatRoom room = serverDef.getUserList().map.get(client_id).getCurrentChatRoom();
		String roomid = room.getRoomId();
		Set<String> members = room.getAll_MembersName();
		String owner = room.getOwner();
		return jsonDo.doMarshall_Who(roomid, members, owner);
	}

	/*---------------------------------------------------------------*/
	protected String isCreateRoom(String new_roomid) {
		boolean isWord = new_roomid.matches("[a-zA-Z0-9]+");
		// 如果名字起得不规范,不许新建:
		if (!isWord || !(new_roomid.length() >= 3 && new_roomid.length() <= 16)) {
			return jsonDo.doMarshall_CreateRoom(new_roomid, false);
		}
		// 本地,远程聊天室，如果找到重名的，那么不许新建:
		if (serverDef.getLocal_RoomList().map.keySet().contains(new_roomid)
				|| serverDef.getRemote_RoomList().map.keySet().contains(new_roomid)) {
			return jsonDo.doMarshall_CreateRoom(new_roomid, false);
		}
		//此人不能是群主：
		if (isOwner) {
			return jsonDo.doMarshall_CreateRoom(new_roomid, false);
		}
		// 否则可以新建该聊天室：
		return jsonDo.doMarshall_CreateRoom(new_roomid, true);
	}

	/*---------------------------------------------------------------*/
	protected String isJoinRoom(String target_room) {
		// 如果这厮不是群主，遍历所有聊天室，如果确实有这么个聊天室，则可以加入：
		String former = currentRoom.getRoomId();
		if (!isOwner) {
			if(local_RoomList.map.containsKey(target_room) ||
					remote_RoomList.map.containsKey(target_room)) {
				return jsonDo.doMarshall_JoinRoom(client_id, former, target_room, true);
			}
		}
		// 不然不行：
		return jsonDo.doMarshall_JoinRoom(client_id, former, target_room, false);
	}

	/*---------------------------------------------------------------*/
	protected String isMoveJoin(String former, String roomid, String identity) {
		// 遍历所有本地聊天室，如果确实有这么个聊天室，则可以加入：
		if (local_RoomList.map.containsKey(roomid)) {
			return jsonDo.doMarshall_MoveJoin(former, roomid, identity);
		} else {
			// 不然不行,转战mainHall：
			String mainhall = mainHall.getRoomId();
			return jsonDo.doMarshall_MoveJoin(former, mainhall, identity);
		}
	}
	/*---------------------------------------------------------------*/
	protected String isDeleteRoom(String delete_target) {
		// 遍历chatroom,如果找到这个room，且client_id是群主，那么可以删：
		if (local_RoomList.map.containsKey(delete_target) && isOwner) {

			return jsonDo.doMarshall_DeleteRoom(delete_target, true);
		} else {
			return jsonDo.doMarshall_DeleteRoom(delete_target, false);
		}
	}

	/*---------------------------------------------------------------*/
	protected String isMessage(String content) {
		return jsonDo.doMarshall_Message(client_id, content);
	}

	/*---------------------------------------------------------------*/
	protected String isQuit(String client_id) {
		if (isOwner) {
			return jsonDo.doMarshall_Quit(client_id, true);
		}
		return jsonDo.doMarshall_Quit(client_id, false);
	}

	/*---------------------------------------------------------------*/
}
