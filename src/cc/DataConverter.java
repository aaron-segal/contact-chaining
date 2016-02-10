package cc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class DataConverter {

	public static void usage() {
		System.err.println("Usage: java cc.DataConverter inputPath outputPath numTelecoms");
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			usage();
			return;
		}

		File file = new File(args[0]);
		Scanner scan;
		try {
			scan = new Scanner(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		int nTelecoms = Integer.parseInt(args[2]);

		ArrayList<HashMap<Integer, HashSet<Integer>>> graph =
				new ArrayList<HashMap<Integer, HashSet<Integer>>>();
		for (int i = 0; i < nTelecoms; i++) {
			graph.add(new HashMap<Integer, HashSet<Integer>>());
		}

		// Initialize scanner
		int linesToSkip = 0;
		while (scan.hasNextLine() && scan.nextLine().startsWith("#")) {
			linesToSkip++;
		}
		scan.reset();
		for (int i = 0; i < linesToSkip; i++) {
			scan.nextLine();
		}

		// For each user, generate a set of contacts and store it.
		int nUsers = 0;
		while (scan.hasNextInt()) {
			// Passively add 1 to all ints we read, to exclude ID 0
			int source = scan.nextInt() + 1;
			int dest = scan.nextInt() + 1;
			// Create user dataset if this is a new user
			if (!graph.get(DataGen.provider(source, nTelecoms)).containsKey(source)) {
				graph.get(DataGen.provider(source, nTelecoms)).put(source, new HashSet<Integer>());
				nUsers++;
				if (nUsers % 100 == 0) {
					System.out.println(nUsers);
				}
			}
			if (!graph.get(DataGen.provider(dest, nTelecoms)).containsKey(dest)) {
				graph.get(DataGen.provider(dest, nTelecoms)).put(dest, new HashSet<Integer>());
				nUsers++;
				if (nUsers % 100 == 0) {
					System.out.println(nUsers);
				}
			}
			// Add an undirected version of this link.
			graph.get(DataGen.provider(source, nTelecoms)).get(source).add(dest);
			graph.get(DataGen.provider(dest, nTelecoms)).get(dest).add(source);
		}

		scan.close();
		System.out.println(nUsers + " users generated");

		// Now store data to file, converting from HashSet<Integer> to int[].
		for (int i = 0; i < graph.size(); i++) {
			try {
				FileOutputStream fos = new FileOutputStream(args[1] + i);
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
				System.out.println("Wrote " + telecomData.size() + " users to " + args[1] + i);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}