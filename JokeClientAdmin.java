/*
 * JokeClient.java
 * Attributed to Elliott, after Hughes, Shoffner, Winslow with alterations by Westropp
 */

import java.io.*;
import java.net.*;

/**
 * This class is to be used with the Joke Server - a multi-threader server, used
 * to provide jokes/proverbs to the Joke client, this class controls the
 *
 * @author Kevin Patrick Westropp
 */
public class JokeClientAdmin {

    public static void main(String args[]) {
        // String variable for storing server name = localhost - whatever computer is running the Server. */
        String serverName;
        if (args.length < 1) {
            // if server is on the local machine */
            serverName = "localhost";
        } else {
            // else whatever the IP address of the machine running the server */
            serverName = args[0];
        }
        System.out.println("Kevin Westropp's Joke Client Admin.\n");
        printLocalAddress();
        System.out.println("Connected to server: " + serverName + ", on Port: 2565");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String command; // String variable for saving user input. */
            do {
                System.out.print("Enter server mode: (a): 'joke' (b): 'proverb' (c): 'maintenance' (to end client session): 'quit' (to shutdown server): 'shutdown' -: "); // Prompt admin for a server mode */
                // forces output to be written to System.out */
                System.out.flush();
                // reads the input/response from user */
                command = in.readLine();
                // if command is not quit and the mode is valid, you send the command to the server */
                if (command.indexOf("quit") < 0) {
                    if (command.indexOf("joke") < 0 || command.indexOf("proverb") < 0 || command.indexOf("maintenance") < 0 || command.indexOf("shutdown") < 0) {
                        connectToServer(command, serverName);
                    } else {
                        System.out.println("Mode is not valid, please try again.");
                    }
                }
            } while (command.indexOf("quit") < 0);
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
    static void connectToServer(String command, String serverName) {
        Socket adsock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;

        try {
            /*Open a connection to server port */
            adsock = new Socket(serverName, 2565);

            //Create filter I/O streams for the socket */
            fromServer = new BufferedReader(new InputStreamReader(adsock.getInputStream()));
            toServer = new PrintStream(adsock.getOutputStream());

            // Send userName to server */
            toServer.println(command);
            toServer.flush();

            // Read two or three lines of response from the server,
            // and block while synchronously waiting. */
            for (int i = 1; i <= 3; i++) {
                textFromServer = fromServer.readLine();
                if (textFromServer != null) {
                    System.out.println(textFromServer);
                }
            }
            // close the socket connection */
            adsock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            // prints stack trace of exception. */
            x.printStackTrace();
        }
    }
}
