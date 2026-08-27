package com.twinsubs.domain.service;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.SubtitleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Smart Multi-Segment matcher (BETA).
 * Matches primary and secondary entries using significant overlap ratios,
 * handling 1-to-N and N-to-1 subtitle mappings cleanly without duplicate propagation.
 */
public final class SmartMultiSegmentMatcher implements SubtitleMatcher {

    private static final long MIN_OVERLAP_MS = 50;
    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        Map<Integer, List<SubtitleEntry>> primaryToSecondaryMap = new HashMap<>();
        Set<Integer> usedSecondaryIndices = new HashSet<>();

        // 1. First pass: Assign each secondary entry exclusively to its BEST matching primary entry (Winner takes all)
            for (int sIdx = 0; sIdx < secondaryEntries.size(); sIdx++) {
                SubtitleEntry secondary = secondaryEntries.get(sIdx);
            int bestPrimaryIndex = -1;
            long maxOverlapMs = MIN_OVERLAP_MS;

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
                usedSecondaryIndices.add(sIdx);
                }
            }

        // 2. Second pass: Group contiguous/split secondary phrases assigned to the same primary entry
        List<MergedSubtitleEntry> results = new ArrayList<>();

        for (int pIdx = 0; pIdx < primaryEntries.size(); pIdx++) {
            SubtitleEntry primary = primaryEntries.get(pIdx);
            List<SubtitleEntry> assignedSecondaryList = primaryToSecondaryMap.get(pIdx);
            if (assignedSecondaryList != null && !assignedSecondaryList.isEmpty()) {
                assignedSecondaryList.sort(Comparator.comparingLong(SubtitleEntry::getStartTimeMs));

                String concatenatedText = assignedSecondaryList.stream()
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

        // 3. Add truly unassigned orphan secondary entries
        for (int sIdx = 0; sIdx < secondaryEntries.size(); sIdx++) {
            if (!usedSecondaryIndices.contains(sIdx)) {
                SubtitleEntry orphan = secondaryEntries.get(sIdx);
            results.add(new MergedSubtitleEntry(
                orphan.getStartTimeMs(),
                orphan.getEndTimeMs(),
                null,
                orphan.getText()
            ));
        }
        }

        // 4. Final chronological sort
        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}