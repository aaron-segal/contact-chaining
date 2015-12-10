package cc;

import java.util.*;
import java.io.*; 
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

	public static final String PORT = "PORT";
	public static final String INPUT_FILE = "INPUT";
	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";

	private static void usage() {
		System.err.println("Usage: java cc.Telecom config_file [-i input_file] [-k key_file] [-q] [-s]");
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
			if (args[i].equals("-i")) {
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
			keys = new Keys(config.getProperty(PRIVATE_KEY), config.getProperty(PUBLIC_KEYS),
					getAgencyIds(numAgencies));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		myData = new TelecomData(config.getProperty(INPUT_FILE));

		try {
			listenSocket = new ServerSocket(port);
			println("IP:Host = " + "127.0.0.1" + ":" + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port:" + port);
			return;
		}
	}
		

	/**
	 * Operating on the assumption that agencies have ids -1 through -numAgencies,
	 * generate that array. We may want to change this assumption in a future version.
	 * @param numAgencies The number of agencies there are.
	 * @return An array with integers from -1 to -numAgencies, inclusive.
	 */
	private static int[] getAgencyIds(int numAgencies) {
		int[] agencyIds = new int[numAgencies];
		for (int i = 0; i < numAgencies; i++) {
			agencyIds[i] = -1-i;
		}
		return agencyIds;
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
			TelecomResponse response = myData.queryResponse(i, numTelecoms, keys);
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

	public static void main(String[] args) {
		Telecom primary = new Telecom(args);
		
		try {
			while (true) {
			Socket agencySocket = primary.listenSocket.accept();
			TelecomResponder responder = new TelecomResponder(agencySocket, primary.myData);
			responder.run();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
