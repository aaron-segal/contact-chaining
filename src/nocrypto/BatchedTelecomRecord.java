package nocrypto;

import java.io.Serializable;

public class BatchedTelecomRecord implements Serializable {

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
	private TelecomRecord[] telecomRecords;
	// The maximum degree of users that agencies care about. Ignored if 0.
	private int maxDegree = 0;

	public BatchedTelecomRecord(TelecomRecord telecomRecord) {
		telecomRecords = new TelecomRecord[1];
		telecomRecords[0] = telecomRecord;
		type = QueryType.SEARCH;
	}

	public BatchedTelecomRecord(TelecomRecord[] telecomRecords) {
		this.telecomRecords = telecomRecords;
		type = QueryType.SEARCH;
	}

	public void setType(QueryType type) {
		this.type = type;
	}

	public QueryType getType() {
		return type;
	}

	/**
	 * @return All telecom Records.
	 */
	public TelecomRecord[] getRecords() {
		return telecomRecords;
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
