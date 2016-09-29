package Server.ClientConnection;

import Server.ChatRoom.LocalChatRoom;

public class ClientInfo {
	/*---------------------------------------------------------------*/
	private String client_id;
	private LocalChatRoom currentChatRoom;
	private ClientConnection currentCC;
	private boolean isOwner;

	/*---------------------------------------------------------------*/
	public ClientInfo
	(String client_id, LocalChatRoom currentChatRoom, ClientConnection currentCC, boolean isOwner) {
		this.client_id = client_id;
		this.currentChatRoom = currentChatRoom;
		this.currentCC = currentCC;
		this.isOwner = isOwner;
	}

	/*---------------------------------------------------------------*/
	public String getClient_id() {
		return client_id;
	}

	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}
	/*---------------------------------------------------------------*/
	public LocalChatRoom getCurrentChatRoom() {
		return currentChatRoom;
	}

	public void setCurrentChatRoom(LocalChatRoom currentChatRoom) {
		this.currentChatRoom = currentChatRoom;
	}
	/*---------------------------------------------------------------*/
	public ClientConnection getCurrentCC() {
		return currentCC;
	}

	public void setCurrentCC(ClientConnection currentCC) {
		this.currentCC = currentCC;
	}
	/*---------------------------------------------------------------*/
	public boolean getIsOwner() {
		return isOwner;
	}

	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}
	/*---------------------------------------------------------------*/
}
