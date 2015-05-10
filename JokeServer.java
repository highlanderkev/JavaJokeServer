/*
 * JokeServer.java
 * Attributed to Elliott, after Hughes, Shoffner, Winslow with alterations by Westropp
 */

import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Status class holds data elements for storing the state of the communication
 * between the client/user and the server.
 */
class Status {

    String userName; // string of user name */
    Boolean[] userJokes; // Boolean array to hold what jokes have been sent */
    Boolean[] userProverbs; // Boolean array to hold what proverbs have been sent */

    /**
     * This is a basic constructor for a new Status object.
     *
     * @param user name
     */
    Status(String user) {
        userName = user;
        userJokes = flushArray(new Boolean[5]);
        userProverbs = flushArray(new Boolean[5]);
    }

    /**
     * Flushes/clears the array to false for storing the record.
     *
     * @param Boolean array to be setup for keeping record
     */
    static Boolean[] flushArray(Boolean[] record) {
        for (int i = 0; i < 5; i++) {
            record[i] = false;
        }
        return record;
    }
}

/**
 * AdminLooper class which implements the Runnable class and loops waiting for
 * an administration client connection.
 */
class AdminLooper implements Runnable {

    public static boolean adminControlSwitch = true;

    /**
     * Sets the administration control switch for shutdown.
     *
     * @param control boolean false set to false for shutdown
     */
    public static synchronized void setControl(boolean control) {
        if (!control) {
            adminControlSwitch = false;
        } else {
            adminControlSwitch = true;
        }
    }

    /**
     * Queries for administration control switch setting.
     *
     * @return true for control switch not set, false otherwise
     */
    public static synchronized boolean getControl() {
        if (adminControlSwitch) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Run method for the Administration Looper which starts new administration
     * threads on client connection.
     */
    @Override
    public void run() {
        System.out.println("Admin looper thread running.");

        int q_len = 6; // Number of requests for OpSys to queue */
        int port = 2565; // Listening in on different port for admins */
        Socket adsock;

        try {
            ServerSocket servsock = new ServerSocket(port, q_len);
            while (adminControlSwitch) {
                //wait for the next ADMIN client connection */
                adsock = servsock.accept();
                // start a new admin worker thread when an admin client connects */
                new AdminWorker(adsock).start();
            }
        } catch (IOException ioe) {
            System.out.println("Server admin error");
            // prints Exception Stack Trace. */
            ioe.printStackTrace();
        }
    }
}

/**
 * Administration Worker Thread which communicates with the Joke Administration
 * Client to set server mode and shutdown the server.
 */
class AdminWorker extends Thread {

    Socket adSock; // Administration socket for incoming admin connection

    /**
     * Basic constructor for an Administration worker thread.
     *
     * @param s socket of incoming connection
     */
    AdminWorker(Socket s) {
        adSock = s;
    }

