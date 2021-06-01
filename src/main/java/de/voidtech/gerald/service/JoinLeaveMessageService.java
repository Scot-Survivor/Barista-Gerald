package main.java.de.voidtech.gerald.service;

import java.awt.Color;

import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import main.java.de.voidtech.gerald.entities.JoinLeaveMessage;
import main.java.de.voidtech.gerald.entities.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@Service
public class JoinLeaveMessageService {
	
	@Autowired
	private ServerService serverService;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	private boolean customJoinLeaveMessageEnabled(long guildID) {
		try(Session session = sessionFactory.openSession())
		{
			JoinLeaveMessage joinLeaveMessage = (JoinLeaveMessage) session.createQuery("FROM JoinLeaveMessage WHERE ServerID = :serverID")
                    .setParameter("serverID", guildID)
                    .uniqueResult();
			return joinLeaveMessage != null;
		}
	}
	
	private JoinLeaveMessage getJoinLeaveMessageEntity(long guildID) {
		try(Session session = sessionFactory.openSession())
		{
			JoinLeaveMessage joinLeaveMessage = (JoinLeaveMessage) session.createQuery("FROM JoinLeaveMessage WHERE ServerID = :serverID")
                    .setParameter("serverID", guildID)
                    .uniqueResult();
			return joinLeaveMessage;
		}
	}
	
	public void sendJoinMessage(GuildMemberJoinEvent event) {
		Server server = serverService.getServer(event.getGuild().getId());
		if (customJoinLeaveMessageEnabled(server.getId())) {
			JoinLeaveMessage joinLeaveMessage = getJoinLeaveMessageEntity(server.getId());
			if (!joinLeaveMessage.getMemberDM() || event.getUser().isBot()) {
				GuildChannel channel = event.getJDA().getGuildChannelById(joinLeaveMessage.getChannelID());
				String message = joinLeaveMessage.getJoinMessage();
				String member = event.getMember().getAsMention();

				MessageEmbed joinMessageEmbed = new EmbedBuilder()
						.setColor(Color.green)
						.setDescription(member + " **" + message + "**")
						.setTimestamp(null)
						.build();

				((MessageChannel) channel).sendMessage(joinMessageEmbed).queue();
			}
			else {
				String message = joinLeaveMessage.getJoinMessage();
				String member = event.getUser().getAsMention();
				User user = event.getUser();

				MessageEmbed joinMessageEmbed = new EmbedBuilder()
						.setColor(Color.green)
						.setDescription(member + " **" + message + "**")
						.setTimestamp(null)
						.build();

				user.openPrivateChannel().queue((channel) ->
						channel.sendMessage(joinMessageEmbed).queue());
			}
		}
	}
	
	public void sendLeaveMessage(GuildMemberRemoveEvent event) {
		Server server = serverService.getServer(event.getGuild().getId());
		if (customJoinLeaveMessageEnabled(server.getId())) {
			JoinLeaveMessage joinLeaveMessage = getJoinLeaveMessageEntity(server.getId());
			GuildChannel channel = event.getJDA().getGuildChannelById(joinLeaveMessage.getChannelID());
			String message = joinLeaveMessage.getLeaveMessage();
			String member = event.getUser().getAsMention();

			MessageEmbed leaveMessageEmbed = new EmbedBuilder()
					.setColor(Color.red)
					.setDescription(member + " **" + message + "**")
					.setTimestamp(null)
					.build();

			((MessageChannel) channel).sendMessage(leaveMessageEmbed).queue();
		}
	}
}
