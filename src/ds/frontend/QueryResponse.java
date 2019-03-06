package ds.frontend;

import ds.core.Timestamp;
import ds.movies.MovieDetails;

import java.io.Serializable;

/**
 * Response given to the frontend for a query request.
 */
public class QueryResponse implements Serializable {
    /**
     * New frontend timestamp
     */
    private Timestamp timestamp;

    /**
     * Data for the query response
     */
    private MovieDetails rankings;

    public QueryResponse(Timestamp timestamp, MovieDetails movieDetails) {
        this.timestamp = timestamp;
        this.rankings = movieDetails;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    MovieDetails getMovieDetails() {
        return rankings;
    }
}
