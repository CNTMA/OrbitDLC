/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab.market;

import brokke.cc.system.market.MarketClient;
import brokke.cc.system.market.MarketDetails;
import brokke.cc.system.market.MarketInstaller;
import brokke.cc.system.market.MarketState;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.tab.market.MarketAdminView;
import brokke.cc.ui.clickgui.tab.market.MarketCatalogView;
import brokke.cc.ui.clickgui.tab.market.MarketCodeView;
import brokke.cc.ui.clickgui.tab.market.MarketPublishView;
import brokke.cc.ui.clickgui.tab.market.MarketScriptView;
import brokke.cc.ui.clickgui.tab.market.MarketWalletView;
import brokke.cc.ui.clickgui.tab.market.MarketWidgets;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MarketTab
extends GuiComponent {
    public static final int CATALOG = 0;
    public static final int SCRIPT = 1;
    public static final int PUBLISH = 2;
    public static final int WALLET = 3;
    public static final int ADMIN = 4;
    private static final Map<String, String> FIELDS = new HashMap<String, String>();
    private static int page = 0;
    public final MarketWidgets ui;
    public final MarketCatalogView catalog;
    public final MarketScriptView script;
    public final MarketCodeView code;
    public final MarketPublishView publish;
    public final MarketWalletView wallet;
    public final MarketAdminView admin;
    public long pageSwitchTime = 0L;
    private float tabAnim = MarketTab.tabIndex(page);
    private long lastRender = 0L;
    private String toastText;
    private Color toastColor = Color.rgb(200, 205, 215);
    private long toastTime;

    public MarketTab(ClickGuiContext ctx) {
        super(ctx);
        this.ui = new MarketWidgets(ctx);
        this.catalog = new MarketCatalogView(ctx, this);
        this.script = new MarketScriptView(ctx, this);
        this.code = new MarketCodeView(ctx);
        this.publish = new MarketPublishView(ctx, this);
        this.wallet = new MarketWalletView(ctx, this);
        this.admin = new MarketAdminView(ctx, this);
        if (page == 1) {
            page = 0;
        }
    }

    public static String field(String id) {
        return FIELDS.getOrDefault(id, "");
    }

    public static void setField(String id, String value) {
        FIELDS.put(id, value == null ? "" : value);
    }

    public static boolean isNumericField(String id) {
        return id.equals("market:price") || id.equals("market:amount") || id.equals("market:admin-amount");
    }

    public static boolean isNickField(String id) {
        return id.equals("market:target");
    }

    public static int fieldLimit(String id) {
        return switch (id) {
            case "market:name" -> 32;
            case "market:description" -> 200;
            case "market:long" -> 4000;
            case "market:tags" -> 90;
            case "market:price" -> 7;
            case "market:version" -> 16;
            case "market:changelog" -> 500;
            case "market:target" -> 16;
            case "market:amount" -> 9;
            case "market:note" -> 100;
            case "market:review" -> 500;
            case "market:admin-q" -> 64;
            case "market:admin-amount" -> 9;
            case "market:admin-note" -> 100;
            default -> 48;
        };
    }

    public void toast(String text, boolean ok) {
        this.toastText = text;
        this.toastColor = ok ? MarketWidgets.GREEN : MarketWidgets.RED;
        this.toastTime = System.currentTimeMillis();
    }

    public void openPage(int target) {
        if (page == target) {
            return;
        }
        this.ctx.focus.setAbField(null, "");
        page = target;
        this.pageSwitchTime = System.currentTimeMillis();
        this.ctx.scrollTarget = 0.0f;
        if (target == 3) {
            this.wallet.load();
        }
        if (target == 2) {
            this.publish.onOpen();
        }
        if (target == 4) {
            this.admin.load();
        }
    }

    public void openScript(String id) {
        this.script.open(id);
        page = -1;
        this.openPage(1);
    }

    private static int tabIndex(int p) {
        return p == 4 ? 3 : (p == 3 ? 2 : (p == 2 ? 1 : 0));
    }

    private void onOpen() {
        MarketInstaller.rescan();
        MarketState.refresh(r -> {
            if (!r.ok()) {
                this.toast(r.message(), false);
            }
        });
        this.catalog.ensureLoaded();
        if (page == 3) {
            this.wallet.load();
        }
        if (page == 4) {
            this.admin.load();
        }
        int checked = 0;
        for (MarketInstaller.Installed entry : MarketInstaller.installed().values()) {
            if (checked++ >= 30) break;
            MarketClient.script(entry.id(), r -> {
                if (r.ok()) {
                    MarketState.noteVersion(entry.id(), ((MarketDetails)r.value()).script().card().version());
                }
            });
        }
    }

    public float renderMarket(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        int[] nArray;
        String[] stringArray;
        if (now - this.lastRender > 1500L) {
            this.onOpen();
        }
        this.lastRender = now;
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        boolean isAdmin = MarketState.admin();
        if (!isAdmin && page == 4) {
            page = 0;
        }
        if (isAdmin) {
            String[] stringArray2 = new String[4];
            stringArray2[0] = "\u041a\u0430\u0442\u0430\u043b\u043e\u0433";
            stringArray2[1] = "\u041f\u0443\u0431\u043b\u0438\u043a\u0430\u0446\u0438\u044f";
            stringArray2[2] = "\u041a\u043e\u0448\u0435\u043b\u0451\u043a";
            stringArray = stringArray2;
            stringArray2[3] = "\u0410\u0434\u043c\u0438\u043d";
        } else {
            String[] stringArray3 = new String[3];
            stringArray3[0] = "\u041a\u0430\u0442\u0430\u043b\u043e\u0433";
            stringArray3[1] = "\u041f\u0443\u0431\u043b\u0438\u043a\u0430\u0446\u0438\u044f";
            stringArray = stringArray3;
            stringArray3[2] = "\u041a\u043e\u0448\u0435\u043b\u0451\u043a";
        }
        String[] tabs = stringArray;
        if (isAdmin) {
            int[] nArray2 = new int[4];
            nArray2[0] = 0;
            nArray2[1] = 2;
            nArray2[2] = 3;
            nArray = nArray2;
            nArray2[3] = 4;
        } else {
            int[] nArray3 = new int[3];
            nArray3[0] = 0;
            nArray3[1] = 2;
            nArray = nArray3;
            nArray3[2] = 3;
        }
        int[] targets = nArray;
        float tabH = 14.0f;
        float tabW = (w - 4.0f) / (float)tabs.length;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float tabPos = this.tabAnim;
        this.tabAnim = this.smooth(this.tabAnim, MarketTab.tabIndex(page), 14.0f);
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        for (int i = 0; i < tabs.length; ++i) {
            int target = targets[i];
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "market-tab-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float act = Math.clamp(1.0f - Math.abs(tabPos - (float)i), 0.0f, 1.0f);
            Text.of(Fonts.MEDIUM, tabs[i], tx + tabW * 0.5f, y + 2.0f + tabH * 0.5f, 5.4f).centered().color(GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (page != target) {
                    this.openPage(target);
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        y += tabH + 4.0f + 5.0f;
        float toast = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.toastTime - 2600L) / 500.0f));
        if (this.toastText != null && toast > 0.01f) {
            float toastIn = GuiDraw.easeOut((float)(now - this.toastTime) / 250.0f);
            Text.of(Fonts.MEDIUM, GuiDraw.fit(this.toastText, 5.0f, w - 4.0f), x + 2.0f + (1.0f - toastIn) * 4.0f, y + 3.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(this.toastColor.withAlpha(a * toast * toastIn)).render(graphics);
            y += 9.0f;
        }
        float pa = a * GuiDraw.easeOut((float)(now - this.pageSwitchTime) / 300.0f);
        y = page == 1 ? this.script.render(graphics, x, y, w, pa, top, bottom, now) : (page == 2 ? this.publish.render(graphics, x, y, w, pa, top, bottom, now) : (page == 3 ? this.wallet.render(graphics, x, y, w, pa, top, bottom, now) : (page == 4 ? this.admin.render(graphics, x, y, w, pa, top, bottom, now) : this.catalog.render(graphics, x, y, w, pa, top, bottom, now))));
        return y - startY + 6.0f;
    }

    public void renderHeaderBalance(GuiGraphicsExtractor graphics, float rightX, float cy, float a) {
        if (!MarketState.hasBalance()) {
            Text.of(Fonts.MEDIUM, MarketState.loading() ? "..." : "-", rightX, cy, 5.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(MarketWidgets.MUTED.withAlpha(a)).render(graphics);
            return;
        }
        float used = this.ui.amount(graphics, MarketState.balance(), rightX, cy, 5.4f, a, MarketWidgets.GOLD, true);
        if (MarketState.admin()) {
            float bw = Text.of(Fonts.MEDIUM, "\u0410\u0434\u043c\u0438\u043d").size(4.2f).width() + 6.0f;
            this.ui.badge(graphics, "\u0410\u0434\u043c\u0438\u043d", rightX - used - 4.0f - bw, cy, MarketWidgets.RED, a);
        } else if (MarketState.banned()) {
            float bw = Text.of(Fonts.MEDIUM, "\u0411\u0430\u043d").size(4.2f).width() + 6.0f;
            this.ui.badge(graphics, "\u0411\u0430\u043d", rightX - used - 4.0f - bw, cy, MarketWidgets.RED, a);
        }
    }
}

