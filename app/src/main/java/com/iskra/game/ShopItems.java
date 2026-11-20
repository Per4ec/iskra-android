package com.iskra.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Магазин за звёзды (порт массива SHOP из оригинала).
 */
public class ShopItems {

    public final String id;
    public final String n;
    public final String info;
    public final int base;
    public final int step;
    public final boolean premium;

    public ShopItems(String id, String n, String info, int base, int step, boolean premium) {
        this.id = id; this.n = n; this.info = info;
        this.base = base; this.step = step; this.premium = premium;
    }

    public int price(int lv) {
        return base + step * lv;
    }

    public String desc(int lv) {
        switch (id) {
            case "dmg": return String.format(Locale.US, "+%d%% → +%d%%", 7 * lv, 7 * (lv + 1));
            case "rate": return String.format(Locale.US, "+%d%% → +%d%%", 6 * lv, 6 * (lv + 1));
            case "hp": return String.format(Locale.US, "+%d → +%d HP", 15 * lv, 15 * (lv + 1));
            case "mag": return String.format(Locale.US, "×%.1f → ×%.1f", 1 + 0.12 * lv, 1 + 0.12 * (lv + 1));
            case "xpm": return String.format(Locale.US, "+%d%% → +%d%%", 7 * lv, 7 * (lv + 1));
            case "arm": return String.format(Locale.US, "−%d%% → −%d%%", 5 * lv, 5 * (lv + 1));
            case "dcd": return String.format(Locale.US, "%.2fс → %.2fс", Math.max(1, 2.2 * Math.pow(.93, lv)), Math.max(1, 2.2 * Math.pow(.93, lv + 1)));
            case "overcharge": return String.format(Locale.US, "%d → %d выстрел(а)", 1 + lv, 1 + lv + 1);
            case "gravwell": return String.format(Locale.US, "×%.1f → ×%.1f", 1 + 0.5 * lv, 1 + 0.5 * (lv + 1));
            case "avenger": return String.format(Locale.US, "+%d%% → +%d%% при HP<50%%", 30 * lv, 30 * (lv + 1));
            case "crit": return String.format(Locale.US, "%d%% → %d%% крит", 2 * lv, 2 * (lv + 1));
            case "regen": return String.format(Locale.US, "+%.1f → +%.1f HP/с", 0.1 * lv, 0.1 * (lv + 1));
            case "spd": return String.format(Locale.US, "+%d%% → +%d%%", 3 * lv, 3 * (lv + 1));
            case "lucky": return String.format(Locale.US, "+%d%% → +%d%%", 2 * lv, 2 * (lv + 1));
            default: return "";
        }
    }

    /** Целая часть (до стрелки) для состояния MAX. */
    public String descCur(int lv) {
        String s = desc(lv);
        int p = s.indexOf('→');
        return p >= 0 ? s.substring(0, p).trim() : s;
    }

    public void fx(GameView g) {
        GameView.St st = g.st;
        switch (id) {
            case "dmg": st.dmg *= 1.07; break;
            case "rate": st.rate *= .94; break;
            case "hp": st.hpm += 15; g.hp += 15; break;
            case "mag": st.mag *= 1.12; break;
            case "xpm": st.xpm *= 1.07; break;
            case "arm": st.arm *= .95; break;
            case "dcd": st.dcd *= .93; break;
            case "overcharge": st.shots++; break;
            case "gravwell": st.mag *= 1.5; break;
            case "avenger": st.bers += 1; break;
            case "crit": st.crit = Math.min(.75f, st.crit + .02f); break;
            case "regen": st.regen += .1f; break;
            case "spd": st.spd *= 1.03f; break;
            case "lucky": st.lucky += .02f; break;
        }
    }

    public static List<ShopItems> all() {
        List<ShopItems> l = new ArrayList<>();
        l.add(new ShopItems("dmg", "Калибровка", "Весь урон от оружия и клинков растёт.", 3, 1, false));
        l.add(new ShopItems("rate", "Форсаж", "Оружие стреляет чаще.", 3, 1, false));
        l.add(new ShopItems("hp", "Резерв", "Увеличивает максимум здоровья и сразу лечит.", 4, 2, false));
        l.add(new ShopItems("mag", "Гравитация", "Кристаллы опыта притягиваются с большего расстояния.", 2, 1, false));
        l.add(new ShopItems("xpm", "Резонанс", "Каждый кристалл даёт больше опыта.", 2, 1, false));
        l.add(new ShopItems("arm", "Покрытие", "Снижает получаемый урон.", 4, 2, false));
        l.add(new ShopItems("dcd", "Импульс", "Рывок восстанавливается быстрее.", 3, 1, false));
        l.add(new ShopItems("overcharge", "Оверчардж", "Добавляет один выстрел в каждую очередь.", 12, 6, true));
        l.add(new ShopItems("gravwell", "Гравитационный колодец", "Усиливает притяжение кристаллов.", 10, 6, true));
        l.add(new ShopItems("avenger", "Мститель", "Усиливает урон, когда здоровья меньше половины.", 14, 8, true));
        l.add(new ShopItems("crit", "Прицел", "Повышает шанс критического удара.", 3, 1, false));
        l.add(new ShopItems("regen", "Заживление", "Здоровье восстанавливается со временем.", 4, 2, false));
        l.add(new ShopItems("spd", "Лёгкость", "Персонаж двигается быстрее.", 3, 1, false));
        l.add(new ShopItems("lucky", "Фортуна", "Шанс выпадения дополнительного кристалла.", 4, 2, false));
        return l;
    }
}
