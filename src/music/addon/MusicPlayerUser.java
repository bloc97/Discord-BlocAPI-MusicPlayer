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
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import modules.help.Help;
import music.GuildPlayer;
import music.GuildPlayerFactory;
import music.Music;
import music.PlayingNowGenerator;
import net.bloc97.helpers.Levenshtein;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfDeafenEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
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
    public String getFullHelp() {
        return "Discord Music Player by Bloc97\n\n" + 
               getShortHelp();
    }

    @Override
    public String getShortHelp() {
        return "**!start** - *Starts the music bot if it is idle*\n" +
               "**!queue** <File|URL> - *Enqueues the chosen track*\n" +
               "**!voteskip** - *Vote to skip the current track, skip on majority*\n" +
               "**<File|URL>** - *Enqueues the chosen track*\n" +
               "*Note: These commands only work in the #Music channel of a guild.*\n\n" +
               "**!list** <Page> - *Lists the available tracks*\n\n" +
               "You can drag and drop an audio file in the bot's PM or in the #Music channel to upload.";
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
        
        if (container.getAsString().equalsIgnoreCase("help")) {
            Help.showHelp(e, this);
            return true;
        } else if (container.getAsString().equalsIgnoreCase("list")) {
            container.next();

            int nPerPage = 40;

            File folder = new File("music");
            File[] fileList = folder.listFiles();
            File folderUpload = new File("upload");
            File[] fileUploadList = folderUpload.listFiles();

            int page = container.getAsNumber().intValue();
            int pagesN = (int)Math.ceil(fileUploadList.length/(double)nPerPage);

            page = Math.max(1, page);
            page = Math.min(page, pagesN);

            int startN = Math.min((page - 1) * nPerPage, fileUploadList.length);
            int endN = Math.min(startN + nPerPage, fileUploadList.length);

            if (fileUploadList.length < 1) {
                startN = 0;
                endN = 0;
            }
            
            String finalString = "```\nAvailable Tracks:\n\n";
            for (int i=startN; i<endN; i++) {
                finalString += fileUploadList[i].getName() + "\n";
            }
            finalString += "\nPage " + page + "/" + pagesN + "```";

            e.getAuthor().openPrivateChannel().complete().sendMessage(finalString).queue();

            return true;

        } else if (e.getChannelType() == ChannelType.TEXT) {

            GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());

            if (container.getAsString().equalsIgnoreCase("start") || container.getAsString().equalsIgnoreCase("join")) {

                VoiceChannel voiceChannel = e.getGuild().getAudioManager().getConnectedChannel();

                if (voiceChannel == null) {
                    List<VoiceChannel> voiceChannels = e.getGuild().getVoiceChannelsByName("music", true);
                    if (voiceChannels.isEmpty()) {
                        return true;
                    }
                    VoiceChannel userVoiceChannel = voiceChannels.get(0);
                    if (userVoiceChannel == null) {
                        return true;
                    }
                    e.getGuild().getAudioManager().openAudioConnection(userVoiceChannel);
                    e.getGuild().getAudioManager().setSelfDeafened(true);
                    player.sendNewInfoMessage();
                }

            } else if (container.getAsString().equalsIgnoreCase("queue") || container.getAsString().equalsIgnoreCase("play")) {
                if (!container.hasNext()) {
                    return true;
                }
                container.next();
                String partialName = container.getRemainingContentAsString();
                parseTrackInput(partialName, player, e);

            } else if (container.getAsString().equalsIgnoreCase("voteskip") || container.getAsString().equalsIgnoreCase("votenext") || container.getAsString().equalsIgnoreCase("next") || container.getAsString().equalsIgnoreCase("skip")) {

                player.voteNext(e.getMember());

            } else if (!container.getAsString().startsWith("`")) {
                List<Message.Attachment> attachments = e.getMessage().getAttachments();
                if (!attachments.isEmpty()) {
                    Message.Attachment attachment = attachments.get(0);
                    attachment.download(new File("upload\\" + attachment.getFileName()));
                    return true;
                }

                String partialName = container.getRemainingContentAsString();
                parseTrackInput(partialName, player, e);
            }
            
        } else if (e.getChannelType() == ChannelType.PRIVATE) {
            List<Message.Attachment> attachments = e.getMessage().getAttachments();
            if (!attachments.isEmpty()) {
                Message.Attachment attachment = attachments.get(0);
                attachment.download(new File("upload\\" + attachment.getFileName()));
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
                parseTrackInput(splitPartialName, player, e);
            }
            return;
        }
        
        if (partialName.startsWith("http") || partialName.startsWith("www")) {
            player.enqueue(partialName, e);
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
                player.enqueue(closestFile.getPath(), e);
            }
        }
    }

    @Override
    public boolean onGuildReact(GenericGuildMessageReactionEvent e, AudioPlayerManager playerManager) {
        GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
        
        if (e.getReactionEmote().getName().equals("⏭")) {
            if (e instanceof GuildMessageReactionAddEvent) {
                player.voteNext(e.getMember());
            } else if (e instanceof GuildMessageReactionRemoveEvent) {
                player.cancelVoteNext(e.getMember());
            }
        } else if (e.getReactionEmote().getName().equals("🔂")) {
            
        } else {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean onVoiceEvent(GenericGuildVoiceEvent e, AudioPlayerManager playerManager) { //Removes the vote of all users leaving voice channel
        GuildPlayer player = GuildPlayerFactory.getGuildPlayer(playerManager, e.getGuild());
        
        if (e instanceof GuildVoiceLeaveEvent) {
            GuildVoiceLeaveEvent ee = (GuildVoiceLeaveEvent) e;
            
            if ("music".equalsIgnoreCase(ee.getChannelLeft().getName())) {
                player.cancelVoteNext(e.getMember());
            }
            
        } else if (e instanceof GuildVoiceMoveEvent) {
            GuildVoiceMoveEvent ee = (GuildVoiceMoveEvent) e;
            
            if ("music".equalsIgnoreCase(ee.getChannelLeft().getName())) {
                player.cancelVoteNext(e.getMember());
            }
            
        } else if (e instanceof GuildVoiceDeafenEvent) {
            GuildVoiceSelfDeafenEvent ee = (GuildVoiceSelfDeafenEvent) e;
            
            if (ee.isSelfDeafened()) {
                player.cancelVoteNext(e.getMember());
            }
            
        } else {
            return false;
        }
        return true;
    }
    
}
