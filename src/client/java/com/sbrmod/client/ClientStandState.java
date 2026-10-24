package com.sbrmod.client;

import com.sbrmod.StandData;

/** The local player's own Stand data, as last sent by the server. */
public final class ClientStandState {
	private static StandData current = StandData.EMPTY;

	private ClientStandState() {
	}

	public static StandData get() {
		return current;
	}

	public static void set(StandData data) {
		current = data;
	}
}
