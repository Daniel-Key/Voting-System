import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

//SSL Server class
public class JavaSSLServer {

    private static final int port = 8000;
    private static ArrayList<String> registeredStudentHashes;
    private static ArrayList<String> candidates;
    private static ArrayList<Integer> votes;
    private static ArrayList<JavaServerThread> threads;
    private static ServerSocket sslServerSocket;
    private static Socket socket;
    private static int timeOutSeconds = 3600;
    private static Thread timingThread;

    public static boolean ballotEnded = false;

    //Main method, checks that the program has been legally run and catches possible exceptions
    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                readInRegisteredStudents(args[0]);
                readInCandidates(args[1]);
                createTimeOut();
                createSSLServer();
            }
            catch (IOException ioe){
                ioe.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Usage: java JavaSSLServer ../registeredStudents.csv ../candidates.txt");
        }
    }

    //Reads in registered student information from the given .csv file, calls a method to hash each line,
    //and stores the hashes in an ArrayList
    private static void readInRegisteredStudents(String file) throws IOException, NoSuchAlgorithmException{
        registeredStudentHashes = new ArrayList<>();
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        while (br.ready()) {
            String line = br.readLine();
            hashData(line, registeredStudentHashes);
        }
        br.close();
    }


    //Reads in candidate information from the given .txt file, and stores it in an ArrayList
    private static void readInCandidates(String file) throws IOException, NoSuchAlgorithmException{
        candidates = new ArrayList<>();
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        while (br.ready()) {
            String line = br.readLine();
            candidates.add(line);
        }
        //Creates a separate ArrayList, which will track the votes for each candidate
        votes = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            votes.add(0);
        }
    }

    //Creates an SSL server within an SSLContext using the server's KeyStore
    private static void createSSLServer() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException{
        //Reads the KeyStore password in from a properties file, to prevent coding it directly into the program
        Properties properties = new Properties();
        FileInputStream propertiesIn = new FileInputStream("../config.properties");
        properties.load(propertiesIn);
        propertiesIn.close();

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = properties.getProperty("password").toCharArray();
        FileInputStream keyStoreIn = new FileInputStream("../electionKeyStore/electionKeyStore");
        ks.load(keyStoreIn, password);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, password);
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagerFactory.getKeyManagers(), null, null);

        SSLServerSocketFactory sslServerSocketFactory = context.getServerSocketFactory();

        sslServerSocket = sslServerSocketFactory.createServerSocket(port);
        System.out.println("SSL ServerSocket started");
        threads = new ArrayList<>();

        //Each new client connection creates a new instance of JavaServerThread, allowing multiple client-server connections
        while (true) {
            JavaServerThread serverThread = new JavaServerThread(sslServerSocket.accept(), registeredStudentHashes, candidates, votes);
            Thread thread = new Thread(serverThread);
            thread.start();
            threads.add(serverThread);
        }
    }

    //Creates a thread to time the ballot, closing it after a given time,
    //or after all registered students have voted
    private static void createTimeOut() {
        timingThread = new Thread() {
            public void run() {
                try {
                    //For loop runs for the number of seconds defined by timeOutSeconds
                    //Each time it sleeps for a second, then checks if the ballot has ended early by all registered students voting
                    for (int i = 0; i < timeOutSeconds; i++) {
                        sleep(1000);
                        if (ballotEnded) {
                            break;
                        }
                    }
                    System.out.println("Ballot closed");
                    endBallot();
                } catch (InterruptedException e) {
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            private void endBallot() throws InterruptedException, IOException {
                for (int i = 0; i < threads.size(); i++) {
                    JavaServerThread thread = threads.get(i);
                    thread.sendResults();
                }
                sleep(3000);
                sslServerSocket.close();
            }
        };
        timingThread.start();
    }

    //Hashes a line of student authentication data using MD5, and returns a String of the hex format of the hash
    private static void hashData(String student, ArrayList<String> list) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(student.getBytes("UTF-8"));
        byte[] digest = md.digest();
        StringBuffer stringBuffer = new StringBuffer();
        //Creates a comparable format of the hash
        for (byte bytes : digest) {
            stringBuffer.append(String.format("%02x", bytes & 0xff));
        }
        list.add(stringBuffer.toString());
    }
}

