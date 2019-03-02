package ds.client;

import ds.frontend.FrontEnd;
import ds.movies.MovieDetails;

import java.rmi.RemoteException;
import java.util.Scanner;

@FunctionalInterface
interface FEMutationRequest {
    boolean perform(RequestParameters parameters) throws RemoteException;
}

public class Client {

    private Scanner scanner = new Scanner(System.in);

    private FrontEnd frontEnd;

    private void assertNumberOfParameters(CommandLineInput command, int number) {
        if (command.getNumberOfArguments() < number) {
            throw new IllegalArgumentException("not enough arguments given");
        }
    }

    private void performQueryCommand(CommandLineInput command) throws RemoteException {
        assertNumberOfParameters(command, 1);

        RequestParameters parameters = command.getNumberOfArguments() >= 2 ?
                new RequestParameters(command.getUserId(), command.getMovieId()) :
                new RequestParameters(-1, command.getMovieId());

        MovieDetails movieDetails = frontEnd.query(parameters);

        System.out.println(movieDetails);
    }

    private void performMutationRequest(CommandLineInput command, FEMutationRequest mutationRequest) throws RemoteException{
        assertNumberOfParameters(command, 3);

        RequestParameters parameters = new RequestParameters(
                command.getUserId(), command.getMovieId(), command.getRating());

        boolean success = mutationRequest.perform(parameters);

        System.out.println(success ?
                "Operation completed successfully" :
                "Operation failed");
    }

    private void performUpdateCommand(CommandLineInput command) throws RemoteException {
        performMutationRequest(command, frontEnd::update);
    }

    private void performSubmitCommand(CommandLineInput command) throws RemoteException {
        performMutationRequest(command, frontEnd::submit);
    }

    private void showHelpPrompt() {
        System.out.println("-- Distributed Systems Client --");
        System.out.println("Available Commands:");
        System.out.println("query <movie-id> [<user-id>]");
        System.out.println("submit <movie-id> <user-id> <rating>");
        System.out.println("update <movie-id> <user-id> <rating>");
    }

    private String askForCommand() {
        System.out.print(">");
        return scanner.nextLine();
    }

    private void startLoopInteraction() {
        showHelpPrompt();

        while (true) {
            String input = askForCommand();

            try {
                CommandLineInput command = new CommandLineInput(input);

                if (input.startsWith("query ")) {
                    performQueryCommand(command);
                } else if (input.startsWith("update ")) {
                    performUpdateCommand(command);
                } else if (input.startsWith("submit ")) {
                    performSubmitCommand(command);
                } else if ("quit".equals(input)) {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Failed to parse the input!");
                showHelpPrompt();
            } catch (IllegalArgumentException e) {
                System.out.println("Error when parsing input:" + e.getMessage());
                showHelpPrompt();
            } catch (RemoteException e) {
                System.out.println("A remote error occurred during an operation");
            }
        }
    }

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
}

