/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.input.KeyEvent
 */
package brokke.cc.ui.clickgui.scripts;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class CodeEditor {
    private static final String INDENT = "    ";
    private static final int UNDO_LIMIT = 200;
    private String text = "";
    private int caret;
    private int anchor;
    private int[] lineStarts = new int[]{0};
    private boolean modified;
    private final Deque<String[]> undo = new ArrayDeque<String[]>();
    private final Deque<String[]> redo = new ArrayDeque<String[]>();
    private long lastEditTime;

    public void setText(String value) {
        this.text = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').replace("\t", INDENT);
        this.anchor = 0;
        this.caret = 0;
        this.modified = false;
        this.undo.clear();
        this.redo.clear();
        this.rebuild();
    }

    public String text() {
        return this.text;
    }

    public boolean modified() {
        return this.modified;
    }

    public void markSaved() {
        this.modified = false;
    }

    private void rebuild() {
        int i;
        ArrayList<Integer> starts = new ArrayList<Integer>();
        starts.add(0);
        for (i = 0; i < this.text.length(); ++i) {
            if (this.text.charAt(i) != '\n') continue;
            starts.add(i + 1);
        }
        this.lineStarts = new int[starts.size()];
        for (i = 0; i < starts.size(); ++i) {
            this.lineStarts[i] = (Integer)starts.get(i);
        }
    }

    public int lineCount() {
        return this.lineStarts.length;
    }

    public int lineStart(int line) {
        return this.lineStarts[Math.max(0, Math.min(this.lineStarts.length - 1, line))];
    }

    public int lineEnd(int line) {
        int l = Math.max(0, Math.min(this.lineStarts.length - 1, line));
        return l + 1 < this.lineStarts.length ? this.lineStarts[l + 1] - 1 : this.text.length();
    }

    public String line(int line) {
        return this.text.substring(this.lineStart(line), this.lineEnd(line));
    }

    public int lineOf(int pos) {
        int lo = 0;
        int hi = this.lineStarts.length - 1;
        while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (this.lineStarts[mid] <= pos) {
                lo = mid;
                continue;
            }
            hi = mid - 1;
        }
        return lo;
    }

    public int caret() {
        return this.caret;
    }

    public int caretLine() {
        return this.lineOf(this.caret);
    }

    public int caretColumn() {
        return this.caret - this.lineStart(this.caretLine());
    }

    public int selectionStart() {
        return Math.min(this.caret, this.anchor);
    }

    public int selectionEnd() {
        return Math.max(this.caret, this.anchor);
    }

    public boolean hasSelection() {
        return this.caret != this.anchor;
    }

    public void moveTo(int line, int column, boolean select) {
        int l = Math.max(0, Math.min(this.lineCount() - 1, line));
        int pos = this.lineStart(l) + Math.max(0, Math.min(column, this.lineEnd(l) - this.lineStart(l)));
        this.setCaret(pos, select);
    }

    private void setCaret(int pos, boolean select) {
        this.caret = Math.max(0, Math.min(this.text.length(), pos));
        if (!select) {
            this.anchor = this.caret;
        }
    }

    public void selectWordAt(int line, int column) {
        int e;
        int s;
        this.moveTo(line, column, false);
        for (s = this.caret; s > 0 && CodeEditor.isWord(this.text.charAt(s - 1)); --s) {
        }
        for (e = this.caret; e < this.text.length() && CodeEditor.isWord(this.text.charAt(e)); ++e) {
        }
        this.anchor = s;
        this.caret = e;
    }

    private void snapshot(boolean merge) {
        long now = System.currentTimeMillis();
        if (!merge || now - this.lastEditTime >= 600L || this.undo.isEmpty()) {
            this.undo.push(new String[]{this.text, String.valueOf(this.caret)});
            while (this.undo.size() > 200) {
                this.undo.removeLast();
            }
        }
        this.lastEditTime = now;
        this.redo.clear();
    }

    private void replace(int from, int to, String insert) {
        this.text = this.text.substring(0, from) + insert + this.text.substring(to);
        this.caret = this.anchor = from + insert.length();
        this.modified = true;
        this.rebuild();
    }

    public void type(String input) {
        int l;
        String before;
        if (input == null || input.isEmpty()) {
            return;
        }
        this.snapshot(input.length() == 1 && !this.hasSelection());
        String s = input.replace("\r\n", "\n").replace('\r', '\n').replace("\t", INDENT);
        if (s.equals("}") && !this.hasSelection() && (before = this.text.substring(this.lineStart(l = this.caretLine()), this.caret)).isBlank() && before.length() >= INDENT.length()) {
            this.replace(this.caret - INDENT.length(), this.caret, "}");
            return;
        }
        this.replace(this.selectionStart(), this.selectionEnd(), s);
    }

    private void newline() {
        int indentLen;
        this.snapshot(false);
        int l = this.caretLine();
        String current = this.line(l);
        for (indentLen = 0; indentLen < current.length() && current.charAt(indentLen) == ' '; ++indentLen) {
        }
        Object indent = current.substring(0, indentLen);
        String before = this.text.substring(this.lineStart(l), this.selectionStart()).stripTrailing();
        String after = this.text.substring(this.selectionEnd(), this.lineEnd(l)).strip();
        if (before.endsWith("{") || before.endsWith("(") || before.endsWith("->")) {
            if (after.startsWith("}") || after.startsWith(")")) {
                int start = this.selectionStart();
                this.replace(start, this.selectionEnd(), "\n" + (String)indent + "    \n" + (String)indent);
                this.setCaret(start + 1 + ((String)indent).length() + INDENT.length(), false);
                return;
            }
            indent = (String)indent + INDENT;
        }
        this.replace(this.selectionStart(), this.selectionEnd(), "\n" + (String)indent);
    }

    private void indentSelection(boolean outdent) {
        this.snapshot(false);
        int first = this.lineOf(this.selectionStart());
        int last = this.lineOf(this.selectionEnd());
        if (this.hasSelection() && this.selectionEnd() == this.lineStart(last) && last > first) {
            --last;
        }
        if (!this.hasSelection() && !outdent) {
            this.replace(this.caret, this.caret, INDENT);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int l = first; l <= last; ++l) {
            Object ln = this.line(l);
            if (outdent) {
                int strip;
                for (strip = 0; strip < INDENT.length() && strip < ((String)ln).length() && ((String)ln).charAt(strip) == ' '; ++strip) {
                }
                ln = ((String)ln).substring(strip);
            } else {
                ln = INDENT + (String)ln;
            }
            sb.append((String)ln);
            if (l >= last) continue;
            sb.append('\n');
        }
        int from = this.lineStart(first);
        int to = this.lineEnd(last);
        this.replace(from, to, sb.toString());
        this.anchor = from;
        this.caret = from + sb.length();
    }

    private void deleteBack(boolean word) {
        String before;
        if (this.hasSelection()) {
            this.snapshot(false);
            this.replace(this.selectionStart(), this.selectionEnd(), "");
            return;
        }
        if (this.caret == 0) {
            return;
        }
        this.snapshot(true);
        int from = word ? this.wordLeft(this.caret) : (!(before = this.text.substring(this.lineStart(this.caretLine()), this.caret)).isEmpty() && before.isBlank() && before.length() % INDENT.length() == 0 ? this.caret - INDENT.length() : this.caret - 1);
        this.replace(from, this.caret, "");
    }

    private void deleteForward(boolean word) {
        if (this.hasSelection()) {
            this.snapshot(false);
            this.replace(this.selectionStart(), this.selectionEnd(), "");
            return;
        }
        if (this.caret >= this.text.length()) {
            return;
        }
        this.snapshot(true);
        int to = word ? this.wordRight(this.caret) : this.caret + 1;
        this.replace(this.caret, to, "");
    }

    private void restore(Deque<String[]> from, Deque<String[]> to) {
        if (from.isEmpty()) {
            return;
        }
        to.push(new String[]{this.text, String.valueOf(this.caret)});
        String[] state = from.pop();
        this.text = state[0];
        this.rebuild();
        this.setCaret(Integer.parseInt(state[1]), false);
        this.modified = true;
    }

    public boolean key(KeyEvent event) {
        int key = event.key();
        boolean ctrl = event.hasControlDown() || InputConstants.isKeyDown((int)224) || InputConstants.isKeyDown((int)228);
        boolean shift = event.hasShiftDown() || InputConstants.isKeyDown((int)225) || InputConstants.isKeyDown((int)229);
        Minecraft mc = Minecraft.getInstance();
        if (ctrl) {
            switch (key) {
                case 4: {
                    this.anchor = 0;
                    this.caret = this.text.length();
                    return true;
                }
                case 6: 
                case 27: {
                    int s = this.selectionStart();
                    int e = this.selectionEnd();
                    if (s == e) {
                        int l = this.caretLine();
                        s = this.lineStart(l);
                        e = Math.min(this.text.length(), this.lineEnd(l) + 1);
                    }
                    mc.keyboardHandler.setClipboard(this.text.substring(s, e));
                    if (key == 27) {
                        this.snapshot(false);
                        this.replace(s, e, "");
                    }
                    return true;
                }
                case 25: {
                    String clip = mc.keyboardHandler.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        this.type(clip);
                    }
                    return true;
                }
                case 29: {
                    if (shift) {
                        this.restore(this.redo, this.undo);
                    } else {
                        this.restore(this.undo, this.redo);
                    }
                    return true;
                }
                case 28: {
                    this.restore(this.redo, this.undo);
                    return true;
                }
                case 7: {
                    this.snapshot(false);
                    int l = this.caretLine();
                    int col = this.caretColumn();
                    String ln = this.line(l);
                    this.replace(this.lineEnd(l), this.lineEnd(l), "\n" + ln);
                    this.moveTo(l + 1, col, false);
                    return true;
                }
            }
        }
        switch (key) {
            case 80: {
                if (this.hasSelection() && !shift) {
                    this.setCaret(this.selectionStart(), false);
                } else {
                    this.setCaret(ctrl ? this.wordLeft(this.caret) : this.caret - 1, shift);
                }
                return true;
            }
            case 79: {
                if (this.hasSelection() && !shift) {
                    this.setCaret(this.selectionEnd(), false);
                } else {
                    this.setCaret(ctrl ? this.wordRight(this.caret) : this.caret + 1, shift);
                }
                return true;
            }
            case 82: {
                this.moveTo(this.caretLine() - 1, this.caretColumn(), shift);
                return true;
            }
            case 81: {
                this.moveTo(this.caretLine() + 1, this.caretColumn(), shift);
                return true;
            }
            case 75: {
                this.moveTo(this.caretLine() - 15, this.caretColumn(), shift);
                return true;
            }
            case 78: {
                this.moveTo(this.caretLine() + 15, this.caretColumn(), shift);
                return true;
            }
            case 74: {
                if (ctrl) {
                    this.setCaret(0, shift);
                } else {
                    int first;
                    int l = this.caretLine();
                    String ln = this.line(l);
                    for (first = 0; first < ln.length() && ln.charAt(first) == ' '; ++first) {
                    }
                    int target = this.caretColumn() == first ? 0 : first;
                    this.setCaret(this.lineStart(l) + target, shift);
                }
                return true;
            }
            case 77: {
                this.setCaret(ctrl ? this.text.length() : this.lineEnd(this.caretLine()), shift);
                return true;
            }
            case 42: {
                this.deleteBack(ctrl);
                return true;
            }
            case 76: {
                this.deleteForward(ctrl);
                return true;
            }
            case 40: 
            case 88: {
                this.newline();
                return true;
            }
            case 43: {
                this.indentSelection(shift);
                return true;
            }
        }
        return false;
    }

    private int wordLeft(int pos) {
        int p;
        for (p = pos; p > 0 && !CodeEditor.isWord(this.text.charAt(p - 1)); --p) {
        }
        while (p > 0 && CodeEditor.isWord(this.text.charAt(p - 1))) {
            --p;
        }
        return p;
    }

    private int wordRight(int pos) {
        int p;
        for (p = pos; p < this.text.length() && !CodeEditor.isWord(this.text.charAt(p)); ++p) {
        }
        while (p < this.text.length() && CodeEditor.isWord(this.text.charAt(p))) {
            ++p;
        }
        return p;
    }

    private static boolean isWord(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}

