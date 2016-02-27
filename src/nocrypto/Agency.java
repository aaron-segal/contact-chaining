package nocrypto;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import nocrypto.TelecomResponse.MsgType;

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
	protected int targetId, maxDistance, maxDegree;
	protected int targetDegree;
	protected Properties config;


	private long startSetupTime, startProtoTime, finishTime;
	private AtomicLong networkBytes;
	private long agencyCpuTime, telecomCpuTime;
	private Date timeStamp; // The date and time when the protocol began

	public static final String ID = "ID";
	public static final String NUM_AGENCIES = "AGENCIES";
	public static final String NUM_TELECOMS = "TELECOMS";
	public static final String MAX_DISTANCE = "MAXDISTANCE";
	public static final String MAX_DEGREE = "MAXDEGREE";
	public static final String TARGET_ID = "TARGET";
	public static final String OUTPUT_PATH = "OUTPUTPATH";
	public static final String TIMING_RECORD_PATH = "TIMINGPATH";
	public static final int MAX_TRIES = 10;
	public static final long SLEEP_BETWEEN_TRIES = 1000;

	protected ArrayList<Integer> agencyOutput;
	protected HashMap<Integer, ArrayList<TelecomRecord>> investigationLists;

	protected ThreadMXBean bean;


	protected void usage() {
		System.err.println("Usage: java cc.Agency config_file [-c config_file] [-d max_degree] [-i id_of_target] [-l max_length] [-k private_key_file] [-o output_path] [-q] [-s]");
	}

	public Agency(String[] args) {
		startSetupTime = System.currentTimeMillis();
		bean = ManagementFactory.getThreadMXBean();
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
		targetId = Integer.parseInt(config.getProperty(TARGET_ID, "0"));
		networkBytes = new AtomicLong();
		agencyCpuTime = 0;
		telecomCpuTime = 0;
		agencyOutput = new ArrayList<Integer>();
		investigationLists = new HashMap<Integer, ArrayList<TelecomRecord>>();
	}


	/**
	 * Stores a number of bytes sent or received over a network. Thread-safe.
	 * @param bytesTransferred The number of bytes sent or received.
	 */
	public void recordBytes(long bytesTransferred) {
		networkBytes.addAndGet(bytesTransferred);
	}

	/**
	 * Stores the CPU Time used by an agency thread.
	 * Not thread-safe.
	 * @param time CPU time to store, in nanoseconds.
	 */
	public void recordAgencyCpuTime(long time) {
		agencyCpuTime += time;
	}

	/**
	 * Stores the CPU Time used by a telecom.
	 * Not thread-safe.
	 * @param time CPU time to store, in nanoseconds.
	 */
	public void recordTelecomCpuTime(long time) {
		telecomCpuTime += time;
	}

	/**
	 * Adds the specified TelecomRecord to the list, marking that we will
	 * send this as a request to the telecoms. 
	 * @param TelecomRecord the TelecomRecord to add.
	 */
	private void addTelecomRecord(TelecomRecord TelecomRecord) {
		int owner = TelecomRecord.getOwner();
		if (!investigationLists.containsKey(owner)) {
			investigationLists.put(owner, new ArrayList<TelecomRecord>());
		}
		investigationLists.get(owner).add(TelecomRecord);
	}

	/**
	 * Returns, in array form, all known telecom records for a given telecom.
	 * @param owner The telecom we want records for
	 * @return An array of TelecomRecords for that telecom
	 */
	protected TelecomRecord[] getRecords(int owner) {
		TelecomRecord[] records = new TelecomRecord[0];
		records = investigationLists.get(owner).toArray(records);
		return records;
	}

	/**
	 * @return the number of telecom records in investigationLists
	 */
	protected int recordsRemaining() {
		int total = 0;
		for (ArrayList<TelecomRecord> list : investigationLists.values()) {
			total += list.size();
		}
		return total;
	}

	protected void processTelecomResponse(TelecomResponse telecomResponse,
			int distance) {
		if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
			agencyOutput.add(telecomResponse.getAgencyUserId());
			if (distance == maxDistance &&
					telecomResponse.getTelecomRecords() != null) {
				println(telecomResponse.getTelecomRecords().length +
						" not added to queue. Maximum path length reached");
			} else if (distance == maxDistance) {
				println("No records added to queue. Maximum path length reached");
			} else if (telecomResponse.getTelecomRecords().length > maxDegree &&
					distance > 0){
				println(telecomResponse.getTelecomRecords().length +
						" not added to queue. Exceeds maximum degree.");
			} else {
				for (TelecomRecord tc : telecomResponse.getTelecomRecords()) {
					addTelecomRecord(tc);
				}
				println(telecomResponse.getTelecomRecords().length + 
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
		recordAgencyCpuTime(bean.getCurrentThreadCpuTime());
		finishTime = System.currentTimeMillis();
		agencyCpuTime /= 1000000L;
		telecomCpuTime /= 1000000L;
		long kilobytes = networkBytes.get() / 1024L;
		println("Setup time (ms)       : " + (startProtoTime - startSetupTime));
		println("Protocol runtime (ms) : " + (finishTime - startProtoTime));
		println("Total runtime (ms)    : " + (finishTime - startSetupTime));
		println("Agency CPU Time (ms)  : " + agencyCpuTime);
		println("Telecom CPU Time (ms) : " + telecomCpuTime);
		println("Bytes transferred (KB): " + kilobytes);

		/* 
		 * If a log file has been specified, save timing info to it.
		 * The top line of the log file should be:
		 * Timestamp,Agencies,Degree of target,Ciphertexts in result,Maximum path length,Maximum branching degree,Setup time (ms),Protocol time (ms),Total time (ms),Agency CPU Time (ms),Telecom CPU Time (ms),Bytes transferred (KB),
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
			bw.write(agencyOutput.size() + ",");
			bw.write(maxDistance + ",");
			bw.write(maxDegree + ",");
			bw.write((startProtoTime - startSetupTime) + ",");
			bw.write((finishTime - startProtoTime) + ",");
			bw.write((finishTime - startSetupTime) + ",");
			bw.write(agencyCpuTime + ",");
			bw.write(telecomCpuTime + ",");
			bw.write(kilobytes + ",");
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
	 * Saves the output agency records to a file if a filename is specified,
	 * or else just prints the number of agency records that would have been
	 * saved to file.
	 * This prints something even if quiet mode is turned on. 
	 */
	public void writeOutput() {
		println("Search complete.");
		// This second line prints even if quiet mode is on. This is deliberate.
		System.out.println(agencyOutput.size() + " users found.");
		if (config.getProperty(OUTPUT_PATH, "").isEmpty()) {
			return;
		}
		try {
			File file = new File(config.getProperty(OUTPUT_PATH));
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("# NOTICE! This is not an encrypted results file.");
			bw.newLine();
			bw.write("# It should never be used for any reason.");
			bw.newLine();
			for (int userId : agencyOutput) {
				bw.write("%" + userId);
				bw.newLine();
			}
			bw.flush();
			bw.close();
			println("Wrote agency records to " + config.getProperty(OUTPUT_PATH));
		} catch (IOException e) {
			System.err.println("Couldn't write to file " + config.getProperty(OUTPUT_PATH) + "!");
			e.printStackTrace();
		}


	}

}
