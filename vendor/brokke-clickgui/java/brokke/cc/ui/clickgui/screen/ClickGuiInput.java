/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.client.input.MouseButtonEvent
 */
package brokke.cc.ui.clickgui.screen;

import brokke.cc.core.bind.Keybinds;
import brokke.cc.core.module.Category;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.tab.AutoBuyTab;
import brokke.cc.ui.clickgui.tab.EventsTab;
import brokke.cc.ui.clickgui.tab.market.MarketTab;
import brokke.cc.util.sound.ClientSound;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class ClickGuiInput
extends GuiComponent {
    private boolean skipNextChar = false;

    public ClickGuiInput(ClickGuiContext ctx) {
        super(ctx);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index;
        if (this.ctx.closing) {
            return false;
        }
        if (this.ctx.bindingModule != null) {
            if (event.button() == 1) {
                this.ctx.bindingModule = null;
                ClientSound.ui(ClientSound.Ui.CLICK);
            } else {
                this.ctx.moduleCard.finishModuleBind(Keybinds.fromMouse(event.button()));
            }
            return true;
        }
        if (this.ctx.bindingSetting != null && event.button() != 1) {
            this.ctx.bindingSetting.setKey(Keybinds.fromMouse(event.button()));
            this.ctx.bindingSetting = null;
            ClientSound.ui(ClientSound.Ui.BIND_SET);
            return true;
        }
        float mx = this.getMouseX();
        float my = this.getMouseY();
        boolean left = event.button() == 1;
        boolean right = event.button() == 3;
        this.ctx.focus.abClickPrev = this.ctx.focus.abField;
        if (left) {
            this.ctx.focus.clear();
            this.ctx.editingString = null;
        }
        if (left && (index = this.ctx.sidebar.categoryAt(mx, my)) >= 0) {
            if (this.ctx.autoSborOpen) {
                this.ctx.autoSborOpen = false;
                this.ctx.scrollTarget = 0.0f;
                this.ctx.scrollValue = 0.0f;
                this.ctx.switchTime = System.currentTimeMillis();
                this.ctx.content.titleAnim.set(0.0f);
            }
            if (this.ctx.searchBar.search.length() > 0) {
                this.ctx.searchBar.clearSearch();
                this.ctx.switchTime = System.currentTimeMillis();
                this.ctx.content.titleAnim.set(0.0f);
            }
            if (index != this.ctx.selected) {
                ClientSound.ui(index > this.ctx.selected ? ClientSound.Ui.CATEGORY_NEXT : ClientSound.Ui.CATEGORY_PREV);
                this.ctx.selected = index;
                this.ctx.scrollTarget = 0.0f;
                this.ctx.scrollValue = 0.0f;
                this.ctx.scrollbar.thumbYValue = Float.NaN;
                this.ctx.friends.friendsSwitchTime = this.ctx.switchTime = System.currentTimeMillis();
                this.ctx.cosmetics.cosmeticSwitchTime = this.ctx.switchTime;
                this.ctx.autoBuy.abSwitchTime = this.ctx.switchTime;
                this.ctx.bots.botSwitchTime = this.ctx.switchTime;
                this.ctx.content.titleAnim.set(0.0f);
            }
            return true;
        }
        if (left && this.ctx.maxScroll > 0.0f && mx >= this.ctx.scrollbar.trackBox[0] - 2.0f && mx <= this.ctx.scrollbar.trackBox[0] + this.ctx.scrollbar.trackBox[2] + 2.0f && my >= this.ctx.scrollbar.trackBox[1] && my <= this.ctx.scrollbar.trackBox[1] + this.ctx.scrollbar.trackBox[3]) {
            this.ctx.scrollbar.scrollDragOffset = my >= this.ctx.scrollbar.thumbBox[1] && my <= this.ctx.scrollbar.thumbBox[1] + this.ctx.scrollbar.thumbBox[3] ? my - this.ctx.scrollbar.thumbBox[1] : this.ctx.scrollbar.thumbBox[3] * 0.5f;
            this.ctx.scrollbar.scrollDragging = true;
            return true;
        }
        if (event.button() == 2 && this.ctx.hits.clickMiddle(mx, my)) {
            return true;
        }
        return this.ctx.hits.click(mx, my, left, right);
    }

    public boolean charTyped(CharacterEvent event) {
        if (this.skipNextChar) {
            this.skipNextChar = false;
            return true;
        }
        if (this.ctx.bindingModule != null) {
            return true;
        }
        if (this.ctx.editingString != null) {
            if (event.isAllowedChatCharacter()) {
                this.ctx.editingString.append(event.codepointAsString());
            }
            return true;
        }
        if (this.ctx.focus.abField != null && this.ctx.bindingSetting == null) {
            if (event.isAllowedChatCharacter()) {
                this.ctx.focus.appendAbText(event.codepointAsString());
            }
            return true;
        }
        if (this.ctx.focus.configFocused && this.ctx.bindingSetting == null) {
            for (char c : event.codepointAsString().toCharArray()) {
                if (this.ctx.configs.configInput.length() >= 24 || !Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != ' ') continue;
                this.ctx.configs.configInput.append(c);
                this.ctx.configs.configCharTimes.add(System.currentTimeMillis());
                ClientSound.ui(ClientSound.Ui.HOVER);
            }
            return true;
        }
        if (this.ctx.focus.friendFocused && this.ctx.bindingSetting == null) {
            String typed = event.codepointAsString();
            for (char c : typed.toCharArray()) {
                boolean allowed;
                allowed = this.ctx.friends.friendsTab == 1 ? this.ctx.friends.friendInput.length() < 24 && (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-') : this.ctx.friends.friendInput.length() < 16 && (Character.isLetterOrDigit(c) && c < '\u0080' || c == '_');
                if (!allowed) continue;
                this.ctx.friends.friendInput.append(c);
                this.ctx.friends.friendCharTimes.add(System.currentTimeMillis());
                ClientSound.ui(ClientSound.Ui.HOVER);
            }
            return true;
        }
        if (this.ctx.focus.searchFocused && this.ctx.bindingSetting == null && event.isAllowedChatCharacter()) {
            if (this.ctx.focus.searchSelectedAll) {
                this.ctx.searchBar.clearSearch();
            }
            if (this.ctx.searchBar.search.length() < 32) {
                String typed = event.codepointAsString();
                this.ctx.searchBar.search.append(typed);
                for (int i = 0; i < typed.length(); ++i) {
                    this.ctx.searchBar.charTimes.add(System.currentTimeMillis());
                }
                ClientSound.ui(ClientSound.Ui.HOVER);
            }
            return true;
        }
        return false;
    }

    private void pasteMarket() {
        String id = this.ctx.focus.abField;
        String pasted = this.ctx.minecraft().keyboardHandler.getClipboard();
        if (pasted == null) {
            return;
        }
        boolean numeric = MarketTab.isNumericField(id);
        int limit = MarketTab.fieldLimit(id);
        for (int i = 0; i < pasted.length() && this.ctx.focus.abInput.length() < limit; ++i) {
            char c = pasted.charAt(i);
            if (c < ' ') {
                c = ' ';
            }
            if (!(numeric ? Character.isDigit(c) : c >= ' ' && c != '\u007f')) continue;
            this.ctx.focus.abInput.append(c);
            this.ctx.focus.abCharTimes.add(System.currentTimeMillis());
        }
    }

    public boolean keyPressed(KeyEvent event) {
        boolean ctrl;
        if (this.ctx.bindingModule != null) {
            int key = event.key();
            if (key == 261 || key == 76 || key == 259 || key == 42) {
                this.ctx.moduleCard.finishModuleBind(0);
            } else if (key == 256 || key == 41) {
                this.ctx.bindingModule = null;
                ClientSound.ui(ClientSound.Ui.CLICK);
            } else {
                this.ctx.moduleCard.finishModuleBind(key);
            }
            this.skipNextChar = true;
            return true;
        }
        if (this.ctx.editingString != null) {
            int key = event.key();
            boolean ctrlDown = event.hasControlDown();
            if (key == 41 || key == 40 || key == 88) {
                this.ctx.editingString = null;
                ConfigManager.saveConfig("backup");
                ClientSound.ui(ClientSound.Ui.CLICK);
            } else if (key == 42) {
                if (ctrlDown) {
                    this.ctx.editingString.clear();
                } else {
                    this.ctx.editingString.removeLast();
                }
            } else if (ctrlDown && key == 25) {
                String pasted = this.ctx.minecraft().keyboardHandler.getClipboard();
                if (pasted != null) {
                    this.ctx.editingString.append(pasted.replaceAll("[\r\n\t]", ""));
                }
            } else if (ctrlDown && key == 6) {
                this.ctx.minecraft().keyboardHandler.setClipboard(this.ctx.editingString.get());
            } else {
                return false;
            }
            return true;
        }
        if (this.ctx.autoSborOpen && this.ctx.focus.abField == null && !this.ctx.focus.configFocused && this.ctx.bindingSetting == null && event.key() == 41) {
            this.ctx.autoSborOpen = false;
            this.ctx.scrollTarget = 0.0f;
            this.ctx.scrollValue = 0.0f;
            this.ctx.switchTime = System.currentTimeMillis();
            this.ctx.content.titleAnim.set(0.0f);
            ClientSound.ui(ClientSound.Ui.CLICK);
            return true;
        }
        boolean bl = ctrl = event.hasControlDown() || (event.modifiers() & 0xC0) != 0;
        if (this.ctx.bindingSetting == null && ctrl && event.key() == 9) {
            if (this.ctx.screen.getSelectedCategory() == Category.AUTOBUY) {
                this.ctx.focus.setAbField("search", AutoBuyTab.abSearch);
            } else if (this.ctx.screen.getSelectedCategory() == Category.EVENTS) {
                this.ctx.focus.setAbField("events:search", EventsTab.eventSearch);
            } else {
                this.ctx.focus.setSearchFocus(true);
            }
            ClientSound.ui(ClientSound.Ui.CLICK);
            return true;
        }
        if (this.ctx.focus.abField != null && this.ctx.bindingSetting == null) {
            int key = event.key();
            if (ctrl && key == 4) {
                this.ctx.focus.abSelectedAll = !this.ctx.focus.abInput.isEmpty();
                return true;
            }
            if (ctrl && (key == 6 || key == 27)) {
                if (this.ctx.focus.abSelectedAll) {
                    this.ctx.minecraft().keyboardHandler.setClipboard(this.ctx.focus.abInput.toString());
                    if (key == 27) {
                        this.ctx.focus.clearAbText();
                        this.ctx.focus.syncAbSearch();
                    }
                }
                return true;
            }
            if (ctrl && key == 25) {
                this.ctx.focus.appendAbText(this.ctx.minecraft().keyboardHandler.getClipboard());
                return true;
            }
            if (key == 80 || key == 79) {
                this.ctx.focus.abSelectedAll = false;
                return true;
            }
            if (key == 76 && this.ctx.focus.abSelectedAll) {
                this.ctx.focus.clearAbText();
                this.ctx.focus.syncAbSearch();
                return true;
            }
            if ((key == 40 || key == 88) && "globals:chat".equals(this.ctx.focus.abField)) {
                this.ctx.globals.sendChat();
                return true;
            }
            if ((key == 40 || key == 88) && "cloudcfg:code".equals(this.ctx.focus.abField)) {
                this.ctx.focus.setAbField(null, "");
                this.ctx.configs.redeemCode();
                return true;
            }
            if (key == 41 || key == 40 || key == 88) {
                this.ctx.focus.setAbField(null, "");
            } else if (key == 42 && this.ctx.focus.abInput.length() > 0) {
                if (ctrl || this.ctx.focus.abSelectedAll) {
                    this.ctx.focus.clearAbText();
                } else {
                    this.ctx.focus.abInput.setLength(this.ctx.focus.abInput.length() - 1);
                    if (!this.ctx.focus.abCharTimes.isEmpty()) {
                        this.ctx.focus.abCharTimes.remove(this.ctx.focus.abCharTimes.size() - 1);
                    }
                }
                this.ctx.focus.syncAbSearch();
            }
            return true;
        }
        if (this.ctx.focus.configFocused && this.ctx.bindingSetting == null) {
            int key = event.key();
            if (key == 41) {
                this.ctx.focus.setConfigFocus(false);
            } else if (key == 40 || key == 88) {
                this.ctx.configs.submitConfig();
            } else if (key == 42 && this.ctx.configs.configInput.length() > 0) {
                if (ctrl) {
                    this.ctx.configs.configInput.setLength(0);
                    this.ctx.configs.configCharTimes.clear();
                } else {
                    this.ctx.configs.configInput.setLength(this.ctx.configs.configInput.length() - 1);
                    if (!this.ctx.configs.configCharTimes.isEmpty()) {
                        this.ctx.configs.configCharTimes.remove(this.ctx.configs.configCharTimes.size() - 1);
                    }
                }
            }
            return true;
        }
        if (this.ctx.focus.friendFocused && this.ctx.bindingSetting == null) {
            int key = event.key();
            if (key == 41) {
                this.ctx.focus.setFriendFocus(false);
            } else if (key == 40 || key == 88) {
                this.ctx.friends.submitFriend();
            } else if (key == 42 && this.ctx.friends.friendInput.length() > 0) {
                if (ctrl) {
                    this.ctx.friends.friendInput.setLength(0);
                    this.ctx.friends.friendCharTimes.clear();
                } else {
                    this.ctx.friends.friendInput.setLength(this.ctx.friends.friendInput.length() - 1);
                    if (!this.ctx.friends.friendCharTimes.isEmpty()) {
                        this.ctx.friends.friendCharTimes.remove(this.ctx.friends.friendCharTimes.size() - 1);
                    }
                }
            }
            return true;
        }
        if (this.ctx.focus.searchFocused && this.ctx.bindingSetting == null) {
            int key = event.key();
            if (ctrl && key == 4) {
                this.ctx.focus.searchSelectedAll = this.ctx.searchBar.search.length() > 0;
                return true;
            }
            if (ctrl && (key == 6 || key == 27)) {
                if (this.ctx.focus.searchSelectedAll) {
                    this.ctx.minecraft().keyboardHandler.setClipboard(this.ctx.searchBar.search.toString());
                    if (key == 27) {
                        this.ctx.searchBar.clearSearch();
                    }
                }
                return true;
            }
            if (ctrl && key == 25) {
                if (this.ctx.focus.searchSelectedAll) {
                    this.ctx.searchBar.clearSearch();
                }
                String pasted = this.ctx.minecraft().keyboardHandler.getClipboard();
                for (int i = 0; i < pasted.length() && this.ctx.searchBar.search.length() < 32; ++i) {
                    char character = pasted.charAt(i);
                    if (character < ' ' || character == '\u007f') continue;
                    this.ctx.searchBar.search.append(character);
                    this.ctx.searchBar.charTimes.add(System.currentTimeMillis());
                }
                return true;
            }
            if (key == 263 || key == 80 || key == 262 || key == 79) {
                this.ctx.focus.searchSelectedAll = false;
                return true;
            }
            if ((key == 261 || key == 76) && this.ctx.focus.searchSelectedAll) {
                this.ctx.searchBar.clearSearch();
                return true;
            }
            if (key == 256 || key == 41 || key == 257 || key == 40) {
                this.ctx.focus.setSearchFocus(false);
                return true;
            }
            if (key == 259 || key == 42) {
                if (ctrl || this.ctx.focus.searchSelectedAll) {
                    this.ctx.searchBar.clearSearch();
                } else if (this.ctx.searchBar.search.length() > 0) {
                    this.ctx.searchBar.removeLast();
                }
                return true;
            }
            return true;
        }
        if (this.ctx.bindingSetting != null) {
            int key = event.key();
            if (key == 261 || key == 76 || key == 259 || key == 42) {
                this.ctx.bindingSetting.setKey(-1);
                ClientSound.ui(ClientSound.Ui.BIND_CLEAR);
            } else if (key != 256 && key != 41) {
                this.ctx.bindingSetting.setKey(key);
                ClientSound.ui(ClientSound.Ui.BIND_SET);
            } else {
                ClientSound.ui(ClientSound.Ui.CLICK);
            }
            this.ctx.bindingSetting = null;
            this.skipNextChar = true;
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        if (this.ctx.dragSetting != null) {
            ClientSound.resetSlider();
        }
        this.ctx.dragSetting = null;
        this.ctx.scrollbar.scrollDragging = false;
        this.ctx.themes.pickerDrag = 0;
        if (this.ctx.colorDragSetting != null) {
            ConfigManager.saveConfig("backup");
        }
        this.ctx.colorDragSetting = null;
        this.ctx.colorDragPart = 0;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.ctx.screen.getSelectedCategory() == Category.FRIENDS && this.ctx.searchBar.search.isEmpty() && this.ctx.globals.scrollChat(verticalAmount)) {
            return true;
        }
        this.ctx.scrollTarget = Math.clamp(this.ctx.scrollTarget - (float)verticalAmount * 24.0f, 0.0f, this.ctx.maxScroll);
        return true;
    }
}

