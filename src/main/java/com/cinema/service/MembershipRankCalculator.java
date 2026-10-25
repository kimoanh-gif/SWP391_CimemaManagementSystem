package com.cinema.service;

import com.cinema.model.MembershipRank;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * BR-32: a Customer's membership rank is determined by TierPoints and the
 * configured MembershipRank thresholds (MinPoints). Pure logic, no database access.
 */
@Component
public class MembershipRankCalculator {

    /** Highest active rank whose MinPoints &lt;= tierPoints. */
    public Optional<MembershipRank> currentRank(List<MembershipRank> ranks, int tierPoints) {
        return sorted(ranks).stream()
                .filter(r -> minPoints(r) <= tierPoints)
                .reduce((lower, higher) -> higher);
    }

    /** Lowest active rank whose MinPoints &gt; tierPoints (empty when already at the top rank). */
    public Optional<MembershipRank> nextRank(List<MembershipRank> ranks, int tierPoints) {
        return sorted(ranks).stream()
                .filter(r -> minPoints(r) > tierPoints)
                .findFirst();
    }

    /** Progress 0..100 from the current rank threshold to the next rank threshold. */
    public int progressPercent(MembershipRank current, MembershipRank next, int tierPoints) {
        if (next == null) return 100;
        int from = current != null ? minPoints(current) : 0;
        int to = minPoints(next);
        if (to <= from) return 100;
        long percent = Math.round((tierPoints - from) * 100.0 / (to - from));
        return (int) Math.max(0, Math.min(100, percent));
    }

    private static List<MembershipRank> sorted(List<MembershipRank> ranks) {
        if (ranks == null) return List.of();
        return ranks.stream()
                .filter(r -> r.getIsActive() == null || r.getIsActive())
                .sorted(Comparator.comparingInt(MembershipRankCalculator::minPoints))
                .toList();
    }

    private static int minPoints(MembershipRank rank) {
        return rank.getMinPoints() != null ? rank.getMinPoints() : 0;
    }
}
