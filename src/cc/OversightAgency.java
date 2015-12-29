package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import cc.Agency.QueueTCT;
import cc.TelecomResponse.MsgType;

public class OversightAgency extends Agency {

	public static final String LEADER_IP = "LEADERIP";

	private Socket leaderSocket = null;
	private boolean connected = false;
	private ObjectOutputStream leaderOStream;
	private ObjectInputStream leaderIStream;

	@Override
	protected void usage() {
		System.err.println("Usage: java cc.OversightAgency config_file_ [-c config_file] [-k private_key_file] [-q] [-s]");
	}

	public OversightAgency(String[] args) {
		super(args);
		String[] address = config.getProperty(LEADER_IP).split(":");
		String leaderIp = address[0]; // ip
		int leaderPort = Integer.parseInt(address[1]); //port

		println("Attemping to connect to host " + leaderIp +
				" on port " + leaderPort + ".");

		connected = false;
		for (int i = 0; i < MAX_TRIES && !connected; i++) {
			try {
				leaderSocket = new Socket(leaderIp, leaderPort);
				leaderOStream = new ObjectOutputStream(leaderSocket.getOutputStream());
				leaderIStream = new ObjectInputStream(leaderSocket.getInputStream());
				println("Connected!");
				connected = true;
			} catch (UnknownHostException e) {
				System.err.println("Don't know about host: " + leaderIp);
				return;
			} catch (IOException e) {
				System.err.println("Waiting for connection to " + leaderIp + ":" + leaderPort);
				try {
					Thread.sleep(SLEEP_BETWEEN_TRIES);
				} catch (InterruptedException f) {
					continue;
				}
			}
		}
		if (!connected) {
			System.err.println("Could not connect to " + leaderIp + ":" + leaderPort);
			return;
		}
	}

	public void go() throws Exception {
		leaderOStream.writeInt(id);
		leaderOStream.flush();
		int leaderTarget = leaderIStream.readInt();
		if (leaderTarget != targetId) {
			println("Failure: Investigating agency gave target " + leaderTarget +
					" but out target is " + targetId + ".");
		} else {
			println("Connected, targeting id " + targetId);
		}

		//Test: Verify signature and sign
		SignedTelecomCiphertext signedTC =
				(SignedTelecomCiphertext) leaderIStream.readObject();
		//There should be only one signature so far, with the leader's id
		int leaderId = 0;
		for (int n : signedTC.signatures.keySet()) {
			leaderId = n;
		}
		if (!keys.verify(leaderId, signedTC)) {
			println("Failure: Investigating agency's signature (ID " + leaderId + ") does not verify!");
			return;
		}
		println("Success, signature verified");
		leaderOStream.writeObject(keys.sign(signedTC.telecomCiphertext));
		leaderOStream.flush();

		// Before entering the loop, store the fact that we were looking at the
		// primary target.
		int distance = maxDistance;

		// Enter main loop. The queue will be empty at first, so this has to be a
		// do-while loop.
		do {
			// Read, verify, and process the response of the previous telecom.
			SignedTelecomResponse prevTR =
					(SignedTelecomResponse) leaderIStream.readObject();
			TelecomResponse telecomResponse = prevTR.telecomResponse;
			if (!keys.verify(prevTR.telecomId, telecomResponse, prevTR.signature)) {
				// If we failed to verify the signature, complain bitterly and quit.
				System.err.println("Failed to verify a signature on a response from "
						+ prevTR.telecomId);
				return;
			}
			if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
				agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
				if (distance < 1) {
					println(telecomResponse.getTelecomCiphertexts().length +
							" not added to queue; maximum path length reached");
				} else if (telecomResponse.getTelecomCiphertexts().length > maxDegree &&
						distance < maxDistance) {
					// Ignore maximum degree restriction for the original target
					println(telecomResponse.getTelecomCiphertexts().length +
							" not added to queue; exceeds maximum degree");
				} else {
					for (TelecomCiphertext tc : telecomResponse.getTelecomCiphertexts()) {
						investigationQueue.addLast(new QueueTCT(tc, distance - 1));
					}
					println(telecomResponse.getTelecomCiphertexts().length + 
							" added to queue");
				}
			} else if (telecomResponse.getMsgType() == MsgType.ALREADY_SENT) {
				println("MsgType: " + telecomResponse.getMsgType());
			} else {
				System.err.println("Error: MsgType: " + telecomResponse.getMsgType());
				return;
			}

			// Look at what the next query to the telecoms should be and give the
			// leader a signature on that telecom ciphertext.
			QueueTCT queueNext = investigationQueue.pop();
			println("Remaining in queue: " + investigationQueue.size());
			distance = queueNext.distance;
			leaderOStream.writeObject(keys.sign(queueNext.data));
			leaderOStream.flush();
		} while (!investigationQueue.isEmpty());
	}

	public void close() {
		try {
			leaderOStream.close();
			leaderIStream.close();
			leaderSocket.close();
		} catch (IOException e) {}
	}

	public static void main(String[] args) {
		OversightAgency oAgency = new OversightAgency(args);
		try {
			oAgency.go();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oAgency.close();
		}
	}
}
