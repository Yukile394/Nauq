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

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("Oto vurma: " + onOff(NauqConfig.autoHit)), b -> {
            NauqConfig.autoHit = !NauqConfig.autoHit;
            b.setMessage(Text.literal("Oto vurma: " + onOff(NauqConfig.autoHit)));
        }).dimensions(x, y, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Ses: " + onOff(NauqConfig.sound)), b -> {
            NauqConfig.sound = !NauqConfig.sound;
            b.setMessage(Text.literal("Ses: " + onOff(NauqConfig.sound)));
        }).dimensions(x, y + 24, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Vurus esigi: " + pct()), b -> {
            int i = 0;
            for (int k = 0; k < NauqConfig.THRESHOLDS.length; k++)
                if (Math.abs(NauqConfig.THRESHOLDS[k] - NauqConfig.critThreshold) < 0.001f) i = k;
            NauqConfig.critThreshold = NauqConfig.THRESHOLDS[(i + 1) % NauqConfig.THRESHOLDS.length];
            b.setMessage(Text.literal("Vurus esigi: " + pct()));
        }).dimensions(x, y + 48, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> close())
            .dimensions(x, y + 80, 200, 20).build());
    }

    private static String pct() { return Math.round(NauqConfig.critThreshold * 100) + "%"; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4 - 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        NauqConfig.save();
        if (client != null) client.setScreen(parent);
    }
}
