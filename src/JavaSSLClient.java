import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

//SSL Client class
public class JavaSSLClient {

    private static final int port = 8000;
    private static Socket socket;
    private static ArrayList<String> candidates;
    private static BufferedReader bufferedReader;
    private static Scanner scanner;
    private static PrintWriter out;

    //Main method, catches possible exceptions thrown by the client
    public static void main(String[] args) {
        try {
            createSSLClient();
            authenticate();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    //Creates an SSL socket, within an SSL context using the client's KeyStore
    private static void createSSLClient() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        Properties properties = new Properties();
        FileInputStream propertiesIn = new FileInputStream("../config.properties");
        properties.load(propertiesIn);
        propertiesIn.close();

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = properties.getProperty("password").toCharArray();
        FileInputStream keyStoreIn = new FileInputStream("../clientKeyStore/clientKeyStore");
        ks.load(keyStoreIn, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();

        socket = sslSocketFactory.createSocket("localhost", port);
    }

    //Prompts the client to enter authentication information, sends it to the server,
    //and ensures the client is authenticated to vote before continuing
    private static void authenticate() throws IOException{
        out = new PrintWriter(socket.getOutputStream(), true);

        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        scanner = new Scanner(System.in);

        boolean authenticated = false;

        while (!authenticated) {
            System.out.println("Please enter your name");
            String name = scanner.nextLine();
            out.println(name);
            System.out.println("Please enter your matriculation number");
            String matric = scanner.nextLine();
            out.println(matric);
            System.out.println("Please enter your date of birth");
            String dob = scanner.nextLine();
            out.println(dob);
            out.println("Authentication Sent");
            String check = bufferedReader.readLine();
            if ((check != null) && (check.equals("Authenticated"))) {
                System.out.println();
                System.out.println("Authentication successful");
                System.out.println();
                receiveCandidates();
                authenticated = true;
            }
            else {
                System.out.println("Authentication failed, please try again");
            }
        }
    }

    //Receives the list of candidates from the server and initialises an ArrayList to store them
    private static void receiveCandidates() throws IOException {
        candidates = new ArrayList<>();
        String candidate = bufferedReader.readLine();
        while ((candidate != null) && (!(candidate.equals("Complete")))) {
            candidates.add(candidate);
            candidate = bufferedReader.readLine();
        }
        vote();
    }

    //Displays the possible candidates to the client, prompts them to enter their vote,
    //and then sends the data to the server, continuing once the vote is accepted as valid
    private static void vote() throws IOException {
        System.out.println("Please cast your vote by typing the name of the candidate you wish to vote for. The choices are as follows: ");
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println(candidates.get(i));
        }
        System.out.println();

        boolean voteCast = false;
        while (!voteCast) {
            String vote = scanner.nextLine();
            out.println(vote);
            String check = bufferedReader.readLine();
            if ((check != null) && (check.equals("Vote Cast"))) {
                System.out.println();
                System.out.println("Your vote has been received and counted");
                System.out.println("The results of the election will be sent once the ballot closes");
                System.out.println();
                voteCast = true;
            }
            else {
                System.out.println("Vote not recognised, please try again");
            }
        }
        receiveResults();
    }

    //Receives the results of the ballot from the server, once the ballot has been closed
    private static void receiveResults() throws IOException {
        boolean resultsReceived = false;
        while (!resultsReceived) {
            String check = bufferedReader.readLine();
            if ((check != null) && (check.equals("Returning Results"))) {
                String result = bufferedReader.readLine();
                while (!(result.equals("Results Returned"))) {
                    System.out.println(result);
                    result = bufferedReader.readLine();
                }
                resultsReceived = true;
            }
        }
    }
}