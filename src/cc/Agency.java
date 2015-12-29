package cc;

import java.io.FileReader;
import java.io.IOException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Properties;

/**
 * Represents an oversight agency. Its job is to check everything the investigating agency
 * does, and sign its messages. It should have the same output as the investigating agency.
 * @author Aaron Segal
 *
 */
public abstract class Agency {

	//if the -q flag is passed, nothing will be output except at the very end
	public static boolean quiet = false;
	//if the -s flag is passed, no timings or statistics will be saved
	protected boolean suppress_timing = false; 

	protected int numAgencies, numTelecoms;
	protected int id;
	protected Keys keys;
	protected int targetId, maxDistance, maxDegree;
	protected Properties config;

	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";
	public static final String MAX_DISTANCE = "MAXDISTANCE";
	public static final String MAX_DEGREE = "MAXDEGREE";
	public static final String TARGET_ID = "TARGET";
	public static final String SIGNING_KEYPATH = "SIGKEYPATH";
	public static final int MAX_TRIES = 10;
	public static final long SLEEP_BETWEEN_TRIES = 1000;


	protected void usage() {
		System.err.println("Usage: java cc.Agency config_file_ [-c config_file] [-k private_key_file] [-q] [-s]");
	}

	public Agency(String[] args) {

		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		config = new Properties();
		try {
			FileReader configFile = new FileReader(args[0]);
			config.load(configFile);
			configFile.close();
		} catch (IOException e) {
			System.err.println("Could not load config file " + args[0]);
			System.exit(1);
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
		numAgencies = Integer.parseInt(config.getProperty(NUM_AGENCIES, "0"));
		numTelecoms = Integer.parseInt(config.getProperty(NUM_TELECOMS, "0"));
		id = Integer.parseInt(config.getProperty(ID));
		println("ID = " + id);
		try {
			keys = new Keys(config.getProperty(PRIVATE_KEY),
					config.getProperty(PUBLIC_KEYS),
					config.getProperty(SIGNING_KEYPATH),
					id,
					getAgencyIds(numAgencies));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		targetId = Integer.parseInt(config.getProperty(TARGET_ID, "0"));

	}


	/**
	 * Operating on the assumption that agencies have ids -1 through -numAgencies,
	 * generate that array. We may want to change this assumption in a future version.
	 * @param numAgencies The number of agencies there are.
	 * @return An array with integers from -1 to -numAgencies, inclusive.
	 */
	public static int[] getAgencyIds(int numAgencies) {
		int[] agencyIds = new int[numAgencies];
		for (int i = 0; i < numAgencies; i++) {
			agencyIds[i] = -1-i;
		}
		return agencyIds;
	}

	/** 
	 * Prints a string to stdout if we are not in quiet mode.
	 * @param s The string to print.
	 */
	protected void println(String s) {
		if (!quiet) {
			System.out.println(s);
		}
	}

	/**
	 * 
	 * @return the targetId
	 */
	public int getTargetId() {
		return targetId;
	}

}
