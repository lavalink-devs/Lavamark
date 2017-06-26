/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.frederikam.lavamark;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lavamark {

    private static final Logger log = LoggerFactory.getLogger(Lavamark.class);

    static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
    private static final String DEFAULT_PLAYLIST = "https://www.youtube.com/watch?v=7v154aLVo70&list=LLqqLoSLryroL7b7TAL8gfhQ&index=22";
    private static final String DEFAULT_OPUS = "https://www.youtube.com/watch?v=M_36UBLkni8";
    private static final long INTERVAL = 2 * 1000;
    private static final long STEP_SIZE = 20;
    private static final Object WAITER = new Object();

    private static List<AudioTrack> tracks;
    private static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();


    public static void main(String[] args) {
        /* Set up the player manager */
        PLAYER_MANAGER.enableGcMonitoring();
        PLAYER_MANAGER.setItemLoaderThreadPoolSize(100);
        PLAYER_MANAGER.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);

        log.info("Loading AudioTracks");

        String identifier;
        if (args.length >= 1 && args[0].equals("opus")) {
            identifier = DEFAULT_OPUS;
        } else {
            identifier = DEFAULT_PLAYLIST;
        }

        tracks = new PlaylistLoader().loadTracksSync(identifier);
        log.info(tracks.size() + " tracks loaded. Beginning benchmark...");

        try {
            doLoop();
        } catch (Exception e) {
            log.error("Benchmark ended due to exception!");
            throw new RuntimeException(e);
        }

        System.exit(0);
    }

    private static void doLoop() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            spawnPlayers();

            AudioConsumer.Results results = AudioConsumer.getResults();
            log.info("Players: " + players.size() + ", Null frames: " + results.getLossPercentString());

            if(results.getEndReason() != AudioConsumer.EndReason.NONE) {
                log.info("Benchmark ended. Reason: " + results.getEndReason());
                break;
            }

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
        return tracks.get(rand).makeClone();
    }

}
