package main.java.de.voidtech.gerald.commands.utils;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.commands.CommandCategory;
import main.java.de.voidtech.gerald.entities.NitroliteAlias;
import main.java.de.voidtech.gerald.entities.NitroliteEmote;
import main.java.de.voidtech.gerald.service.EmoteService;
import main.java.de.voidtech.gerald.service.NitroliteService;
import main.java.de.voidtech.gerald.service.ServerService;
import main.java.de.voidtech.gerald.service.WebhookManager;
import main.java.de.voidtech.gerald.util.ParsingUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

@Command
public class NitroliteCommand extends AbstractCommand {
    
	@Autowired
	private NitroliteService nitroliteService;
	
	@Autowired
	private ServerService serverService;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private EmoteService emoteService;
	
	@Autowired
	private WebhookManager webhookManager;
		
	private boolean aliasAlreadyExists(String name, long serverID) {
		try(Session session = sessionFactory.openSession())
		{
			NitroliteAlias alias = (NitroliteAlias) session.createQuery("FROM NitroliteAlias WHERE ServerID = :serverID AND aliasName = :aliasName")
                    .setParameter("serverID", serverID)
                    .setParameter("aliasName", name)
                    .uniqueResult();
			return alias != null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<NitroliteAlias> getAliases(long serverID) {
		try(Session session = sessionFactory.openSession())
		{
			List<NitroliteAlias> aliases = (List<NitroliteAlias>) session.createQuery("FROM NitroliteAlias WHERE ServerID = :serverID")
                    .setParameter("serverID", serverID)
                    .list();
			return (List<NitroliteAlias>) aliases;
		}
	}
	
	private void createEmoteAlias(Message message, String aliasName, String aliasID) {
		long serverID = serverService.getServer(message.getGuild().getId()).getId();
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();
		
			NitroliteAlias alias = new NitroliteAlias(serverID, aliasName, aliasID);
			
			alias.setAliasName(aliasName);
			alias.setEmoteID(aliasID);
			alias.setServer(serverID);
			
			session.saveOrUpdate(alias);
			session.getTransaction().commit();
		}
		message.getChannel().sendMessage("**Alias created with the name **`" + aliasName + "`**!**").queue();
	}
	
	private void removeAlias(String aliasName, long serverID, Message message) {
		try(Session session = sessionFactory.openSession())
		{
			session.getTransaction().begin();
			session.createQuery("DELETE FROM NitroliteAlias WHERE ServerID = :serverID AND AliasName = :aliasName")
				.setParameter("serverID", serverID)
				.setParameter("aliasName", aliasName)
				.executeUpdate();
			session.getTransaction().commit();
		}
		message.getChannel().sendMessage("**Alias with name **`" + aliasName + "`** has been deleted!**").queue();
	}
	
	private void searchEmoteDatabase(Message message, List<String> args) {
		String search = args.get(1);
		
        List<NitroliteEmote> result = emoteService.getEmotes(search, message.getJDA());
        
        String searchResult = "**Database searched for: **`" + search + "`\n";
        if (result.size() == 0) {
        	searchResult += "Nothing found :(";
        } else {
        	for (NitroliteEmote emote: result) {
                searchResult += nitroliteService.constructEmoteString(emote) + " - " + emote.getName() + " - " + emote.getID() + "\n";
            }
        }
        
        webhookManager.postMessageWithFallback(
        		message, searchResult,
        		message.getJDA().getSelfUser().getAvatarUrl(),
        		message.getJDA().getSelfUser().getName(),
        		webhookManager.getOrCreateWebhook((TextChannel) message.getChannel(), "BGNitrolite"));

	}
	
    private void addEmoteAlias(Message message, List<String> args) {    
    	if (args.size() < 3) {
    		message.getChannel().sendMessage("**You need to supply more arguments!**\n\n" + this.getUsage()).queue();
    	} else {
    		long serverID = serverService.getServer(message.getGuild().getId()).getId();
    		if (aliasAlreadyExists(args.get(1), serverID)) {
    			message.getChannel().sendMessage("**An alias with that name already exists!**").queue();
    		} else {
    			if (!ParsingUtils.isInteger(args.get(2))) {
    				message.getChannel().sendMessage("**You have not supplied a valid emote ID!**").queue();
    			} else if (emoteService.getEmoteById(args.get(2), message.getJDA()) == null) {
    				message.getChannel().sendMessage("**That emote cannot be accessed. Is Gerald in the server with that emote?**").queue();
    			} else {
    				createEmoteAlias(message, args.get(1), args.get(2));
    			}
    		}
    	}
	}
    
    private void removeEmoteAlias(Message message, List<String> args) {
    	if (args.size() < 2) {
    		message.getChannel().sendMessage("**You need to supply more arguments!**\n\n" + this.getUsage()).queue();
    	} else {
    		long serverID = serverService.getServer(message.getGuild().getId()).getId();
    		if (!aliasAlreadyExists(args.get(1), serverID)) {
    			message.getChannel().sendMessage("**An alias with that name does not exist!**").queue();
    		} else {
    			removeAlias(args.get(1), serverID, message);
    		}
    	}
    }

    private void sendAllAliases(Message message) {
    	long serverID = serverService.getServer(message.getGuild().getId()).getId();
    	List<NitroliteAlias> aliasesList = getAliases(serverID);
    	
    	String aliasMessage = "**Aliases for this server:**\n";
    	
    	if (aliasesList.size() == 0) {
    		aliasMessage += "Nothing here... Create some aliases!";
    	} else {
        	for (NitroliteAlias alias : aliasesList) {
        		NitroliteEmote emote = emoteService.getEmoteById(alias.getEmoteID(), message.getJDA());
        		aliasMessage += nitroliteService.constructEmoteString(emote) + " - **Alias:** `" + alias.getAliasName() + "` **ID:** `" + alias.getEmoteID() + "`\n";
        	}	
    	}
    	
        webhookManager.postMessageWithFallback(
        		message, aliasMessage,
        		message.getJDA().getSelfUser().getAvatarUrl(),
        		message.getJDA().getSelfUser().getName(),
        		webhookManager.getOrCreateWebhook((TextChannel) message.getChannel(), "BGNitrolite"));
    }
    
	@Override
    public void executeInternal(Message message, List<String> args) {
		switch (args.get(0)) {
		case "search":
			searchEmoteDatabase(message, args);
			break;
		
		case "add":
			addEmoteAlias(message, args);
			break;
			
		case "delete":
			removeEmoteAlias(message, args);
			break;
		
		case "aliases":
			sendAllAliases(message);
			break;
			
		default:
			message.getChannel().sendMessage("**That's not a valid subcommand! Try something like this:**\n\n" + this.getUsage()).queue();
		}
    }

	@Override
    public String getDescription() {
        return "No Nitro? No problem!\n\n"
        		+ "Nitrolite uses some magic code to allow you to use your favourite emotes anywhere with Gerald!\n"
        		+ "To do so, simply write out your message, but add your emotes like this: [:a_cool_emote:] (note the square brackets, they are required)\n"
        		+ "If you want to use a specific emote, add an alias! Use the search to first find the emote you are looking for, Then add an alias using its ID and a name of your choice!\n"
        		+ "NOTE: Alias names cannot contain spaces. Use either dashes or underscores! Also, this feature works best when Gerald can manage webhooks!";
    }

    @Override
    public String getUsage() {
        return "[:an_awesome_emote:]\n"
        		+ "nitrolite search (emote_name)\n"
        		+ "nitrolite add (alias_name) (emote_id)\n"
        		+ "nitrolite delete (alias_name)\n"
        		+ "nitrolite aliases";
    }

	@Override
	public String getName() {
		return "nitrolite";
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
		String[] aliases = {"nitro", "nl", "emotes", "emote"};
		return aliases;
	}
}
