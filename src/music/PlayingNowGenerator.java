/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.awt.Color;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 *
 * @author bowen
 */
public abstract class PlayingNowGenerator {
    public static MessageEmbed generateEmptyPlayingNow() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("Not Playing", null, null);
        eb.setColor(Color.CYAN);
        
        return eb.build();
    }
    public static MessageEmbed generatePlayingNow(AudioTrack track) {
        
        if (track == null) {
            return generateEmptyPlayingNow();
        }
        
        EmbedBuilder eb = new EmbedBuilder();
        AudioTrackInfo info = track.getInfo();
        
        String title = info.title;
        if (title.equals("Unknown title")) {
            title = retreiveFileMain(info.identifier);
        }
        
        try {
            eb.setAuthor(title, info.uri, null);
        } catch (IllegalArgumentException ex) {
            eb.setAuthor(title, null, null);
        }
        
        eb.setDescription(retrieveTime(track.getPosition()) + " | " + retrieveTime(track.getDuration()));
        //eb.setDescription(info.author);
        //eb.setTitle("Now Playing");
        eb.setColor(Color.CYAN);
        eb.setFooter(retreiveUrlMain(info.uri), null);
        
        return eb.build();
    }
    
    public static String generatePlayQueue(BlockingQueue<AudioTrack> queue) {
        String finalString = "```java\nMain Playlist (" + queue.size() + ")\n\n";
        int i = 1;
        for (AudioTrack track : queue) {
            AudioTrackInfo info = track.getInfo();
            
            String title = info.title;
            
            if (title.equals("Unknown title")) {
                title = retreiveFileMain(info.identifier);
            }
            
            finalString += "(" + i + ")- " + title + " [" + retrieveTime(info.length) + "]\n";
            i++;
        }
        
        if (finalString.length() > 1000) {
            finalString = finalString.substring(0, 1000) + "\n...";
        }
        finalString += "\n```";
        return finalString;
    }
    
    public static String retrieveTime(long miliseconds) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(miliseconds % (1000 * 60));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(miliseconds % (1000 * 60 * 60));
        long hours = TimeUnit.MILLISECONDS.toHours(miliseconds);
        
        String secondsString = (seconds < 10) ? "0" + seconds : "" + seconds;
        String minutesString = "" + minutes;
        String hoursString = (hours == 0) ? "" : hours + ":";
        
        return hoursString + minutesString + ":" + secondsString;
    }
    
    public static String retreiveFileMain(String path) {
        if (path.startsWith("music")) {
            path = path.substring(6);
        } else if (path.startsWith("upload")) {
            path = path.substring(7);
        }
        if (path.contains(".")) {
            path = path.substring(0, path.lastIndexOf('.'));
        }
        return path;
    }
    
    public static String retreiveFileExt(String path) {
        if (path.contains(".")) {
            path = path.substring(path.lastIndexOf('.') + 1);
        }
        return path;
    }
    
    public static String retreiveUrlMain(String url) {
        if (url.startsWith("http") || url.startsWith("www")) {
            try {
                if (url.contains("www.")) {
                    String parsed = url.substring(url.indexOf('.') + 1, url.indexOf('.', url.indexOf('.') + 1));
                    return parsed.toUpperCase().charAt(0) + parsed.substring(1);
                } else {
                    String parsed = url.substring(url.indexOf("https://") + 8, url.indexOf('.', url.indexOf("https://") + 8));
                    return parsed.toUpperCase().charAt(0) + parsed.substring(1);
                }
            } catch (Exception ex) {
                return url;
            }
        } else if (url.contains(".")) {
            return retreiveFileExt(url);
        } else {
            return url;
        }
    }
}
