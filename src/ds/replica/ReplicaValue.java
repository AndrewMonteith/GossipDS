package ds.replica;

import ds.client.RequestParameters;
import ds.movies.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplicaValue {
    private Map<Integer, Movie> movies;
    private Map<Integer, RankingCounter> rankings;

    public MovieDetails getDetailsForMovie(RequestParameters parameters) {
        int userId = parameters.getUserId();

        Movie movie = movies.get(parameters.getMovieId());
        RankingCounter ranking = rankings.get(parameters.getMovieId());

        return userId != -1 ? new MovieDetails(movie, ranking, userId) : new MovieDetails(movie, ranking);
    }

    private void setRanking(int userId, int movieId, float ranking) {
        rankings.get(movieId)
                .setRanking(userId, ranking);
    }

    public void setRanking(RequestParameters parameters) {
        setRanking(parameters.getUserId(), parameters.getMovieId(), parameters.getRanking());
    }


    public boolean updateRanking(RequestParameters parameters) {
        return rankings.get(parameters.getMovieId())
                .updateRanking(parameters.getUserId(), parameters.getRanking());
    }

    public boolean hasUserRankedMovie(int userId, int movieId) {
        return movies.containsKey(movieId)
                && rankings.get(movieId).userHasRanked(userId);
    }

    private void loadMoviesIntoMemory() {
        MovieReader movieReader = new MovieReader();

        while (movieReader.hasNextMovie()) {
            Movie movie = movieReader.readMovie();

            movies.put(movie.getId(), movie);
            rankings.put(movie.getId(), new RankingCounter());
        }
    }

    private void loadMovieRankings() {
        CSVReader reader = new CSVReader("./movie-data/ratings.csv");

        while (reader.hasNextLine()) {
            List<String> attributes = reader.readAttributes();

            int userId = Integer.parseInt(attributes.get(0));
            int movieId = Integer.parseInt(attributes.get(1));
            float ranking = Float.parseFloat(attributes.get(2)); // format string is "4.0"

            setRanking(userId, movieId, ranking);
        }
    }

    public ReplicaValue() {
        movies = new HashMap<>();
        rankings = new HashMap<>();

        loadMoviesIntoMemory();
        loadMovieRankings();
    }

}
