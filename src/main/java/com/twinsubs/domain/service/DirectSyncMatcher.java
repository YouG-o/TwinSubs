package com.twinsubs.domain.service;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.SubtitleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Direct Sync Matcher (Default).
 * Preserves independent native timestamps for both primary and secondary subtitle tracks
 * without artificial alignment or temporal warping.
 */
public final class DirectSyncMatcher implements SubtitleMatcher {

    @Override
    public List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries) {
        Objects.requireNonNull(primaryEntries, "Primary entries cannot be null");
        Objects.requireNonNull(secondaryEntries, "Secondary entries cannot be null");

        List<MergedSubtitleEntry> results = new ArrayList<>();

        for (SubtitleEntry primary : primaryEntries) {
            results.add(new MergedSubtitleEntry(
                primary.getStartTimeMs(),
                primary.getEndTimeMs(),
                primary.getText(),
                null
            ));
        }

        for (SubtitleEntry secondary : secondaryEntries) {
            results.add(new MergedSubtitleEntry(
                secondary.getStartTimeMs(),
                secondary.getEndTimeMs(),
                null,
                secondary.getText()
            ));
        }

        results.sort(Comparator
            .comparingLong(MergedSubtitleEntry::getStartTimeMs)
            .thenComparingLong(MergedSubtitleEntry::getEndTimeMs)
        );

        return results;
    }
}
