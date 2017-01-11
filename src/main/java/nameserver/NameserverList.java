package nameserver;

import java.util.concurrent.CopyOnWriteArrayList;

public class NameserverList extends CopyOnWriteArrayList<ServerInfo> {

	private static final long serialVersionUID = 6794100362656328852L;

	public String getNameserverListText() {
		String output = "";

		for (ServerInfo info : this) {
			output = output + info.domain + "\n";
		}
		return output;
	}

	public INameserver getServer(String domain) {
		INameserver result = null;
		for (ServerInfo info : this)
		{
			if (info.domain.equals(domain))
			{
				result = info.server;
			}
		}
		return result;
	}
}