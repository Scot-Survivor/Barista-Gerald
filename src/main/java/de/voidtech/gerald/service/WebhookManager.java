package main.java.de.voidtech.gerald.service;

import java.io.OutputStream;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;

@Service
public class WebhookManager {
	
	@Autowired
	private ThreadManager threadManager;
	
    private static final Logger LOGGER = Logger.getLogger(WebhookManager.class.getName());
	
	public Webhook getOrCreateWebhook(TextChannel targetChannel, String webhookName, String selfID) {
		
		List<Webhook> webhooks = targetChannel.retrieveWebhooks().complete();
		for (Webhook webhook : webhooks) {
			if (webhook.getName().equals(webhookName) && webhook.getOwnerAsUser().getId().equals(selfID)) {
				return webhook;
			}
		}
		Webhook newWebhook = targetChannel.createWebhook(webhookName).complete();
		return newWebhook;		
	}
	
	private void executeWebhookPost(String content, String avatarUrl, String username, Webhook webhook) {
		String messageToBeSent = content.replaceAll("@everyone", "``@``everyone").replaceAll("@here", "``@``here");
		
		JSONObject webhookPayload = new JSONObject();
        webhookPayload.put("content", messageToBeSent);
        webhookPayload.put("username", username);
        webhookPayload.put("avatar_url", avatarUrl);
        webhookPayload.put("tts", false);
        try {              			
        	
            URL url = new URL(webhook.getUrl());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Barista-Gerald");
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            OutputStream stream = connection.getOutputStream();
            stream.write(webhookPayload.toString().getBytes());
            stream.flush();
            stream.close();

            connection.getInputStream().close();
            connection.disconnect();
        	
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during ServiceExecution: " + ex.getMessage());
            ex.printStackTrace();
        }
	}
	
	public void postMessage(String content, String avatarUrl, String username, Webhook webhook) {
        Runnable webhookThreadRunnable = new Runnable() {
			public void run() {
				executeWebhookPost(content, avatarUrl, username, webhook);
			}
		};
		threadManager.getThreadByName("T-Webhook").execute(webhookThreadRunnable);   
	}
	
	public void postMessageWithFallback(Message message, String content, String avatarUrl, String username, String webhookName) {
		EnumSet<Permission> perms = message.getGuild().getSelfMember().getPermissions((GuildChannel) message.getChannel());
		
        if (perms.contains(Permission.MANAGE_WEBHOOKS)) {
        	postMessage(
        			content,
        			avatarUrl,
        			username,
        			getOrCreateWebhook((TextChannel) message.getChannel(),
        					webhookName,
        					message.getJDA().getSelfUser().getId())
        	);
         } else {
             message.getChannel().sendMessage(content).queue();
         }
	}
	
}
