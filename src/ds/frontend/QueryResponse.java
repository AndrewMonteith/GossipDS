package ds.frontend;

import ds.core.Timestamp;
import ds.movies.MovieDetails;

import java.io.Serializable;

public class QueryResponse implements Serializable {
    private Timestamp timestamp;
    private MovieDetails rankings;

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public MovieDetails getMovieDetails() {
        return rankings;
    }

    public QueryResponse(Timestamp timestamp, MovieDetails movieDetails) {
        this.timestamp = timestamp;
        this.rankings = movieDetails;
    }
}
