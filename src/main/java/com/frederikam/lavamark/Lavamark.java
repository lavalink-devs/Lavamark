package com.frederikam.lavamark;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Lavamark {

    private static final Logger log = LoggerFactory.getLogger(Lavamark.class);

    static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
    private static final String DEFAULT_PLAYLIST = "https://www.youtube.com/watch?v=7v154aLVo70&list=LLqqLoSLryroL7b7TAL8gfhQ&index=22";
    private static final long INTERVAL = 5 * 1000;
    private static final long STEP_SIZE = 10;
    private static final Object WAITER = new Object();

    private static List<AudioTrack> tracks;
    private static List<Player> players = new ArrayList<>();


    public static void main(String[] args) {
        /* Set up the player manager */
        PLAYER_MANAGER.enableGcMonitoring();
        PLAYER_MANAGER.setItemLoaderThreadPoolSize(100);
        PLAYER_MANAGER.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);

        log.info("Loading AudioTracks");
        tracks = new PlaylistLoader().loadTracksSync(DEFAULT_PLAYLIST);
        log.info(tracks.size() + " tracks loaded. Beginning benchmark...");

        try {
            doLoop();
        } catch (Exception e) {
            log.error("Benchmark ended due to exception!");
            throw new RuntimeException(e);
        }
    }

    private static void doLoop() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            spawnPlayers();
            log.info("Players: " + players.size());

            synchronized (WAITER) {
                WAITER.wait(INTERVAL);
            }
        }
    }

    private static void spawnPlayers() {
        for (int i = 0; i < STEP_SIZE; i++) {
            players.add(new Player());
        }
    }

    static AudioTrack getTrack() {
        int rand = (int)(Math.random() * tracks.size());
        return tracks.get(rand);
    }

}
