package ds.client;

import ds.frontend.FrontEnd;
import ds.frontend.FrontEndApi;
import ds.movies.Movie;
import ds.movies.MovieDetails;
import ds.movies.RankingCounter;
import ds.replica.ReplicaStatus;

import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.function.Function;

import static ds.core.NetworkSimulator.NUMBER_OF_REPLICAS;

@FunctionalInterface
interface FEMutationRequest {
    boolean perform(RequestParameters parameters) throws RemoteException;
}

/**
 * Distributed System Client that connects to a front end.
 */
public class Client {

    private Scanner scanner = new Scanner(System.in);

    private FrontEndApi frontEnd;

    public Client() throws RemoteException {
        frontEnd = new FrontEnd();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();

            client.startLoopInteraction();
        } catch (RemoteException e) {
            System.out.println("A remote exception occurred ");
        }
    }

    private void assertNumberOfParameters(CommandLineInput command, int number) {
        if (command.getNumberOfArguments() < number) {
            throw new IllegalArgumentException("not enough arguments given");
        }
    }

    /**
     * Prints the response of a query request onto the screen
     * in a user friendly format.
     * @param movieDetails details returned from the front end
     * @param userId userId that performed the query request
     */
    private void printQueryResultsToScreen(MovieDetails movieDetails, int userId) {
        Movie movie = movieDetails.getMovie();
        RankingCounter rankings = movieDetails.getRankingCounter();

        System.out.println("Movie Name:" + movie.getName());
        System.out.println("Released:" + movie.getYear());

        if (movieDetails.getUserRanking() != -1) {
            System.out.printf("The user %d rated it %.2f\n", userId, movieDetails.getUserRanking());
        }

        System.out.println("Average Rating:" + rankings.getAverageRanking());
    }

    /**
     * Contacts the front end to do a query request.
     * @param command input from the cmdline client
     * @throws RemoteException
     */
    private void performQueryCommand(CommandLineInput command) throws RemoteException {
        assertNumberOfParameters(command, 1);

        RequestParameters parameters = command.getNumberOfArguments() >= 2 ?
                new RequestParameters(command.getParameter2(), command.getParameter1()) :
                new RequestParameters(-1, command.getParameter1());

        MovieDetails movieDetails = frontEnd.query(parameters);

        printQueryResultsToScreen(movieDetails, command.getParameter2());
    }

    private void performMutationRequest(CommandLineInput command, FEMutationRequest mutationRequest, Function<Boolean, String> getMessage) throws RemoteException {
        assertNumberOfParameters(command, 3);

        RequestParameters parameters = new RequestParameters(
                command.getParameter2(), command.getParameter1(), command.getParameter3());

        boolean success = mutationRequest.perform(parameters);

        System.out.println(getMessage.apply(success));
    }

    /**
     * Contacts the front end to do a update request.
     * @param command input from the cmdline client
     * @throws RemoteException
     */
    private void performUpdateCommand(CommandLineInput command) throws RemoteException {
        performMutationRequest(command, frontEnd::update,
                success -> success ? "Update operation was successful" : "Cannot update non-existent rating");
    }

    /**
     * Contacts the front end to do a submit request
     * @param command input from the cmdline client
     * @throws RemoteException
     */
    private void performSubmitCommand(CommandLineInput command) throws RemoteException {
        performMutationRequest(command, frontEnd::submit,
                success -> success ? "Submit operation was successful" : "Cannot submit an existent rating");
    }

    /**
     * Changes the status of a replica via the frontend
     * @param command input from the cmdline client
     * @throws RemoteException
     */
    private void performChangeStatusCommand(CommandLineInput command) throws RemoteException {
        int replicaId = command.getParameter1();

        if (replicaId < 0 || replicaId > NUMBER_OF_REPLICAS) {
            System.out.println("Replica Id must be 0, 1, or 2");
            return;
        }

        ReplicaStatus status = ReplicaStatus.fromInteger(command.getParameter2());

        frontEnd.changeReplicaStatus(replicaId, status);
    }

    private void showHelpPrompt() {
        System.out.println("-- Distributed Systems Client --");
        System.out.println("Available Commands:");
        System.out.println("query <movie-id> [<user-id>]");
        System.out.println("submit <movie-id> <user-id> <rating>");
        System.out.println("update <movie-id> <user-id> <rating>");
        System.out.println("status <replica-id> <status-id> (0=Online, 1=Overloaded, 2=Offline)");
        System.out.println("quit - to exit");
    }

    private String askForCommand() {
        System.out.print("> ");
        return scanner.nextLine().trim();
    }

    private void startLoopInteraction() {
        showHelpPrompt();

        while (true) {
            String input = askForCommand();

            try {
                if ("quit".equals(input)) {
                    return;
                }

                CommandLineInput command = new CommandLineInput(input);

                if (input.startsWith("query ")) {
                    performQueryCommand(command);
                } else if (input.startsWith("update ")) {
                    performUpdateCommand(command);
                } else if (input.startsWith("submit ")) {
                    performSubmitCommand(command);
                } else if (input.startsWith("status")) {
                    performChangeStatusCommand(command);
                } else {
                    System.out.println("Unknown command");
                    showHelpPrompt();
                }
            } catch (NumberFormatException e) {
                System.out.println("Failed to parse the input!");
                showHelpPrompt();
            } catch (IllegalArgumentException e) {
                System.out.println("Error when parsing input:" + e.getMessage());
                showHelpPrompt();
            } catch (RemoteException e) {
                System.out.println("A remote error occurred during an operation");
                e.printStackTrace();
            }
        }
    }
}

