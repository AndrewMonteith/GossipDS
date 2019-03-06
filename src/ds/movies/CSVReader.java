package ds.movies;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CSVReader {
    private Scanner scanner;

    public CSVReader(String fileName) {
        try {
            scanner = new Scanner(new File(fileName));
            scanner.nextLine(); // Skip title line.
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("initalisation error, couldn't read data " + fileName);
        }
    }

    private List<String> splitLineIntoAttributes(String line) {
        // There may be a CSV in an attribute, escaped using quotes.
        // So just splitting by "," will not work.

        List<String> attributes = new ArrayList<>();
        String[] rawSplit = line.split(",");

        for (int i = 0; i < rawSplit.length; ++i) {
            String attribute = rawSplit[i];

            if (attribute.startsWith("\"") && !attribute.endsWith("\"")) {
                i += 1;
                while (!attribute.endsWith("\"")) {
                    attribute += rawSplit[i];
                    if (!attribute.endsWith("\"")) {
                        ++i;
                    }
                }

                attribute = attribute.substring(1, attribute.length() - 1);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    public List<String> readAttributes() {
        if (!scanner.hasNextLine()) {
            return null;
        }

        return splitLineIntoAttributes(scanner.nextLine());
    }

    public boolean hasNextLine() {
        return scanner.hasNextLine();
    }
}
