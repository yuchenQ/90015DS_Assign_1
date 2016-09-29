package Server.ChatServer;

public class ServerInfo {
	/*---------------------------------------------------------------*/
	private String serverName;
	private String address;
	private int clientPort;
	private int managementPort;

	/*---------------------------------------------------------------*/
	public ServerInfo(String serverName, String address, int clientPort, int managementPort) {
		this.serverName = serverName;
		this.address = address;
		this.clientPort = clientPort;
		this.managementPort = managementPort;
	}

	/*---------------------------------------------------------------*/
	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	/*---------------------------------------------------------------*/
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	/*---------------------------------------------------------------*/
	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	/*---------------------------------------------------------------*/
	public int getManagementPort() {
		return managementPort;
	}

	public void setManagementPort(int managementPort) {
		this.managementPort = managementPort;
	}
	
	/*---------------------------------------------------------------*/
}
