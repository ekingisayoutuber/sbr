package com.sbrmod;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public class ModAttachments {
	/**
	 * Per-player Stand data. Saved with the player and kept on death.
	 * It is NOT synced automatically; StandLogic.set() sends it to its owner only.
	 */
	public static final AttachmentType<StandData> STAND = AttachmentRegistry.<StandData>builder()
		.initializer(() -> StandData.EMPTY)
		.persistent(StandData.CODEC)
		.copyOnDeath()
		.buildAndRegister(SteelBallRun.id("stand"));

	public static void initialize() {
		// Loading this class registers the attachment.
	}
}
