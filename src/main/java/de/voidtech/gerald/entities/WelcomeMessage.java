package main.java.de.voidtech.gerald.entities;


import javax.persistence.*;

@Entity
@Table(name = "joinleavemessage")
public class WelcomeMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private long guildID;

    @Column
    private String welcomeMessage;

    @Deprecated
        //ONLY FOR HIBERNATE, DO NOT USE
    WelcomeMessage() {
    }

    public WelcomeMessage(long guildId, String welcomeMsg) {
        this.guildID = guildId;
        this.welcomeMessage = welcomeMsg;
    }

    public long getGuildID() {
        return guildID;
    }

    public void setGuildID(long guildID) {
        this.guildID = guildID;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }
    public void setWelcomeMessage(String welcomeMsg){
        this.welcomeMessage = welcomeMsg;
    }

    public long getId() {
        return id;
    }
}
