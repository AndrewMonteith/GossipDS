package ds.client;

import java.io.Serializable;

/**
 * Metadata associated with each request made to a network.
 */
public class RequestParameters implements Serializable {
    private int movieId;

    private int userId;

    private float ranking;

    public RequestParameters(int userId, int movieId) {
        this.userId = userId;
        this.movieId = movieId;
    }

    public RequestParameters(int userId, int movieId, float ranking) {
        this(userId, movieId);
        this.ranking = ranking;
    }

    public int getMovieId() {
        return movieId;
    }

    public int getUserId() {
        return userId;
    }

    public float getRanking() {
        return ranking;
    }
}
