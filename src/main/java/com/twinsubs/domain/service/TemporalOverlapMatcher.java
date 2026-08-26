package com.twinsubs.domain.service;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.SubtitleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * V0 implementation of SubtitleMatcher based on temporal overlap.
 */
public final class TemporalOverlapMatcher implements SubtitleMatcher {

    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        List<MergedSubtitleEntry> results = new ArrayList<>();
        Set<Integer> usedSecondaryIndices = new HashSet<>();

        // 1. Match primary entries with best overlapping secondary entry
        for (SubtitleEntry primary : primaryEntries) {
            int bestMatchIndex = -1;
            long maxOverlapMs = 0;

            for (int i = 0; i < secondaryEntries.size(); i++) {
                if (usedSecondaryIndices.contains(i)) {
                    continue;
                }

                SubtitleEntry secondary = secondaryEntries.get(i);
                long overlapMs = primary.calculateOverlapMs(secondary);

                if (overlapMs > maxOverlapMs) {
                    maxOverlapMs = overlapMs;
                    bestMatchIndex = i;
                }
            }

            if (bestMatchIndex != -1) {
                usedSecondaryIndices.add(bestMatchIndex);
                SubtitleEntry matchedSecondary = secondaryEntries.get(bestMatchIndex);

                results.add(new MergedSubtitleEntry(
                    primary.getStartTimeMs(),
                    primary.getEndTimeMs(),
                    primary.getText(),
                    matchedSecondary.getText()
                ));
            } else {
                // No overlapping secondary found for this primary entry
                results.add(new MergedSubtitleEntry(
                    primary.getStartTimeMs(),
                    primary.getEndTimeMs(),
                    primary.getText(),
                    null
                ));
            }
        }

        // 2. Add orphan secondary entries that were not matched
        for (int i = 0; i < secondaryEntries.size(); i++) {
            if (!usedSecondaryIndices.contains(i)) {
                SubtitleEntry orphan = secondaryEntries.get(i);
                results.add(new MergedSubtitleEntry(
                    orphan.getStartTimeMs(),
                    orphan.getEndTimeMs(),
                    null,
                    orphan.getText()
                ));
            }
        }

        // 3. Sort chronologically by start time, then by end time
        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}