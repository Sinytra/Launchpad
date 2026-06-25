package org.sinytra.launchpad.impl;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class VersionConverter {
    public static final String ANY = "[0,)";

    public static String convert(Collection<? extends VersionPredicate> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return ANY;
        }
        List<VersionInterval> intervals = new ArrayList<>(predicates.size());
        for (VersionPredicate predicate : predicates) {
            intervals.add(predicate.getInterval());
        }
        return convertIntervals(intervals);
    }

    private static String convertIntervals(Collection<? extends VersionInterval> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return ANY;
        }
        List<String> fragments = new ArrayList<>(intervals.size());
        for (VersionInterval interval : intervals) {
            String maven = toMaven(interval);
            if (maven != null) {
                fragments.add(maven);
            }
        }
        if (fragments.isEmpty()) {
            return ANY;
        }
        return String.join(",", fragments);
    }

    static String toMaven(VersionInterval interval) {
        if (interval == null) {
            return null;
        }

        Version minV = interval.getMin();
        Version maxV = interval.getMax();
        boolean minInclusive = interval.isMinInclusive();
        boolean maxInclusive = interval.isMaxInclusive();

        if (minV == null && maxV == null) {
            return ANY;
        }

        String min = minV == null ? null : minV.getFriendlyString();
        String max = maxV == null ? null : maxV.getFriendlyString();

        if (min != null && min.equals(max) && !minInclusive && !maxInclusive) {
            return null;
        }

        if (min != null && min.equals(max) && minInclusive && maxInclusive) {
            return "[" + min + "]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(minInclusive ? '[' : '(');
        if (min != null) {
            sb.append(min);
        }
        sb.append(',');
        if (max != null) {
            sb.append(max);
        }
        sb.append(maxInclusive ? ']' : ')');
        return sb.toString();
    }

    private VersionConverter() {
    }
}