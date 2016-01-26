package cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;

import cc.SignedTelecomCiphertext.QueryType;
import cc.TelecomResponse.MsgType;

/**
 * Container class to hold a telecom's data regarding its users.
 * @author Aaron Segal
 */

/*
 * How we decide how many threads to spawn for a given job:
 * If we have less than 2*MIN_ITEMS_PER_THREAD, we just do 1 thread.
 * At 2*MIN_ITEMS_PER_THREAD, and every interval of MIN_ITEMS_PER_THREAD beyond,
 * we start a new thread up to a maximum of maxThreads * MIN_ITEMS_PER_THREAD.
 * Beyond that, we just divide up the items as evenly as we can among the threads.
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
	// Keys used for crypto operations.
	private TelecomKeys keys;

	// current* are accessed by encryption threads
	public TelecomCiphertext[] currentCiphertexts;
	public TelecomResponse[] currentResponses;

	// We won't consider starting a new thread for responding unless we can give it
	// this many items to work on.
	public static final int MIN_ITEMS_PER_THREAD = 5;

	@SuppressWarnings("unchecked")
	public TelecomData(String filename, int numTelecoms, TelecomKeys keys, int maxThreads) {
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
		this.keys = keys;
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

	public TelecomResponse[] queryResponse(TelecomCiphertext[] telecomCiphertexts,
			QueryType type) {
		currentCiphertexts = telecomCiphertexts;
		currentResponses = new TelecomResponse[currentCiphertexts.length];
		int threads = currentCiphertexts.length / MIN_ITEMS_PER_THREAD;
		threads = Math.max(threads, 1);
		threads = Math.min(threads, maxThreads);
		int itemsPerThread = (currentCiphertexts.length + threads - 1) / threads;
		// Convert from TelecomCiphertext to AgencyCiphertext in threads
		ConvertWorker[] workers = new ConvertWorker[threads];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new ConvertWorker(this, itemsPerThread * i,
					itemsPerThread, keys, type, i);
			workers[i].start();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
			}
		}
		// At this point, we are done.
		return currentResponses;
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
