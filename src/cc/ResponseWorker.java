package cc;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

import cc.SignedTelecomCiphertext.QueryType;
import cc.TelecomResponse.MsgType;

public class ResponseWorker extends CPUTrackingThread {

	private TelecomKeys keys;
	private int threadId;
	private TelecomData data;
	// Where in the array to start working
	private int startIndex;
	// Maximum number of ciphertexts to work on
	private int itemsToDo;
	// If SEARCH, get neighboring telecoms. If CONCLUDE, don't. 
	private QueryType queryType;

	public ResponseWorker(TelecomData data, int startIndex, int itemsToDo,
			TelecomKeys keys, QueryType queryType, int threadId) {
		super();
		this.data = data;
		this.startIndex = startIndex;
		this.itemsToDo = itemsToDo;
		this.keys = keys;
		this.threadId = threadId;
		this.queryType = queryType;
	}

	/**
	 * Decrypts TelecomCiphertexts to ints, and then re-encrypts to agency
	 * ciphertexts (BigInteger[]), starting from startIndex until either toEncrypt
	 * items have been converted, or the end of the array is reached. 
	 * Duplicate items will appear as null agency ciphertexts.
	 */
	public void runReal() {
		CommutativeElGamal commEncrypter = new CommutativeElGamal();
		for (int i = startIndex; i - startIndex < itemsToDo &&
				i < data.currentCiphertexts.length; i++) {
			// First figure out which user is being requested
			int userId;
			try {
				userId = keys.decrypt(data.currentCiphertexts[i].getEncryptedId(),
						threadId);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				return;
			}

			// Check to see if this gets an error response
			MsgType responseType = data.chooseResponseType(userId);
			if (responseType != MsgType.DATA) {
				data.currentResponses[i] = new TelecomResponse(responseType);
				continue;
			}

			// If not, compute the appropriate agency ciphertext
			BigInteger[] agencyCiphertext = new BigInteger[1];
			agencyCiphertext[0] = BigInteger.valueOf(userId);
			for (int agencyId : keys.getAgencyIds()) {
				agencyCiphertext = commEncrypter.encrypt(agencyId,
						keys.getAgencyPublicKey(agencyId), agencyCiphertext);
			}

			// If we have reached the maximum chaining distance, stop here
			if (queryType == QueryType.CONCLUDE) {
				data.currentResponses[i] = new TelecomResponse(agencyCiphertext);
				continue;
			}

			// Otherwise, we need to provide telecom ciphertexts for all neighbors.
			int[] neighbors = data.getNeighbors(userId);
			TelecomCiphertext[] encryptedNeighbors =
					new TelecomCiphertext[neighbors.length];
			for (int j = 0; j < neighbors.length; j++) {
				int owner = DataGen.provider(neighbors[j], data.getNumTelecoms());
				encryptedNeighbors[j] = new TelecomCiphertext();
				encryptedNeighbors[j].setOwner(owner);
				encryptedNeighbors[j].setEncryptedId(keys.encrypt(owner,
						neighbors[j], threadId));
			}
			data.currentResponses[i] =
					new TelecomResponse(agencyCiphertext, encryptedNeighbors);
		}
	}

}
