package ds.client;

class CommandLineInput {

    private int parameter1;
    private int parameter2;
    private float parameter3 = -1;

    private int numberOfArguments;

    CommandLineInput(String command) throws NumberFormatException {
        String[] splitCommand = command.split(" ");

        if (splitCommand.length < 2) {
            throw new IllegalArgumentException("command wasn't long enough");
        }

        numberOfArguments = Math.min(splitCommand.length - 1, 3);

        parameter1 = Integer.parseInt(splitCommand[1]);

        if (splitCommand.length > 2) {
            parameter2 = Integer.parseInt(splitCommand[2]);
        }

        if (splitCommand.length > 3) {
            parameter3 = Float.parseFloat(splitCommand[3]);
        }
    }

    int getNumberOfArguments() {
        return numberOfArguments;
    }

    int getParameter2() {
        return parameter2;
    }

    int getParameter1() {
        return parameter1;
    }

    float getParameter3() {
        return parameter3;
    }
}
