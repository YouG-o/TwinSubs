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
 * Enhanced Smart Multi-Segment matcher (BETA).
 * Handles 1-to-N, N-to-1, and spread subtitle mapping with gap filling and best-fit overlap.
 */
public final class SmartMultiSegmentMatcher implements SubtitleMatcher {

    private static final long MIN_OVERLAP_THRESHOLD_MS = 50;
    private static final long MAX_GAP_PROPAGATION_MS = 600; // Tolerance to bridge small timing gaps between dialogue turns

    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        Map<Integer, List<SubtitleEntry>> primaryToSecondaryMap = new HashMap<>();
        List<SubtitleEntry> orphanSecondaryEntries = new ArrayList<>();

        // 1. Best-fit assignment of each secondary entry to primary entries based on max overlap
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

        // 2. Propagation pass: if a primary entry is empty, check if adjacent primary entries share the same secondary block or close timeframe
        for (int pIdx = 0; pIdx < primaryEntries.size(); pIdx++) {
            List<SubtitleEntry> assigned = primaryToSecondaryMap.get(pIdx);
            if ((assigned == null || assigned.isEmpty()) && pIdx > 0) {
                SubtitleEntry currentPrimary = primaryEntries.get(pIdx);
                SubtitleEntry prevPrimary = primaryEntries.get(pIdx - 1);
                
                // If current primary starts shortly after previous primary ends, and previous has secondary text
                if (currentPrimary.getStartTimeMs() - prevPrimary.getEndTimeMs() <= MAX_GAP_PROPAGATION_MS) {
                    List<SubtitleEntry> prevAssigned = primaryToSecondaryMap.get(pIdx - 1);
                    if (prevAssigned != null && !prevAssigned.isEmpty()) {
                        // Share the last secondary entry if it bridges the gap
                        SubtitleEntry lastPrevSec = prevAssigned.get(prevAssigned.size() - 1);
                        if (currentPrimary.calculateOverlapMs(lastPrevSec) > 0 || 
                            Math.abs(currentPrimary.getStartTimeMs() - lastPrevSec.getStartTimeMs()) < 1500) {
                            primaryToSecondaryMap
                                .computeIfAbsent(pIdx, k -> new ArrayList<>())
                                .add(lastPrevSec);
                        }
                    }
                }
            }
        }

        List<MergedSubtitleEntry> results = new ArrayList<>();

        // 3. Construct merged entries
        for (int pIdx = 0; pIdx < primaryEntries.size(); pIdx++) {
            SubtitleEntry primary = primaryEntries.get(pIdx);
            List<SubtitleEntry> assignedSecondaryList = primaryToSecondaryMap.get(pIdx);

            if (assignedSecondaryList != null && !assignedSecondaryList.isEmpty()) {
                // Deduplicate and sort chronologically
                List<SubtitleEntry> uniqueAssigned = assignedSecondaryList.stream()
                    .distinct()
                    .sorted(Comparator.comparingLong(SubtitleEntry::getStartTimeMs))
                    .collect(Collectors.toList());

                String concatenatedText = uniqueAssigned.stream()
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

        // 4. Add orphan secondary entries
        for (SubtitleEntry orphan : orphanSecondaryEntries) {
            results.add(new MergedSubtitleEntry(
                orphan.getStartTimeMs(),
                orphan.getEndTimeMs(),
                null,
                orphan.getText()
            ));
        }

        // 5. Chronological sort
        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}