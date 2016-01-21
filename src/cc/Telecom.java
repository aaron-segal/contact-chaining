package cc;

import java.security.GeneralSecurityException;
import java.util.*;
import java.io.*; 
import java.net.ServerSocket;
import java.net.Socket;

import cc.SignedTelecomCiphertext.QueryType;

public class Telecom {

	//if the -q flag is passed, nothing will be output except at the very end
	public static boolean quiet = false;

	protected int port;
	protected int maxThreads;
	protected int numAgencies, numTelecoms, id;
	protected TelecomData data;
	protected TelecomKeys keys;
	protected ServerSocket listenSocket = null;

	private Socket agencySocket;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;

	// Included for additional safety. Use the configurable
	// MAX_THREADS instead.
	public static final int MAX_THREADS_ALLOWED = 8;

	public static final String PORT = "PORT";
	public static final String INPUT_FILE = "INPUT";
	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";
	public static final String SIGNING_KEYPATH = "SIGKEYPATH";
	public static final String MAX_THREADS = "MAXTHREADS";

	private static void usage() {
		System.err.println("Usage: java cc.Telecom config_file [-c config_file] [-i input_data_file] [-k private_key_file] [-t threads] [-q]");
	}

	public Telecom(String[] args) {

		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		Properties config = new Properties();
		try {
			FileReader configFile = new FileReader(args[0]);
			config.load(configFile);
			configFile.close();
		} catch (IOException e) {
			System.err.println("Could not load config file " + args[0]);
			return;
		}

		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("-c")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					try {
						FileReader configFile2 = new FileReader(args[i+1]);
						config.load(configFile2);
						configFile2.close();
					} catch (IOException e) {
						System.err.println("Could not load config file " + args[i+1]);
						System.exit(1);
					}
					i++;
				}
			} else if (args[i].equals("-i")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(INPUT_FILE, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-k")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(PRIVATE_KEY, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-t")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(MAX_THREADS, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-q")) {
				quiet = true;
			} else {
				usage();
				return;
			}
		}
		port = Integer.parseInt(config.getProperty(PORT));
		numAgencies = Integer.parseInt(config.getProperty(NUM_AGENCIES, "0"));
		numTelecoms = Integer.parseInt(config.getProperty(NUM_TELECOMS, "0"));
		maxThreads = Integer.parseInt(config.getProperty(MAX_THREADS, "1"));
		if (maxThreads < 0 || maxThreads > MAX_THREADS_ALLOWED) {
			System.err.println("Number of threads must be between 1 and " + MAX_THREADS_ALLOWED);
			usage();
			return;
		} else if (maxThreads == 0) {
			System.err.println("Treating 0 threads as " + MAX_THREADS_ALLOWED);
			maxThreads = MAX_THREADS_ALLOWED;
		}
		id = Integer.parseInt(config.getProperty(ID));
		println("ID = " + id);
		try {
			keys = new TelecomKeys(config.getProperty(PRIVATE_KEY),
					config.getProperty(PUBLIC_KEYS),
					config.getProperty(SIGNING_KEYPATH),
					id,
					Agency.getAgencyIds(numAgencies),
					maxThreads);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		data = new TelecomData(config.getProperty(INPUT_FILE), numTelecoms, keys,
				maxThreads);

		try {
			listenSocket = new ServerSocket(port);
			println("IP:Host = " + "127.0.0.1" + ":" + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port:" + port);
			return;
		}
	}

	protected void println(String s) {
		if (!quiet) {
			System.out.println(s);
		}
	}

	private void sendResponse(TelecomResponse response) throws IOException {
		SignedTelecomResponse signedTR = new SignedTelecomResponse(response, id);
		signedTR.setSignature(keys.sign(response));
		outputStream.writeObject(signedTR);
		outputStream.flush();
	}

	// Waits for a connection, then responds to requests over that connection.
	// Does this forever.
	public void serveRequests() {
		// This while loop lets the telecom run continuously, across multiple protocol runs.
		while (true) {
			try {
				agencySocket = listenSocket.accept();
				println("Got a connection from agency at " + agencySocket.getInetAddress().toString());
				outputStream = new ObjectOutputStream(agencySocket.getOutputStream());
				inputStream = new ObjectInputStream(agencySocket.getInputStream());
				data.resetSent();
				// This while loop makes sure that we continuously respond to
				// queries over our open connection.
				while (true) {
					SignedTelecomCiphertext signedTC =
							(SignedTelecomCiphertext) inputStream.readObject();
					boolean signaturesVerify = true;
					// check to make sure all signatures verify
					for (int agencyId : keys.getAgencyIds()) {
						signaturesVerify &= keys.verify(agencyId, signedTC);
					}
					if (!signaturesVerify) {
						sendResponse(new TelecomResponse(TelecomResponse.MsgType.INVALID_SIGNATURE));
						return;
					}
					TelecomResponse response;
					if (signedTC.getType() == QueryType.SEARCH) {
						// Update maxDegree if we don't already know it
						if (data.getMaxDegree() == Integer.MAX_VALUE && signedTC.getMaxDegree() > 0) {
							data.setMaxDegree(signedTC.getMaxDegree());
						}
						int queryId =
								keys.decrypt(signedTC.getCiphertext().getEncryptedId());
						response = data.searchQueryResponse(queryId);
					} else {
						// Handle final requests differently.
						response = data.concludeQueryResponse(signedTC.getCiphertexts());
					}
					sendResponse(response);
				}
			} catch (IOException e) {
				System.err.println("Connection lost. Waiting for new connection");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	public void close() {
		try {
			outputStream.close();
			inputStream.close();
			agencySocket.close();
		} catch (IOException e) {}
	}

	public static void main(String[] args) {
		Telecom primary = new Telecom(args);
		primary.serveRequests();
		primary.close();
	}


}
