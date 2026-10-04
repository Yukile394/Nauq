package net.nauq;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class NauqScreen extends Screen {
    private final Screen parent;

    public NauqScreen(Screen parent) {
        super(Text.literal("Nauq Ayarlar"));
        this.parent = parent;
    }

    private static String onOff(boolean b) { return b ? "ACIK" : "KAPALI"; }
    private static String pct() { return Math.round(NauqConfig.critThreshold * 100) + "%"; }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 6 + 10;

        // Mod ana ac/kapa (R tusuyla ayni)
        addDrawableChild(ButtonWidget.builder(Text.literal("Nauq: " + onOff(NauqConfig.enabled)), b -> {
            NauqConfig.enabled = !NauqConfig.enabled;
            b.setMessage(Text.literal("Nauq: " + onOff(NauqConfig.enabled)));
        }).dimensions(x, y, 200, 20).build());

        // Vurus esigi (kilic bari ne kadar dolunca vursun)
        addDrawableChild(ButtonWidget.builder(Text.literal("Vurus esigi: " + pct()), b -> {
            int i = 0;
            for (int k = 0; k < NauqConfig.THRESHOLDS.length; k++)
                if (Math.abs(NauqConfig.THRESHOLDS[k] - NauqConfig.critThreshold) < 0.001f) i = k;
            NauqConfig.critThreshold = NauqConfig.THRESHOLDS[(i + 1) % NauqConfig.THRESHOLDS.length];
            b.setMessage(Text.literal("Vurus esigi: " + pct()));
        }).dimensions(x, y + 24, 200, 20).build());

        // Ses ac/kapa
        addDrawableChild(ButtonWidget.builder(Text.literal("Ses: " + onOff(NauqConfig.sound)), b -> {
            NauqConfig.sound = !NauqConfig.sound;
            b.setMessage(Text.literal("Ses: " + onOff(NauqConfig.sound)));
        }).dimensions(x, y + 48, 200, 20).build());

        // Ses secimi
        addDrawableChild(ButtonWidget.builder(Text.literal(soundLabel()), b -> {
            NauqConfig.soundIdx = (NauqConfig.soundIdx + 1) % NauqClient.SOUNDS.length;
            b.setMessage(Text.literal(soundLabel()));
        }).dimensions(x, y + 72, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> close())
            .dimensions(x, y + 104, 200, 20).build());
    }

    private static String soundLabel() {
        return "Ses secimi: " + ((NauqConfig.soundIdx % NauqClient.SOUNDS.length) + 1) + "/" + NauqClient.SOUNDS.length;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 6 - 10, 0xFFFFFF);
    }

    @Override
    public void close() {
        NauqConfig.save();
        if (client != null) client.setScreen(parent);
    }
}
