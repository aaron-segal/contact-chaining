package cc;

import java.security.Signature;
import java.security.SignatureException;
import java.util.*;
import java.io.*; 
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import cc.TelecomResponse.MsgType;

public class Telecom {

	//if the -q flag is passed, nothing will be output except at the very end
	public static boolean quiet = false;
	//if the -s flag is passed, no timings or statistics will be saved
	private boolean suppress_timing = false; 

	protected int port;
	protected int numAgencies, numTelecoms, id;
	protected TelecomData myData;
	protected Keys keys;
	protected ServerSocket listenSocket = null;

	private Socket agencySocket;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;

	public static final String PORT = "PORT";
	public static final String INPUT_FILE = "INPUT";
	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";
	public static final String SIGNING_KEYPATH = "SIGKEYPATH";

	private static void usage() {
		System.err.println("Usage: java cc.Telecom config_file [-c config_file] [-i input_data_file] [-k private_key_file] [-q] [-s]");
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
			} else if (args[i].equals("-q")) {
				quiet = true;
			} else if (args[i].equals("-s")) {
				suppress_timing = true;
			} else {
				usage();
				return;
			}
		}
		port = Integer.parseInt(config.getProperty(PORT));
		numAgencies = Integer.parseInt(config.getProperty(NUM_AGENCIES, "0"));
		numTelecoms = Integer.parseInt(config.getProperty(NUM_TELECOMS, "0"));
		id = Integer.parseInt(config.getProperty(ID));
		println("ID = " + id);
		try {
			keys = new Keys(config.getProperty(PRIVATE_KEY),
					config.getProperty(PUBLIC_KEYS),
					config.getProperty(SIGNING_KEYPATH),
					id,
					Agency.getAgencyIds(numAgencies));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		myData = new TelecomData(config.getProperty(INPUT_FILE), numTelecoms);

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

	public void test() {
		int small = 0, large = 0;
		for (int i = 0; i < 10000; i += 2) {
			System.out.print(i + " : ");
			TelecomResponse response = myData.queryResponse(i, keys);
			if (response.getMsgType() == MsgType.DATA) {
				int numNeighbors = response.getTelecomCiphertexts().length;
				if (numNeighbors < 201) {
					small++;
				} else {
					large++;
				}
				System.out.println(numNeighbors);
			} else {
				System.out.println(response.getMsgType());
			}
		}
		System.out.println("large pct: " + large + " out of " + (large + small));

	}

	private void sendResponse(TelecomResponse response) throws IOException {
		SignedTelecomResponse signedTR = new SignedTelecomResponse(response, id);
		signedTR.signature = keys.sign(response);
		outputStream.writeObject(signedTR);
		outputStream.flush();
	}

	// Waits for a connection, then responds to requests over that connection.
	// Does this forever.
	public void serveRequests() {
		// This while loop makes sure that the agency can try again if the
		// connection breaks somehow.
		while (true) {
			try {
				agencySocket = listenSocket.accept();
				outputStream = new ObjectOutputStream(agencySocket.getOutputStream());
				inputStream = new ObjectInputStream(agencySocket.getInputStream());
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
					BigInteger queryId = keys.getPrivateKey().
							decrypt(signedTC.telecomCiphertext.getEncryptedId());
					TelecomResponse response = myData.queryResponse(queryId.intValue(), keys);
					sendResponse(response);
				}
			} catch (IOException e) {
				System.err.println("Connection lost. Waiting for new connection");
			} catch (ClassNotFoundException e) {
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
