package com.twinsubs.domain.service;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.SubtitleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Smart Multi-Segment matcher (BETA).
 * Matches primary and secondary entries using significant overlap ratios,
 * handling 1-to-N and N-to-1 subtitle mappings cleanly without duplicate propagation.
 */
public final class SmartMultiSegmentMatcher implements SubtitleMatcher {

    private static final long MIN_OVERLAP_MS = 200; // Minimum required overlap in milliseconds
    private static final double MIN_OVERLAP_RATIO = 0.20; // Or at least 20% of the primary subtitle duration
    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        List<MergedSubtitleEntry> results = new ArrayList<>();
        Set<Integer> matchedSecondaryIndices = new HashSet<>();

        // 1. For each primary entry, find all secondary entries with significant overlap
        for (SubtitleEntry primary : primaryEntries) {
            long primaryDuration = Math.max(1, primary.getEndTimeMs() - primary.getStartTimeMs());
            List<SubtitleEntry> matchingSecondaryList = new ArrayList<>();
            for (int sIdx = 0; sIdx < secondaryEntries.size(); sIdx++) {
                SubtitleEntry secondary = secondaryEntries.get(sIdx);
                long overlapMs = primary.calculateOverlapMs(secondary);

                double overlapRatio = (double) overlapMs / primaryDuration;

                // Include secondary if overlap is significant
                if (overlapMs >= MIN_OVERLAP_MS || overlapRatio >= MIN_OVERLAP_RATIO) {
                    matchingSecondaryList.add(secondary);
                    matchedSecondaryIndices.add(sIdx);
                }
            }

            if (!matchingSecondaryList.isEmpty()) {
                // Sort matching secondary entries chronologically
                matchingSecondaryList.sort(Comparator.comparingLong(SubtitleEntry::getStartTimeMs));

                String concatenatedText = matchingSecondaryList.stream()
                    .map(s -> s.getText().replace("\n", " ").replaceAll("\\s+", " ").trim())
                    .distinct()
                    .collect(Collectors.joining(" "));

                results.add(new MergedSubtitleEntry(
                    primary.getStartTimeMs(),
                    primary.getEndTimeMs(),
                    primary.getText(),
                    concatenatedText
                ));
            } else {
                results.add(new MergedSubtitleEntry(
                    primary.getStartTimeMs(),
                    primary.getEndTimeMs(),
                    primary.getText(),
                    null
                ));
            }
        }

        // 2. Add orphan secondary entries that had no overlap with any primary entry
        for (int sIdx = 0; sIdx < secondaryEntries.size(); sIdx++) {
            if (!matchedSecondaryIndices.contains(sIdx)) {
                SubtitleEntry orphan = secondaryEntries.get(sIdx);
            results.add(new MergedSubtitleEntry(
                orphan.getStartTimeMs(),
                orphan.getEndTimeMs(),
                null,
                orphan.getText()
            ));
        }
        }

        // 3. Final chronological sort
        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}