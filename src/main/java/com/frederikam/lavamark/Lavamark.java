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

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lavamark {

    private static final Logger log = LoggerFactory.getLogger(Lavamark.class);

    static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
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

        String jarPath = Lavamark.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);

        Options options = new Options()
            .addOption("b", "block", true, "The IPv6 block to use for rotation (YouTube only). This must be specified as CIDR notation.")
            .addOption("s", "step", true, "The number of players to spawn after a fixed interval. Be careful when using large values.")
            .addOption("i", "identifier", true, "The audio identifier to use for the test. Must be a URL pointing to a supported audio source.")
            .addOption("t", "transcode", false, "Simulate a load by forcing transcoding.")
            .addOption("h", "help", false, "Displays the supported command line arguments.");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        CommandLine parsed;

        try {
            parsed = parser.parse(options, args);
        } catch (ParseException parseException) {
            formatter.printHelp("java -jar " + jarName, options);
            return;
        }

        if (parsed.hasOption("help")) {
            formatter.printHelp("java -jar " + jarName, options);
            return;
        }

        boolean transcode = parsed.hasOption("transcode");

        if (transcode) {
            ConnectorNativeLibLoader.loadConnectorLibrary();
        }

        if (parsed.hasOption("block")) {
            String ipBlock = parsed.getOptionValue("block");

            YoutubeAudioSourceManager ytasm = PLAYER_MANAGER.source(YoutubeAudioSourceManager.class);
            RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(Collections.singletonList(new Ipv6Block(ipBlock)));
            new YoutubeIpRotatorSetup(planner).forSource(ytasm).setup();

            log.info("IP rotation configured.");
        }

        String identifier = parsed.getOptionValue("identifier", DEFAULT_OPUS);

        log.info("Loading AudioTracks from identifier {}", identifier);

        tracks = new PlaylistLoader().loadTracksSync(identifier);
        log.info(tracks.size() + " tracks loaded. Beginning benchmark...");

        long stepSize = STEP_SIZE;

        if (parsed.hasOption("step")) {
            stepSize = Math.max(1, Long.parseLong(parsed.getOptionValue("step")));
            log.info("Step set to {} players every {} milliseconds.", stepSize, INTERVAL);
        }

        try {
            doLoop(stepSize, transcode);
        } catch (Exception e) {
            log.error("Benchmark ended due to exception!");
            throw new RuntimeException(e);
        }

        System.exit(0);
    }

    private static void doLoop(long stepSize, boolean transcode) throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            spawnPlayers(stepSize, transcode);

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

    private static void spawnPlayers(long stepSize, boolean transcode) {
        for (int i = 0; i < stepSize; i++) {
            players.add(new Player(transcode));
        }
    }

    static AudioTrack getTrack() {
        int rand = (int)(Math.random() * tracks.size());
        return tracks.get(rand).makeClone();
    }

}
