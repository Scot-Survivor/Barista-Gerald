package main.java.de.voidtech.gerald.routines.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import main.java.de.voidtech.gerald.GlobalConstants;
import main.java.de.voidtech.gerald.annotations.Routine;
import main.java.de.voidtech.gerald.entities.Server;
import main.java.de.voidtech.gerald.routines.AbstractRoutine;
import main.java.de.voidtech.gerald.routines.RoutineCategory;
import main.java.de.voidtech.gerald.service.ChatbotService;
import main.java.de.voidtech.gerald.service.GeraldConfig;
import main.java.de.voidtech.gerald.service.ServerService;
import main.java.de.voidtech.gerald.util.ParsingUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

@Routine
public class PingResponseRoutine extends AbstractRoutine {

	@Autowired
	private ServerService serverService;
	
    @Autowired
    private GeraldConfig config;
    
    @Autowired
    private ChatbotService geraldAI;
	
    private void sendPingInfoMessage(Message message) {
    	Server guild = serverService.getServer(message.getGuild().getId());
		String prefix = guild.getPrefix() == null ? config.getDefaultPrefix() : guild.getPrefix();
		new GlobalConstants();
		String linktree = GlobalConstants.LINKTREE_URL;
		
		MessageEmbed pingResponseEmbed = new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle("You called? :telephone:", linktree)
				.setDescription("**This Guild's prefix is:** " + prefix + "\nTry " + prefix + "help to see some commands!")
				.build();
		message.getChannel().sendMessageEmbeds(pingResponseEmbed).queue();
    }
    
	@Override
	public void executeInternal(Message message) {
		if (message.getChannelType().equals(ChannelType.TEXT) && message.getMentionedUsers().contains(message.getJDA().getSelfUser())) {
			List<String> messageBlocks = new ArrayList<String>(Arrays.asList(message.getContentRaw().split(" ")));
			if (messageBlocks.size() == 1 && ParsingUtils.filterSnowflake(message.getContentRaw()).equals(message.getJDA().getSelfUser().getId()))
				sendPingInfoMessage(message);
			else {
				message.getChannel().sendTyping();
				message.getChannel().sendMessage(geraldAI.getReply(message.getContentDisplay(), message.getId())).queue();
			}
		}
	}

	@Override
	public String getDescription() {
		return "Allows Gerald to respond when he is mentioned";
	}

	@Override
	public boolean allowsBotResponses() {
		return false;
	}

	@Override
	public boolean canBeDisabled() {
		return true;
	}

	@Override
	public String getName() {
		return "r-ping";
	}
	
	@Override
	public RoutineCategory getRoutineCategory() {
		return RoutineCategory.UTILS;
	}

}
