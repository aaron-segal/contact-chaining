package cc;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.math3.distribution.LogNormalDistribution;


/**
 * We are going to use 1-indexed integer ids for users and 0-indexed integer ids
 * for telecoms.
 * If nTelcoms is the number of telecoms, then the user with id X
 *   belongs to telecom (X % nTelecoms).
 * Each user makes a random number of contacts, distributed according to the
 * Log-normal distribution.
 * When a user makes a contact, it picks another id randomly, and both user ids add
 *   the other to their contact lists.
 * 
 * The files are written to filename0, filename1, etc.
 */

public class DataGenLogNormal {

	// These come from Seshadri et al. 2008  
	// https://www.cs.cmu.edu/~jure/pubs/dpln-kdd08.pdf
	public static double MU = 1.6389;
	public static double SIGMA2 = 1.5454;

	public static void usage() {
		System.err.println("Usage: java cc.DataGen filename numTelecoms numUsers");
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			usage();
			return;
		}
		int nTelecoms = Integer.parseInt(args[1]);
		int nUsers = Integer.parseInt(args[2]);

		Random rand = new Random();
		LogNormalDistribution logNorm = new LogNormalDistribution(MU, SIGMA2);

		ArrayList<HashMap<Integer, HashSet<Integer>>> graph =
				new ArrayList<HashMap<Integer, HashSet<Integer>>>();
		for (int i = 0; i < nTelecoms; i++) {
			graph.add(new HashMap<Integer, HashSet<Integer>>());
		}

		// For each user, generate a blank set of contacts.
		for (int currId = 1; currId <= nUsers; currId++) {
			HashSet<Integer> userData = new HashSet<Integer>();
			graph.get(DataGen.provider(currId, nTelecoms)).put(currId, userData);
		}

		// For each user, fill out its set of contacts.
		for (int currId = 1; currId <= nUsers; currId++) {
			HashSet<Integer> userData = graph.get(DataGen.provider(currId, nTelecoms)).get(currId);

			// Choose a random number of new contacts to make. Pick them at random.
			int outDegree = (int)Math.ceil(logNorm.sample());
			for (int j = 0; j < outDegree; j++) {
				// Do not permit self-contacts
				int idToAdd = 1 + rand.nextInt(nUsers - 1);
				if (idToAdd >= currId) {
					idToAdd++;
				}
				// Add the chosen contact to this user's set, and vice versa.
				userData.add(idToAdd);
				graph.get(DataGen.provider(idToAdd, nTelecoms)).get(idToAdd).add(currId);
			}

			// Add this user's data to the appropriate telecom
			graph.get(DataGen.provider(currId, nTelecoms)).put(currId, userData);

			if (currId % 100 == 0) {
				System.out.println(currId + " users generated");
			}
		}

		if (nUsers % 100 != 0) {
			System.out.println(nUsers + " users generated");
		}

		// Now store data to file, converting from HashSet<Integer> to int[].
		for (int i = 0; i < graph.size(); i++) {
			try {
				FileOutputStream fos = new FileOutputStream(args[0] + i);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				HashMap<Integer, HashSet<Integer>> telecomData = graph.get(i);
				HashMap<Integer, int[]> convertedData = new HashMap<Integer, int[]>();
				for (int userId : telecomData.keySet()) {
					HashSet<Integer> setData = telecomData.get(userId);
					int[] arrayData = new int[setData.size()];
					int j = 0;
					for (int k : setData) {
						arrayData[j] = k;
						j++;
					}
					convertedData.put(userId, arrayData);
				}
				oos.writeObject(convertedData);
				System.out.println("Wrote " + telecomData.size() + " users to " + args[0] + i);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}