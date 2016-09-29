package Server.Tool;

import org.kohsuke.args4j.Option;

public class CmdLineArgs {
	/*---------------------------------------------------------------*/
	@Option(required = true, name = "-n", aliases = { "--serverid" }, usage = "ServerID")
	private String serverid = null;
	/*---------------------------------------------------------------*/
	@Option(required = true, name = "-l", usage = "servers_conf")
	private String path = null;

	/*---------------------------------------------------------------*/
	public void serServerId(String serverid) {
		this.serverid = serverid;
	}

	/*---------------------------------------------------------------*/
	public void setserverconf(String path) {
		this.path = path;
	}

	/*---------------------------------------------------------------*/
	public String getServerid() {
		return this.serverid;
	}

	/*---------------------------------------------------------------*/
	public String getserversconf() {
		return this.path;
	}

	/*---------------------------------------------------------------*/
}
