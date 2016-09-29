package Server.ChatRoom;

public class ChatRoom {
	/*---------------------------------------------------------------*/
	protected String roomid;

	/*---------------------------------------------------------------*/
	public ChatRoom(String roomid) {
		this.roomid = roomid;
	}

	/*---------------------------------------------------------------*/
	public String getRoomId() {
		return this.roomid;
	}

	/*---------------------------------------------------------------*/
	public void setRoomId(String roomid) {
		this.roomid = roomid;
	}
	/*---------------------------------------------------------------*/
}
