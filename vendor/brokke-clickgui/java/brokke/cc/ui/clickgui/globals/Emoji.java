/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class Emoji {
    public static final List<Group> GROUPS = List.of(Emoji.group(128512, "1f600 1f603 1f604 1f601 1f606 1f605 1f602 1f923 1f60a 1f607 1f642 1f609 1f60d 1f970 1f618 1f60b 1f61c 1f92a 1f60e 1f913 1f929 1f973 1f60f 1f612 1f644 1f62c 1f614 1f622 1f62d 1f621 1f92c 1f631 1f633 1f914 1f92b 1f971 1f634 1f480 1f47b 1f921 1f608 1f4a9 1f47d 1f916"), Emoji.group(128077, "1f44d 1f44e 1f44c 1f91d 1f44f 1f64f 1f4aa 1f44b 270c 1f91e 1f918 1f919 1f448 1f449 1f446 1f447 270a 1f44a 1f64c 1f595"), Emoji.group(10084, "2764 1f9e1 1f49b 1f49a 1f499 1f49c 1f5a4 1f90d 1f494 1f495 1f496 1f498 1f525 2728 2b50 1f4af 1f4a5 1f4a2 1f4a4 2705 274c 2757 2753 1f389"), Emoji.group(127918, "1f3ae 1f3c6 1f947 2694 1f5e1 1f6e1 1f3f9 1f48e 1f4b0 1f4b8 1f381 1f451 1f680 1f4a3 1f9e8 26a1 1f3af 1f3b2 1f9ea 1f9ed 1f5fa 23f0 1f512 1f511"), Emoji.group(128054, "1f436 1f431 1f98a 1f43a 1f43b 1f43c 1f428 1f981 1f42f 1f437 1f438 1f435 1f414 1f427 1f40d 1f422 1f41d 1f98b 1f40c 1f577 1f988 1f409 1f984 1f42c"), Emoji.group(127829, "1f34e 1f34c 1f349 1f353 1f352 1f351 1f34d 1f965 1f955 1f33d 1f354 1f35f 1f355 1f32d 1f32e 1f357 1f356 1f969 1f363 1f369 1f36a 1f382 1f37a 2615"));
    private static final Map<Integer, Identifier> TEXTURES = new HashMap<Integer, Identifier>();

    private Emoji() {
    }

    private static Group group(int icon, String codes) {
        String[] parts = codes.split(" ");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            arr[i] = Integer.parseInt(parts[i], 16);
        }
        return new Group(Integer.toHexString(icon), arr);
    }

    public static Identifier texture(int code) {
        return TEXTURES.get(code);
    }

    public static Identifier texture(String hex) {
        return TEXTURES.get(Integer.parseInt(hex, 16));
    }

    public static String string(int code) {
        return new String(Character.toChars(code));
    }

    public static List<Segment> split(String text) {
        ArrayList<Segment> out = new ArrayList<Segment>();
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            if (cp == 65039 || cp == 8205) {
                i += len;
                continue;
            }
            if (TEXTURES.containsKey(cp)) {
                if (!run.isEmpty()) {
                    out.add(new Segment(run.toString(), -1));
                    run.setLength(0);
                }
                out.add(new Segment("", cp));
            } else if (Character.isSupplementaryCodePoint(cp)) {
                run.append('?');
            } else {
                run.appendCodePoint(cp);
            }
            i += len;
        }
        if (!run.isEmpty()) {
            out.add(new Segment(run.toString(), -1));
        }
        return out;
    }

    static {
        for (Group g : GROUPS) {
            for (int code : g.codes()) {
                TEXTURES.put(code, Identifier.fromNamespaceAndPath((String)"brokke", (String)("textures/emoji/" + Integer.toHexString(code) + ".png")));
            }
        }
    }

    public record Group(String icon, int[] codes) {
    }

    public record Segment(String text, int emoji) {
        public boolean isEmoji() {
            return this.emoji >= 0;
        }
    }
}

