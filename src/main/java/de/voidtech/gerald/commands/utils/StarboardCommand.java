package main.java.de.voidtech.gerald.commands.utils;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.commands.CommandCategory;
import main.java.de.voidtech.gerald.entities.Server;
import main.java.de.voidtech.gerald.entities.StarboardConfig;
import main.java.de.voidtech.gerald.service.ServerService;
import main.java.de.voidtech.gerald.service.StarboardService;
import main.java.de.voidtech.gerald.util.MRESameUserPredicate;
import main.java.de.voidtech.gerald.util.ParsingUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Command
public class StarboardCommand extends AbstractCommand {
	
	@Autowired
	private ServerService serverService;
	
	@Autowired
	private StarboardService starboardService;
	
	@Autowired
	EventWaiter waiter;
	
	private void getAwaitedReply(Message message, String question, Consumer<String> result) {
        message.getChannel().sendMessage(question).queue();
        waiter.waitForEvent(MessageReceivedEvent.class,
                new MRESameUserPredicate(message.getAuthor()),
                event -> {
                    result.accept(event.getMessage().getContentRaw());
                }, 30, TimeUnit.SECONDS, 
                () -> message.getChannel().sendMessage(String.format("Request timed out.")).queue());
    }
	
	private void setupStarboard(Message message, List<String> args, Server server) {
		if (starboardService.getStarboardConfig(server.getId()) != null)
			message.getChannel().sendMessage("**A Starboard has already been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();
		else {
			getAwaitedReply(message, "**Please enter a channel ID or mention to be used for the starboard:**", response -> {
				String channelID = ParsingUtils.filterSnowflake(response);
				if (!ParsingUtils.isSnowflake(channelID))
					message.getChannel().sendMessage("**The channel you provided is not valid!**").queue();
				else if (!message.getGuild().getChannels().contains(message.getJDA().getGuildChannelById(channelID)))
						message.getChannel().sendMessage("**The channel you provided is not in this server!**").queue();
				else promptForStarCount(message, channelID, server);
			});
		}
	}
	
	private void promptForStarCount(Message message, String channelID, Server server) {
		getAwaitedReply(message, "**Please enter the star count:**", response -> {
			if (!ParsingUtils.isInteger(response))
				message.getChannel().sendMessage("**You need to specify a number for the star count!**").queue();
			else if (Integer.parseInt(response) < 1) 
				message.getChannel().sendMessage("**Your star count must be at least 1! We recommend 5**").queue();
			else starboardService.completeStarboardSetup(message, channelID, response, server);
		});
	}

	private void disableStarboard(Message message, Server server) {
		if (starboardService.getStarboardConfig(server.getId()) != null)
			starboardService.deleteStarboardConfig(message, server);
		else
			message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();	
	}

	private void changeChannel(Message message, List<String> args, Server server) {
		if (starboardService.getStarboardConfig(server.getId()) != null) {
			String channelID = ParsingUtils.filterSnowflake(args.get(1));
			if (!ParsingUtils.isSnowflake(channelID))
				message.getChannel().sendMessage("**The channel you provided is not valid!**").queue();
			else if (!message.getGuild().getChannels().contains(message.getJDA().getGuildChannelById(channelID)))
					message.getChannel().sendMessage("**The channel you provided is not in this server!**").queue();
			else {
				StarboardConfig config = starboardService.getStarboardConfig(server.getId());
				config.setChannelID(channelID);
				starboardService.updateConfig(config);
				message.getChannel().sendMessage("**Message channel has been changed to <#" + channelID + ">!**").queue();
			}
		} else message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();	
	}

	private void changeRequiredStarCount(Message message, List<String> args, Server server) {
		if (starboardService.getStarboardConfig(server.getId()) != null) {
			if (!ParsingUtils.isInteger(args.get(1))) message.getChannel().sendMessage("**You need to specify a number for the star count!**").queue();
			else {
				StarboardConfig config = starboardService.getStarboardConfig(server.getId());
				config.setRequiredStarCount(Integer.parseInt(args.get(1)));
				starboardService.updateConfig(config);
				message.getChannel().sendMessage("**Required count has been changed to " + args.get(1) + "!**").queue();
			}
		} else message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();	
	}

	private void showIgnoredChannels(Message message, Server server) {
		if (starboardService.getStarboardConfig(server.getId()) != null) {
			List<String> ignoredChannels = starboardService.getIgnoredChannels(server.getId());
			String ignoredChannelMessage = "";
			if (ignoredChannels == null) {
				ignoredChannelMessage = "None ignored!";
			} else {
				for (String id : ignoredChannels)
					ignoredChannelMessage = ignoredChannelMessage + "<#" + id + ">\n";
			}
		message.getChannel().sendMessageEmbeds(constructIgnoredChannelEmbed(ignoredChannelMessage)).queue();		
		} else message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();
	}

	private void ignoreChannel(Message message, List<String> args, Server server) {
		if (message.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
			if (starboardService.getStarboardConfig(server.getId()) != null) {
				String channelID = ParsingUtils.filterSnowflake(args.get(1));
				if (!ParsingUtils.isSnowflake(channelID))
					message.getChannel().sendMessage("**The channel you provided is not valid!**").queue();
				else if (!message.getGuild().getChannels().contains(message.getJDA().getGuildChannelById(channelID)))
						message.getChannel().sendMessage("**The channel you provided is not in this server!**").queue();
					else {
						if (starboardService.getIgnoredChannels(server.getId()) == null) {
							starboardService.addChannelToIgnorelist(server.getId(), channelID);
							message.getChannel().sendMessage("<#" + channelID + "> **has been added to the blacklist!**").queue();
						} else if (starboardService.getIgnoredChannels(server.getId()).contains(channelID))
							message.getChannel().sendMessage("**This channel is already blacklisted!**").queue();
						else {
							starboardService.addChannelToIgnorelist(server.getId(), channelID);
							message.getChannel().sendMessage("<#" + channelID + "> **has been added to the blacklist!**").queue();							
						}
				}
			} else message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();
		}
	}
	
	private void unignoreChannel(Message message, List<String> args, Server server) {
		if (message.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
			if (starboardService.getStarboardConfig(server.getId()) != null) {
				String channelID = ParsingUtils.filterSnowflake(args.get(1));
				if (!ParsingUtils.isSnowflake(channelID))
					message.getChannel().sendMessage("**The channel you provided is not valid!**").queue();
				else if (!message.getGuild().getChannels().contains(message.getJDA().getGuildChannelById(channelID)))
						message.getChannel().sendMessage("**The channel you provided is not in this server!**").queue();
					else {
						if (starboardService.getIgnoredChannels(server.getId()) == null)
							message.getChannel().sendMessage("**There is no blacklist yet!**").queue();
						else if (starboardService.getIgnoredChannels(server.getId()).contains(channelID)) {
							starboardService.removeFromIgnorelist(server.getId(), channelID);
							message.getChannel().sendMessage("<#" + channelID + "> **has been removed from the blacklist!**").queue();	
						} else
							message.getChannel().sendMessage("**This channel is not yet blacklisted!**").queue();						
				}
			} else message.getChannel().sendMessage("**A Starboard has not been set up here! Did you mean to use one of these?**\n\n" + this.getUsage()).queue();
		}
	}
	
	private MessageEmbed constructIgnoredChannelEmbed(String ignoredChannelMessage) {
		MessageEmbed ignoredChannelEmbed = new EmbedBuilder()
				.setTitle("Starboard Ignored Channels")
				.setColor(Color.ORANGE)
				.setDescription(ignoredChannelMessage)
				.build();
		return ignoredChannelEmbed;
	}

	@Override
	public void executeInternal(Message message, List<String> args) {
		if (message.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
			Server server = serverService.getServer(message.getGuild().getId());
			
			switch (args.get(0)) {
			case "setup":
				setupStarboard(message, args, server);
				break;
			case "count":
				changeRequiredStarCount(message, args, server);
				break;
			case "channel":
				changeChannel(message, args, server);
				break;
			case "disable":
				disableStarboard(message, server);
				break;
			case "ignore":
				ignoreChannel(message, args, server);
				break;
			case "unignore":
				unignoreChannel(message, args, server);
				break;
			case "ignored":
				showIgnoredChannels(message, server);
				break;
			default:
				message.getChannel().sendMessage("**That's not a valid subcommand! Try this instead:**\n\n" + this.getUsage()).queue();
			}
		} else message.getChannel().sendMessage("**You need the ** `Manage Channels` **Permission to do that!**").queue();	
	}

	@Override
	public String getDescription() {
		return "Do you like quoting things? Funny, interesting and more? Perfect!\n"
				+ "Our starboard system allows you to react to messages with the :star: emote and have them automatically sent to"
				+ " a starboard channel in your server! Your server admins can choose the channel and number of stars needed to get it pinned!"
				+ " Additionally, they may choose to ignore some channels. Ignore a channel with the ignore command, allow it again with the"
				+ " unignore command, and show all the ignored channels with the ignored command!";
	}

	@Override
	public String getUsage() {
		return "starboard setup [Channel mention / ID] [Required star count]\n"
				+ "starboard count [New number of stars needed]\n"
				+ "starboard channel [New channel mention / ID]\n"
				+ "starboard disable\n"
				+ "starboard ignore [channel mention / ID]\n"
				+ "starboard unignore [channel mention / ID]\n"
				+ "starboard ignored\n\n"
				+ "NOTE: You MUST run the setup command first!";
	}

	@Override
	public String getName() {
		return "starboard";
	}

	@Override
	public CommandCategory getCommandCategory() {
		return CommandCategory.UTILS;
	}

	@Override
	public boolean isDMCapable() {
		return false;
	}

	@Override
	public boolean requiresArguments() {
		return true;
	}

	@Override
	public String[] getCommandAliases() {
		String[] aliases = {"autoquote", "quotechannel", "sb"};
		return aliases;
	}
	
	@Override
	public boolean canBeDisabled() {
		return true;
	}

}
