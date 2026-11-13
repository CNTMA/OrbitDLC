/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.module.Category;
import brokke.cc.core.module.Module;
import brokke.cc.core.module.ModuleManager;
import brokke.cc.core.setting.BindSetting;
import brokke.cc.core.setting.ColorSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.system.autobuy.AutoBuyConfig;
import brokke.cc.system.autobuy.AutoBuyContext;
import brokke.cc.system.bot.client.BotClient;
import brokke.cc.system.bot.gui.BotModuleView;
import brokke.cc.system.bot.modules.BotAutoBuy;
import brokke.cc.system.bot.modules.api.BotModule;
import brokke.cc.system.bot.net.BotAttach;
import brokke.cc.ui.clickgui.NewClickGuiScreen;
import brokke.cc.ui.clickgui.globals.GlobalsTab;
import brokke.cc.ui.clickgui.screen.ClickGuiInput;
import brokke.cc.ui.clickgui.screen.ContentPane;
import brokke.cc.ui.clickgui.screen.HitBoxes;
import brokke.cc.ui.clickgui.screen.TextFocus;
import brokke.cc.ui.clickgui.tab.AutoBuyItemsView;
import brokke.cc.ui.clickgui.tab.AutoBuyTab;
import brokke.cc.ui.clickgui.tab.AutoSborTab;
import brokke.cc.ui.clickgui.tab.BotRowView;
import brokke.cc.ui.clickgui.tab.BotsTab;
import brokke.cc.ui.clickgui.tab.CloudFriendsView;
import brokke.cc.ui.clickgui.tab.ConfigsTab;
import brokke.cc.ui.clickgui.tab.CosmeticsTab;
import brokke.cc.ui.clickgui.tab.DocsPage;
import brokke.cc.ui.clickgui.tab.EventsTab;
import brokke.cc.ui.clickgui.tab.FriendsTab;
import brokke.cc.ui.clickgui.tab.McpPage;
import brokke.cc.ui.clickgui.tab.ScriptsTab;
import brokke.cc.ui.clickgui.tab.SearchBar;
import brokke.cc.ui.clickgui.tab.Sidebar;
import brokke.cc.ui.clickgui.tab.ThemesTab;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.ui.clickgui.widget.CommonWidgets;
import brokke.cc.ui.clickgui.widget.ModuleCard;
import brokke.cc.ui.clickgui.widget.ScrollbarWidget;
import brokke.cc.ui.clickgui.widget.SettingWidgets;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class ClickGuiContext {
    public BotAutoBuy autoBuyOwner = AutoBuyContext.currentBotModule();
    public final Map<String, Module> botModuleViews = new HashMap<String, Module>();
    public final NewClickGuiScreen screen;
    public final HitBoxes hits;
    public final TextFocus focus;
    public final ClickGuiInput input;
    public final ContentPane content;
    public final ScrollbarWidget scrollbar;
    public final ModuleCard moduleCard;
    public final SettingWidgets settings;
    public final CommonWidgets widgets;
    public final Sidebar sidebar;
    public final SearchBar searchBar;
    public final ThemesTab themes;
    public final FriendsTab friends;
    public final CloudFriendsView cloudFriends;
    public final GlobalsTab globals;
    public final CosmeticsTab cosmetics;
    public final ConfigsTab configs;
    public final EventsTab events;
    public final ScriptsTab scripts;
    public final McpPage mcp;
    public final DocsPage docs;
    public final AutoBuyTab autoBuy;
    public final AutoSborTab autoSbor;
    public final AutoBuyItemsView autoBuyItems;
    public final BotsTab bots;
    public final BotRowView botRows;
    public final MarketTab market;
    public final Map<Object, Animation> fx = new HashMap<Object, Animation>();
    public boolean hologramRendering;
    public boolean closing = false;
    public boolean autoSborOpen = false;
    public int selected = 0;
    public float panelX;
    public float panelY;
    public float renderScale = 1.0f;
    public float renderSlide;
    public long lastFrameNanos = 0L;
    public float frameDt = 0.016666668f;
    public long switchTime = System.currentTimeMillis();
    public float scrollValue = 0.0f;
    public float scrollTarget = 0.0f;
    public float maxScroll = 0.0f;
    public Object hoverNow = null;
    public Object hoverLast = null;
    public Setting<?> dragSetting = null;
    public float dragScreenX;
    public float dragScreenW;
    public ColorSetting colorDragSetting;
    public int colorDragPart;
    public BindSetting bindingSetting = null;
    public StringSetting editingString = null;
    public Module bindingModule = null;
    public String configToastText = null;
    public Color configToastColor = Color.rgb(200, 205, 215);
    public long configToastTime = 0L;

    public ClickGuiContext(NewClickGuiScreen screen) {
        this.screen = screen;
        this.hits = new HitBoxes();
        this.focus = new TextFocus(this);
        this.input = new ClickGuiInput(this);
        this.content = new ContentPane(this);
        this.scrollbar = new ScrollbarWidget(this);
        this.moduleCard = new ModuleCard(this);
        this.settings = new SettingWidgets(this);
        this.widgets = new CommonWidgets(this);
        this.sidebar = new Sidebar(this);
        this.searchBar = new SearchBar(this);
        this.themes = new ThemesTab(this);
        this.friends = new FriendsTab(this);
        this.cloudFriends = new CloudFriendsView(this);
        this.globals = new GlobalsTab(this);
        this.cosmetics = new CosmeticsTab(this);
        this.configs = new ConfigsTab(this);
        this.events = new EventsTab(this);
        this.scripts = new ScriptsTab(this);
        this.mcp = new McpPage(this);
        this.docs = new DocsPage(this);
        this.autoBuy = new AutoBuyTab(this);
        this.autoSbor = new AutoSborTab(this);
        this.autoBuyItems = new AutoBuyItemsView(this);
        this.bots = new BotsTab(this);
        this.botRows = new BotRowView(this);
        this.market = new MarketTab(this);
    }

    public Minecraft minecraft() {
        return this.screen.client();
    }

    public float sliderDragFraction() {
        float mouseX = (float)(this.minecraft().mouseHandler.xpos() / 2.0);
        return Math.clamp((mouseX - this.dragScreenX) / Math.max(0.001f, this.dragScreenW), 0.0f, 1.0f);
    }

    public float getMouseX() {
        if (this.hologramRendering) {
            return -1000.0f;
        }
        float cx = DisplayUtil.width() * 0.5f;
        return ((float)(this.minecraft().mouseHandler.xpos() / 2.0) - cx) / this.renderScale + cx;
    }

    public float getMouseY() {
        if (this.hologramRendering) {
            return -1000.0f;
        }
        float cy = DisplayUtil.height() * 0.5f;
        return ((float)(this.minecraft().mouseHandler.ypos() / 2.0) - cy) / this.renderScale + cy - this.renderSlide;
    }

    public boolean hov(Object key, boolean on) {
        if (on) {
            this.hoverNow = key;
        }
        return on;
    }

    public float anim(Object key, boolean on, float speed) {
        Animation a = this.fx.computeIfAbsent(key, k -> {
            Animation n = new Animation(on ? 1.0f : 0.0f);
            n.setEasing(Easing.CUBIC_OUT);
            return n;
        });
        a.animate(on ? 1.0f : 0.0f, 1.0f, speed);
        return Math.clamp(a.value(), 0.0f, 1.0f);
    }

    public float smooth(float current, float target, float rate) {
        return current + (target - current) * (1.0f - (float)Math.exp(-rate * this.frameDt));
    }

    public boolean hovered(float x, float y, float w, float h) {
        float mx = this.getMouseX();
        float my = this.getMouseY();
        return !this.closing && mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void configToast(String text, Color color) {
        this.configToastText = text;
        this.configToastColor = color;
        this.configToastTime = System.currentTimeMillis();
    }

    public AutoBuyConfig autoBuyConfig() {
        return this.autoBuyOwner == null ? AutoBuyConfig.cfg() : this.autoBuyOwner.getEngine().getConfig();
    }

    public Object botGuiLock() {
        return this.autoBuyOwner != null && this.autoBuyOwner.bot != null ? this.autoBuyOwner.bot.getRenderStateLock() : this;
    }

    public List<Module> guiModules() {
        BotClient bot;
        BotClient botClient = bot = this.autoBuyOwner == null ? null : this.autoBuyOwner.bot;
        if (bot == null || BotAttach.isAttached(bot)) {
            return new ArrayList<Module>(ModuleManager.all());
        }
        ArrayList<Module> modules = new ArrayList<Module>();
        for (BotModule nativeModule : bot.getModules().getModules()) {
            Module view = this.botModuleViews.computeIfAbsent(nativeModule.getName(), name -> {
                Category category = Category.MISC;
                for (Module template : ModuleManager.all()) {
                    if (!template.name().replace(" ", "").equalsIgnoreCase((String)name)) continue;
                    category = template.category();
                    break;
                }
                return new BotModuleView(nativeModule, category);
            });
            modules.add(view);
        }
        return modules;
    }

    public void saveAutoBuy() {
        this.autoBuyConfig().saveConfig();
    }
}

