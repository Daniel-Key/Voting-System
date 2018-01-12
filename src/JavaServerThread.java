import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

//Class representing a thread with a connection between the server and a client
public class JavaServerThread implements Runnable {

    private static Socket socket;
    private static ArrayList<String> registeredStudentHashes;
    private static ArrayList<String> candidates;
    private static ArrayList<Integer> votes;
    private static PrintWriter out;
    private static BufferedReader bufferedReader;
    private static int voterIndex;

    //Constructor setting required local references
    JavaServerThread(Socket socket, ArrayList<String> registeredStudentHashes, ArrayList<String> candidates, ArrayList<Integer> votes) {
        this.socket = socket;
        this.registeredStudentHashes = registeredStudentHashes;
        this.candidates = candidates;
        this.votes = votes;
    }

    //Method run when the .start() method is called on the new JavaServerThread object
    public void run() {
        try {
            System.out.println("User is now connected to the server");
            authenticate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    //Receives authentication information from the client, and checked whether they are authorised to vote
    private void authenticate() throws IOException, NoSuchAlgorithmException {
        out = new PrintWriter(socket.getOutputStream(), true);
        voterIndex = -1;

        String authenticationString = receiveAuthentication();

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(authenticationString.getBytes("UTF-8"));
        byte[] digest = md.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (byte bytes : digest) {
            stringBuffer.append(String.format("%02x", bytes & 0xff));
        }
        String authenticationHash = stringBuffer.toString();
        boolean authenticated = false;
        for (int i = 0; i < registeredStudentHashes.size(); i++) {
            if (authenticationHash.equals(registeredStudentHashes.get(i))) {
                voterIndex = i;
                authenticated = true;
                out.println("Authenticated");
                sendCandidates();
            }
        }
        if (!authenticated) {
            out.println("Not Authenticated");
            authenticate();
        }
        //Needs work
    }

    //Method called from authenticate()- reads in the authentication information from the client,
    //and returns it in csv format
    private String receiveAuthentication() throws IOException {
        String input = "";
        StringBuilder sb = new StringBuilder();
        while (!(input.equals("Authentication Sent"))) {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            input = bufferedReader.readLine();
            if ((input != null) && (!(input.equals("Authentication Sent")))) {
                sb.append(input);
                sb.append(",");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    //Sends the list of candidates to the client
    private void sendCandidates() throws IOException {
        for (int i = 0; i < candidates.size(); i++) {
            out.println(candidates.get(i));
        }
        out.println("Complete");
        receiveVote();
    }

    //Receives voting information from the client, and checks that it is a valid vote
    private void receiveVote() throws IOException {
        String input = "";
        int candidateIndex = -1;
        boolean voteReceived = false;
        while (!voteReceived) {
            input = bufferedReader.readLine();
            boolean voteRecognised = false;
            for (int i = 0; i < candidates.size(); i++) {
                if (input.equals(candidates.get(i))) {
                    voteRecognised = true;
                    candidateIndex = i;
                }
            }
            if (voteRecognised) {
                out.println("Vote Cast");
                voteReceived = true;
            }
            else {
                out.println("Vote Not Cast");
            }
        }
        storeVote(candidateIndex);
    }

    //Stores a valid vote
    private static void storeVote(int candidateIndex) {
        int currentVotes = votes.get(candidateIndex);
        votes.set(candidateIndex, ++currentVotes);
        //Removes the client from the ArrayList of hashes of students registered to vote,
        //preventing voting twice
        registeredStudentHashes.remove(voterIndex);
        if (registeredStudentHashes.size() == 0) {
            JavaSSLServer.ballotEnded = true;
        }
    }

    //Sends the results of the ballot to the client once the ballot is closed
    public void sendResults() {
        out.println("Returning Results");
        out.println("Final results: ");
        for (int i = 0; i < candidates.size(); i++) {
            out.println(candidates.get(i) + ": " + votes.get(i) + " votes");
        }
        out.println();
        out.println("Results Returned");
    }
}
