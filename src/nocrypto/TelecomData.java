package nocrypto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;

import nocrypto.BatchedTelecomRecord.QueryType;
import nocrypto.TelecomResponse.MsgType;

/**
 * Container class to hold a telecom's data regarding its users.
 * @author Aaron Segal
 */

/*
 * How we decide how many threads to spawn for a given job:
 * We just divide up the items as evenly as we can among the threads.
 */

public class TelecomData {
	// Stores plaintext data about who contacted who.
	private HashMap<Integer, int[]> contacts;
	// Records whether we sent the agencies information about each user.
	private HashSet<Integer> alreadySent;
	// The number of telecoms there are
	private int numTelecoms;
	// The maximum degree of users the agencies are interested in
	private int maxDegree;
	// The maximum number of threads we can use for decryption.
	private int maxThreads;

	// current* are accessed by encryption threads
	public TelecomRecord[] currentRecords;
	public TelecomResponse[] currentResponses;

	// Used for tracking CPU time.
	private long cpuTime = 0L;


	@SuppressWarnings("unchecked")
	public TelecomData(String filename, int numTelecoms, int maxThreads) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			contacts = (HashMap<Integer, int[]>) ois.readObject();
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		alreadySent = new HashSet<Integer>();
		this.numTelecoms = numTelecoms;
		this.maxDegree = Integer.MAX_VALUE;
		this.maxThreads = maxThreads;
	}

	public TelecomData(HashMap<Integer, int[]> contacts, int numTelecoms) {
		this.contacts = contacts;
		alreadySent = new HashSet<Integer>();
		this.numTelecoms = numTelecoms;
	}

	/**
	 * Forgets which IDs were already sent, allowing them to be sent again.
	 * Also resets maxDegree.
	 */
	public void resetSent() {
		alreadySent = new HashSet<Integer>();
		this.maxDegree = Integer.MAX_VALUE;
	}

	/**
	 * Tests a user id to see what type of response it should get. That is, it
	 * checks to make sure we have this user in our database, and that it hasn't
	 * already been sent to the agencies. If that is the case, the id gets added to
	 * alreadySent by this method. This is thread-safe.
	 * @param userId The user id to ask about
	 * @return The type of response we should be sending.
	 */
	public MsgType chooseResponseType(int userId) {
		if (!contacts.containsKey(userId)) {
			return MsgType.NOT_FOUND;
		}
		synchronized (alreadySent) {
			if (alreadySent.contains(userId)) {
				return MsgType.ALREADY_SENT;
			} else {
				alreadySent.add(userId);
				return MsgType.DATA;
			}
		}
	}

	/**
	 * Computes an array of telecom responses, in response to a query containing an
	 * array of telecom records.
	 * @param telecomRecords The query records
	 * @param type SEARCH if agency wants neighbors; CONCLUDE if not
	 * @return the responses to the queries
	 */
	public TelecomResponse[] queryResponse(TelecomRecord[] telecomRecords,
			QueryType type) {
		currentRecords = telecomRecords;
		currentResponses = new TelecomResponse[currentRecords.length];
		int threads = Math.min(currentRecords.length, maxThreads);
		int itemsPerThread = (currentRecords.length + threads - 1) / threads;
		// Compute TelecomResponses to TelecomRecords in threads
		ResponseWorker[] workers = new ResponseWorker[threads];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new ResponseWorker(this, itemsPerThread * i, 
					itemsPerThread, type);
			workers[i].start();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
			} finally {
				cpuTime += workers[i].getCpuTime();
			}
		}
		// At this point, we are done.
		return currentResponses;
	}

	/**
	 * Returns the CPU time used by the telecom's subthreads since the last time
	 * this method was called.
	 * @return the CPU time (ns)
	 */
	public long getCpuTime() {
		long oldCpuTime = cpuTime;
		cpuTime = 0;
		return oldCpuTime;
	}

	/**
	 * Gets the immediate neighbors of a given user.
	 * @param userId The user being requested
	 * @return An array of that user's neighbors.
	 */
	public int[] getNeighbors(int userId) {
		return contacts.get(userId);
	}

	/**
	 * @return the number of Telecoms
	 */
	public int getNumTelecoms() {
		return numTelecoms;
	}

	/**
	 * @return the maxDegree
	 */
	public int getMaxDegree() {
		return maxDegree;
	}

	/**
	 * @param maxDegree the maxDegree to set
	 */
	public void setMaxDegree(int maxDegree) {
		this.maxDegree = maxDegree;
	}
}
