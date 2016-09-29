package Server.ChatRoom;

import java.io.*;
import java.util.*;

import Server.ClientConnection.ClientConnection;
import Server.Tool.CrazyitMap;

public class LocalChatRoom extends ChatRoom {
	/*---------------------------------------------------------------*/
	protected String owner;
	protected CrazyitMap<String, ClientConnection> members;

	/*---------------------------------------------------------------*/
	public LocalChatRoom(String roomid, String owner) {
		super(roomid);
		this.owner = owner;
		this.members = new CrazyitMap<String, ClientConnection>();
	}

	/*---------------------------------------------------------------*/
	public String getOwner() {
		return owner;
	}

	/*---------------------------------------------------------------*/
	public CrazyitMap<String, ClientConnection> getAll_Members() {
		return this.members;
	}

	/*---------------------------------------------------------------*/
	public Set<String> getAll_MembersName() {
		return this.members.map.keySet();
	}

	/*---------------------------------------------------------------*/
	public Set<ClientConnection> getAll_ClientThreads() {
		return this.members.valueSet();
	}

	/*---------------------------------------------------------------*/
	public void addMemeber(String client_id, ClientConnection cc) {
		members.put(client_id, cc);
	}

	/*---------------------------------------------------------------*/
	public void addMemebers(CrazyitMap<String, ClientConnection> new_member) {
		Map<String, ClientConnection> new_memberMap = new_member.map;
		members.map.putAll(new_memberMap);
	}

	/*---------------------------------------------------------------*/
	public void removeMember(String member) {
		members.map.remove(member);
	}

	/*---------------------------------------------------------------*/
	public void boardCastAll(String msg) throws IOException {
		Set<ClientConnection> all_cc = members.valueSet();
		for (ClientConnection cc : all_cc) {
			cc.write(msg);
		}
	}
	/*---------------------------------------------------------------*/
}
