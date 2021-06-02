package main.java.de.voidtech.gerald.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "joinleavemessage")

public class JoinLeaveMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private long serverID; 
	
	@Column
	private String channelID; 
	
	@Column
	private String joinMessage; 
	
	@Column
	private String leaveMessage;

	@Column
	private boolean memberDM;

	@Column
	private boolean memberPublic;
	
	@Deprecated
	//ONLY FOR HIBERNATE, DO NOT USE
	JoinLeaveMessage() {
	}
	
	public JoinLeaveMessage(long serverID, String channelID, String joinMessage, String leaveMessage, boolean memberDM, boolean memberPublic)
	{
		this.serverID = serverID;
		this.channelID = channelID;
	    this.joinMessage = joinMessage;
	    this.leaveMessage = leaveMessage;
	    this.memberDM = memberDM;
	    this.memberPublic = memberPublic;
	}
	
	public void setServerID(long serverID) {
		this.serverID = serverID;
	}
	
	public long getServerID() {
		return serverID;
	}
	
	public void setChannelID(String channelID) {
		this.channelID = channelID;
	}

	public void setMemberDM(boolean memberDM) {
		this.memberDM = memberDM;
	}

	public boolean isMemberDM() {
		return memberDM;
	}

	public void setMemberPublic(boolean memberPublic) {
		this.memberPublic = memberPublic;
	}

	public boolean isMemberPublic() {
		return memberPublic;
	}

	public String getChannelID() {
		return channelID;
	}
	
	public void setJoinMessage(String joinMessage) {
		this.joinMessage = joinMessage;
	}
	
	public String getJoinMessage() {
		return joinMessage;
	}
	
	public void setLeaveMessage(String leaveMessage) {
		this.leaveMessage = leaveMessage;
	}
	
	public String getLeaveMessage() {
		return leaveMessage;
	}
}