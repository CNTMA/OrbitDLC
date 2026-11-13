/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.events.GuiEventListener
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.setting.ConfigManager;
import brokke.cc.system.autobuy.AutoBuyConfig;
import brokke.cc.system.autobuy.ItemEntry;
import brokke.cc.system.bot.client.HeadlessBots;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.tab.AutoBuyTab;
import brokke.cc.ui.clickgui.tab.AutoSborTab;
import brokke.cc.ui.clickgui.tab.BotsTab;
import brokke.cc.ui.clickgui.tab.EventsTab;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.util.Log;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.components.events.GuiEventListener;

public final class TextFocus
extends GuiComponent {
    public boolean abSelectedAll;
    public boolean searchFocused = false;
    public boolean searchSelectedAll;
    public boolean friendFocused = false;
    public boolean configFocused = false;
    public String abField = null;
    public String abClickPrev = null;
    public final StringBuilder abInput = new StringBuilder();
    public final List<Long> abCharTimes = new ArrayList<Long>();

    public TextFocus(ClickGuiContext ctx) {
        super(ctx);
    }

    public void clear() {
        this.setSearchFocus(false);
        this.setFriendFocus(false);
        this.setConfigFocus(false);
        this.setAbField(null, "");
    }

    public void setSearchFocus(boolean focused) {
        if (this.searchFocused == focused) {
            return;
        }
        boolean was = this.searchFocused || this.friendFocused || this.configFocused || this.abField != null;
        this.searchFocused = focused;
        if (!focused) {
            this.searchSelectedAll = false;
        }
        if (focused) {
            this.commitAbField();
            this.friendFocused = false;
            this.configFocused = false;
        }
        this.updateTextInput(was);
    }

    public void setFriendFocus(boolean focused) {
        if (this.friendFocused == focused) {
            return;
        }
        boolean was = this.searchFocused || this.friendFocused || this.configFocused || this.abField != null;
        this.friendFocused = focused;
        if (focused) {
            this.commitAbField();
            this.searchFocused = false;
            this.configFocused = false;
        }
        this.updateTextInput(was);
    }

    public void setConfigFocus(boolean focused) {
        if (this.configFocused == focused) {
            return;
        }
        boolean was = this.searchFocused || this.friendFocused || this.configFocused || this.abField != null;
        this.configFocused = focused;
        if (focused) {
            this.commitAbField();
            this.searchFocused = false;
            this.friendFocused = false;
        }
        this.updateTextInput(was);
    }

    private void updateTextInput(boolean was) {
        boolean now;
        boolean bl = now = this.searchFocused || this.friendFocused || this.configFocused || this.abField != null;
        if (was != now && this.ctx.minecraft() != null) {
            this.ctx.minecraft().onTextInputFocusChange((GuiEventListener)this.ctx.screen, now);
        }
    }

    public void setAbField(String id, String initial) {
        if (Objects.equals(this.abField, id)) {
            return;
        }
        boolean was = this.searchFocused || this.friendFocused || this.configFocused || this.abField != null;
        this.commitAbField();
        this.abField = id;
        this.abInput.setLength(0);
        this.abCharTimes.clear();
        this.abSelectedAll = false;
        if (id != null) {
            this.searchFocused = false;
            this.friendFocused = false;
            this.configFocused = false;
            this.abInput.append(initial);
            for (int i = 0; i < initial.length(); ++i) {
                this.abCharTimes.add(0L);
            }
        }
        this.updateTextInput(was);
    }

    private void commitAbField() {
        if (this.abField == null) {
            return;
        }
        String id = this.abField;
        String value = this.abInput.toString().trim();
        this.abField = null;
        this.abSelectedAll = false;
        if (this.ctx.settings.textSettings.containsKey(id)) {
            this.ctx.settings.textSettings.get(id).setValue(this.abInput.toString());
            ConfigManager.saveConfig("backup");
            return;
        }
        if (id.equals("globals:chat")) {
            this.ctx.globals.draft = this.abInput.toString();
            return;
        }
        if (id.startsWith("profile:")) {
            this.ctx.globals.profile.commitField(id, this.abInput.toString().trim());
            return;
        }
        if (id.startsWith("cloudcfg:")) {
            this.ctx.configs.cloudFields.put(id, this.abInput.toString().trim());
            return;
        }
        if (AutoSborTab.commit(id, this.abInput.toString())) {
            return;
        }
        if (id.startsWith("market:")) {
            MarketTab.setField(id, value);
            return;
        }
        if (this.ctx.botRows.textSettings.containsKey(id)) {
            this.ctx.botRows.textSettings.get(id).setValue(this.abInput.toString());
            return;
        }
        if (id.equals("events:search")) {
            EventsTab cfr_ignored_0 = this.ctx.events;
            EventsTab.eventSearch = value;
            return;
        }
        if (id.startsWith("bot:")) {
            BotsTab cfr_ignored_1 = this.ctx.bots;
            BotsTab.BOT_FIELDS.put(id, this.abInput.toString().trim());
            if (id.equals("bot:server")) {
                BotsTab cfr_ignored_2 = this.ctx.bots;
                HeadlessBots.setLastAddress(BotsTab.BOT_FIELDS.get(id));
            }
            if (id.equals("bot:proxy")) {
                BotsTab cfr_ignored_3 = this.ctx.bots;
                HeadlessBots.setLastProxy(BotsTab.BOT_FIELDS.get(id));
            }
            return;
        }
        AutoBuyConfig cfg = this.ctx.autoBuyConfig();
        try {
            if (id.equals("nick")) {
                cfg.autoPayNick = value;
            } else if (id.equals("keepbalance")) {
                cfg.autoPayKeepBalance = value.isEmpty() ? 0L : Long.parseLong(value);
            } else if (id.equals("minprice")) {
                cfg.minPrice = value.isEmpty() ? 0L : Long.parseLong(value);
            } else if (id.equals("ban")) {
                if (!value.isEmpty()) {
                    cfg.addBan(value);
                }
            } else if (id.startsWith("price:")) {
                ItemEntry e = cfg.entries.get(Integer.parseInt(id.substring(6)));
                e.maxUnitPrice = value.isEmpty() ? 0L : Long.parseLong(value);
            } else if (id.startsWith("dur:")) {
                ItemEntry e = cfg.entries.get(Integer.parseInt(id.substring(4)));
                int n = e.minDurabilityPercent = value.isEmpty() ? 0 : Math.clamp((long)Integer.parseInt(value), 0, 100);
            }
            if (!id.equals("search")) {
                this.ctx.saveAutoBuy();
            }
        }
        catch (RuntimeException ignored) {
            Log.swallow(ignored);
        }
    }

    public boolean abNumeric() {
        return this.abField != null && (this.abField.equals("minprice") || this.abField.equals("keepbalance") || this.abField.startsWith("price:") || this.abField.startsWith("dur:"));
    }

    public void appendAbText(String text) {
        int limit;
        boolean nick;
        if (this.abField == null || text == null || text.isEmpty()) {
            return;
        }
        boolean numeric = this.abNumeric() || MarketTab.isNumericField(this.abField);
        boolean bl = nick = this.abField.equals("nick") || this.abField.equals("ban") || this.abField.equals("bot:name") || this.abField.equals("bot:prefix") || MarketTab.isNickField(this.abField);
        limit = numeric ? (this.abField.equals("keepbalance") ? 19 : 12) : (nick ? (this.abField.equals("bot:prefix") ? 12 : 16) : this.abField.startsWith("bot:") ? 64 : 32);
        if (this.abField.startsWith("market:")) {
            limit = MarketTab.fieldLimit(this.abField);
        }
        if (this.abField.equals("globals:chat")) {
            limit = 240;
        }
        if (this.abField.equals("cloudcfg:code")) {
            limit = 16;
        }
        if (this.abField.equals("profile:status")) {
            limit = 40;
        }
        if (this.abField.equals("profile:bio")) {
            limit = 140;
        }
        if (this.abField.equals("cloudcfg:uses")) {
            limit = 5;
            numeric = true;
        }
        if (this.ctx.botRows.textSettings.containsKey(this.abField)) {
            limit = this.ctx.botRows.textSettings.get(this.abField).maxLength();
        }
        if (this.ctx.settings.textSettings.containsKey(this.abField)) {
            limit = this.ctx.settings.textSettings.get(this.abField).maxLength();
        }
        StringBuilder accepted = new StringBuilder();
        int available = limit - (this.abSelectedAll ? 0 : this.abInput.length());
        for (char character : text.toCharArray()) {
            boolean allowed;
            allowed = numeric ? character >= '0' && character <= '9' : (nick ? Character.isLetterOrDigit(character) && character < '\u0080' || character == '_' : !Character.isISOControl(character) && character != '\u00a7');
            if (!allowed || accepted.length() >= available) continue;
            accepted.append(character);
        }
        if (accepted.isEmpty()) {
            return;
        }
        if (this.abSelectedAll) {
            this.clearAbText();
        }
        this.abInput.append((CharSequence)accepted);
        for (int i = 0; i < accepted.length(); ++i) {
            this.abCharTimes.add(System.currentTimeMillis());
        }
        this.syncAbSearch();
        ClientSound.ui(ClientSound.Ui.HOVER);
    }

    public void clearAbText() {
        this.abInput.setLength(0);
        this.abCharTimes.clear();
        this.abSelectedAll = false;
    }

    public void syncAbSearch() {
        String value = this.abInput.toString();
        boolean changed = false;
        if ("search".equals(this.abField)) {
            AutoBuyTab cfr_ignored_0 = this.ctx.autoBuy;
            changed = !AutoBuyTab.abSearch.equals(value);
            AutoBuyTab cfr_ignored_1 = this.ctx.autoBuy;
            AutoBuyTab.abSearch = value;
        } else if ("events:search".equals(this.abField)) {
            changed = !EventsTab.eventSearch.equals(value);
            EventsTab.eventSearch = value;
        }
        if (changed) {
            this.ctx.scrollTarget = 0.0f;
            this.ctx.scrollValue = 0.0f;
            this.ctx.scrollbar.thumbYValue = Float.NaN;
        }
    }
}

