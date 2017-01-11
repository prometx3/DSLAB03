package nameserver;

import java.util.LinkedList;

public class UserInfoList extends LinkedList<UserInfo> {

	private static final long serialVersionUID = -3489134570481252575L;
	
	public String getAdressesText()
	{
		String result = "";
		
		for (UserInfo info : this)
		{
			result = result + info.name + " " + info.address + "\n"; 
		}
		
		return result;
	}
	
	public UserInfo getUserInfo(String name)
	{
		UserInfo result = null;
		
		for (UserInfo info : this)
		{
			if (info.name.equals(name))
			{
				result = info;
				break;
			}			
		}
		return result;
	}

}
