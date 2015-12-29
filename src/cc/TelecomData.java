package cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;

import cc.TelecomResponse.MsgType;

/**
 * Container class to hold a telecom's data regarding its users.
 * @author Aaron Segal
 *
 */

public class TelecomData {
	// Stores plaintext data about who contacted who.
	private HashMap<Integer, int[]> contacts;
	// Records whether we sent the agencies information about each user.
	private HashSet<Integer> alreadySent;
	// The number of telecoms there are
	private int numTelecoms;

	@SuppressWarnings("unchecked")
	public TelecomData(String filename, int numTelecoms) {
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
	}

	public TelecomData(HashMap<Integer, int[]> contacts, int numTelecoms) {
		this.contacts = contacts;
		alreadySent = new HashSet<Integer>();
		this.numTelecoms = numTelecoms;
	}

	/**
	 * Forgets which IDs were already sent, allowing them to be sent again.
	 */
	public void resetSent() {
		alreadySent = new HashSet<Integer>();
	}

	public TelecomResponse queryResponse(int userId, Keys keys) {
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
					keys.getPublicKey(agencyId), agencyCiphertext);
		}

		// Compute set of neighbors
		int[] neighbors = contacts.get(userId);
		TelecomCiphertext[] encryptedNeighbors = new TelecomCiphertext[neighbors.length];
		ElGamal encrypter = new ElGamal();
		for (int i = 0; i < neighbors.length; i++) {
			int owner = DataGen.provider(neighbors[i], numTelecoms);
			encryptedNeighbors[i] = new TelecomCiphertext();
			encryptedNeighbors[i].setOwner(owner);
			encryptedNeighbors[i].setEncryptedId(encrypter.encrypt(
					keys.getPublicKey(owner),BigInteger.valueOf(neighbors[i])));
		}

		// Mark that we have sent this userId.
		alreadySent.add(userId);
		return new TelecomResponse(agencyCiphertext, encryptedNeighbors);
	}
}
