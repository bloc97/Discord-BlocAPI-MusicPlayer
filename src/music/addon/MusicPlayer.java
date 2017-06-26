/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music.addon;

import addon.AddonEmptyImpl;
import music.Music.AudioAddon;
import music.GuildPlayer;
import music.GuildPlayerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import container.TokenAdvancedContainer;
import container.detector.TokenDetectorContainer;
import container.detector.TokenStringDetector;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

/**
 *
 * @author bowen
 */

public class MusicPlayer extends AddonEmptyImpl implements AudioAddon {
    

    public MusicPlayer() {
    }
    
    @Override
    public String getName() {
        return "Music Player";
    }

    @Override
    public short getUid() {
        return 0;
    }

    @Override
    public TokenDetectorContainer getTriggerDetector() {
        return new TokenDetectorContainer(new TokenStringDetector("music"));
    }

    @Override
    public boolean hasPermissions(MessageReceivedEvent e) {
        return true;
    }

    @Override
    public boolean onMessage(MessageReceivedEvent e, TokenAdvancedContainer container, AudioPlayerManager playerManager) {
        
        if (container.getAsString().equalsIgnoreCase("join")) {
            
            VoiceChannel voiceChannel = e.getGuild().getAudioManager().getConnectedChannel();
            
            if (voiceChannel == null) {
                VoiceChannel userVoiceChannel = e.getMember().getVoiceState().getChannel();
                if (userVoiceChannel == null) {
                    return false;
                }
                e.getGuild().getAudioManager().openAudioConnection(userVoiceChannel);
                GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
                player.sendNewInfoMessage();
                return true;
            }
            
        } else if (container.getAsString().equalsIgnoreCase("play")) {
            if (!container.hasNext()) {
                return false;
            }
            container.next();
            String link = container.getAsString();
            
            AudioManager audioManager = e.getGuild().getAudioManager();
            if (audioManager == null || !audioManager.isConnected()) {
                return false;
            }
            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
            player.enplay(link);
            
            return true;
            
        }  else if (container.getAsString().equalsIgnoreCase("queue")) {
            if (!container.hasNext()) {
                return false;
            }
            container.next();
            String link = container.getAsString();
            
            AudioManager audioManager = e.getGuild().getAudioManager();
            if (audioManager == null || !audioManager.isConnected()) {
                return false;
            }
            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
            player.enqueue(link);
            
            return true;
            
        } else if (container.getAsString().equalsIgnoreCase("next")) {
            
            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
            player.next();
            return true;
        } else if (container.getAsString().equalsIgnoreCase("leave")) {
            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
            
            AudioManager audioManager = player.getAudioManager();
            if (!audioManager.isConnected()) {
                return false;
            }
            audioManager.closeAudioConnection();
            player.removeInfoMessage();
            return true;
        }
        return false;
    }

    
}
