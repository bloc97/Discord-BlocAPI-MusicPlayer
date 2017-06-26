/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author bowen
 */
public abstract class GuildPlayerFactory {
    
    private static final ConcurrentMap<Guild, GuildPlayer> map = new ConcurrentHashMap<>();
    
    public static GuildPlayer getGuildPlayer(AudioPlayerManager playerManager, Guild guild) {
        if (map.containsKey(guild)) {
            return map.get(guild);
        }
        
        GuildPlayer audioPlayer = new GuildPlayer(playerManager, guild);
        
        map.put(guild, audioPlayer);
        return audioPlayer;
    }
    
}
