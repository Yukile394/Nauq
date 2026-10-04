package net.nauq;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class NauqClient implements ClientModInitializer {
    // Rahatlatici, orta seviye sesler
    private static final SoundEvent[] SOUNDS = {
        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
        SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
        SoundEvents.ENTITY_ALLAY_ITEM_GIVEN,
        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP
    };
    private static final float VOLUME = 0.55f;   // orta
    private static final float SWORD_FULL_TICKS = 12.5f; // netherite kilic tam sarj suresi (tick)

    private static int sinceHit = 99;

    private static KeyBinding toggleKey, soundKey, cycleKey, configKey;

    @Override
    public void onInitializeClient() {
        NauqConfig.load();
        toggleKey = reg("key.nauq.toggle", GLFW.GLFW_KEY_R);
        soundKey  = reg("key.nauq.sound", GLFW.GLFW_KEY_H);
        cycleKey  = reg("key.nauq.cycle", GLFW.GLFW_KEY_J);
        configKey = reg("key.nauq.config", GLFW.GLFW_KEY_K);
        ClientTickEvents.START_CLIENT_TICK.register(NauqClient::tick);
    }

    private static KeyBinding reg(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyBinding(id, InputUtil.Type.KEYSYM, key, "category.nauq"));
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        while (configKey.wasPressed()) {
            mc.setScreen(new NauqScreen(null));
        }
        while (toggleKey.wasPressed()) {
            NauqConfig.autoHit = !NauqConfig.autoHit;
            NauqConfig.save();
            p.sendMessage(Text.literal("Nauq oto vurma: " + (NauqConfig.autoHit ? "ACIK" : "KAPALI")), true);
            play(mc, NauqConfig.autoHit ? 1.3f : 0.8f, false);
        }
        while (soundKey.wasPressed()) {
            NauqConfig.sound = !NauqConfig.sound;
            NauqConfig.save();
            p.sendMessage(Text.literal("Nauq ses: " + (NauqConfig.sound ? "ACIK" : "KAPALI")), true);
            play(mc, 1.0f, true);
        }
        while (cycleKey.wasPressed()) {
            NauqConfig.soundIdx = (NauqConfig.soundIdx + 1) % SOUNDS.length;
            NauqConfig.save();
            p.sendMessage(Text.literal("Nauq ses secimi: " + (NauqConfig.soundIdx + 1) + "/" + SOUNDS.length), true);
            play(mc, 1.0f, false);
        }

        sinceHit++;

        if (!NauqConfig.autoHit) return;
        if (!p.isFallFlying()) return;

        // Sadece kilic elindeyken vur (fisek elindeyken vurma)
        if (!p.getMainHandStack().isOf(Items.NETHERITE_SWORD)) return;

        // Kilic bari esik kadar dolmadan vurma (ardarda hizli vurusu engeller)
        if ((sinceHit + 0.5f) / SWORD_FULL_TICKS < NauqConfig.critThreshold) return;

        // Hitbox kontrolu: crosshair'in hedefi
        Entity t = mc.targetedEntity;
        if (!(t instanceof LivingEntity le) || !le.isAlive() || !le.isAttackable()) return;

        // Sadece kritik atabiliyorsa vur
        if (!canCrit(p)) return;

        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        sinceHit = 0;
        play(mc, 1.1f, false);
    }

    // fallDistance: 1.21.0-1.21.1 float, sonrasi double. Intermediary adiyla bulunur.
    private static Field fallField;
    private static boolean fallInit;

    private static double fall(Entity e) {
        if (!fallInit) {
            fallInit = true;
            MappingResolver r = FabricLoader.getInstance().getMappingResolver();
            for (String d : new String[]{"F", "D"}) {
                try {
                    String n = r.mapFieldName("intermediary", "net.minecraft.class_1297", "field_6017", d);
                    Field f = Entity.class.getDeclaredField(n);
                    f.setAccessible(true);
                    fallField = f;
                    break;
                } catch (Throwable ignored) {}
            }
        }
        if (fallField != null) {
            try { return ((Number) fallField.get(e)).doubleValue(); } catch (Throwable ignored) {}
        }
        return e.getVelocity().y < 0 ? 1 : 0;
    }

    private static boolean canCrit(ClientPlayerEntity p) {
        return !p.isOnGround() && fall(p) > 0 && !p.isClimbing()
            && !p.isTouchingWater() && !p.hasStatusEffect(StatusEffects.BLINDNESS)
            && !p.hasVehicle() && !p.isSprinting();
    }

    private static void play(MinecraftClient mc, float pitch, boolean force) {
        if (!NauqConfig.sound && !force) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(SOUNDS[NauqConfig.soundIdx % SOUNDS.length], pitch, VOLUME));
    }
}
