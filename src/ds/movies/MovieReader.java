package ds.movies;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieReader {
    private CSVReader reader;

    public boolean hasNextMovie() {
        return reader.hasNextLine();
    }

    private String[] splitAttributeString(String attributeString) {
        return attributeString.split("|");
    }

    private final Pattern nameAndYearPattern = Pattern.compile("([^\\(]+) \\((\\d+)\\)");

    public Movie readMovie() {
        List<String> attributes = reader.readAttributes();

        int id = Integer.parseInt(attributes.get(0));
        String rawName = attributes.get(1);
        String[] genres = splitAttributeString(attributes.get(2));

        Matcher nameYearMatch = nameAndYearPattern.matcher(rawName);

        boolean fullyQualifiedName = nameYearMatch.find();
        if (fullyQualifiedName) { // id 40697 :(
            String name = nameYearMatch.group(1).trim();
            int year = Integer.parseInt(nameYearMatch.group(2));

            return new Movie(id, name, year, genres);
        } else {
            return new Movie(id, rawName, -1, genres);
        }
    }

    public MovieReader() {
        reader = new CSVReader("./movie-data/movies.csv");
    }
}
