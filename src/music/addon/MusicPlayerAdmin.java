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
import java.io.File;
import modules.help.Help;
import music.GuildPlayer;
import music.GuildPlayerFactory;
import music.Music;
import static music.addon.MusicPlayerUser.parseTrackInput;
import net.bloc97.helpers.Levenshtein;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

/**
 *
 * @author bowen
 */
public class MusicPlayerAdmin extends AddonEmptyImpl implements Music.AudioAddon {
    
    @Override
    public String getName() {
        return "Music Player Admin Commands";
    }

    @Override
    public String getFullHelp() {
        return getShortHelp();
    }

    @Override
    public String getShortHelp() {
        return "**!join** - *Forces the music bot to join your current voice channel*\n" +
               "**!leave** - *Forces the music bot to leave the guild's voice channel*\n" +
               "**!play** <File|URL> - *Plays the chosen track or resumes playing*\n" +
               "**!stop** - *Stops the music bot*\n" +
               "**!skip** - *Skips the current track*\n" +
               "**!clear** - *Clears the playlist*\n" +
               "**!pause** - *Pauses the music bot*\n" +
               "**!unpause** - *Unauses the music bot*\n" +
               "*Note: These commands only work in the #Music channel of a guild.*";
    }

    @Override
    public short getUid() {
        return 1;
    }

    @Override
    public TokenDetectorContainer getTriggerDetector() {
        return new TokenDetectorContainer(new TokenStringDetector("musicadmin"));
    }

    @Override
    public boolean hasPermissions(MessageReceivedEvent e) {
        
        if (e.getChannelType() != ChannelType.TEXT) {
            return false;
        }
        
        for (Role role : e.getMember().getRoles()) {
            if (role.hasPermission(Permission.ADMINISTRATOR)) {
                return true;
            }
        }
        
        if (e.getMember().isOwner()) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean onMessage(MessageReceivedEvent e, TokenAdvancedContainer container, AudioPlayerManager playerManager) {
        
        if (e.getChannelType() == ChannelType.TEXT) {
            
            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());

            if (container.getAsString().equalsIgnoreCase("join")) {

                VoiceChannel voiceChannel = e.getGuild().getAudioManager().getConnectedChannel();

                if (voiceChannel == null) {
                    VoiceChannel userVoiceChannel = e.getMember().getVoiceState().getChannel();
                    if (userVoiceChannel == null) {
                        return true;
                    }
                    e.getGuild().getAudioManager().openAudioConnection(userVoiceChannel);
                    player.sendNewInfoMessage();
                }

            } else if (container.getAsString().equalsIgnoreCase("play")) {

                if (!container.hasNext()) {
                    if (player.getAudioPlayer().isPaused()) {
                        player.unpause();
                    } else if (player.getAudioPlayer().getPlayingTrack() == null) {
                        player.next();
                    }
                    return true;
                }
                container.next();
                String partialName = container.getRemainingContentAsString();
                parseTrackInput(partialName, player, e);


            } else if (container.getAsString().equalsIgnoreCase("skip") || container.getAsString().equalsIgnoreCase("next")) {
                player.next();
            } else if (container.getAsString().equalsIgnoreCase("stop")) {
                player.stop();
            } else if (container.getAsString().equalsIgnoreCase("clear")) {
                player.clear();
            } else if (container.getAsString().equalsIgnoreCase("pause")) {
                player.pause();
            } else if (container.getAsString().equalsIgnoreCase("unpause")) {
                player.unpause();
            } else if (container.getAsString().equalsIgnoreCase("leave")) {
                AudioManager audioManager = player.getAudioManager();
                if (audioManager.isConnected()) {
                    audioManager.closeAudioConnection();
                    player.removeInfoMessage();
                }
            } else if (container.getAsString().equalsIgnoreCase("help")) {
                Help.showHelp(e, this);
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
    
    public static void parseTrackInput(String partialName, GuildPlayer player, MessageReceivedEvent e) {
        
        if (partialName.contains("\n")) {
            String[] partialNameList = partialName.split("\n");
            for (String splitPartialName : partialNameList) {
                parseTrackInput(splitPartialName, player, e); //play only first of the list
                return;
            }
            return;
        }
        
        if (partialName.startsWith("http") || partialName.startsWith("www")) {
            player.enplay(partialName, e);
        } else {
            File folder = new File("music");
            File[] fileList = folder.listFiles();
            File folderUpload = new File("upload");
            File[] fileUploadList = folderUpload.listFiles();

            File closestFile = null;
            int searchScore = Integer.MAX_VALUE;

            for (File file : fileList) {
                int thisScore = Levenshtein.subwordDistance(file.getName().toLowerCase(), partialName.toLowerCase());
                if (thisScore < searchScore) {
                    searchScore = thisScore;
                    closestFile = file;
                }
            }
            for (File file : fileUploadList) {
                int thisScore = Levenshtein.subwordDistance(file.getName().toLowerCase(), partialName.toLowerCase());
                if (thisScore < searchScore) {
                    searchScore = thisScore;
                    closestFile = file;
                }
            }

            if (closestFile != null) {
                player.enplay(closestFile.getPath(), e);
            }
        }
    }
}
