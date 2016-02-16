package nocrypto;

import java.io.Serializable;

/**
 * Wrapper around a set of TelecomResponses.
 * @author Aaron Segal
 */
public class BatchedTelecomResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private TelecomResponse[] telecomResponses;
	private int telecomId;
	private long cpuTime;

	public BatchedTelecomResponse(TelecomResponse telecomResponse, int telecomId) {
		telecomResponses = new TelecomResponse[1];
		telecomResponses[0] = telecomResponse;
		this.telecomId = telecomId;
	}

	public BatchedTelecomResponse(TelecomResponse[] telecomResponses, int telecomId) {
		this.telecomResponses = telecomResponses;
		this.telecomId = telecomId;
	}

	/**
	 * @return the telecomResponses
	 */
	public TelecomResponse[] getTelecomResponses() {
		return telecomResponses;
	}

	/**
	 * @return the telecomId
	 */
	public int getTelecomId() {
		return telecomId;
	}

	/**
	 * @param telecomId the telecomId to set
	 */
	public void setTelecomId(int telecomId) {
		this.telecomId = telecomId;
	}

	/**
	 * @return the cpuTime
	 */
	public long getCpuTime() {
		return cpuTime;
	}

	/**
	 * @param cpuTime the cpuTime to set
	 */
	public void setCpuTime(long cpuTime) {
		this.cpuTime = cpuTime;
	}

}
