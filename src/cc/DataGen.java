package cc;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


/**
 * We are going to use 0-indexed integer ids for users and for telecoms.
 * If nTelcoms is the number of telecoms, then the user with id X
 *   belongs to telecom (X % nTelcoms).
 * We are assuming that each user/node has a totally random set of contacts.
 * The degree of each node is distributed as follows:
 *   A node has a 99% chance to be "small", and a 1% chance to be "large".
 *   "Small" nodes have 3-200 contacts (chosen uniformly).
 *   "Large" nodes have 3-5000 contacts (chosen uniformly).
 * The large type represent businesses.
 */

public class DataGen {
	public static double LARGE_CHANCE = 0.01;
	public static int SMALL_MIN = 3;
	public static int SMALL_MAX = 200;
	public static int LARGE_MIN = 3;
	public static int LARGE_MAX = 5000;

	public static void usage() {
		System.err.println("Usage: java cc.DataGen filename numTelecoms numUsers");
	}

	// Method to return which telecom owns which user id. May be changed later.
	public static int provider(int userId, int numTelecoms) {
		return userId % numTelecoms;
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			usage();
			return;
		}
		int nTelecoms = Integer.parseInt(args[1]);
		int nUsers = Integer.parseInt(args[2]);
		if (LARGE_MAX >= nUsers) {
			System.err.println("Please allow for more than " + LARGE_MAX + " users.");
			return;
		}

		Random rand = new Random();

		ArrayList<HashMap<Integer, HashSet<Integer>>> graph =
				new ArrayList<HashMap<Integer, HashSet<Integer>>>();
		for (int i = 0; i < nTelecoms; i++) {
			graph.add(new HashMap<Integer, HashSet<Integer>>());
		}

		// For each user, generate a set of contacts and store it.
		for (int currId = 0; currId < nUsers; currId++) {
			HashSet<Integer> userData = new HashSet<Integer>();
			// First pick how many contacts it has
			int degree;
			if (rand.nextDouble() < LARGE_CHANCE) {
				degree = rand.nextInt(LARGE_MAX - LARGE_MIN + 1) + LARGE_MIN;
			} else {
				degree = rand.nextInt(SMALL_MAX - SMALL_MIN + 1) + SMALL_MIN;
			}

			// Now fill out its list of contacts. No duplicates, no self-contacts.
			for (int j = 0; j < degree; j++) {
				// Do not permit self-contacts
				int idToAdd = rand.nextInt(nUsers - 1);
				if (idToAdd >= currId) {
					idToAdd++;
				}
				// Attempt to add this id, don't increment counter if it was a duplicate
				boolean duplicate = !userData.add(idToAdd);
				if (duplicate) {
					j--;
				}
			}

			// Add this user's data to the appropriate telecom
			graph.get(provider(currId, nTelecoms)).put(currId, userData);
		}

		// Now store data to file.
		try {
			FileOutputStream fos = new FileOutputStream(args[0]);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			for (HashMap<Integer, HashSet<Integer>> telecom : graph) {
				oos.writeObject(telecom);
			}
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
