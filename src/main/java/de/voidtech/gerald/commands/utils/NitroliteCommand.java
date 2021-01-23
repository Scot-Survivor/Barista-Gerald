package main.java.de.voidtech.gerald.commands.utils;

import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.service.NitroliteService;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class NitroliteCommand extends AbstractCommand {
    @Override
    public void executeInternal(Message message, List<String> args) {
        NitroliteService nls = NitroliteService.getInstance();

        List<Emote> emotes = message.getJDA()//
                .getEmoteCache()//
                .stream()//
                .filter(emote -> emote.getName().equals(args.get(0)))//
                .collect(Collectors.toList());

        if (!emotes.isEmpty()) {
            final String content = StringUtils.join(args.subList(1, args.size()), " ") +
                    " " + nls.constructEmoteString(emotes.get(0));

            nls.sendMessage(message, content);
        }
    }

    @Override
    public String getDescription() {
        return "Enables you to use emotes from servers Barista-Gerald is on everywhere";
    }

    @Override
    public String getUsage() {
        return "emote_name [text](optional)";
    }
}
