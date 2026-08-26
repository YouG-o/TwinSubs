package com.twinsubs.infrastructure.ffmpeg;

import com.twinsubs.domain.model.SubtitleTrack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface isolating media processing operations (probe streams, extract tracks, remux).
 */
public interface FfmpegService {

    /**
     * Inspects a media file and retrieves all available subtitle streams.
     */
    List<SubtitleTrack> inspectSubtitleTracks(Path mediaFilePath) throws IOException, InterruptedException;

    /**
     * Extracts a specific subtitle stream from a media file to a temporary SRT file.
     */
    Path extractTrackToSrt(Path mediaFilePath, int trackIndex) throws IOException, InterruptedException;

    /**
     * Embeds a generated ASS subtitle file into a MKV container via remuxing (no video/audio re-encoding).
     */
    void embedAssInMkv(Path inputMediaFile, Path assFilePath, Path outputMkvFile) throws IOException, InterruptedException;

    /**
     * Remuxes an MP4 video into a MKV container without re-encoding.
     */
    void remuxMp4ToMkv(Path inputMp4File, Path outputMkvFile) throws IOException, InterruptedException;
}