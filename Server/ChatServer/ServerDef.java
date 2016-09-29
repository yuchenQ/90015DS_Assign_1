package Server.ChatServer;

import java.util.*;

import Server.ChatRoom.LocalChatRoom;
import Server.ChatRoom.RemoteChatRoom;
import Server.ClientConnection.ClientInfo;
import Server.Tool.CrazyitMap;

public class ServerDef {
	/*---------------------------------------------------------------*/
	private CrazyitMap<String, ClientInfo> userList;
	private CrazyitMap<String, LocalChatRoom> local_RoomList;
	private CrazyitMap<String, RemoteChatRoom> remote_RoomList;
	private Set<String> lockedClients;
	private String server_id;
	private LocalChatRoom mainHall;

	/*---------------------------------------------------------------*/
	public ServerDef(String server_id) {

		this.userList = new CrazyitMap<String, ClientInfo>();
		this.local_RoomList = new CrazyitMap<String, LocalChatRoom>();
		this.remote_RoomList = new CrazyitMap<String, RemoteChatRoom>();
		this.lockedClients = Collections.synchronizedSet(new HashSet<String>());
		this.server_id = server_id;
		this.mainHall = new LocalChatRoom("", "");
	}

	/*---------------------------------------------------------------*/
	public String getServerId() {
		return server_id;
	}

	public void setServerId(String server_id) {
		this.server_id = server_id;
	}

	/*---------------------------------------------------------------*/
	public CrazyitMap<String, ClientInfo> getUserList() {
		return userList;
	}

	public void addUserList(String name, ClientInfo client) {
		this.userList.put(name, client);
	}

	public void removeUser(String name) {
		this.userList.map.remove(name);
	}
	/*---------------------------------------------------------------*/
	public CrazyitMap<String, LocalChatRoom> getLocal_RoomList() {
		return local_RoomList;
	}

	public void addLocal_RoomList(String roomid, LocalChatRoom chatroom) {
		this.local_RoomList.put(roomid, chatroom);
	}

	public void removeLocalRoom(String roomid) {
		this.local_RoomList.map.remove(roomid);
	}

	public void removeLocalRoom(LocalChatRoom room) {
		this.local_RoomList.removeByValue(room);
	}
	/*---------------------------------------------------------------*/
	public CrazyitMap<String, RemoteChatRoom> getRemote_RoomList() {
		return remote_RoomList;
	}

	public void addRemote_RoomList(String roomid, RemoteChatRoom chatroom) {
		this.remote_RoomList.put(roomid, chatroom);
	}

	public void removeRemoteRoom(String roomid) {
		this.remote_RoomList.map.remove(roomid);
	}

	public void removeRemoteRoom(RemoteChatRoom room) {
		this.remote_RoomList.removeByValue(room);
	}
	/*---------------------------------------------------------------*/
	public Set<String> getLockedClients() {
		return lockedClients;
	}

	public void addLockedClients(String name) {
		this.lockedClients.add(name);
	}

	public void removeLockedClients(String name){
		this.lockedClients.remove(name);
	}
	/*---------------------------------------------------------------*/
	public LocalChatRoom getMainHall() {
		return mainHall;
	}

	public void setMainHall(LocalChatRoom mainHall) {
		this.mainHall = mainHall;
	}
	/*---------------------------------------------------------------*/
}
