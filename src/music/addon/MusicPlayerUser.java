/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music.addon;

import addon.AddonEmptyImpl;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import container.TokenAdvancedContainer;
import container.detector.TokenDetectorContainer;
import container.detector.TokenStringDetector;
import java.util.List;
import music.GuildPlayer;
import music.GuildPlayerFactory;
import music.Music;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

/**
 *
 * @author bowen
 */
public class MusicPlayerUser extends AddonEmptyImpl implements Music.AudioAddon  {
    
    @Override
    public String getName() {
        return "Music Player User Commands";
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
        
        GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
        
        if (container.getAsString().equalsIgnoreCase("start")) {
            
            VoiceChannel voiceChannel = e.getGuild().getAudioManager().getConnectedChannel();
            
            if (voiceChannel == null) {
                List<VoiceChannel> voiceChannels = e.getGuild().getVoiceChannelsByName("music", true);
                if (voiceChannels.isEmpty()) {
                    return false;
                }
                VoiceChannel userVoiceChannel = voiceChannels.get(0);
                if (userVoiceChannel == null) {
                    return false;
                }
                e.getGuild().getAudioManager().openAudioConnection(userVoiceChannel);
                player.sendNewInfoMessage();
                return true;
            }
            
        } else if (container.getAsString().equalsIgnoreCase("queue")) {
            if (!container.hasNext()) {
                return false;
            }
            container.next();
            String link = container.getAsString();
            
            AudioManager audioManager = e.getGuild().getAudioManager();
            if (audioManager == null || !audioManager.isConnected()) {
                return false;
            }
            player.enqueue(link);
            
            return true;
            
        } else if (container.getAsString().equalsIgnoreCase("voteskip") || container.getAsString().equalsIgnoreCase("votenext")) {
            player.next();
            return true;
        }
        return false;
    }

}
