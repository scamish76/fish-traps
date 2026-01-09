package com.tech_monkey.fishtraps.screen;

import com.tech_monkey.fishtraps.FishTraps;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FishTrapScreen extends HandledScreen<FishTrapScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of(FishTraps.MOD_ID, "textures/gui/fish_trap_gui.png");

    public FishTrapScreen(FishTrapScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int guiX = (width - backgroundWidth) / 2;
        int guiY = (height - backgroundHeight) / 2;

        boolean openWater = this.handler.isOpenWater();

        // Circle position: upper right corner of GUI
        int circleX = guiX + backgroundWidth - 18; // 18 pixels from right edge
        int circleY = guiY + 8; // 8 pixels from top
        int radius = 5;

        // Draw filled circle
        drawFilledCircle(context, circleX, circleY, radius, openWater);

        // Tooltip when hovering over circle
        if (isMouseOverCircle(mouseX, mouseY, circleX, circleY, radius + 2)) {
            Text tooltip = openWater 
                ? Text.translatable("gui.fishtraps.open_water.yes") 
                : Text.translatable("gui.fishtraps.open_water.no");
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, boolean openWater) {
        // Color: bright green for open water, dark red for not
        int color = openWater ? 0xFF55FF55 : 0xFFDD3333;
        
        // Draw a filled circle by drawing horizontal lines
        for (int y = -radius; y <= radius; y++) {
            int width = (int) Math.sqrt(radius * radius - y * y);
            int startX = centerX - width;
            int endX = centerX + width;
            context.fill(startX, centerY + y, endX + 1, centerY + y + 1, color);
        }
        
        // Draw darker border for contrast
        int borderColor = openWater ? 0xFF338833 : 0xFF881111;
        drawCircleOutline(context, centerX, centerY, radius, borderColor);
    }

    private void drawCircleOutline(DrawContext context, int centerX, int centerY, int radius, int color) {
        // Draw circle outline using Bresenham's circle algorithm
        int x = 0;
        int y = radius;
        int d = 3 - 2 * radius;
        
        while (y >= x) {
            // Draw 8 octants
            drawPixel(context, centerX + x, centerY + y, color);
            drawPixel(context, centerX - x, centerY + y, color);
            drawPixel(context, centerX + x, centerY - y, color);
            drawPixel(context, centerX - x, centerY - y, color);
            drawPixel(context, centerX + y, centerY + x, color);
            drawPixel(context, centerX - y, centerY + x, color);
            drawPixel(context, centerX + y, centerY - x, color);
            drawPixel(context, centerX - y, centerY - x, color);
            
            x++;
            if (d > 0) {
                y--;
                d = d + 4 * (x - y) + 10;
            } else {
                d = d + 4 * x + 6;
            }
        }
    }

    private void drawPixel(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 1, y + 1, color);
    }

    private boolean isMouseOverCircle(int mouseX, int mouseY, int centerX, int centerY, int radius) {
        int dx = mouseX - centerX;
        int dy = mouseY - centerY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }
}