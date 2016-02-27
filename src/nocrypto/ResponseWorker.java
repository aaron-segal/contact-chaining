package nocrypto;

import nocrypto.BatchedTelecomRecord.QueryType;
import nocrypto.TelecomResponse.MsgType;

public class ResponseWorker extends cc.CPUTrackingThread {

	private TelecomData data;
	// Where in the array to start working
	private int startIndex;
	// Maximum number of records to work on
	private int itemsToDo;
	// If SEARCH, get neighboring telecoms. If CONCLUDE, don't. 
	private QueryType queryType;

	public ResponseWorker(TelecomData data, int startIndex, int itemsToDo,
			QueryType queryType) {
		super();
		this.data = data;
		this.startIndex = startIndex;
		this.itemsToDo = itemsToDo;
		this.queryType = queryType;
	}

	/**
	 * Creates TelecomResponses for each TelecomRecord received, starting
	 * from startIndex until either itemsToDo items have been converted,
	 * or the end of the array is reached. 
	 * Duplicate items will appear as null agency records.
	 */
	public void runReal() {
		for (int i = startIndex; i - startIndex < itemsToDo &&
				i < data.currentRecords.length; i++) {
			// First figure out which user is being requested
			int userId;
			userId = data.currentRecords[i].getUserId();

			// Check to see if this gets an error response
			MsgType responseType = data.chooseResponseType(userId);
			if (responseType != MsgType.DATA) {
				data.currentResponses[i] = new TelecomResponse(responseType);
				continue;
			}

			// If we have reached the maximum chaining distance, add only user id
			if (queryType == QueryType.CONCLUDE) {
				data.currentResponses[i] = new TelecomResponse(userId);
				continue;
			}

			// Otherwise, we need to provide telecom records for all neighbors.
			int[] neighbors = data.getNeighbors(userId);
			TelecomRecord[] neighborRecords =
					new TelecomRecord[neighbors.length];
			for (int j = 0; j < neighbors.length; j++) {
				int owner = cc.DataGen.provider(neighbors[j], data.getNumTelecoms());
				neighborRecords[j] = new TelecomRecord();
				neighborRecords[j].setOwner(owner);
				neighborRecords[j].setUserId(neighbors[j]);
			}
			data.currentResponses[i] =
					new TelecomResponse(userId, neighborRecords);
		}
	}

}
