package com.substack.application.usecase;

import com.substack.application.dto.OutputOption;
import com.substack.application.dto.ProcessingProgress;
import com.substack.domain.model.MediaFile;
import com.substack.domain.model.MergedSubtitleEntry;
import com.substack.domain.model.PositionMode;
import com.substack.domain.model.SubtitleEntry;
import com.substack.domain.model.SubtitleStyle;
import com.substack.domain.service.SubtitleMatcher;
import com.substack.infrastructure.ffmpeg.FfmpegService;
import com.substack.infrastructure.formatter.AssFormatter;
import com.substack.infrastructure.parser.SrtParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Main orchestrator use case executing the complete bilingual subtitle processing pipeline.
 */
public final class ProcessBilingualSubtitlesUseCase {

    private final FfmpegService ffmpegService;
    private final SrtParser srtParser;
    private final SubtitleMatcher subtitleMatcher;
    private final AssFormatter assFormatter;

    public ProcessBilingualSubtitlesUseCase(FfmpegService ffmpegService,
                                           SrtParser srtParser,
                                           SubtitleMatcher subtitleMatcher,
                                           AssFormatter assFormatter) {
        this.ffmpegService = Objects.requireNonNull(ffmpegService);
        this.srtParser = Objects.requireNonNull(srtParser);
        this.subtitleMatcher = Objects.requireNonNull(subtitleMatcher);
        this.assFormatter = Objects.requireNonNull(assFormatter);
    }

    /**
     * Processes a list of media files with the user-defined styles and output preferences.
     */
    public void execute(List<MediaFile> files,
                        int primaryTrackIndex,
                        int secondaryTrackIndex,
                        SubtitleStyle primaryStyle,
                        SubtitleStyle secondaryStyle,
                        PositionMode positionMode,
                        OutputOption outputOption,
                        Consumer<ProcessingProgress> progressCallback) throws IOException, InterruptedException {

        Objects.requireNonNull(files, "Files list cannot be null");
        int totalFiles = files.size();

        for (int i = 0; i < totalFiles; i++) {
            MediaFile mediaFile = files.get(i);
            double baseRatio = (double) i / totalFiles;
            reportProgress(progressCallback, mediaFile.getFileName(), "Extracting subtitle tracks...", baseRatio);

            Path tempPrimarySrt = null;
            Path tempSecondarySrt = null;

            try {
                // 1. Extract tracks to temp SRT files
                tempPrimarySrt = ffmpegService.extractTrackToSrt(mediaFile.getFilePath(), primaryTrackIndex);
                tempSecondarySrt = ffmpegService.extractTrackToSrt(mediaFile.getFilePath(), secondaryTrackIndex);

                reportProgress(progressCallback, mediaFile.getFileName(), "Parsing subtitles...", baseRatio + (0.2 / totalFiles));

                // 2. Parse SRT contents
                List<SubtitleEntry> primaryEntries = srtParser.parse(Files.readString(tempPrimarySrt));
                List<SubtitleEntry> secondaryEntries = srtParser.parse(Files.readString(tempSecondarySrt));

                reportProgress(progressCallback, mediaFile.getFileName(), "Matching subtitle timelines...", baseRatio + (0.4 / totalFiles));

                // 3. Match subtitle entries using V0 overlap algorithm
                List<MergedSubtitleEntry> mergedEntries = subtitleMatcher.match(primaryEntries, secondaryEntries);

                reportProgress(progressCallback, mediaFile.getFileName(), "Formatting ASS content...", baseRatio + (0.6 / totalFiles));

                // 4. Generate ASS text content
                String assContent = assFormatter.format(mergedEntries, primaryStyle, secondaryStyle, positionMode);

                // 5. Output management
                Path assOutputPath = getAssOutputPath(mediaFile.getFilePath());
                Files.writeString(assOutputPath, assContent);

                if (outputOption == OutputOption.EMBED_IN_MKV) {
                    reportProgress(progressCallback, mediaFile.getFileName(), "Remuxing into MKV container...", baseRatio + (0.8 / totalFiles));
                    Path finalMkvPath = getFinalMkvPath(mediaFile.getFilePath());

                    ffmpegService.embedAssInMkv(mediaFile.getFilePath(), assOutputPath, finalMkvPath);

                    // Delete temp ASS if embedded successfully
                    Files.deleteIfExists(assOutputPath);
                }

                reportProgress(progressCallback, mediaFile.getFileName(), "Done!", (double) (i + 1) / totalFiles);

            } finally {
                // Clean temp extraction files
                if (tempPrimarySrt != null) Files.deleteIfExists(tempPrimarySrt);
                if (tempSecondarySrt != null) Files.deleteIfExists(tempSecondarySrt);
            }
        }
    }

    private Path getAssOutputPath(Path mediaFilePath) {
        String fileName = mediaFilePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        String baseName = (lastDotIndex > 0) ? fileName.substring(0, lastDotIndex) : fileName;
        return mediaFilePath.getParent().resolve(baseName + ".ass");
    }

    private Path getFinalMkvPath(Path mediaFilePath) {
        String fileName = mediaFilePath.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".mkv")) {
            // Overwrite/remux into temporary mkv then rename or append suffix
            int lastDotIndex = fileName.lastIndexOf('.');
            String baseName = fileName.substring(0, lastDotIndex);
            return mediaFilePath.getParent().resolve(baseName + ".bilingual.mkv");
        } else {
            // MP4 remuxed to MKV
            int lastDotIndex = fileName.lastIndexOf('.');
            String baseName = fileName.substring(0, lastDotIndex);
            return mediaFilePath.getParent().resolve(baseName + ".mkv");
        }
    }

    private void reportProgress(Consumer<ProcessingProgress> callback, String fileName, String message, double ratio) {
        if (callback != null) {
            callback.accept(new ProcessingProgress(fileName, message, ratio));
        }
    }
}