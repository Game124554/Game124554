package dev.stumble.client;

import dev.stumble.common.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Receives the server's news about bodies. Handlers run on the network thread, so hop to the game thread. */
public final class ClientNet {
    private ClientNet() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Net.START, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            Net.Body body = Net.Body.read(buf).sane();
            client.execute(() -> RagdollManager.remoteStart(id, body));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.FALL, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            client.execute(() -> RagdollManager.remoteFall(id));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.END, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            float yaw = buf.readFloat();
            int ticks = buf.readByte();
            client.execute(() -> RagdollManager.remoteEnd(id, x, y, z, yaw, ticks));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.IMPULSE, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            float jx = buf.readFloat(), jy = buf.readFloat(), jz = buf.readFloat();
            client.execute(() -> RagdollManager.remoteImpulse(id, jx, jy, jz));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.PUSH_ANIM, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            client.execute(() -> {
                if (client.level != null) {
                    net.minecraft.world.entity.Entity e = client.level.getEntity(id);
                    if (e != null) {
                        PushAnimation.start(e);
                    }
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.KNOCKDOWN, (client, handler, buf, sender) -> {
            int mode = buf.readByte();
            float jx = buf.readFloat(), jy = buf.readFloat(), jz = buf.readFloat();
            client.execute(() -> RagdollManager.knockedDown(mode, jx, jy, jz));
        });
    }
}
