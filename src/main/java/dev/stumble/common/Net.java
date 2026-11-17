package dev.stumble.common;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet channels. Players' own ragdolls are run by their own game and announced to the server,
 * which tells everyone nearby. Mob ragdolls are run by the server, and every nearby game runs a
 * copy from the same seed so they look the same.
 */
public final class Net {
    // game -> server
    /** int targetId, or -1 to push thin air (others still see the animation) */
    public static final ResourceLocation PUSH = id("push");
    /** int targetId, float speed: I walked into this mob */
    public static final ResourceLocation BUMP = id("bump");
    /** I went down: long seed, byte mode, 3 floats velocity, 3 floats blow, float yaw */
    public static final ResourceLocation SELF_START = id("self_start");
    /** I lost my balance. */
    public static final ResourceLocation SELF_FALL = id("self_fall");
    /** I am getting up: float yaw, byte ticks */
    public static final ResourceLocation SELF_END = id("self_end");

    // server -> game
    /** int entityId, long seed, byte mode, 3 floats velocity, 3 floats blow, float yaw */
    public static final ResourceLocation START = id("start");
    /** int entityId: stop trying to balance */
    public static final ResourceLocation FALL = id("fall");
    /** int entityId, 3 doubles position, float yaw, byte ticks: get up there */
    public static final ResourceLocation END = id("end");
    /** int entityId, 3 floats: a blow to a body that is already down */
    public static final ResourceLocation IMPULSE = id("impulse");
    /** byte mode, 3 floats: someone pushed you */
    public static final ResourceLocation KNOCKDOWN = id("knockdown");
    /** int playerId: that player just shoved; play the two-handed push on their model */
    public static final ResourceLocation PUSH_ANIM = id("push_anim");

    private Net() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("stumble", path);
    }

    public static FriendlyByteBuf start(int entityId, long seed, int mode, double vx, double vy, double vz,
                                        double jx, double jy, double jz, float yaw) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        writeBody(buf, seed, mode, vx, vy, vz, jx, jy, jz, yaw);
        return buf;
    }

    public static void writeBody(FriendlyByteBuf buf, long seed, int mode, double vx, double vy, double vz,
                                 double jx, double jy, double jz, float yaw) {
        buf.writeLong(seed);
        buf.writeByte(mode);
        buf.writeFloat((float) vx);
        buf.writeFloat((float) vy);
        buf.writeFloat((float) vz);
        buf.writeFloat((float) jx);
        buf.writeFloat((float) jy);
        buf.writeFloat((float) jz);
        buf.writeFloat(yaw);
    }

    public static FriendlyByteBuf id(int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        return buf;
    }

    public static FriendlyByteBuf end(int entityId, double x, double y, double z, float yaw, int ticks) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeByte(ticks);
        return buf;
    }

    public static FriendlyByteBuf knockdown(int mode, double jx, double jy, double jz) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeByte(mode);
        buf.writeFloat((float) jx);
        buf.writeFloat((float) jy);
        buf.writeFloat((float) jz);
        return buf;
    }

    public static FriendlyByteBuf vec(int entityId, double x, double y, double z) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        buf.writeFloat((float) x);
        buf.writeFloat((float) y);
        buf.writeFloat((float) z);
        return buf;
    }

    /** Body data as read from a START or SELF_START packet. */
    public record Body(long seed, int mode, float vx, float vy, float vz, float jx, float jy, float jz, float yaw) {
        public static Body read(FriendlyByteBuf buf) {
            return new Body(buf.readLong(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        /** Clamp what a game sent us to sane values. */
        public Body sane() {
            return new Body(seed, Math.max(0, Math.min(3, mode)), c(vx), c(vy), c(vz), c(jx), c(jy), c(jz),
                    Float.isFinite(yaw) ? yaw : 0f);
        }

        private static float c(float v) {
            return Float.isFinite(v) ? Math.max(-3f, Math.min(3f, v)) : 0f;
        }
    }
}
