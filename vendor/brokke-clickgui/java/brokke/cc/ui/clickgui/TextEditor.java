/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.input.KeyEvent
 */
package brokke.cc.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class TextEditor {
    private int cursor;
    private int anchor;

    public void reset(String text) {
        this.anchor = text == null ? 0 : text.length();
        this.cursor = this.anchor;
    }

    public int cursor(String text) {
        return TextEditor.clamp(this.cursor, text);
    }

    public int selectionStart(String text) {
        return Math.min(TextEditor.clamp(this.cursor, text), TextEditor.clamp(this.anchor, text));
    }

    public int selectionEnd(String text) {
        return Math.max(TextEditor.clamp(this.cursor, text), TextEditor.clamp(this.anchor, text));
    }

    public boolean hasSelection(String text) {
        return this.selectionStart(text) != this.selectionEnd(text);
    }

    public void setCursor(String text, int pos, boolean select) {
        this.cursor = TextEditor.clamp(pos, text);
        if (!select) {
            this.anchor = this.cursor;
        }
    }

    public void selectAll(String text) {
        this.anchor = 0;
        this.cursor = text == null ? 0 : text.length();
    }

    public void selectWord(String text, int pos) {
        int e;
        int p;
        int s;
        if (text == null || text.isEmpty()) {
            return;
        }
        for (s = p = TextEditor.clamp(pos, text); s > 0 && TextEditor.isWordChar(text.charAt(s - 1)); --s) {
        }
        for (e = p; e < text.length() && TextEditor.isWordChar(text.charAt(e)); ++e) {
        }
        if (s == e) {
            this.anchor = 0;
            this.cursor = text.length();
        } else {
            this.anchor = s;
            this.cursor = e;
        }
    }

    public String type(String text, String input, int maxLength) {
        if (input == null || input.isEmpty()) {
            return text;
        }
        return this.insert(text, input, maxLength);
    }

    public Result key(KeyEvent event, String text, int maxLength) {
        if (text == null) {
            text = "";
        }
        int key = event.key();
        boolean ctrl = event.hasControlDown() || InputConstants.isKeyDown((int)224) || InputConstants.isKeyDown((int)228);
        boolean shift = event.hasShiftDown() || InputConstants.isKeyDown((int)225) || InputConstants.isKeyDown((int)229);
        int c = this.cursor((String)text);
        if (ctrl && key == 4) {
            this.selectAll((String)text);
            return Result.handled((String)text);
        }
        if (ctrl && (key == 6 || key == 27)) {
            if (this.hasSelection((String)text)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(((String)text).substring(this.selectionStart((String)text), this.selectionEnd((String)text)));
                if (key == 27) {
                    text = this.deleteSelection((String)text);
                }
            }
            return Result.handled((String)text);
        }
        if (ctrl && key == 25) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                text = this.insert((String)text, clip.replace("\r", "").replace("\n", ""), maxLength);
            }
            return Result.handled((String)text);
        }
        switch (key) {
            case 80: {
                if (this.hasSelection((String)text) && !shift) {
                    this.setCursor((String)text, this.selectionStart((String)text), false);
                } else {
                    this.setCursor((String)text, ctrl ? TextEditor.wordLeft((String)text, c) : c - 1, shift);
                }
                return Result.handled((String)text);
            }
            case 79: {
                if (this.hasSelection((String)text) && !shift) {
                    this.setCursor((String)text, this.selectionEnd((String)text), false);
                } else {
                    this.setCursor((String)text, ctrl ? TextEditor.wordRight((String)text, c) : c + 1, shift);
                }
                return Result.handled((String)text);
            }
            case 74: 
            case 82: {
                this.setCursor((String)text, 0, shift);
                return Result.handled((String)text);
            }
            case 77: 
            case 81: {
                this.setCursor((String)text, ((String)text).length(), shift);
                return Result.handled((String)text);
            }
            case 42: {
                if (this.hasSelection((String)text)) {
                    return Result.handled(this.deleteSelection((String)text));
                }
                int from = ctrl ? TextEditor.wordLeft((String)text, c) : Math.max(0, c - 1);
                text = ((String)text).substring(0, from) + ((String)text).substring(c);
                this.setCursor((String)text, from, false);
                return Result.handled((String)text);
            }
            case 76: {
                if (this.hasSelection((String)text)) {
                    return Result.handled(this.deleteSelection((String)text));
                }
                int to = ctrl ? TextEditor.wordRight((String)text, c) : Math.min(((String)text).length(), c + 1);
                text = ((String)text).substring(0, c) + ((String)text).substring(to);
                this.setCursor((String)text, c, false);
                return Result.handled((String)text);
            }
        }
        return Result.ignored((String)text);
    }

    private String insert(String text, String input, int maxLength) {
        if (text == null) {
            text = "";
        }
        int s = this.selectionStart(text);
        int e = this.selectionEnd(text);
        int room = maxLength - (text.length() - (e - s));
        if (room <= 0) {
            return text;
        }
        if (input.length() > room) {
            input = input.substring(0, room);
        }
        String out = text.substring(0, s) + input + text.substring(e);
        this.setCursor(out, s + input.length(), false);
        return out;
    }

    private String deleteSelection(String text) {
        int s = this.selectionStart(text);
        int e = this.selectionEnd(text);
        String out = text.substring(0, s) + text.substring(e);
        this.setCursor(out, s, false);
        return out;
    }

    private static int wordLeft(String text, int pos) {
        int p;
        for (p = pos; p > 0 && !TextEditor.isWordChar(text.charAt(p - 1)); --p) {
        }
        while (p > 0 && TextEditor.isWordChar(text.charAt(p - 1))) {
            --p;
        }
        return p;
    }

    private static int wordRight(String text, int pos) {
        int p;
        for (p = pos; p < text.length() && !TextEditor.isWordChar(text.charAt(p)); ++p) {
        }
        while (p < text.length() && TextEditor.isWordChar(text.charAt(p))) {
            ++p;
        }
        return p;
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private static int clamp(int pos, String text) {
        int len = text == null ? 0 : text.length();
        return Math.max(0, Math.min(len, pos));
    }

    public record Result(boolean handled, String text) {
        static Result handled(String text) {
            return new Result(true, text);
        }

        static Result ignored(String text) {
            return new Result(false, text);
        }
    }
}

