package com.substack.domain.service;

import com.substack.domain.model.MergedSubtitleEntry;
import com.substack.domain.model.SubtitleEntry;

import java.util.List;

/**
 * Contract for matching primary and secondary subtitle track entries.
 */
public interface SubtitleMatcher {

    /**
     * Matches primary and secondary subtitle entries into a list of merged entries.
     *
     * @param primaryEntries   Reference timeline subtitle entries.
     * @param secondaryEntries Secondary subtitle entries to align with primary timeline.
     * @return Ordered list of merged entries.
     */
    List<MergedSubtitleEntry> match(List<SubtitleEntry> primaryEntries, List<SubtitleEntry> secondaryEntries);
}