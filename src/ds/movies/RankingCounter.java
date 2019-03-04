package ds.movies;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RankingCounter implements Serializable {
    private Map<Integer, Float> userRankings = new HashMap<>();

    public void setRanking(int userId, float ranking) {
        userRankings.put(userId, ranking);
    }

    public boolean updateRanking(int userId, float ranking) {
        if (!userRankings.containsKey(userId)) {
            return false;
        }

        userRankings.put(userId, ranking);

        return true;
    }

    float getRankingForUser(int userId) {
        return userRankings.containsKey(userId) ? userRankings.get(userId) : -1;
    }

    public int getNumberForRanking(float ranking) {
        return (int) userRankings.entrySet().stream()
                .filter(entry -> entry.getValue() == ranking)
                .count();
    }

    public Set<Float> getRankings() {
        return new HashSet<>(userRankings.values());
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        Set<Float> rankings = getRankings();

        output.append("[ ");
        for (float ranking : rankings) {
            output.append(String.format("%.1f => %d, ", ranking, getNumberForRanking(ranking)));
        }
        output.append(" ]");

        return output.toString();
    }

    public boolean userHasRanked(int userId) {
        return userRankings.containsKey(userId);
    }
}