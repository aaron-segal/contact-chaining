package cc;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import java.util.Arrays;
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
 * 
 * The files are written to filename0, filename1, etc.
 */

@SuppressWarnings("unused")
public class DataGenExperimental {
	public static double LARGE_CHANCE = 0.01;
	public static int SMALL_MIN = 3;
	public static int SMALL_MAX = 20;
	public static int LARGE_MIN = 5;
	public static int LARGE_MAX = 500;

	public static void usage() {
		System.err.println("Usage: java cc.DataGenExperimental numTelecoms numUsers");
	}

	// Method to return which telecom owns which user id. May be changed later.
	public static int provider(int userId, int numTelecoms) {
		return userId % numTelecoms;
	}

	public static int randDegree(Random rand) {
		if (rand.nextDouble() < LARGE_CHANCE) {
			return rand.nextInt(LARGE_MAX - LARGE_MIN + 1) + LARGE_MIN;
		} else {
			return rand.nextInt(SMALL_MAX - SMALL_MIN + 1) + SMALL_MIN;
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			return;
		}
		int nUsers = Integer.parseInt(args[0]);
		if (LARGE_MAX >= nUsers) {
			System.err.println("Please allow for more than " + LARGE_MAX + " users.");
			return;
		}

		Random rand = new Random();

		HashMap<Integer, HashSet<Integer>> graph =
				new HashMap<Integer, HashSet<Integer>>();

		// For each user, generate a blank set of contacts..
		for (int currId = 1; currId <= nUsers; currId++) {
			HashSet<Integer> userData = new HashSet<Integer>();
			graph.put(currId, userData);
		}

		LogNormalDistribution lnd = new LogNormalDistribution(1.6389,1.5454);

		// For each user, generate a set of phone calls it has made.
		for (int currId = 1; currId <= nUsers; currId++) {
			if (currId % 10 == 0) {
				System.out.println(currId);
			}
			HashSet<Integer> currentSet = graph.get(currId);
			// First pick how many contacts it has
			int degree = (int)Math.ceil(lnd.sample());

			// Now fill out its list of contacts. No duplicates, no self-contacts.
			for (int j = 0; j < degree; j++) {
				// Do not permit self-contacts
				int idToAdd = 1 + rand.nextInt(nUsers - 1);
				if (idToAdd >= currId) {
					idToAdd++;
				}
				// Add this id in both directions.
				currentSet.add(idToAdd);
				graph.get(idToAdd).add(currId);
			}
			graph.put(currId, currentSet);
		}

		System.out.println(nUsers + " users generated");

		// Now analyze data.
		int[] sizes = new int[graph.size()];
		for (int i = 0; i < nUsers; i++) {
			sizes[i] = graph.get(i+1).size();
		}
		Arrays.sort(sizes);
		int[] percentiles = new int[101];
		for (int i = 0; i < 100; i++) {
			percentiles[i] = sizes[i * (sizes.length / 100)];
		}
		percentiles[100] = sizes[sizes.length-1];
		System.out.println(Arrays.toString(percentiles));
	}
}