package net.typho.vibrancy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class AlphaWarningScreen extends Screen {
	private static final Text TITLE_TEXT = Text.translatable("screen.vibrancy.alpha.title");
	private final Runnable onClose;
	@Nullable
	private NarratedMultilineTextWidget textWidget;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 90, 33);

	public AlphaWarningScreen(Runnable onClose) {
		super(TITLE_TEXT);
		this.onClose = onClose;
	}

	@Override
	public void init() {
		DirectionalLayoutWidget layoutWidget = layout.addBody(DirectionalLayoutWidget.vertical());
		layoutWidget.getMainPositioner().alignHorizontalCenter().margin(4);
		textWidget = layoutWidget.add(new NarratedMultilineTextWidget(width, title, textRenderer), positioner -> positioner.margin(8));

		layout.addFooter(ButtonWidget.builder(ScreenTexts.CONTINUE, button -> close()).build());
		layout.forEachChild(this::addDrawableChild);
		initTabNavigation();
	}

	@Override
	protected void initTabNavigation() {
		if (textWidget != null) {
			textWidget.initMaxWidth(width);
		}

		layout.refreshPositions();
	}

	@Override
	public void close() {
		Vibrancy.SEEN_ALPHA_TEXT = true;
		onClose.run();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
        context.drawTexture(Vibrancy.LOGO_TEXTURE, width / 2 - 128, 30, 0.0F, 0.0F, 256, 42, 256, 42);
		RenderSystem.disableBlend();
	}
}
