package main.java.de.voidtech.gerald;

import java.util.Arrays;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import main.java.de.voidtech.gerald.entities.GlobalConfig;
import main.java.de.voidtech.gerald.listeners.MessageListener;
import main.java.de.voidtech.gerald.listeners.ReadyListener;
import main.java.de.voidtech.gerald.service.GeraldConfig;
import main.java.de.voidtech.gerald.service.GlobalConfigService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.entities.EntityBuilder;

@SpringBootApplication
public class Gerald {
	
	@Bean
	@DependsOn(value = "sessionFactory")
	@Autowired
	public JDA getJDA(MessageListener msgListener, GeraldConfig configService, GlobalConfigService globalConfService, EventWaiter eventWaiter) throws LoginException, InterruptedException
	{
		GlobalConfig globalConf = globalConfService.getGlobalConfig();
		
		
		return JDABuilder.createDefault(configService.getToken()).enableCache(CacheFlag.CLIENT_STATUS)//
				.enableIntents(Arrays.asList(GatewayIntent.values()))//
				.setMemberCachePolicy(MemberCachePolicy.ALL)//
				.setBulkDeleteSplittingEnabled(false)//
				.setCompression(Compression.NONE)//
				.addEventListeners(eventWaiter, msgListener, new ReadyListener())//
				 .setActivity(EntityBuilder.createActivity(globalConf.getStatus(),
						 GlobalConstants.STREAM_URL, globalConf.getActivity()))
				.build()//
				.awaitReady();
	}
	
	@Bean
	public EventWaiter getEventWaiter()
    {
		return new EventWaiter();
    }

	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(Gerald.class);
		
		GeraldConfig configService = new GeraldConfig();
		Properties properties = new Properties();
		
		properties.put("spring.datasource.url", configService.getConnectionURL());
		properties.put("spring.datasource.username", configService.getDBUser());
		properties.put("spring.datasource.password", configService.getDBPassword());
		properties.put("spring.datasource.url", configService.getConnectionURL());
		properties.put("spring.jpa.properties.hibernate.dialect", configService.getHibernateDialect());
		properties.put("jdbc.driver", configService.getDriver());
		properties.put("spring.jpa.hibernate.ddl-auto", "update");
		
		springApp.setDefaultProperties(properties);		
		springApp.run(args);
	}
}