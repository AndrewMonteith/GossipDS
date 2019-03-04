package ds.client;

public class CommandLineInput {

    private int movieId;
    private int userId;
    private float rating = -1;

    private int numberOfArguments;

    public int getNumberOfArguments() {
        return numberOfArguments;
    }

    public int getUserId() {
        return userId;
    }

    public int getMovieId() {
        return movieId;
    }

    public float getRating() {
        return rating;
    }


    CommandLineInput(String command) throws NumberFormatException {
        String[] splitCommand = command.split(" ");

        if (splitCommand.length < 2) {
            throw new IllegalArgumentException("command wasn't long enough");
        }

        numberOfArguments = Math.min(splitCommand.length-1, 3);

        movieId = Integer.parseInt(splitCommand[1]);

        if (splitCommand.length > 2) {
            userId = Integer.parseInt(splitCommand[2]);
        }

        if (splitCommand.length > 3) {
            rating = Float.parseFloat(splitCommand[3]);
        }
    }
}
