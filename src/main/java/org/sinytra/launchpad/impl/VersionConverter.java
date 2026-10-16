/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class VersionConverter {
    public static final String ANY = "*";
    private static final String BUILD_METADATA_CEILING = "+~";
    private static final String LOWEST_PRERELEASE = "alpha";

    public static String convert(Collection<? extends VersionPredicate> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return ANY;
        }
        List<VersionInterval> intervals = new ArrayList<>(predicates.size());
        for (VersionPredicate predicate : predicates) {
            intervals = VersionInterval.or(intervals, predicate.getInterval());
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

        String min = minV == null ? null : toMavenVersion(minV);
        String max = maxV == null ? null : toMavenVersion(maxV);

        if (min != null && min.equals(max) && !minInclusive && !maxInclusive) {
            return null;
        }

        // Work around NeoForge comparing with metadata in mind while Fabric does not
        if (max != null && maxInclusive && maxV instanceof SemanticVersion) {
            max += BUILD_METADATA_CEILING;
            maxInclusive = false;
        }
        if (min != null && !minInclusive && minV instanceof SemanticVersion) {
            min += BUILD_METADATA_CEILING;
            minInclusive = true;
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

    private static String toMavenVersion(Version version) {
        String str = version.getFriendlyString();
        if (version instanceof SemanticVersion) {
            int buildIdx = str.indexOf('+');
            if (buildIdx >= 0) {
                str = str.substring(0, buildIdx);
            }
            if (str.endsWith("-")) {
                str += LOWEST_PRERELEASE;
            }
        }
        return str;
    }

    private VersionConverter() {
    }
}