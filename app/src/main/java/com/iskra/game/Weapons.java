package com.iskra.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Оружейная (порт WD/EFD/TN/TD/TR/TC из оригинала).
 */
public class Weapons {

    /** Описание типа оружия. */
    public static class Def {
        public final String id, n;
        public final float r;    // скорострельность (множитель кулдауна)
        public final float dm;   // множитель урона
        public final float s;    // скорость снаряда
        public final float l;    // время жизни снаряда
        public final int p;      // базовый пробой
        public final int h;      // оттенок для эффектов
        public final int c;      // цвет
        public final float spr;  // разброс
        public final int cnt;    // число снарядов
        public final boolean ln; // луч (рисовать штрих)
        public final boolean rk; // ракета (AoE при попадании)
        public final boolean bm; // бумеранг
        public final float sz;   // размер

        Def(String id, String n, float r, float dm, float s, float l, int p, int h, int c,
            float spr, int cnt, boolean ln, boolean rk, boolean bm, float sz) {
            this.id = id; this.n = n; this.r = r; this.dm = dm; this.s = s; this.l = l;
            this.p = p; this.h = h; this.c = c; this.spr = spr; this.cnt = cnt;
            this.ln = ln; this.rk = rk; this.bm = bm; this.sz = sz;
        }
    }

    public static final Map<String, Def> WD = new LinkedHashMap<>();
    static {
        WD.put("pist",   new Def("pist",   "Импульсник", .8f,  1.25f, 640,  1.1f, 0, 38, 0xFFFFC76B, 0, 1, false, false, false, 1));
        WD.put("smg",    new Def("smg",    "Рой",        .26f, .42f,  720,  .9f,  0, 168, 0xFF8FF0D4, .14f, 0, false, false, false, 1));
        WD.put("shot",   new Def("shot",   "Дробовик",   1.5f, .6f,   600,  .5f,  0, 28, 0xFFFF8A63, .55f, 6, false, false, false, 1));
        WD.put("snip",   new Def("snip",   "Рельса",     2.3f, 5f,    1500, .8f,  4, 200, 0xFF5FD0FF, 0, 0, true,  false, false, 1));
        WD.put("rock",   new Def("rock",   "Ракетница",  1.9f, 2.4f,  380,  1.5f, 0, 16, 0xFFFF5F3D, 0, 0, false, true,  false, 1.6f));
        WD.put("lase",   new Def("lase",   "Лазер",      1f,   1.5f,  2400, .22f, 99, 300, 0xFFFF7AD9, 0, 0, true,  false, false, 1));
        WD.put("boom",   new Def("boom",   "Бумеранг",   1.1f, 1.9f,  560,  1.4f, 99, 90, 0xFFC9F57A, 0, 0, false, false, true,  1.5f));
        WD.put("flam",   new Def("flam",   "Огнемёт",    .1f,  .3f,   340,  .4f,  99, 20, 0xFFFF9A6A, .4f, 0, false, false, false, 1.7f));
        WD.put("volley", new Def("volley", "Залп",       1.2f, 1f,    700,  .8f,  1, 120, 0xFFFFB3D9, .3f, 3, false, false, false, 1));
        WD.put("stinger",new Def("stinger","Жало",       .18f, .25f,  800,  .7f,  0, 200, 0xFFB7FFE9, .18f, 0, false, false, false, 1));
        WD.put("tesla",  new Def("tesla",  "Тесла",      .9f,  .9f,   1100, .5f,  2, 260, 0xFF7FD6FF, 0, 0, true,  false, false, 1));
        WD.put("mine",   new Def("mine",   "Мина",       2.6f, 4.2f,  200,  2.2f, 0, 40, 0xFFFFD166, 0, 0, false, true,  false, 2.2f));
        WD.put("cryo",   new Def("cryo",   "Криоган",    .7f,  .7f,   520,  1f,   1, 190, 0xFFA8F0FF, .22f, 2, false, false, false, 1));
        WD.put("vdark",  new Def("vdark",  "Пустота",    1.4f, 2.8f,  460,  1.2f, 3, 280, 0xFFB07BFF, 0, 0, false, false, false, 1.3f));
    }

    /** Эффекты оружия. */
    public static class FxDef {
        public final String k, n;
        public final int h;
        FxDef(String k, String n, int h) { this.k = k; this.n = n; this.h = h; }
    }

    public static final Map<String, FxDef> EF = new LinkedHashMap<>();
    static {
        String[][] d = {
            {"freeze", "Заморозка", "195"}, {"shock", "Электричество", "55"},
            {"burn", "Огонь", "18"}, {"poison", "Яд", "120"},
            {"blast", "Взрыв", "30"}, {"leech", "Вампиризм", "350"},
            {"push", "Отдача", "210"}, {"rico", "Рикошет", "160"},
            {"mark", "Метка", "280"}, {"split", "Раскол", "330"},
            {"stun", "Оглушение", "230"}, {"bleed", "Кровотечение", "350"},
            {"vuln", "Уязвимость", "40"}, {"quake", "Землетрясение", "25"},
            {"chain", "Цепь", "200"}, {"frost", "Мороз", "205"},
            {"heat", "Жар", "12"},
        };
        for (String[] e : d) EF.put(e[0], new FxDef(e[0], e[1], Integer.parseInt(e[2])));
    }

    public static final String[] TN = {"I", "II", "III", "IV"};
    public static final float[] TD = {1, 1.7f, 2.6f, 3.8f};
    public static final float[] TR = {1, .9f, .8f, .7f};
    public static final int[] TC = {6, 16, 38, 80};

    /** Экземпляр оружия. */
    public static class Wpn {
        public String u;      // уникальный id
        public String id;     // ключ WD
        public int t;         // уровень 1..4
        public List<int[]> fx = new ArrayList<>(); // [0]=ключ эффекта, [1]=уровень
        public int cost;
        public boolean sold;  // куплено из витрины

        public Wpn copy() {
            Wpn w = new Wpn();
            w.u = u; w.id = id; w.t = t; w.cost = cost; w.sold = sold;
            w.fx = new ArrayList<>();
            for (int[] f : fx) w.fx.add(new int[]{f[0], f[1]});
            return w;
        }
    }

    /** Характеристики экземпляра для карточки. */
    public static String statsLine(GameView g, Wpn w) {
        Def d = WD.get(w.id);
        float dmg = g.st.dmg * d.dm * TD[w.t - 1];
        String s = "Урон " + Math.round(dmg);
        if (d.cnt > 0) s += "×" + d.cnt;
        s += " · " + String.format(Locale.US, "%.1f", 1f / (g.st.rate * d.r * TR[w.t - 1])) + "/с";
        return s;
    }

    public static String fxName(int idx) {
        String[] keys = EF.keySet().toArray(new String[0]);
        FxDef f = EF.get(keys[idx]);
        return f == null ? "" : f.n;
    }
}
