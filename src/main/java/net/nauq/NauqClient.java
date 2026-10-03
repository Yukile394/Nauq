package net.nauq;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.nauq.mixin.PlayerInventoryAccessor;
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
    private static final int ARM_TICKS = 25;     // fisekten sonra aktif pencere
    private static final int HIT_INTERVAL = 13;  // netherite kilic tam sarj ~12.5 tick

    private static boolean enabled = false, soundOn = true, lastUse = false;
    private static int soundIdx = 0, armed = 0, fwSlot = -1, sinceHit = 99;

    private static KeyBinding toggleKey, soundKey, cycleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = reg("key.nauq.toggle", GLFW.GLFW_KEY_R);
        soundKey  = reg("key.nauq.sound", GLFW.GLFW_KEY_H);
        cycleKey  = reg("key.nauq.cycle", GLFW.GLFW_KEY_J);
        ClientTickEvents.START_CLIENT_TICK.register(NauqClient::tick);
    }

    private static KeyBinding reg(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyBinding(id, InputUtil.Type.KEYSYM, key, "category.nauq"));
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            armed = 0;
            p.sendMessage(Text.literal("Nauq: " + (enabled ? "ACIK" : "KAPALI")), true);
            play(mc, enabled ? 1.3f : 0.8f, false);
        }
        while (soundKey.wasPressed()) {
            soundOn = !soundOn;
            p.sendMessage(Text.literal("Nauq ses: " + (soundOn ? "ACIK" : "KAPALI")), true);
            play(mc, 1.0f, true);
        }
        while (cycleKey.wasPressed()) {
            soundIdx = (soundIdx + 1) % SOUNDS.length;
            p.sendMessage(Text.literal("Nauq ses secimi: " + (soundIdx + 1) + "/" + SOUNDS.length), true);
            play(mc, 1.0f, false);
        }

        sinceHit++;

        // Fisek kullanimi algila (tus basma kenari)
        boolean use = mc.options.useKey.isPressed();
        if (enabled && use && !lastUse && p.isFallFlying()
                && p.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
            armed = ARM_TICKS;
            fwSlot = slot(p);
        }
        lastUse = use;

        if (!enabled || armed <= 0) return;
        armed--;
        if (!p.isFallFlying() || sinceHit < HIT_INTERVAL) return;

        // Hitbox kontrolu: crosshair'in hedefi
        Entity t = mc.targetedEntity;
        if (!(t instanceof LivingEntity le) || !le.isAlive() || !le.isAttackable()) return;

        // Sadece kritik atabiliyorsa vur
        if (!canCrit(p)) return;

        int sword = findSword(p);
        if (sword < 0) return;

        int back = fwSlot >= 0 ? fwSlot : slot(p);
        slotOf(p, sword);               // kiliça gec
        mc.interactionManager.attackEntity(p, t);            // vur (slot sync icinde)
        p.swingHand(Hand.MAIN_HAND);
        slotOf(p, back);                // fisek slotuna don
        sinceHit = 0;
        play(mc, 1.1f, false);
    }

    private static int slot(ClientPlayerEntity p) {
        return ((PlayerInventoryAccessor) p.getInventory()).nauq$getSelectedSlot();
    }

    private static void slotOf(ClientPlayerEntity p, int s) {
        ((PlayerInventoryAccessor) p.getInventory()).nauq$setSelectedSlot(s);
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

    private static int findSword(ClientPlayerEntity p) {
        for (int i = 0; i < 9; i++)
            if (p.getInventory().getStack(i).isOf(Items.NETHERITE_SWORD)) return i;
        return -1;
    }

    private static void play(MinecraftClient mc, float pitch, boolean force) {
        if (!soundOn && !force) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(SOUNDS[soundIdx], pitch, VOLUME));
    }
}
