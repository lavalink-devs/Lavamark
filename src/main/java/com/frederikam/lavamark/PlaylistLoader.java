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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;

public class PlaylistLoader implements AudioLoadResultHandler {

    private RuntimeException exception = null;
    private List<AudioTrack> results = null;

    List<AudioTrack> loadTracksSync(String identifier) {
        Lavamark.PLAYER_MANAGER.loadItem(identifier, this);

        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (exception != null) {
            throw exception;
        }

        return results;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        results = new ArrayList<>();
        results.add(track);
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        results = playlist.getTracks();
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void noMatches() {
        exception = new RuntimeException("No matches");
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        this.exception = exception;
        synchronized (this) {
            notify();
        }
    }
}
