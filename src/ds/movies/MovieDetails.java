package ds.movies;

import java.io.Serializable;

public class MovieDetails implements Serializable {
    private Movie movie;

    private RankingCounter rankingCounter;

    private float userRanking;

    public MovieDetails(Movie movie, RankingCounter rankingCounter) {
        this.movie = movie;
        this.rankingCounter = rankingCounter;
        this.userRanking = -1;
    }

    public MovieDetails(Movie movie, RankingCounter rankingCounter, int userId) {
        this(movie, rankingCounter);
        userRanking = rankingCounter.getRankingForUser(userId);
    }

    public Movie getMovie() {
        return movie;
    }

    public RankingCounter getRankingCounter() {
        return rankingCounter;
    }

    public float getUserRanking() {
        return userRanking;
    }

    @Override
    public String toString() {
        return String.format("Movie: %s\n Ranking: %s\n Specific User Ranking: %f",
                movie, rankingCounter, userRanking);
    }
}
