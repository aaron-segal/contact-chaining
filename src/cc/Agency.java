package cc;

import java.io.FileReader;
import java.io.IOException;
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

	protected int numAgencies;
	protected int id;
	protected Keys keys;
	protected int targetId;
	protected Properties config;

	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String TARGET_ID = "TARGET";

	protected void usage() {
		System.err.println("Usage: java cc.Agency config_file [-k key_file] [-q] [-s]");
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
			return;
		}

		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("-k")) {
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
		id = Integer.parseInt(config.getProperty(ID));
		println("ID = " + id);
		try {
			keys = new Keys(config.getProperty(PRIVATE_KEY), config.getProperty(PUBLIC_KEYS),
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
	private static int[] getAgencyIds(int numAgencies) {
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

}
