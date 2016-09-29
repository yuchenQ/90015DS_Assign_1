package Server.Tool;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonOperation {
	/*---------------------------------------------------------------*/
	public JSONObject obj;
	public JSONParser parser;
	public JSONArray array;

	/*---------------------------------------------------------------*/
	public JsonOperation() {
		this.obj = new JSONObject();
		this.parser = new JSONParser();
		this.array = new JSONArray();
	}

	/*---------------------------------------------------------------*/
	// 工具方法：输入一个JSONString, 一个key, 这个key是否存在:
	public boolean isContainsKey(String input, String key) {
		this.obj = new JSONObject();
		this.parser = new JSONParser();
		try {
			obj = (JSONObject) parser.parse(input);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return obj.containsKey(key);
	}
	/*---------------------------------------------------------------*/
	// 工具方法：输入一个JSONString， 和想调哪个key的信息，返回那个key对应的信息：
	public String doUnmarshalling(String input, String n) {
		this.obj = new JSONObject();
		this.parser = new JSONParser();
		try {
			obj = (JSONObject) parser.parse(input);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return obj.get(n).toString();
	}
	// 工具方法：输入一个JSONString，返回JSONObject：
	public JSONObject doUnmarshalling(String input) {
		this.obj = new JSONObject();
		this.parser = new JSONParser();
		try {
			obj = (JSONObject) parser.parse(input);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return obj;
	}
	/*---------------------------------------------------------------*/
	// 工具方法：输入一个JSONObject， 返回一个JSONString：
	public String doMarshalling(JSONObject object) {

		StringWriter output = new StringWriter();
		try {
			object.writeJSONString(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output.toString();
	}

	/*---------------------------------------------------------------*/
	// 对应 Newidentity：
	@SuppressWarnings("unchecked")
	public String doMarshall_Newid(boolean isapproved) {
		this.obj = new JSONObject();

		if (isapproved == false) {
			obj.put("type", "newidentity");
			obj.put("approved", "false");
		} else if (isapproved == true) {
			obj.put("type", "newidentity");
			obj.put("approved", "true");
		}
		return doMarshalling(obj);
	}

	@SuppressWarnings("unchecked")
	public String newId_LockIdentity(String server_id, String newidentity) {
		this.obj = new JSONObject();
		obj.put("type", "lockidentity");
		obj.put("serverid", server_id);
		obj.put("identity", newidentity);

		return doMarshalling(obj);
	}
	@SuppressWarnings("unchecked")
	public String newId_LockIdentity(String server_id, String newidentity, boolean islocked) {
		this.obj = new JSONObject();
		obj.put("type", "lockidentity");
		obj.put("serverid", server_id);
		obj.put("identity", newidentity);

		if(islocked) {
			obj.put("locked", "true");
		} else {
			obj.put("locked", "false");
		}
		return doMarshalling(obj);
	}
	// newidentity的广播消息：
	@SuppressWarnings("unchecked")
	public String newId_roomchange(String client_id, String roomid) {
		this.obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", "");
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
	// 对应 List：
	@SuppressWarnings("unchecked")
	public String doMarshall_List(ArrayList<String> roomlist) {
		this.obj = new JSONObject();
		this.array = new JSONArray();
		array.addAll(roomlist);

		obj.put("type", "roomlist");
		obj.put("rooms", array);
		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
	// 对应 Who：
	@SuppressWarnings("unchecked")
	public String doMarshall_Who(String roomid, Set<String> members, String owner) {
		this.obj = new JSONObject();
		this.array = new JSONArray();
		array.addAll(members);

		obj.put("type", "roomcontents");
		obj.put("roomid", roomid);
		obj.put("identities", array);
		obj.put("owner", owner);

		return doMarshalling(obj);
	}

	/*---------------------------------------------------------------*/
	// 对应 CreateRoom：
	@SuppressWarnings("unchecked")
	public String doMarshall_CreateRoom(String new_roomid, boolean isapproved) {
		this.obj = new JSONObject();
		if(!isapproved) {
			obj.put("type", "createroom");
			obj.put("roomid", new_roomid);
			obj.put("approved", "false");
		} else {
			obj.put("type", "createroom");
			obj.put("roomid", new_roomid);
			obj.put("approved", "true");
		}
		return doMarshalling(obj);
	}
	// createroom成功后的广播信息:
	@SuppressWarnings("unchecked")
	public String CR_BoardCastMsg(String client_id, String former, String new_roomid) {
		this.obj = new JSONObject();

		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", former);
		obj.put("roomid", new_roomid);

		return doMarshalling(obj);
	}
	// 写入给目标服务器新房信息：
	@SuppressWarnings("unchecked")
	public String CR_LockRoomId(String roomid, String serverid) {
		this.obj = new JSONObject();
		obj.put("type", "lockroomid");
		obj.put("serverid", serverid);
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}
	// 目标服务器返回的信息：
	@SuppressWarnings("unchecked")
	public String CR_LockRoomId(String serverid, String roomid, boolean isapproved) {
		this.obj = new JSONObject();
		obj.put("type", "lockroomid");
		obj.put("serverid", serverid);
		obj.put("roomid", roomid);
		if (isapproved) {
			obj.put("locked", "true");
		} else {
			obj.put("locked", "false");
		}

		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
	// 对应 JoinRoom：
	@SuppressWarnings("unchecked")
	public String doMarshall_JoinRoom(String client_id, String former,
			String target_room, boolean isapproved) {
		this.obj = new JSONObject();

		if (!isapproved) {
			obj.put("type", "roomchange");
			obj.put("identity", client_id);
			obj.put("former", former);
			obj.put("roomid", former);
		} else if (isapproved) {
			obj.put("type", "roomchange");
			obj.put("identity", client_id);
			obj.put("former", former);
			obj.put("roomid", target_room);
		}
		return doMarshalling(obj);
	}
	//join的远程连接信息，传回给client:
	@SuppressWarnings("unchecked")
	public String doMarshall_JoinRemote(String roomid, String host, String port) {
		this.obj = new JSONObject();

		obj.put("type", "route");
		obj.put("roomid", roomid);
		obj.put("host", host);
		obj.put("port", port);

		return doMarshalling(obj);
	}
	//join的用户信息，传回给CC处理:
	@SuppressWarnings("unchecked")
	public String doMarshall_MoveJoin(String former, String roomid, String identity) {
		this.obj = new JSONObject();
		obj.put("type", "movejoin");
		obj.put("identity", identity);
		obj.put("former", former);
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}
	//movejoin的广播信息：
	@SuppressWarnings("unchecked")
	public String MJ_roomchange(String client_id, String former, String roomid) {
		this.obj = new JSONObject();

		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", former);
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}
	//serverchange 信息:
	@SuppressWarnings("unchecked")
	public String MJ_Serverchange(String serverid) {
		this.obj = new JSONObject();
		obj.put("type", "serverchange");
		obj.put("approved", "true");
		obj.put("serverid", serverid);

		return doMarshalling(obj);
	}

	/*---------------------------------------------------------------*/
	@SuppressWarnings("unchecked")
	public String doMarshall_DeleteRoom(String roomid, boolean isapproved) {
		this.obj = new JSONObject();
		if (!isapproved) {
			obj.put("type", "deleteroom");
			obj.put("roomid", roomid);
			obj.put("approved", "false");
		} else if (isapproved) {
			obj.put("type", "deleteroom");
			obj.put("roomid", roomid);
			obj.put("approved", "true");
		}
		return doMarshalling(obj);
	}
	//DeleteRoom 的广播信息:
	@SuppressWarnings("unchecked")
	public String DR_BoardCastMsg(String client_id, String former, String mainhall) {
		this.obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", former);
		obj.put("roomid", mainhall);

		return doMarshalling(obj);
	}
	//DeleteRoom 的远程信息:
	@SuppressWarnings("unchecked")
	public String DR_RemoteMsg(String roomid) {
		this.obj = new JSONObject();
		obj.put("type", "deleteroom");
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
	// 对应 Message：
	@SuppressWarnings("unchecked")
	public String doMarshall_Message(String client_id, String content) {
		this.obj = new JSONObject();

		obj.put("type", "message");
		obj.put("identity", client_id);
		obj.put("content", content);

		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
	@SuppressWarnings("unchecked")
	public String doMarshall_Quit(String client_id, boolean isOwner) {
		this.obj = new JSONObject();

		obj.put("type", "quit");
		obj.put("identity", client_id);
		if (isOwner) {
			obj.put("isowner", "true");
		} else {
			obj.put("isowner", "false");
		}
		return doMarshalling(obj);
	}

	@SuppressWarnings("unchecked")
	public String Quit_RoomChange(String client_id, String former) {
		this.obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", former);
		obj.put("roomid", "");

		return doMarshalling(obj);
	}

	@SuppressWarnings("unchecked")
	public String Quit_RoomChangeMsg(String client_id, String former, String mainhall) {
		this.obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", client_id);
		obj.put("former", former);
		obj.put("roomid", mainhall);

		return doMarshalling(obj);
	}

	@SuppressWarnings("unchecked")
	public String Quit_RemoteMsg(String name, String roomid) {
		this.obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", name);
		obj.put("roomid", roomid);

		return doMarshalling(obj);
	}

	@SuppressWarnings("unchecked")
	public String Quit_RemoteMsg(String name) {
		this.obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", name);

		return doMarshalling(obj);
	}
	/*---------------------------------------------------------------*/
}
