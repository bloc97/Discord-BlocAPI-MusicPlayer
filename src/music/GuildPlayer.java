/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

/**
 *
 * @author bowen
 */
public class GuildPlayer extends AudioEventAdapter implements AudioSendHandler {
    
    private final AudioPlayer audioPlayer;
    private final AudioManager audioManager;
    private final AudioPlayerManager playerManager;
    private AudioFrame lastFrame;
    
    private final TextChannel infoChannel;
    private Message playingMessage;
    private Message playlistMessage;
    
    private final BlockingQueue<AudioTrack> queue;
    
    private final Timer playingTimer;
    
    private Set<User> votedNext = new HashSet<>();
    
    public GuildPlayer(AudioPlayerManager playerManager, Guild guild) {
        this.audioPlayer = playerManager.createPlayer();
        this.audioManager = guild.getAudioManager();
        this.playerManager = playerManager;
        this.queue = new LinkedBlockingQueue<>();
        audioManager.setSendingHandler(this);
        audioPlayer.addListener(this);
        
        infoChannel = guild.getTextChannelsByName("music", true).get(0);
        
        //playlistMessage = playlistChannel.sendMessage(PlayingNowGenerator.generatePlayingNow(audioPlayer.getPlayingTrack())).complete();
        
        playingTimer = new Timer();
        playingTimer.schedule(new TimerTask() {
            private Future lastFuture;
            @Override
            public void run() {
                if (playingMessage != null) {
                    AudioTrack currentTrack = audioPlayer.getPlayingTrack();
                    if (currentTrack != null) {
                        if (lastFuture != null) {
                            lastFuture.cancel(false);
                        }
                        lastFuture = playingMessage.editMessage(PlayingNowGenerator.generatePlayingNow(currentTrack)).submit(true);
                    }
                }
            }
        }, 2000, 4000);
        
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public TextChannel getPlaylistChannel() {
        return infoChannel;
    }

    public void sendNewInfoMessage() {
        if (playingMessage != null || playlistMessage != null) {
            removeInfoMessage();
        }
        playingMessage = infoChannel.sendMessage(PlayingNowGenerator.generateEmptyPlayingNow()).complete();
        playlistMessage = infoChannel.sendMessage(PlayingNowGenerator.generatePlayQueue(queue)).complete();
    }
    
    public void removeInfoMessage() {
        if (playingMessage != null) {
            playingMessage.delete().complete();
            playingMessage = null;
        }
        if (playlistMessage != null) {
            playlistMessage.delete().complete();
            playlistMessage = null;
        }
    }
    
    public void refreshInfoMessage() {
        refreshPlayingInfoMessage();
        refreshPlaylistInfoMessage();
    }
    public void refreshPlayingInfoMessage() {
        if (playingMessage != null) {
            playingMessage.editMessage(PlayingNowGenerator.generatePlayingNow(audioPlayer.getPlayingTrack())).queue();
        }
    }
    public void refreshPlaylistInfoMessage() {
        if (playlistMessage != null) {
            playlistMessage.editMessage(PlayingNowGenerator.generatePlayQueue(queue)).queue();
        }
    }
    
    public void play(AudioTrack track) {
        audioPlayer.playTrack(track);
    }
    
    public void clear() {
        queue.clear();
        refreshInfoMessage();
    }
    
    public void stop() {
        audioPlayer.stopTrack();
    }
    
    public void unpause() {
        audioPlayer.setPaused(false);
    }
    public void pause() {
        audioPlayer.setPaused(true);
    }
    public void togglePause() {
        audioPlayer.setPaused(!audioPlayer.isPaused());
    }
    
    public void queue(AudioTrack track) {
        if (!audioPlayer.startTrack(track, true)) {
            queue.offer(track);
        }
    }
    
    public boolean next() {
        votedNext = new HashSet<>();
        return audioPlayer.startTrack(queue.poll(), false);
    }
    
    public void voteNext(User user, int totalUsers) {
        if (totalUsers - 1 == 1) {
            next();
            return;
        }
        
        votedNext.add(user);
        //if (votedNext.size() >= ((int)Math.ceil((totalUsers - 1) / 2d))) {
        if (votedNext.size()/(double)(totalUsers - 1) > 0.5d) {
            next();
        }
    }
    
    public void enplay(String identifier, MessageReceivedEvent e) {
        playerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                play(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                boolean isFirst = true;
                for (AudioTrack track : playlist.getTracks()) {
                    if (isFirst) {
                        play(track);
                        isFirst = false;
                    } else {
                        queue(track);
                    }
                }
                refreshPlaylistInfoMessage();
            }

            @Override
            public void noMatches() {
                e.getAuthor().openPrivateChannel().complete().sendMessage("Load Failed: No Match!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                e.getAuthor().openPrivateChannel().complete().sendMessage((exception.getMessage() == null) ? "Load Failed" : "Load Failed: " + exception.getMessage()).queue();
            }
        });
    }
    
    public void enqueue(String identifier, MessageReceivedEvent e) {
        playerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                queue(track);
                refreshPlaylistInfoMessage();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    queue(track);
                }
                refreshPlaylistInfoMessage();
            }

            @Override
            public void noMatches() {
                e.getAuthor().openPrivateChannel().complete().sendMessage("Load Failed: No Match!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                e.getAuthor().openPrivateChannel().complete().sendMessage((exception.getMessage() == null) ? "Load Failed" : "Load Failed: " + exception.getMessage()).queue();
            }
        });
    }
    
    @Override
    public boolean canProvide() {
        if (lastFrame == null) {
            lastFrame = audioPlayer.provide();
        }
        return lastFrame != null;
    }
    
    @Override
    public byte[] provide20MsAudio() {
        if (!canProvide()) {
            return new byte[0];
        }
        byte[] data = lastFrame.data;
        lastFrame = null;
        return data;
        
    }
    
    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (next()) {
            } else {
                playingMessage.editMessage(PlayingNowGenerator.generateEmptyPlayingNow()).queue();
                playlistMessage.editMessage(PlayingNowGenerator.generatePlayQueue(queue)).queue();
            }
        } else if (endReason != AudioTrackEndReason.REPLACED) {
            playingMessage.editMessage(PlayingNowGenerator.generateEmptyPlayingNow()).queue();
        }
    }
    
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (playingMessage != null) {
            //playingMessage.editMessage(PlayingNowGenerator.generatePlayingNow(track)).queue();
        } else {
            //playingMessage = infoChannel.sendMessage(PlayingNowGenerator.generatePlayingNow(track)).complete();
        }
        if (playlistMessage != null) {
            playlistMessage.editMessage(PlayingNowGenerator.generatePlayQueue(queue)).queue();
        } else {
            //playlistMessage = infoChannel.sendMessage(PlayingNowGenerator.generatePlayQueue(queue)).complete();
        }
    }
    
    
}
