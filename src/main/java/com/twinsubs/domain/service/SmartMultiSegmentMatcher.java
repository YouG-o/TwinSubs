package com.twinsubs.domain.service;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.SubtitleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Smart Multi-Segment matcher (BETA).
 * Assigns each secondary entry to the primary entry with maximum temporal overlap,
 * concatenating split secondary phrases smoothly.
 */
public final class SmartMultiSegmentMatcher implements SubtitleMatcher {

    private static final long MIN_OVERLAP_THRESHOLD_MS = 100;

    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        // Map primary entry index -> list of assigned secondary entries
        Map<Integer, List<SubtitleEntry>> primaryToSecondaryMap = new HashMap<>();
        List<SubtitleEntry> orphanSecondaryEntries = new ArrayList<>();

        // 1. Assign each secondary entry to the primary entry with maximum overlap
        for (SubtitleEntry secondary : secondaryEntries) {
            int bestPrimaryIndex = -1;
            long maxOverlapMs = MIN_OVERLAP_THRESHOLD_MS;

            for (int pIdx = 0; pIdx < primaryEntries.size(); pIdx++) {
                SubtitleEntry primary = primaryEntries.get(pIdx);
                long overlapMs = primary.calculateOverlapMs(secondary);

                if (overlapMs > maxOverlapMs) {
                    maxOverlapMs = overlapMs;
                    bestPrimaryIndex = pIdx;
                }
            }

            if (bestPrimaryIndex != -1) {
                primaryToSecondaryMap
                    .computeIfAbsent(bestPrimaryIndex, k -> new ArrayList<>())
                    .add(secondary);
            } else {
                orphanSecondaryEntries.add(secondary);
            }
        }

        List<MergedSubtitleEntry> results = new ArrayList<>();

        // 2. Build merged entries for all primary entries
        for (int pIdx = 0; pIdx < primaryEntries.size(); pIdx++) {
            SubtitleEntry primary = primaryEntries.get(pIdx);
            List<SubtitleEntry> assignedSecondaryList = primaryToSecondaryMap.get(pIdx);

            if (assignedSecondaryList != null && !assignedSecondaryList.isEmpty()) {
                // Sort assigned secondary entries chronologically
                assignedSecondaryList.sort(Comparator.comparingLong(SubtitleEntry::getStartTimeMs));

                String concatenatedText = assignedSecondaryList.stream()
                    .map(s -> s.getText().replace("\n", " ").replaceAll("\\s+", " ").trim())
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

        // 3. Add orphan secondary entries
        for (SubtitleEntry orphan : orphanSecondaryEntries) {
            results.add(new MergedSubtitleEntry(
                orphan.getStartTimeMs(),
                orphan.getEndTimeMs(),
                null,
                orphan.getText()
            ));
        }

        // 4. Sort final results chronologically
        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}