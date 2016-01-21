package cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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
	public int[] currentPlaintexts;
	public TelecomCiphertext[] currentCiphertexts;
	public BigInteger[][] currentAgencyCiphertexts;

	// We won't consider starting a new thread for encryption unless we can give it
	// this many items to work on.
	public static final int MIN_ITEMS_PER_THREAD = 10;

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
	 * Tests a user id to see if we should convert it from telecom to agency
	 * ciphertext or whether it should be skipped. If it isn't skipped, it
	 * gets added to alreadySent by this method. This is thread-safe.
	 * @param userId The user id to ask about
	 * @return True if we should skip this user in the last step, False if we should process it.
	 */
	public boolean skipUser(int userId) {
		synchronized (contacts) {
			if (contacts.containsKey(userId) && !alreadySent.contains(userId)) {
				alreadySent.add(userId);
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Computers a response to a query about the neighbors of a single user.
	 * @param userId The user being queried.
	 * @return The (unsigned) response we should send the queryin agency.
	 */
	public TelecomResponse searchQueryResponse(int userId) {
		// Check if userId is valid
		if (!contacts.containsKey(userId)) {
			return new TelecomResponse(MsgType.NOT_FOUND);
		} else if (alreadySent.contains(userId)) {
			return new TelecomResponse(MsgType.ALREADY_SENT);
		}

		// Compute agency ciphertext
		BigInteger[] agencyCiphertext = {BigInteger.valueOf(userId)};
		CommutativeElGamal commEncrypter = new CommutativeElGamal();
		for (int agencyId : keys.getAgencyIds()) {
			agencyCiphertext = commEncrypter.encrypt(agencyId,
					keys.getAgencyPublicKey(agencyId), agencyCiphertext);
		}

		// Compute set of neighbors unless distance == 0
		currentPlaintexts = contacts.get(userId);
		currentCiphertexts = new TelecomCiphertext[currentPlaintexts.length];
		if (currentPlaintexts.length <= maxDegree) {
			int threads = currentPlaintexts.length / MIN_ITEMS_PER_THREAD;
			threads = Math.max(threads, 1);
			threads = Math.min(threads, maxThreads);
			int itemsPerThread = (currentPlaintexts.length + threads - 1) / threads;

			// Do encryption in threads
			EncryptWorker[] workers = new EncryptWorker[threads];
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new EncryptWorker(this, itemsPerThread * i,
						itemsPerThread, keys, i);
				workers[i].start();
			}
			for (int i = 0; i < workers.length; i++) {
				try {
					workers[i].join();
				} catch (InterruptedException e) {
				}
			}
		}

		// Mark that we have sent this userId.
		alreadySent.add(userId);
		return new TelecomResponse(agencyCiphertext, currentCiphertexts);
	}

	public TelecomResponse concludeQueryResponse(TelecomCiphertext[] telecomCiphertexts) {
		currentCiphertexts = telecomCiphertexts;
		currentAgencyCiphertexts = new BigInteger[currentCiphertexts.length][];
		int threads = currentCiphertexts.length / MIN_ITEMS_PER_THREAD;
		threads = Math.max(threads, 1);
		threads = Math.min(threads, maxThreads);
		int itemsPerThread = (currentCiphertexts.length + threads - 1) / threads;
		// Convert from TelecomCiphertext to AgencyCiphertext in threads
		ConvertWorker[] workers = new ConvertWorker[threads];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new ConvertWorker(this, itemsPerThread * i,
					itemsPerThread, keys, i);
			workers[i].start();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				workers[i].join();
			} catch (InterruptedException e) {
			}
		}
		// Condense currentAgencyCiphertexts by removing null ciphertexts.
		// The resulting valid array has length = copy.
		int copy = 0;
		for (int scan = 0; scan < currentAgencyCiphertexts.length; scan++) {
			if (currentAgencyCiphertexts[scan] != null) {
				currentAgencyCiphertexts[copy] = currentAgencyCiphertexts[scan];
				copy++;
			}
		}
		return new TelecomResponse(Arrays.copyOf(currentAgencyCiphertexts, copy));
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
