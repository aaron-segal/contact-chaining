package cc;

import java.io.Serializable;
import java.util.HashMap;

public class SignedTelecomCiphertext implements Serializable {

	/**
	 * There are two kinds of messages.
	 * SEARCH messages have one telecom ciphertext. In response, we want a
	 * corresponding agency ciphertext and telecom ciphertexts for the neighbors.
	 * CONCLUDE messages have many telecom ciphertexts. In response, we want
	 * corresponding agency ciphertexts, but don't care about neighbors.
	 * @author Aaron Segal
	 *
	 */
	public enum QueryType {
		SEARCH, CONCLUDE 
	}


	private static final long serialVersionUID = 1L;
	private QueryType type;
	private TelecomCiphertext[] telecomCiphertexts;
	private HashMap<Integer, byte[]> signatures;
	// The maximum degree of users that agencies care about. Ignored if 0.
	private int maxDegree = 0;

	public SignedTelecomCiphertext(TelecomCiphertext telecomCiphertext) {
		telecomCiphertexts = new TelecomCiphertext[1];
		telecomCiphertexts[0] = telecomCiphertext;
		signatures = new HashMap<Integer, byte[]>();
		type = QueryType.SEARCH;
	}

	public SignedTelecomCiphertext(TelecomCiphertext[] telecomCiphertexts) {
		this.telecomCiphertexts = telecomCiphertexts;
		signatures = new HashMap<Integer, byte[]>();
		type = QueryType.SEARCH;
	}

	public void setType(QueryType type) {
		this.type = type;
	}

	public QueryType getType() {
		return type;
	}

	/**
	 * @return All telecom ciphertexts.
	 */
	public TelecomCiphertext[] getCiphertexts() {
		return telecomCiphertexts;
	}

	/**
	 * Adds a signature from the given id to the ciphertext(s).
	 * Multiple uses of this with the same id will overwrite the old signature.
	 * @param id
	 * @param signature
	 */
	public void addSignature(int id, byte[] signature) {
		signatures.put(id, signature);
	}

	/**
	 * 
	 * @return All signatures on the ciphertexts.
	 */
	public HashMap<Integer, byte[]> getSignatures() {
		return signatures;
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
