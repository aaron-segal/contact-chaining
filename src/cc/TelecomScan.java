package cc;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import cc.TelecomResponse.MsgType;

public class TelecomScan {

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
	public static final int MAX_THREADS_ALLOWED = 16;

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

	public TelecomScan(String[] args) {

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

	}

	protected void println(String s) {
		if (!quiet) {
			System.out.println(s);
		}
	}

	public int scanForSize(int targetSize) {
		for (int userId = 0; userId < 500000; userId++) {
			int[] neighbors = data.getNeighbors(userId);
			if (neighbors != null && neighbors.length == targetSize) {
				return userId;
			}
		}
		return -1; 	
	}


	public void close() {
		try {
			outputStream.close();
			inputStream.close();
			agencySocket.close();
		} catch (IOException e) {}
	}

	public static void main(String[] args) {
		TelecomScan primary = new TelecomScan(args);
		System.out.println(primary.scanForSize(28));
		System.out.println(primary.scanForSize(97));
		primary.close();
	}


}