    /**
     * Get I/O streams from the socket, override annotation to override Thread
     * which is inherited from parent class.
     */
    @Override
    public void run() {
        // intialize and set new PrintStream out to null */
        PrintStream out = null;
        // intialize and set new BufferedReader in to null */
        BufferedReader in = null;
        try {
            out = new PrintStream(adSock.getOutputStream());
            in = new BufferedReader(new InputStreamReader(adSock.getInputStream()));
            // If the control switch for the server is false then the server is going to shutdown */
            if (AdminLooper.getControl() != true) {
                System.out.println("Server is now shutting down as per admin request. Goodbye! ");
                out.println("Server is now shutting down. Goodbye!");
            } else {
                try {
                    // local string to hold command from the admin client */
                    String mode;
                    mode = in.readLine();
                    // if admin requests shutdown the joke server and the admin looper is set for shutdown */
                    if (mode.indexOf("shutdown") > -1) {
                        JokeServer.setControl(false);
                        AdminLooper.setControl(false);
                        System.out.println("Worker has captured a shutdown request.");
                        out.println("Shutdown request has been noted by worker.");
                        out.println("Please send final shutdown request to server.");
                    } else {
                        // otherwise the mode will switch depending on the command */
                        System.out.println("Server is now set to " + mode + "mode.");
                        JokeServer.changeServerMode(mode);
                    }
                } catch (IOException x) {
                    System.out.println("Server read error");
                    // prints Exception Stack Trace. */
                    x.printStackTrace();
                }
            }
            // close this connection, but not the server. */
            adSock.close();
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

/**
 * Class definition for worker class, extends the Java thread class. This class
 * handles the each thread created by the Server Class.
 *
 * @author Kevin Patrick Westropp
 */
class Worker extends Thread {

    /* Class member, socket, local to Worker. */
    Socket sock;

    /**
     * Constructor to assign s to local socket
     */
    Worker(Socket s) {
        sock = s;
    }

    /**
     * Get I/O streams from the socket, override annotation to override Thread
     * which is inherited from parent class. Handles reading userName from
     * client, then finding the state if user is not new, then calling the
     * service handler and finally updating the count to reflect the state of
     * the jokes/proverbs sent.
     */
    @Override
    public void run() {
        // intialize and set new PrintStream out to null */
        PrintStream out = null;
        // intialize and set new BufferedReader in to null */
        BufferedReader in = null;
        try {
            out = new PrintStream(sock.getOutputStream());
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            if (JokeServer.getControl() != true) {
                System.out.println("Server Shutdown mode.");
                out.println("The server is shutting down, Goodbye!");
            } else if (JokeServer.getServerMode() == 3) {
                // if server is in maintenance mode just send back message to try back later */
                System.out.println("Maintenance Mode: Request from Client.");
                out.println("The server is temporarily unavailable -- check-back shortly.");
            } else {
                try {
                    // local Status to hold status/state of user
                    Status currentUser;
                    // local count number to pick a joke/proverb to send to user */
                    int count;
                    // local copy of the userName sent over from the client. */
                    String userName;
                    userName = in.readLine();
                    // checks if the server has a state stored if so it gets it from the server */
                    if (JokeServer.checkUser(userName)) {
                        System.out.println(userName + " has connected.");
                        currentUser = JokeServer.getUserState(userName);
                        if ((count = getCount(currentUser)) < 0) {
                            currentUser = flushArray(currentUser);
                            count = randomCount();
                        }
                    } else {
                        // this is a new user and so a new state must be created to store what has been sent */
                        System.out.println("New User: " + userName + " has connected.");
                        count = randomCount();
                        currentUser = new Status(userName);
                    }
                    // call the service handler with the count, username and printstream */
                    serviceHandler(userName, count, out);
                    // update the count in the users arrays of stored state */
                    currentUser = updateCount(currentUser, count);
                    // last thing to do is store state on the server before we close the socket */
                    JokeServer.storeUserState(userName, currentUser);
                } catch (IOException x) {
                    System.out.println("Server read error");
                    x.printStackTrace();    // prints Exception Stack Trace. */
                }
            }

            sock.close();   // close this connection, but not the server. */
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    /**
     * Updates the count/index of the joke/proverb array to reflect that the
     * joke/proverb has been sent.
     *
     * @param state status object
     * @param count index of joke/proverb: true means it has been sent
     */
    static Status updateCount(Status state, int count) {
        if (JokeServer.getServerMode() == 2) {
            state.userProverbs[count] = true;
        } else {
            state.userJokes[count] = true;
        }
        return state;
    }

    /**
     * Method looks through the array of jokes or proverbs to find a joke or
     * proverb that hasn't been sent to the user yet. If all have been sent then
     * it clears/flushes the array and starts over with another random number.
     *
     * @param state status object of the current user
     * @return integer of the count - index of joke that hasn't been sent yet or
     * a random joke to send.
     */
    static int getCount(Status state) {
        int temp = randomCount();
        int tempAlt = randomCount();
        if (JokeServer.getServerMode() == 2) {
            if (state.userProverbs[temp] == false) {
                return temp;
            } else if (state.userProverbs[tempAlt] == false) {
                return tempAlt;
            } else {
                for (int i = 0; i < 5; i++) {
                    if (state.userProverbs[i] == false) {
                        return i;
                    }
                }
            }
        } else {
            if (state.userJokes[temp] == false) {
                return temp;
            } else if (state.userJokes[tempAlt] == false) {
                return tempAlt;
            } else {
                for (int i = 0; i < 5; i++) {
                    if (state.userJokes[i] == false) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns a random number between 0-4.
     *
     * @return random integer number
     */
    static int randomCount() {
        double temp = Math.random() * 5;
        int count = (int) temp;
        return count;
    }

    /**
     * Flushes/clears the array to false for storing the record.
     *
     * @param Status state of user
     * @return Status state of user
     */
    static Status flushArray(Status state) {
        if (JokeServer.getServerMode() == 2) {
            for (int i = 0; i < 5; i++) {
                state.userProverbs[i] = false;
            }
        } else {
            for (int i = 0; i < 5; i++) {
                state.userJokes[i] = false;
            }
        }
        return state;
    }

    /**
     * This method handles the servicing of the jokes/proverbs out to the
     * client.
     *
     * @param userName of client receiving joke/proverb
     * @param out PrintStream of client to be sent over the socket
     * @param count random/or seemly somewhat random number for picking a
     * joke/proverb
     */
    static void serviceHandler(String userName, int count, PrintStream out) {
        String joke = "";
        if (JokeServer.getServerMode() == 2) {
            switch (count) {
                case 0:
                    joke = proverbD(userName);
                    break;
                case 1:
                    joke = proverbE(userName);
                    break;
                case 2:
                    joke = proverbA(userName);
                    break;
                case 3:
                    joke = proverbC(userName);
                    break;
                case 4:
                    joke = proverbB(userName);
                    break;
            }
        } else {
            switch (count) {
                case 0:
                    joke = jokeE(userName);
                    break;
                case 1:
                    joke = jokeA(userName);
                    break;
                case 2:
                    joke = jokeD(userName);
                    break;
                case 3:
                    joke = jokeC(userName);
                    break;
                case 4:
                    joke = jokeB(userName);
                    break;
            }
        }
        out.println(joke);
        out.flush();
    }

    /**
     * Provides joke A to the user.
     *
     * @param userName name of user
     * @return joke A
     */
    static String jokeA(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joke A: %s, Do you know this one? Most people believe that if it ain't broke, don't fix it...Engineers believe that if it ain't broke, it doesn't have enough features yet.", userName));
        return sb.toString();
    }

    /**
     * Provides joke B to the user.
     *
     * @param userName name of user
     * @return joke A
     */
    static String jokeB(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joke B: %s, Why do they call it hyper text? Too much Java.", userName));
        return sb.toString();
    }

    /**
     * Provides joke C to the user.
     *
     * @param userName name of user
     * @return joke C
     */
    static String jokeC(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joke C: So %s, How many IT guys does it take to screw in a light bulb? None, that's a Facilities problem.", userName));
        return sb.toString();
    }

    /**
     * Provides joke D to the user.
     *
     * @param userName name of user
     * @return joke D
     */
    static String jokeD(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joke D: I bet you haven't hear this one, %s? How many programmers does it take to screw in a light bulb? None, that's a hardware problem. ", userName));
        return sb.toString();
    }

    /**
     * Provides joke E to the user.
     *
     * @param userName name of user
     * @return joke E
     */
    static String jokeE(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joke E: There are only 10 types of people in the world those that understand binary and those that don't. Which one are you, %s?", userName));
        return sb.toString();
    }

    /**
     * Provides proverb A to the user.
     *
     * @param userName name of user
     * @return proverb A
     */
    static String proverbA(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Proverb A: %s, All good things come to those who wait.", userName));
        return sb.toString();
    }

    /**
     * Provides proverb B to the user.
     *
     * @param userName name of user
     * @return proverb B
     */
    static String proverbB(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Proverb B: Great starts make great finishes...%s", userName));
        return sb.toString();
    }

    /**
     * Provides proverb C to the user.
     *
     * @param userName name of user
     * @return proverb C
     */
    static String proverbC(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Proverb C: %s, Practice makes perfect.", userName));
        return sb.toString();
    }

    /**
     * Provides proverb D to the user.
     *
     * @param userName name of user
     * @return proverb D
     */
    static String proverbD(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Proverb D: You're never too old to learn, %s!", userName));
        return sb.toString();
    }

    /**
     * Provides proverb E to the user.
     *
     * @param userName name of user
     * @return proverb E
     */
    static String proverbE(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Proverb E: Wise men/women think alike. Don't they, %s?", userName));
        return sb.toString();
    }
}

/**
 * A multi-threaded server for the Joke Client and Joke Client Administration
 * which can handle multiple requests from clients for jokes/proverbs, by using
 * the worker classes above.
 */
public class JokeServer {

    private static HashMap<String, Status> userStates = new HashMap<>(); // HashMap to store userName and state object */ 
    private static boolean controlSwitch = true; // set to true on startup */
    private static int serverMode = 1; // 1 joke mode; 2 proverb mode; 3 maintenance mode */

    /**
     * Flushes or clears the array back to false, changes the indices back to
     * false which means the joke/proverb has not been told yet.
     *
     * @param record Boolean array
     */
    public static void flushArray(Boolean[] record) {
        for (int i = 0; i < 5; i++) {
            record[i] = false;
        }
    }

    /**
     * This method checks the HashMap of user States and reports back if it is a
     * new user or not.
     *
     * @param user name of user
     * @return true for a stored user state, false if new user
     */
    public static synchronized boolean checkUser(String user) {
        if (userStates.containsKey(user)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method simply stores the state back onto the HashMap mapped to the
     * user name.
     *
     * @param user name of user
     * @param state object
     */
    public static synchronized void storeUserState(String user, Status state) {
        userStates.put(user, state);
    }

    /**
     * Returns the user State back to the thread for handling.
     *
     * @param user
     * @return Status object of user state
     */
    public static synchronized Status getUserState(String user) {
        Status state;
        state = userStates.get(user);
        return state;
    }

    /**
     * Sets the control switch for shutdown.
     *
     * @param control boolean false set to false for shutdown
     */
    public static synchronized void setControl(boolean control) {
        if (!control) {
            controlSwitch = false;
        } else {
            controlSwitch = true;
        }
    }

    /**
     * Queries for control switch setting.
     *
     * @return true for control switch not set, false otherwise
     */
    public static synchronized boolean getControl() {
        if (controlSwitch) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the server mode from the Administration client.
     *
     * @param mode to set the server to 1:joke 2:proverb 3:maintenance
     */
    public static synchronized void changeServerMode(String mode) {
        switch (mode) {
            case "joke":
                serverMode = 1;
                break;
            case "proverb":
                serverMode = 2;
                break;
            case "maintenance":
                serverMode = 3;
                break;
        }
    }

    /**
     * Returns the server mode that the server is in.
     *
     * @return integer of what the server mode is set to
     */
    public static synchronized int getServerMode() {
        int temp = serverMode;
        return temp;
    }

    /**
     * Main method which starts the server to loop and accept incoming
     * connections.
     *
     * @param String array
     */
    public static void main(String a[]) throws IOException {
        int q_len = 6; // Number of requests for OpSys to queue */
        int port = 1699; // start listening on port 1699 */
        Socket sock; // intialize Socket variable sock */

        // create a different thread for admin clients */
        AdminLooper AL = new AdminLooper();
        Thread t = new Thread(AL);
        t.start(); // ...waiting for admin input

        //intialize and set a new server socket to servsock */
        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println("Kevin Westropp's Joke server starting up, listening at port 1699.\n");
        while (controlSwitch) {
            // wait for the next client connection */
            // accept/listen for incoming connections */
            sock = servsock.accept();
            // new worker thread to handle client communication */
            new Worker(sock).start();
        }
    }
}
