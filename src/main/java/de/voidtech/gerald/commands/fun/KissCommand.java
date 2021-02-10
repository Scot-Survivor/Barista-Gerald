package main.java.de.voidtech.gerald.commands.fun;

import java.util.List;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.ActionsCommand;
import net.dv8tion.jda.api.entities.Message;

@Command
public class KissCommand extends ActionsCommand {

	@Override
	public void executeInternal(Message message, List<String> args) {
		super.sendAction(message, "kiss");
	}

	@Override
	public String getDescription() {
		return "Kiss a user!";
	}

	@Override
	public String getUsage() {
		return "kiss @user";
	}

	@Override
	public String getName() {
		return "kiss";
	}

}
