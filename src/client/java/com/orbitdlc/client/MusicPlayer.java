package com.orbitdlc.client;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

final class MusicPlayer {
    record Track(String title, String artist, String cover, boolean playing, boolean previous, boolean toggle, boolean next,
                 boolean error) {
        static final Track EMPTY = new Track(null, null, null, false, false, false, false, false);
        static final Track ERROR = new Track(null, null, null, false, false, false, false, true);

        boolean hasTitle() {
            return title != null && !title.isBlank();
        }
    }

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    private static final Gson GSON = new Gson();
    private volatile Track track = Track.EMPTY;
    private Process process;
    private boolean busy;
    private long nextPoll;
    private int generation;

    synchronized Track poll() {
        if (!WINDOWS) return Track.ERROR;
        if (System.nanoTime() >= nextPoll) request("status");
        return track;
    }

    Track current() {
        return track;
    }

    synchronized boolean control(String command) {
        return request(command);
    }

    private boolean request(String command) {
        if (!WINDOWS || busy) return false;
        busy = true;
        nextPoll = System.nanoTime() + 1_000_000_000L;
        int requestGeneration = generation;
        Thread.startVirtualThread(() -> exchange(command, requestGeneration));
        return true;
    }

    private BufferedReader responses;

    private void exchange(String command, int requestGeneration) {
        Track result = Track.ERROR;
        Process active = null;
        try {
            BufferedReader reader;
            synchronized (this) {
                if (requestGeneration != generation) return;
                if (process == null || !process.isAlive()) {
                    process = start();
                    responses = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                }
                active = process;
                reader = responses;
            }
            Process watched = active;
            // Release a blocked read if the Windows media service stops responding.
            try (var timeout = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())) {
                var deadline = timeout.schedule(() -> watched.destroyForcibly(), 8, java.util.concurrent.TimeUnit.SECONDS);
                try {
                    var input = active.getOutputStream();
                    input.write((command + "\n").getBytes(StandardCharsets.US_ASCII));
                    input.flush();
                    String response = reader.readLine();
                    if (response == null) throw new IOException("Windows media helper exited");
                    result = GSON.fromJson(response, Track.class);
                } finally {
                    deadline.cancel(false);
                }
            }
        } catch (IOException | RuntimeException failure) {
            if (active != null) active.destroyForcibly();
        } finally {
            synchronized (this) {
                if (requestGeneration == generation) {
                    track = result == null ? Track.ERROR : result;
                    if (track.error()) nextPoll = System.nanoTime() + 4_000_000_000L;
                    busy = false;
                }
            }
        }
    }

    synchronized void stop() {
        generation++;
        if (process != null) process.destroyForcibly();
        process = null;
        responses = null;
        track = Track.EMPTY;
        busy = false;
        nextPoll = 0;
    }

    private static Process start() throws IOException {
        try (var resource = MusicPlayer.class.getResourceAsStream("/assets/orbitdlc/windows-media.ps1")) {
            if (resource == null) throw new IOException("Missing Windows media helper");
            var script = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            var encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            var executable = Path.of(System.getenv("SystemRoot"), "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
            return new ProcessBuilder(executable.toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-EncodedCommand", encoded).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        }
    }
}

