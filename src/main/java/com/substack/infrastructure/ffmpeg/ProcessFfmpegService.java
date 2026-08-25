package com.substack.infrastructure.ffmpeg;

import com.substack.domain.model.SubtitleTrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation executing system FFmpeg and FFprobe processes.
 */
public final class ProcessFfmpegService implements FfmpegService {

    private final String ffmpegBinary;
    private final String ffprobeBinary;

    public ProcessFfmpegService() {
        this("ffmpeg", "ffprobe");
    }

    public ProcessFfmpegService(String ffmpegBinary, String ffprobeBinary) {
        this.ffmpegBinary = Objects.requireNonNull(ffmpegBinary);
        this.ffprobeBinary = Objects.requireNonNull(ffprobeBinary);
    }

    @Override
    public List<SubtitleTrack> inspectSubtitleTracks(Path mediaFilePath) throws IOException, InterruptedException {
        List<SubtitleTrack> tracks = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder(
            ffprobeBinary,
            "-v", "error",
            "-select_streams", "s",
            "-show_entries", "stream=index,codec_name:stream_tags=language,title",
            "-of", "csv=p=0",
            mediaFilePath.toAbsolutePath().toString()
        );

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    int index = Integer.parseInt(parts[0].trim());
                    String codec = parts[1].trim();
                    String language = parts.length > 2 ? parts[2].trim() : "und";
                    String title = parts.length > 3 ? parts[3].trim() : "";
                    tracks.add(new SubtitleTrack(index, language, title, codec));
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("ffprobe failed with exit code " + exitCode + " for file: " + mediaFilePath);
        }

        return tracks;
    }

    @Override
    public Path extractTrackToSrt(Path mediaFilePath, int trackIndex) throws IOException, InterruptedException {
        Path tempSrt = Files.createTempFile("substack_extract_", ".srt");

        ProcessBuilder pb = new ProcessBuilder(
            ffmpegBinary, "-y",
            "-i", mediaFilePath.toAbsolutePath().toString(),
            "-map", "0:" + trackIndex,
            tempSrt.toAbsolutePath().toString()
        );

        executeProcess(pb, "FFmpeg track extraction failed");
        return tempSrt;
    }

    @Override
    public void embedAssInMkv(Path inputMediaFile, Path assFilePath, Path outputMkvFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegBinary, "-y",
            "-i", inputMediaFile.toAbsolutePath().toString(),
            "-i", assFilePath.toAbsolutePath().toString(),
            "-c", "copy",
            "-map", "0",
            "-map", "1",
            "-disposition:s:0", "default",
            outputMkvFile.toAbsolutePath().toString()
        );

        executeProcess(pb, "FFmpeg ASS embedding failed");
    }

    @Override
    public void remuxMp4ToMkv(Path inputMp4File, Path outputMkvFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegBinary, "-y",
            "-i", inputMp4File.toAbsolutePath().toString(),
            "-c", "copy",
            outputMkvFile.toAbsolutePath().toString()
        );

        executeProcess(pb, "FFmpeg MP4 to MKV remuxing failed");
    }

    private void executeProcess(ProcessBuilder pb, String errorMessage) throws IOException, InterruptedException {
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(errorMessage + " (exit code " + exitCode + ")");
        }
    }
}