/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.renderpearl.api.textures.FilterMode
 *  com.mojang.renderpearl.api.textures.GpuSampler
 *  com.mojang.renderpearl.api.textures.GpuTextureView
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.navigation.ScreenRectangle
 *  net.minecraft.client.gui.render.TextureSetup
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.client.renderer.RenderPipelines
 *  net.minecraft.client.renderer.state.gui.BlitRenderState
 *  net.minecraft.client.renderer.state.gui.GuiElementRenderState
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Input
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 */
package brokke.cc.ui.clickgui;

import brokke.cc.core.module.Category;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.system.bot.client.BotClient;
import brokke.cc.system.bot.client.HeadlessBots;
import brokke.cc.system.bot.gui.BotControlScreen;
import brokke.cc.system.bot.modules.BotAutoBuy;
import brokke.cc.system.bot.net.BotPlayHandler;
import brokke.cc.system.bot.view.BotWorldView;
import brokke.cc.system.bot.world.BotPlayer;
import brokke.cc.ui.clickgui.ClickGuiWorldHologram;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.ClickGuiLayout;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.PanelBounds;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Input;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class NewClickGuiScreen
extends Screen
implements PanelBounds {
    public static String savedBotExpanded;
    public final Animation botPreviewAnim = new Animation();
    public static boolean botPreviewEnabled;
    public long botPreviewFrameAt;
    public BotControlScreen botPreviewHud;
    public BotClient botPreviewClient;
    public BotWorldView botPreview;
    static final float W = 400.0f;
    static final float H = 250.0f;
    private final Screen previousScreen;
    private final Animation openAnim = new Animation();
    private final ClickGuiContext ctx = new ClickGuiContext(this);
    private static int savedSelected;
    private static float savedScroll;
    private static float savedMaxScroll;
    private static String savedSearch;
    private static int savedCosmeticTab;
    private static int savedFriendsTab;
    private static int savedEditingSlot;

    public NewClickGuiScreen(Screen previousScreen) {
        super((Component)Component.literal((String)"ClickGui"));
        ClickGuiWorldHologram.cancel();
        this.previousScreen = previousScreen;
        this.ctx.selected = Math.clamp((long)savedSelected, 0, ClickGuiLayout.CATEGORIES.size() - 1);
        this.ctx.scrollValue = this.ctx.scrollTarget = savedScroll;
        this.ctx.maxScroll = savedMaxScroll;
        this.ctx.searchBar.search.append(savedSearch);
        this.ctx.searchBar.lastQuery = savedSearch.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < this.ctx.searchBar.search.length(); ++i) {
            this.ctx.searchBar.charTimes.add(0L);
        }
        this.ctx.cosmetics.cosmeticTab = savedCosmeticTab;
        this.ctx.friends.friendsTab = savedFriendsTab;
        this.ctx.themes.editingSlot = savedEditingSlot;
        this.ctx.botRows.botExpanded = savedBotExpanded;
        this.ctx.sidebar.selectAnim.set(this.ctx.selected);
        this.ctx.sidebar.selectAnim.setEasing(Easing.SPRING_SOFT);
        ClientSound.ui(ClientSound.Ui.OPEN);
    }

    public NewClickGuiScreen(Screen previousScreen, Category category) {
        this(previousScreen);
        int index = ClickGuiLayout.CATEGORIES.indexOf((Object)category);
        if (index >= 0) {
            this.ctx.selected = index;
            this.ctx.sidebar.selectAnim.set(index);
            this.ctx.maxScroll = 0.0f;
            this.ctx.scrollTarget = 0.0f;
            this.ctx.scrollValue = 0.0f;
            this.ctx.searchBar.search.setLength(0);
            this.ctx.searchBar.charTimes.clear();
            this.ctx.searchBar.lastQuery = "";
        }
    }

    public NewClickGuiScreen(Screen previousScreen, Category category, BotAutoBuy owner) {
        this(previousScreen, category);
        this.ctx.autoBuyOwner = owner;
    }

    public boolean isTextInputFocused() {
        return this.ctx.focus.searchFocused || this.ctx.focus.friendFocused || this.ctx.focus.configFocused || this.ctx.focus.abField != null;
    }

    public Category getSelectedCategory() {
        return this.ctx.selected >= 0 && this.ctx.selected < ClickGuiLayout.CATEGORIES.size() ? ClickGuiLayout.CATEGORIES.get(this.ctx.selected) : null;
    }

    @Override
    public ScreenRectangle brokke$panelBounds() {
        if (this.minecraft == null) {
            return null;
        }
        float factor = 2.0f / (float)this.minecraft.getWindow().getGuiScale();
        float prog = GuiDraw.guiAnimationsEnabled() ? Math.clamp(this.openAnim.value(), 0.0f, 1.0f) : 1.0f;
        float scale = ClickGuiLayout.guiScale() * (0.92f + 0.08f * prog);
        float px = (DisplayUtil.width() - 400.0f * scale) * 0.5f;
        float py = (DisplayUtil.height() - 250.0f * scale) * 0.5f + (1.0f - prog) * 8.0f * scale;
        return new ScreenRectangle((int)Math.floor(px * factor), (int)Math.floor(py * factor), (int)Math.ceil(400.0f * scale * factor), (int)Math.ceil(250.0f * scale * factor));
    }

    public Screen brokke$previousScreen() {
        return this.previousScreen;
    }

    public boolean isPauseScreen() {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removed() {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            savedSelected = this.ctx.selected;
            savedScroll = this.ctx.scrollValue;
            savedMaxScroll = this.ctx.maxScroll;
            savedSearch = this.ctx.searchBar.search.toString();
            savedCosmeticTab = this.ctx.cosmetics.cosmeticTab;
            savedFriendsTab = this.ctx.friends.friendsTab;
            savedEditingSlot = this.ctx.themes.editingSlot;
            savedBotExpanded = this.ctx.botRows.botExpanded;
            this.closeBotPreview();
            if (this.ctx.autoBuyOwner != null && this.ctx.autoBuyOwner.bot != null) {
                BotClient bot = this.ctx.autoBuyOwner.bot;
                bot.execute(() -> {
                    HeadlessBots.persistProfile(bot);
                    HeadlessBots.savePersistentState();
                });
                this.ctx.saveAutoBuy();
            }
            this.ctx.dragSetting = null;
            this.ctx.scrollbar.scrollDragging = false;
            this.ctx.themes.pickerDrag = 0;
            this.ctx.colorDragSetting = null;
            this.ctx.colorDragPart = 0;
            this.ctx.focus.clear();
            ConfigManager.saveConfig("backup");
            super.removed();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onClose() {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            this.ctx.focus.clear();
            if (!this.ctx.closing) {
                ClientSound.ui(ClientSound.Ui.CLOSE);
            }
            if (GuiDraw.guiAnimationsEnabled() && this.previousScreen == null && this.minecraft != null && this.minecraft.level != null && this.botPreviewClient == null) {
                ClickGuiWorldHologram.spawn(this);
                this.finishClose();
                return;
            }
            this.ctx.closing = true;
        }
    }

    private void finishClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.previousScreen);
        }
    }

    public float closingScale() {
        return this.ctx.renderScale;
    }

    public Minecraft client() {
        return this.minecraft;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            return this.ctx.input.mouseClicked(event, doubleClick);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean charTyped(CharacterEvent event) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            return this.ctx.input.charTyped(event) || super.charTyped(event);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean keyPressed(KeyEvent event) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            return this.ctx.input.keyPressed(event) || super.keyPressed(event);
        }
    }

    public void openAutoSbor() {
        this.ctx.autoSborOpen = true;
        this.ctx.autoSbor.reset();
        this.ctx.searchBar.search.setLength(0);
        this.ctx.searchBar.charTimes.clear();
        this.ctx.searchBar.lastQuery = "";
        this.ctx.maxScroll = 0.0f;
        this.ctx.scrollTarget = 0.0f;
        this.ctx.scrollValue = 0.0f;
        this.ctx.switchTime = System.currentTimeMillis();
        this.ctx.content.titleAnim.set(0.0f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean mouseReleased(MouseButtonEvent event) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            this.ctx.input.mouseReleased();
            return super.mouseReleased(event);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            return this.ctx.input.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            float scale;
            boolean animated;
            if (this.previousScreen != null && !(this.previousScreen instanceof BotControlScreen)) {
                this.previousScreen.extractRenderState(graphics, -1000, -1000, delta);
                graphics.nextStratum();
            }
            if (!(animated = GuiDraw.guiAnimationsEnabled()) && !this.ctx.closing) {
                this.openAnim.set(1.0f);
            } else {
                this.openAnim.animate(this.ctx.closing ? 0.0f : 1.0f, delta, animated ? 2.0f : 22.5f, this.ctx.closing ? Easing.CUBIC_IN_OUT : Easing.EXPO_OUT);
            }
            float prog = Math.clamp(this.openAnim.value(), 0.0f, 1.0f);
            if (this.ctx.closing && this.openAnim.isDone()) {
                this.finishClose();
                return;
            }
            this.ctx.renderScale = scale = ClickGuiLayout.guiScale() * (animated ? 0.92f + 0.08f * prog : 1.0f);
            this.ctx.renderSlide = animated ? (1.0f - prog) * 8.0f : 0.0f;
            float cx = DisplayUtil.width() * 0.5f;
            float cy = DisplayUtil.height() * 0.5f;
            this.ctx.panelX = (DisplayUtil.width() - 400.0f) * 0.5f;
            this.ctx.panelY = (DisplayUtil.height() - 250.0f) * 0.5f;
            BotClient previewClient = this.selectedPreviewBot();
            boolean showPreview = botPreviewEnabled && previewClient != null;
            this.botPreviewAnim.animate(showPreview ? 1.0f : 0.0f, delta, 7.0f, Easing.CUBIC_IN_OUT);
            float previewProgress = Math.clamp(this.botPreviewAnim.value(), 0.0f, 1.0f);
            if (!showPreview) {
                previewClient = this.botPreviewClient;
            }
            float previewWidth = Math.clamp(DisplayUtil.width() - 400.0f - 26.0f, 64.0f, 156.0f);
            float previewPanelX = Math.max(8.0f, (DisplayUtil.width() - 400.0f - previewWidth - 10.0f) * 0.5f);
            this.ctx.panelX += (previewPanelX - this.ctx.panelX) * previewProgress;
            graphics.pose().pushMatrix();
            graphics.pose().scale(2.0f / (float)this.minecraft.getWindow().getGuiScale());
            graphics.pose().translate(cx, cy);
            graphics.pose().scale(scale);
            graphics.pose().translate(-cx, -cy + this.ctx.renderSlide);
            float px = this.ctx.panelX;
            float py = this.ctx.panelY;
            GuiDraw.renderPanelBackground(graphics, px, py, 400.0f, 250.0f, prog);
            this.ctx.hoverNow = null;
            this.ctx.sidebar.renderSidebar(graphics, px, py, prog, delta);
            this.ctx.content.renderContent(graphics, px, py, prog, delta);
            this.ctx.sidebar.renderProfile(graphics, px, py, prog);
            if (previewProgress > 0.001f) {
                this.renderBotPreview(graphics, previewClient, px + 400.0f + 10.0f + (1.0f - previewProgress) * 12.0f, py, previewWidth, prog * previewProgress);
            } else {
                this.closeBotPreview();
            }
            if (this.ctx.hoverNow != null && !this.ctx.hoverNow.equals(this.ctx.hoverLast) && !this.ctx.closing) {
                ClientSound.ui(ClientSound.Ui.HOVER);
            }
            this.ctx.hoverLast = this.ctx.hoverNow;
            graphics.pose().popMatrix();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void renderHologram(GuiGraphicsExtractor graphics, float alpha) {
        Object object = this.ctx.botGuiLock();
        synchronized (object) {
            this.ctx.hologramRendering = true;
            try {
                GuiDraw.renderPanelBackground(graphics, 0.0f, 0.0f, 400.0f, 250.0f, alpha);
                this.ctx.sidebar.renderSidebar(graphics, 0.0f, 0.0f, 1.0f, 0.0f);
                this.ctx.sidebar.renderProfile(graphics, 0.0f, 0.0f, 1.0f);
                this.ctx.content.renderContent(graphics, 0.0f, 0.0f, 1.0f, 0.0f);
            }
            finally {
                this.ctx.hologramRendering = false;
            }
        }
    }

    private BotClient selectedPreviewBot() {
        BotClient selectedBot;
        if (this.getSelectedCategory() != Category.BOTS) {
            return null;
        }
        BotClient botClient = selectedBot = this.ctx.botRows.botExpanded == null ? null : HeadlessBots.get(this.ctx.botRows.botExpanded);
        if (selectedBot != null && selectedBot.isJoined()) {
            return selectedBot;
        }
        for (BotClient client : HeadlessBots.all()) {
            if (!client.isJoined()) continue;
            return client;
        }
        return null;
    }

    private void closeBotPreview() {
        if (this.botPreview != null) {
            this.botPreview.close();
        }
        this.botPreview = null;
        this.botPreviewClient = null;
        this.botPreviewHud = null;
        this.botPreviewFrameAt = 0L;
    }

    private void renderBotPreview(GuiGraphicsExtractor graphics, BotClient client, float x, float y, float w, float alpha) {
        if (client == null) {
            this.closeBotPreview();
            return;
        }
        if (this.botPreviewClient != client) {
            this.closeBotPreview();
            this.botPreviewClient = client;
            this.botPreview = new BotWorldView(client);
            this.botPreviewHud = new BotControlScreen(client.getName());
        }
        float imageHeight = (w - 8.0f) * 9.0f / 16.0f;
        float height = imageHeight + 36.0f;
        RoundedRect.of(x, y, w, height).radius(5.0f).color(Color.rgba(12, 13, 18, Math.round(230.0f * alpha))).render(graphics);
        Text.of(Fonts.MEDIUM, client.getName(), x + 6.0f, y + 6.0f, 5.0f).color(Color.rgb(225, 230, 240).withAlpha(alpha)).render(graphics);
        DisplayUtil.flush();
        long now = System.currentTimeMillis();
        if (!this.ctx.closing && botPreviewEnabled && now - this.botPreviewFrameAt >= 66L) {
            this.botPreviewFrameAt = now;
            this.botPreview.requestRender(640, 360);
        }
        if (this.botPreview.getColorTexture() != null) {
            graphics.guiRenderState.addGuiElement((GuiElementRenderState)new BlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture((GpuTextureView)this.botPreview.getColorTexture(), (GpuSampler)RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)), new Matrix3x2f((Matrix3x2fc)graphics.pose()), Math.round(x + 4.0f), Math.round(y + 17.0f), Math.round(x + w - 4.0f), Math.round(y + 17.0f + imageHeight), 0.0f, 1.0f, 1.0f, 0.0f, Color.WHITE.withAlpha(alpha).argb(), graphics.scissorStack.peek()));
            graphics.nextStratum();
        } else {
            Text.of(Fonts.MEDIUM, "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043c\u0438\u0440\u0430...", x + 6.0f, y + 24.0f, 4.5f).color(Color.rgb(150, 160, 175).withAlpha(alpha)).render(graphics);
        }
        this.botPreviewHud.renderPreviewOverlay(graphics, client, x + 4.0f, y + 17.0f, w - 8.0f, imageHeight, alpha);
        BotPlayer player = client.getPlayer();
        BotPlayHandler handler = client.getPlayHandler();
        String action = "\u041e\u0436\u0438\u0434\u0430\u0435\u0442";
        if (handler != null && handler.hasOpenScreen()) {
            action = handler.getCurrentScreenTitle().getString();
        } else if (player != null && player.input != null) {
            boolean moving;
            Input input = player.input.keyPresses;
            boolean bl = moving = input.forward() || input.backward() || input.left() || input.right();
            action = moving ? (input.jump() ? "\u0411\u0435\u0436\u0438\u0442 \u0438 \u043f\u0440\u044b\u0433\u0430\u0435\u0442" : "\u0414\u0432\u0438\u0433\u0430\u0435\u0442\u0441\u044f") : (input.jump() ? "\u041f\u0440\u044b\u0433\u0430\u0435\u0442" : "\u0421\u0442\u043e\u0438\u0442");
        }
        Text.of(Fonts.MEDIUM, action, x + 6.0f, y + imageHeight + 23.0f, 4.5f).color(Color.rgb(155, 175, 195).withAlpha(alpha)).render(graphics);
    }

    public void selectCategory(Category category) {
        int index = ClickGuiLayout.CATEGORIES.indexOf((Object)category);
        if (index < 0) {
            return;
        }
        this.ctx.searchBar.clearSearch();
        this.ctx.focus.setSearchFocus(false);
        this.ctx.focus.setAbField(null, "");
        if (index != this.ctx.selected) {
            ClientSound.ui(index > this.ctx.selected ? ClientSound.Ui.CATEGORY_NEXT : ClientSound.Ui.CATEGORY_PREV);
        }
        this.ctx.selected = index;
        this.ctx.scrollTarget = 0.0f;
        this.ctx.scrollValue = 0.0f;
        this.ctx.maxScroll = 0.0f;
        this.ctx.scrollbar.thumbYValue = Float.NaN;
        this.ctx.friends.friendsSwitchTime = this.ctx.switchTime = System.currentTimeMillis();
        this.ctx.cosmetics.cosmeticSwitchTime = this.ctx.switchTime;
        this.ctx.autoBuy.abSwitchTime = this.ctx.switchTime;
        this.ctx.bots.botSwitchTime = this.ctx.switchTime;
        this.ctx.content.titleAnim.set(0.0f);
    }

    static {
        botPreviewEnabled = true;
        savedSearch = "";
    }
}

