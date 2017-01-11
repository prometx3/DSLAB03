package nameserver;

import java.rmi.RemoteException;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

public class NameserverRemote implements INameserver {

	private String domain;

	private NameserverList servers;
	private UserInfoList userInfos;

	public String getDomain() {
		return this.domain;
	}

	public NameserverRemote(String domain, NameserverList servers, UserInfoList userInfos) {
		
		String[] reversed = domain.split("\\.");
		if (reversed.length > 1) {
			domain = "";
			for (int i = reversed.length - 1; i >= 0; i--) {
				domain = domain + "." + reversed[i];
			}
			domain = domain.substring(1, domain.length());
		}
		this.domain = domain;
		this.servers = servers;
		this.userInfos = userInfos;
	}

	@Override
	public void registerUser(String username, String address)
			throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

		if (!username.contains(this.domain))
			throw new InvalidDomainException("Invalid Domain!");

		if (getNextIsName(username)) {
			if (userInfos.getUserInfo(username) == null) {
				UserInfo info = new UserInfo();
				info.name = username;
				info.address = address;
				userInfos.add(info);
			} else {
				throw new AlreadyRegisteredException("User already registered!");
			}
		} else {
			INameserverForChatserver server = this.getNameserver(getNextDomainEntry(username));
			
			if (server == null) {
				throw new InvalidDomainException("Invalid Domain!");
			} else {
				server.registerUser(username, address);
			}
		}
	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		if (this.domain.equals(zone)) {
			return this;
		}
		
		return this.servers.getServer(zone);
	}

	private String getNextDomainEntry(String fullDomain) {
		String[] tokensThisDomain = this.domain.split("\\.");
		String[] tokensFullDomain = fullDomain.split("\\.");

		if (this.domain.equals("")) {
			return tokensFullDomain[0];
		}

		return this.domain + (this.domain.equals("") ? "" : ".") + tokensFullDomain[tokensThisDomain.length];
	}

	private boolean getNextIsName(String fullDomain) {
		String[] tokensThisDomain = this.domain.split("\\.");
		String[] tokensFullDomain = fullDomain.split("\\.");

		return tokensThisDomain.length + 1 == tokensFullDomain.length;
	}

	@Override
	public String lookup(String username) throws RemoteException {
		String result = "";

		if (getNextIsName(username)) {
			UserInfo info = this.userInfos.getUserInfo(username);
			if (info != null)
				result = info.address;
		} else {
			INameserver nameserver = this.servers.getServer(getNextDomainEntry(username));
			if (nameserver != null) {
				result = nameserver.lookup(username);
			}
		}

		return result;
	}

	// INameserver
	@Override
	public synchronized void registerNameserver(String domain, INameserver nameserver,
			INameserverForChatserver nameserverForChatserver)
			throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

		INameserver server = null;

		// Check if aktueller Server ist Parent
		if (this.domain.equals(getParentDomain(domain))) {

			// Wenn ja, schau nach ob der server nicht schon vorhanden ist
			server = servers.getServer(domain);
			if (server != null) {
				// Server vorhanden - Fehler
				throw new AlreadyRegisteredException("Server already registered!");
			} else {
				// Server einhängen
				ServerInfo info = new ServerInfo();
				info.domain = domain;
				info.server = nameserver;
				servers.add(info);
			}
		} else {
			// Rekursiver Aufruf notwendig

			String nextDomain = getNextDomainEntry(domain);
			
			// Schau ob der aktuelle Server schon vorhanden ist
			server = servers.getServer(nextDomain);
			if (server == null) {
				throw new InvalidDomainException("Invalid Domain");
			} else {
				server.registerNameserver(domain, nameserver, nameserverForChatserver);
			}
		}
	}

	private String getParentDomain(String domain) {
		if (domain.lastIndexOf(".") == -1) {
			return "";
		}

		return domain.substring(0, domain.lastIndexOf("."));
	}
}
