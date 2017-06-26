/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music;

import addon.Addon;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import music.Music.AudioAddon;
import music.addon.MusicPlayer;
import container.ContainerSettings;
import container.TokenAdvancedContainer;
import dbot.BotCommandTrigger;
import dbot.ModuleEmptyImpl;
import music.addon.MusicPlayerAdmin;
import music.addon.MusicPlayerUser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import token.TokenConverter;

/**
 *
 * @author bowen
 */
public class Music extends ModuleEmptyImpl<AudioAddon> {
    
    private final AudioPlayerManager playerManager;
    
    public interface AudioAddon extends Addon {
        public boolean onMessage(MessageReceivedEvent e, TokenAdvancedContainer container, AudioPlayerManager playerManager);
    }
    
    public Music(ContainerSettings containerSettings, TokenConverter tokenConverter, BotCommandTrigger commandTrigger) {
        super(containerSettings, tokenConverter, commandTrigger, new MusicPlayerUser(), new MusicPlayerAdmin());
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    @Override
    public String getFullName() {
        return "Audio Test";
    }

    @Override
    public String getAuthor() {
        return "Bloc97";
    }

    @Override
    public long getUid() {
        return -419738612l;
    }

    @Override
    public boolean onMessage(MessageReceivedEvent e, TokenAdvancedContainer container) {
        if (e.getChannel().getName().equals("music") && !e.getAuthor().isBot()) {
            e.getMessage().delete().queue();
        }
        for (AudioAddon addon : getAddons()) {
            if (addon.hasPermissions(e)) {
                if (onMessageForEachAddon(addon, e, container)) {
                    return true;
                }
                container.reset();
            }
        }
        return false;
    }
    
    @Override
    public boolean onMessageForEachAddon(AudioAddon addon, MessageReceivedEvent e, TokenAdvancedContainer container) {
        return addon.onMessage(e, container, playerManager);
    }

    @Override
    public BotCommandTrigger getCommandTrigger() {
        return new BotCommandTrigger() {
            @Override
            public boolean isMessageTrigger(JDA client, MessageReceivedEvent e) {
                return e.getChannel().getName().equals("music");
            }

            @Override
            public String preParse(JDA client, MessageReceivedEvent e) {
                return e.getMessage().getRawContent();
            }
        };
    }
    

}
