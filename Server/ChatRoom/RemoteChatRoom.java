package Server.ChatRoom;

public class RemoteChatRoom extends ChatRoom {
	/*---------------------------------------------------------------*/
	protected String server_id;

	/*---------------------------------------------------------------*/
	public RemoteChatRoom(String roomid, String server_id) {
		super(roomid);
		this.server_id = server_id;
	}

	/*---------------------------------------------------------------*/
	public String getServer() {
		return server_id;
	}

	/*---------------------------------------------------------------*/
	public void setServer(String server_id) {
		this.server_id = server_id;
	}
}
