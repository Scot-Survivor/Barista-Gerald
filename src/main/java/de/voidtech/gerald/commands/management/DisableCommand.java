package main.java.de.voidtech.gerald.commands.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.commands.CommandCategory;
import main.java.de.voidtech.gerald.entities.Server;
import main.java.de.voidtech.gerald.routines.AbstractRoutine;
import main.java.de.voidtech.gerald.service.ServerService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

@Command
public class DisableCommand extends AbstractCommand {

	@Autowired
	private List<AbstractCommand> commands;

	@Autowired
	private List<AbstractRoutine> routines;

	@Autowired
	private ServerService serverService;

	@Override
	public void executeInternal(Message message, List<String> args) {
		//TODO REVIEW: You don't need this line, do you? Since this is a command too it has the canBeDisabled() method.
		if (!commands.contains(this)) commands.add(this);
		
		if (message.getMember().hasPermission(Permission.MANAGE_SERVER)) {
			String targetName = args.get(0).toLowerCase();
			if (targetName.equals("all")) disableAllCommands(message);
			else if (targetName.startsWith("r-")) disableRoutine(targetName, message);
			else disableCommand(targetName, message);
		}
	}

	//TODO: REVIEW do not alter argument variables.
	//TODO: REVIEW You see how you do message.getChannel().sendMessage() a bazillion times in this method?
	// You can have a resultMessage variable and just call the message.getChannel().sendMessage() once in the end of the method.
	// That way the lines get way shorter since its only resultMessage = "Haha funny error" instead of the big message.getChannel().sendMessage chongus
	private void disableCommand(String targetName, Message message) {
		AbstractCommand foundCommand = null;
		for (AbstractCommand command : commands) {
			if (command.getName().equals(targetName) || Arrays.asList(command.getCommandAliases()).contains(targetName)) {
				foundCommand = command;
				targetName = command.getName();
				break;
			}
		}
		if (foundCommand == null)
			message.getChannel().sendMessage("**No command was found with name `" + targetName + "`**").queue();
		else if (!foundCommand.canBeDisabled()) 
			message.getChannel().sendMessage("**The command `"+ targetName + "` cannot be disabled/enabled!**").queue();
		else {
			
			Server server = serverService.getServer(message.getGuild().getId());
			if (server.getCommandBlacklist().contains(targetName)) {
				message.getChannel().sendMessage("**This command is already disabled!**").queue();
			} else {
				server.addToCommandBlacklist(targetName);
				serverService.saveServer(server);
				message.getChannel().sendMessage("**Command `" + targetName + "` has been disabled!**").queue();
			}
		}
	}

	private void disableRoutine(String targetName, Message message) {
		AbstractRoutine foundRoutine = null;
		
		for (AbstractRoutine routine: routines) {
			if (routine.getName().equals(targetName)) {
				foundRoutine = routine;
				break;
			}
		}
		
		if (foundRoutine == null) 
			message.getChannel().sendMessage("**No Routine was found with name `" + targetName + "`**").queue();
		else if (!foundRoutine.canBeDisabled()) 
			message.getChannel().sendMessage("**Routine `"+ targetName + "` cannot be disabled/enabled!**").queue();
		else {
			
			Server server = serverService.getServer(message.getGuild().getId());
			if (server.getRoutineBlacklist().contains(targetName))
				message.getChannel().sendMessage("**This routine is already disabled!**").queue();
			else {
				server.addToRoutineBlacklist(targetName);
				serverService.saveServer(server);
				message.getChannel().sendMessage("**Routine `" + targetName + "`has been disabled!**").queue();
			}
		}
	}

	private void disableAllCommands(Message message) {
		Server server = serverService.getServer(message.getGuild().getId());
		List<AbstractCommand> enabledCommands = new ArrayList<AbstractCommand>();
		for (AbstractCommand command : commands) {
			if (command.canBeDisabled() && !server.getCommandBlacklist().contains(command.getName()))
				server.addToCommandBlacklist(command.getName());
			else if (!command.canBeDisabled()) enabledCommands.add(command);		
		}
		serverService.saveServer(server);
		message.getChannel().sendMessage("**All commands have been disabled except for these:**\n```" + createEnabledCommandString(enabledCommands) + "```").queue();
	}

	private String createEnabledCommandString(List<AbstractCommand> enabledCommands) {
		String message = "";
		for (AbstractCommand command : enabledCommands)
			message += command.getName() + "\n";
		return message;
	}

	@Override
	public String getDescription() {
		return "Allows you to disable a command or routine! Note: Some routines or commands cannot be disabled. Routine names always use the format r-[name]";
	}

	@Override
	public String getUsage() {
		return "disable r-nitrolite\n"
				+ "disable ping";
	}

	@Override
	public String getName() {
		return "disable";
	}

	@Override
	public CommandCategory getCommandCategory() {
		return CommandCategory.MANAGEMENT;
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
		String[] aliases = {"lock"};
		return aliases;
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

}
