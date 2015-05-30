/*
 * JokeClient.java
 * Attributed to Elliott, after Hughes, Shoffner, Winslow with alterations by Westropp
 */
package jokes;

import java.io.*;
import java.net.*;

/**
 * This class is to be used with the Joke Server - a multi-threader server, used
 * to provide jokes and proverbs back to the user.
 *
 * @author Kevin Patrick Westropp
 */
public class JokeClient {

    /**
     * Main method for the client, this is where the bulk of the interaction
     * with the user happens - prompting for user name and user input.
     */
    public static void main(String args[]) {
        // String variable for storing server name = localhost/IP address - whatever computer is running Server. */
        String serverName;
        if (args.length < 1) {
            // if server is on the local machine */
            serverName = "localhost";
        } else {
            // else whatever the IP address of the machine running the server */
            serverName = args[0];
        }
        System.out.println("Kevin Westropp's Joke Client.\n");
        printLocalAddress();
        System.out.println("Connected to server: " + serverName + ", on Port: 1699");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            // String variable for saving user name. */
            String userName;
            // Prompt user for a username to lookup previous state if available. */
            System.out.print("Please enter your username: ");
            // force output to be written to System.out - good practice */
            System.out.flush();
            // reads the input/response from user */
            userName = in.readLine();
            // String variable for saving user input. */
            String userInput;
            do {
                // Prompt user to continue - hear a joke or quit. */
                System.out.print("Would you like to hear a joke? 'yes' for a joke or 'no' to quit: ");
                // forces output to be written to System.out - good practice */
                System.out.flush();
                // reads the input/response from user */
                userInput = in.readLine();
                // if userinput is not to quit then we connect to server */
                if (userInput.indexOf("no") < 0) {
                    connectToServer(userName, serverName);
                }
            } while (userInput.indexOf("no") < 0);
            System.out.println("Cancelled by user request.");
        } catch (IOException x) {
            // prints Exception Stack Trace. */
            x.printStackTrace();
        }
    }

    /**
     * This method prints out the local IP address and name.
     */
    static void printLocalAddress() {
        try {
            // gets local host and assign it to variable me */
            InetAddress me = InetAddress.getLocalHost();
            System.out.println("My local name is:      " + me.getHostName());
            System.out.println("My local IP address is: " + toText(me.getAddress()));
        } catch (UnknownHostException x) {
            System.out.println("I appear to be unknown to myself. Firewall?:");
            // prints Exception Stack Trace. */
            x.printStackTrace();
        }
    }

    /**
     * This method makes a string of the IP address using a
     * StringBuffer/StringBuilder.
     *
     * @param ip array
     * @return String of IP array
     */
    static String toText(byte ip[]) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ip.length; i++) {
            if (i > 0) {
                result.append(".");
            }
            result.append(0xff & ip[i]);
        }
        return result.toString();
    }

    /**
     * This method gets the Remote Address requested by the user.
     *
     * @param name of input by user
     * @param serverName = local host
     */
    static void connectToServer(String userName, String serverName) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;

        try {
            /*Open a connection to server port */
            sock = new Socket(serverName, 1699);

            //Create filter I/O streams for the socket */
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());

            // Send userName to server */
            toServer.println(userName);
            // good pratice to flush the stream so as not to lose data */ 
            toServer.flush();

            // Read two or three lines of response from the server, and block while synchronously waiting. */
            for (int i = 1; i <= 1; i++) {
                textFromServer = fromServer.readLine();
                if (textFromServer != null) {
                    System.out.println(textFromServer);
                }
            }
            // close the socket connection */
            sock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            // prints stack trace of exception. */
            x.printStackTrace();
        }
    }
}
