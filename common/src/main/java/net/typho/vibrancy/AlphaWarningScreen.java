package net.typho.vibrancy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlphaWarningScreen extends Screen {
	private static final Component TITLE_TEXT = Component.translatable("screen.vibrancy.alpha.title");
	private final Runnable onClose;
	@Nullable
	private FocusableTextWidget textWidget;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 90, 33);

	public AlphaWarningScreen(Runnable onClose) {
		super(TITLE_TEXT);
		this.onClose = onClose;
	}

	@Override
	public void init() {
		LinearLayout layoutWidget = layout.addToContents(LinearLayout.vertical());
		layoutWidget.defaultCellSetting().alignHorizontallyCenter().padding(4);
		textWidget = layoutWidget.addChild(new FocusableTextWidget(width, title, font), positioner -> positioner.padding(8));

		layout.addToFooter(Button.builder(CommonComponents.GUI_CONTINUE, button -> onClose()).build());
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	@Override
	protected void repositionElements() {
		if (textWidget != null) {
			textWidget.containWithin(width);
		}

		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		Vibrancy.SEEN_ALPHA_TEXT = true;
		onClose.run();
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		graphics.setColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
        graphics.blit(Vibrancy.LOGO_TEXTURE, width / 2 - 128, 30, 0.0F, 0.0F, 256, 42, 256, 42);
		RenderSystem.disableBlend();
	}
}
