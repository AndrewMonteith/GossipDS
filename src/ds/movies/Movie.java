package ds.movies;

import java.io.Serializable;

/**
 * Simple data class for movie objects.
 */
public class Movie implements Serializable {
    private int id;

    private String name;

    private int year;

    private String[] genres;

    public Movie(int id, String name, int year, String[] genres) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.genres = genres;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public String[] getGenres() {
        return genres;
    }

    @Override
    public String toString() {
        return String.format("[%s, %d]", getName(), getYear());
    }
}
