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
 * Matches 1 primary subtitle entry to N overlapping secondary segments,
 * concatenating split phrases and ignoring minor temporal shifts.
 */
public final class SmartMultiSegmentMatcher implements SubtitleMatcher {

    private static final long TIME_TOLERANCE_MS = 300;

    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        List<MergedSubtitleEntry> results = new ArrayList<>();
        Set<Integer> usedSecondaryIndices = new HashSet<>();

        for (SubtitleEntry primary : primaryEntries) {
            long searchStart = Math.max(0, primary.getStartTimeMs() - TIME_TOLERANCE_MS);
            long searchEnd = primary.getEndTimeMs() + TIME_TOLERANCE_MS;

            List<Integer> matchedIndices = new ArrayList<>();

            for (int i = 0; i < secondaryEntries.size(); i++) {
                if (usedSecondaryIndices.contains(i)) {
                    continue;
                }

                SubtitleEntry secondary = secondaryEntries.get(i);
                
                // Check temporal overlap or presence inside tolerance window
                boolean overlaps = Math.min(primary.getEndTimeMs(), secondary.getEndTimeMs()) 
                                 > Math.max(primary.getStartTimeMs(), secondary.getStartTimeMs());
                boolean withinWindow = secondary.getStartTimeMs() >= searchStart && secondary.getEndTimeMs() <= searchEnd;

                if (overlaps || withinWindow) {
                    matchedIndices.add(i);
                }
            }

            if (!matchedIndices.isEmpty()) {
                usedSecondaryIndices.addAll(matchedIndices);

                String concatenatedSecondaryText = matchedIndices.stream()
                    .map(secondaryEntries::get)
                    .map(SubtitleEntry::getText)
                    .collect(Collectors.joining(" "));

                results.add(new MergedSubtitleEntry(
                    primary.getStartTimeMs(),
                    primary.getEndTimeMs(),
                    primary.getText(),
                    concatenatedSecondaryText
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

        // Add orphan secondary entries
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

        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}