package com.sbrmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything the mod remembers about a player.
 *
 * stand    - 0 none, 1 Tusk, 2 D4C
 * act      - Tusk: Act 1-4. D4C: ability mode 1-3.
 * summoned - whether the Stand is currently out
 * spinXp   - long-term Spin mastery (unlocks Tusk Acts, boosts damage)
 * charge   - current Spin charge, 0-100
 *
 * This is synced ONLY to the owning player (see ModAttachments), so other
 * players' clients never learn who is a Stand user.
 */
public record StandData(int stand, int act, boolean summoned, int spinXp, int charge) {
	public static final int NONE = 0;
	public static final int TUSK = 1;
	public static final int D4C = 2;

	public static final StandData EMPTY = new StandData(NONE, 1, false, 0, 0);

	public static final Codec<StandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("stand").forGetter(StandData::stand),
		Codec.INT.fieldOf("act").forGetter(StandData::act),
		Codec.BOOL.fieldOf("summoned").forGetter(StandData::summoned),
		Codec.INT.fieldOf("spin_xp").forGetter(StandData::spinXp),
		Codec.INT.fieldOf("charge").forGetter(StandData::charge)
	).apply(instance, StandData::new));

	public static final StreamCodec<ByteBuf, StandData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, StandData::stand,
		ByteBufCodecs.VAR_INT, StandData::act,
		ByteBufCodecs.BOOL, StandData::summoned,
		ByteBufCodecs.VAR_INT, StandData::spinXp,
		ByteBufCodecs.VAR_INT, StandData::charge,
		StandData::new
	);

	public StandData withStand(int value) {
		return new StandData(value, 1, false, spinXp, charge);
	}

	public StandData withAct(int value) {
		return new StandData(stand, value, summoned, spinXp, charge);
	}

	public StandData withSummoned(boolean value) {
		return new StandData(stand, act, value, spinXp, charge);
	}

	public StandData withSpinXp(int value) {
		return new StandData(stand, act, summoned, value, charge);
	}

	public StandData withCharge(int value) {
		return new StandData(stand, act, summoned, spinXp, Math.max(0, Math.min(100, value)));
	}

	/** Spin level 0-10, derived from spinXp. */
	public int spinLevel() {
		return Math.min(10, spinXp / 15);
	}
}
