package net.enflky.antighost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class AntiGhost implements ClientModInitializer {
    static final String MODID = "antighost";
    private static KeyBinding requestBlocks;

    @Override
    public void onInitializeClient() {
        final String category = "key.categories.antighost";

        requestBlocks = new KeyBinding("key.antighost.reveal", GLFW.GLFW_KEY_G, category);
        KeyBindingHelper.registerKeyBinding(requestBlocks);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (requestBlocks.wasPressed()) {
                this.execute(client);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("ghost").executes(context -> {
                        this.execute(context.getSource().getClient());
                        return 1;
                    })
            );
        });
    }

    public void execute(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        ClientPlayNetworkHandler conn = mc.getNetworkHandler();
        if (conn == null) return;

        BlockPos pos = player.getBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos targetPos = pos.add(dx, dy, dz);
                    PlayerActionC2SPacket packet = new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                            targetPos,
                            Direction.UP
                    );
                    conn.sendPacket(packet);
                }
            }
        }
        player.sendMessage(Text.translatable("msg.antighost.request"), false);
    }
}