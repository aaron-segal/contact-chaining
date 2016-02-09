package cc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import cc.TelecomResponse.MsgType;

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
	protected boolean suppressTiming = false; 

	protected int numAgencies, numTelecoms;
	protected int id;
	protected AgencyKeys keys;
	protected int targetId, maxDistance, maxDegree;
	protected int targetDegree;
	protected Properties config;


	private long startSetupTime, startProtoTime, finishTime;
	private AtomicLong networkBytes;
	private Date timeStamp; // The date and time when the protocol began

	public static final String ID = "ID";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String PUBLIC_KEYS = "PUBLICKEYS";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";
	public static final String MAX_DISTANCE = "MAXDISTANCE";
	public static final String MAX_DEGREE = "MAXDEGREE";
	public static final String TARGET_ID = "TARGET";
	public static final String SIGNING_KEYPATH = "SIGKEYPATH";
	public static final String OUTPUT_PATH = "OUTPUTPATH";
	public static final String TIMING_RECORD_PATH = "TIMINGPATH";
	public static final int MAX_TRIES = 10;
	public static final long SLEEP_BETWEEN_TRIES = 1000;

	protected ArrayList<BigInteger[]> agencyCiphertexts;
	protected HashMap<Integer, ArrayList<TelecomCiphertext>> investigationLists;


	protected void usage() {
		System.err.println("Usage: java cc.Agency config_file [-c config_file] [-d max_degree] [-i id_of_target] [-l max_length] [-k private_key_file] [-o output_path] [-q] [-s]");
	}

	public Agency(String[] args) {
		startSetupTime = System.currentTimeMillis();
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
			} else if (args[i].equals("-d")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(MAX_DEGREE, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-l")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(MAX_DISTANCE, args[i+1]);
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
			} else if (args[i].equals("-o")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(OUTPUT_PATH, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-i")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(TARGET_ID, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-q")) {
				quiet = true;
			} else if (args[i].equals("-s")) {
				suppressTiming = true;
			} else {
				usage();
				return;
			}
		}
		numAgencies = Integer.parseInt(config.getProperty(NUM_AGENCIES, "0"));
		numTelecoms = Integer.parseInt(config.getProperty(NUM_TELECOMS, "0"));
		maxDistance = Integer.parseInt(config.getProperty(MAX_DISTANCE, "0"));
		maxDegree = Integer.parseInt(config.getProperty(MAX_DEGREE, "2147483647"));
		id = Integer.parseInt(config.getProperty(ID));
		timeStamp = new Date();
		if (!config.getProperty(OUTPUT_PATH, "").isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, h:mm a z");
			try {
				File file = new File(config.getProperty(OUTPUT_PATH));
				file.createNewFile();
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("# Data obtained by contact chaining search");
				bw.newLine();
				bw.write("# Timestamp:   " + sdf.format(timeStamp));
				bw.newLine();
				bw.write("# Target:      " + targetId);
				bw.newLine();
				bw.write("# Path length: " + maxDistance);
				bw.newLine();
				bw.flush();
				bw.close();
			} catch (IOException e) {
				System.err.println("Couldn't open file " + config.getProperty(OUTPUT_PATH) + " for writing.");
				e.printStackTrace();
			}
		}
		println("ID = " + id);
		try {
			keys = new AgencyKeys(config.getProperty(PRIVATE_KEY),
					config.getProperty(PUBLIC_KEYS),
					config.getProperty(SIGNING_KEYPATH),
					id,
					getAgencyIds(numAgencies));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		targetId = Integer.parseInt(config.getProperty(TARGET_ID, "0"));
		networkBytes = new AtomicLong();
		agencyCiphertexts = new ArrayList<BigInteger[]>();
		investigationLists = new HashMap<Integer, ArrayList<TelecomCiphertext>>();
	}


	public void recordBytes(int bytesRead) {
		networkBytes.addAndGet(bytesRead);
	}

	/**
	 * Adds the specified telecomCiphertext to the list, marking that we will
	 * send this as a request to the telecoms. 
	 * @param telecomCiphertext the TelecomCiphertext to add.
	 */
	private void addTelecomCiphertext(TelecomCiphertext telecomCiphertext) {
		int owner = telecomCiphertext.getOwner();
		if (!investigationLists.containsKey(owner)) {
			investigationLists.put(owner, new ArrayList<TelecomCiphertext>());
		}
		investigationLists.get(owner).add(telecomCiphertext);
	}

	/**
	 * Returns, in array form, all known telecom ciphertexts for a given telecom.
	 * @param owner The telecom we want ciphertexts for
	 * @return An array of TelecomCiphertexts for that telecom
	 */
	protected TelecomCiphertext[] getCiphertexts(int owner) {
		TelecomCiphertext[] ciphertexts = new TelecomCiphertext[0];
		ciphertexts = investigationLists.get(owner).toArray(ciphertexts);
		return ciphertexts;
	}

	/**
	 * @return the number of telecom ciphertexts in investigationLists
	 */
	protected int ciphertextsRemaining() {
		int total = 0;
		for (ArrayList<TelecomCiphertext> list : investigationLists.values()) {
			total += list.size();
		}
		return total;
	}

	protected void processTelecomResponse(TelecomResponse telecomResponse,
			int distance) {
		if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
			agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
			if (distance == maxDistance &&
					telecomResponse.getTelecomCiphertexts() != null) {
				println(telecomResponse.getTelecomCiphertexts().length +
						" not added to queue. Maximum path length reached");
			} else if (distance == maxDistance) {
				println("No ciphertexts added to queue. Maximum path length reached");
			} else if (telecomResponse.getTelecomCiphertexts().length > maxDegree &&
					distance > 0){
				println(telecomResponse.getTelecomCiphertexts().length +
						" not added to queue. Exceeds maximum degree.");
			} else {
				for (TelecomCiphertext tc : telecomResponse.getTelecomCiphertexts()) {
					addTelecomCiphertext(tc);
				}
				println(telecomResponse.getTelecomCiphertexts().length + 
						" added to queue.");
			}
		} else if (telecomResponse.getMsgType() == MsgType.ALREADY_SENT) {
			println("MsgType: " + telecomResponse.getMsgType());
		} else {
			System.err.println("Error: MsgType: " + telecomResponse.getMsgType());
			return;
		}
	}

	/**
	 * This method should be overridden by Agency's subclasses, which must call
	 * super.contactChaining() at the beginning of this method.
	 */
	public void contactChaining() {
		startProtoTime = System.currentTimeMillis();
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
	 * Marks the end of the protocol. Prints runtime (if quiet mode is off) and
	 * writes timings to log file if suppress_timing is off and a log file is
	 * specified.
	 */
	protected void reportTiming() {
		finishTime = System.currentTimeMillis();
		println("Setup time (ms)      : " + (startProtoTime - startSetupTime));
		println("Protocol runtime (ms): " + (finishTime - startProtoTime));
		println("Total runtime (ms)   : " + (finishTime - startSetupTime));
		println("Bytes transferred (B): " + networkBytes.get());

		/* 
		 * If a log file has been specified, save timing info to it.
		 * The top line of the log file should be:
		 * Timestamp,Agencies,Degree of target,Ciphertexts in result,Maximum path length,Maximum branching degree,Setup time (ms),Protocol time (ms),Total time (ms),Bytes transferred (B),
		 */
		if (suppressTiming || config.getProperty(TIMING_RECORD_PATH, "").isEmpty()) {
			return;
		}
		//Format must not have commas in it if the log is csv format.
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a"); 
		try {
			File file = new File(config.getProperty(TIMING_RECORD_PATH));
			file.createNewFile();
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(sdf.format(timeStamp) + ",");
			bw.write(numAgencies + ",");
			bw.write(targetDegree + ",");
			bw.write(agencyCiphertexts.size() + ",");
			bw.write(maxDistance + ",");
			bw.write(maxDegree + ",");
			bw.write((startProtoTime - startSetupTime) + ",");
			bw.write((finishTime - startProtoTime) + ",");
			bw.write((finishTime - startSetupTime) + ",");
			bw.write(networkBytes.get() + ",");
			bw.newLine();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.err.println("Couldn't open file " + config.getProperty(OUTPUT_PATH) + " for writing.");
			e.printStackTrace();
		}
	}

	/** 
	 * Prints a string to standard output, but only if we are not in quiet mode.
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


	/**
	 * Saves the output agency ciphertexts to a file if a filename is specified,
	 * or else just prints the number of agency ciphertexts that would have been
	 * saved to file.
	 * This prints something even if quiet mode is turned on. 
	 */
	public void writeOutput() {
		println("Search complete.");
		// This second line prints even if quiet mode is on. This is deliberate.
		System.out.println(agencyCiphertexts.size() + " users found.");
		if (config.getProperty(OUTPUT_PATH, "").isEmpty()) {
			return;
		}
		try {
			File file = new File(config.getProperty(OUTPUT_PATH));
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			for (BigInteger[] aCiphertext : agencyCiphertexts) {
				bw.write("%" + Arrays.toString(aCiphertext));
				bw.newLine();
			}
			bw.flush();
			bw.close();
			println("Wrote agency ciphertexts to " + config.getProperty(OUTPUT_PATH));
		} catch (IOException e) {
			System.err.println("Couldn't write to file " + config.getProperty(OUTPUT_PATH) + "!");
			e.printStackTrace();
		}


	}

}
