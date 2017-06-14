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
