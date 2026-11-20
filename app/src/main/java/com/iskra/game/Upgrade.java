package com.iskra.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Усиления при повышении уровня (порт массива UP из оригинала).
 * r: 0 — обычное, 1 — редкое, 2 — эпик.
 */
public class Upgrade {

    public final String id;
    public final int r;
    public final String n;
    public final int c;   // цвет
    public final String d; // описание

    public Upgrade(String id, int r, String n, int c, String d) {
        this.id = id; this.r = r; this.n = n; this.c = c; this.d = d;
    }

    /** Строка эффекта: текущий → следующий уровень. */
    public String s(int lv) {
        switch (id) {
            case "dmg": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(30 * lv), Math.round(30 * (lv + 1)));
            case "rate": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(28 * lv), Math.round(28 * (lv + 1)));
            case "spd": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(14 * lv), Math.round(14 * (lv + 1)));
            case "mag": return String.format(Locale.US, "×%.1f → ×%.1f", 1 + 0.7 * lv, 1 + 0.7 * (lv + 1));
            case "hp": return String.format(Locale.US, "+%d → +%d HP", 30 * lv, 30 * (lv + 1));
            case "xpm": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(30 * lv), Math.round(30 * (lv + 1)));
            case "bspd": return String.format(Locale.US, "×%.2f → ×%.2f", 1 + 0.25 * lv, 1 + 0.25 * (lv + 1));
            case "bsz": return String.format(Locale.US, "×%.2f → ×%.2f", 1 + 0.25 * lv, 1 + 0.25 * (lv + 1));
            case "arm": return String.format(Locale.US, "−%d%% → −%d%%", Math.round((1 - Math.pow(.88, lv)) * 100), Math.round((1 - Math.pow(.88, lv + 1)) * 100));
            case "lucky": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(20 * lv), Math.round(20 * (lv + 1)));
            case "metabolism": return String.format(Locale.US, "+%.1f → +%.1f HP/с", 0.4 * lv, 0.4 * (lv + 1));
            case "shots": return String.format(Locale.US, "%d → %d снаряд(а)", 1 + lv, 1 + lv + 1);
            case "orb": return String.format(Locale.US, "%d → %d клинок", lv, lv + 1);
            case "obd": return String.format(Locale.US, "×%.1f → ×%.1f", 1 + 0.5 * lv, 1 + 0.5 * (lv + 1));
            case "nova": return String.format(Locale.US, "%.1fс → %.1fс", 9 - 1.4 * lv, 9 - 1.4 * (lv + 1));
            case "pierce": return String.format(Locale.US, "%d → %d враг(а)", lv, lv + 1);
            case "crit": return String.format(Locale.US, "%d%% → %d%% шанс", Math.round(15 * lv), Math.round(15 * (lv + 1)));
            case "regen": return String.format(Locale.US, "+%.1f → +%.1f HP/с", 1.5 * lv, 1.5 * (lv + 1));
            case "dcd": return String.format(Locale.US, "%.2fс → %.2fс", Math.max(1, 2.2 * Math.pow(.75, lv)), Math.max(1, 2.2 * Math.pow(.75, lv + 1)));
            case "vamp": return String.format(Locale.US, "+%.1f → +%.1f HP", 0.5 * lv, 0.5 * (lv + 1));
            case "ram": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "thorn": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "aura": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "rico": return String.format(Locale.US, "%d → %d отскок(а)", lv, lv + 1);
            case "shield": return String.format(Locale.US, "%d → %d щит(а)", lv, lv + 1);
            case "bers": return String.format(Locale.US, "+%d%% → +%d%% при HP<50%%", Math.round(35 * lv), Math.round(35 * (lv + 1)));
            case "burst": return String.format(Locale.US, "%d%% → %d%% шанс", Math.round(15 * lv), Math.round(15 * (lv + 1)));
            case "shotgun": return String.format(Locale.US, "+%d → +%d дробь", lv, lv + 1);
            case "drill": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(25 * lv), Math.round(25 * (lv + 1)));
            case "dome": return String.format(Locale.US, "%d → %d HP щита", lv * 5, (lv + 1) * 5);
            case "gravity": return String.format(Locale.US, "+%d%% → +%d%%", Math.round(8 * lv), Math.round(8 * (lv + 1)));
            case "drone": return String.format(Locale.US, "%d → %d дрон(а)", lv, lv + 1);
            case "bolt": return String.format(Locale.US, "%d → %d заряд(а)", lv, lv + 1);
            case "boom": return String.format(Locale.US, "+%d%% → +%d%% AoE", Math.round(40 * lv), Math.round(40 * (lv + 1)));
            case "missile": return String.format(Locale.US, "%d → %d ракета", lv, lv + 1);
            case "rev": return String.format(Locale.US, "%d → %d воскрешение", lv, lv + 1);
            case "dodge": return String.format(Locale.US, "%d%% → %d%% шанс", Math.round(6 * lv), Math.round(6 * (lv + 1)));
            case "bounty": return String.format(Locale.US, "+%d%% → +%d%% звёзд", 5 * lv, 5 * (lv + 1));
            case "knock": return String.format(Locale.US, "+%d%% → +%d%%", 50 * lv, 50 * (lv + 1));
            case "reap": return String.format(Locale.US, "%d%% → %d%% HP", 6 * lv, 6 * (lv + 1));
            case "zoom": return String.format(Locale.US, "+%d%% → +%d%% обзора", 13 * lv, 13 * (lv + 1));
            case "chill": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "ignite": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "arc": return String.format(Locale.US, "%d%% → %d%% шанс", 12 * lv, 12 * (lv + 1));
            case "exec": return String.format(Locale.US, "<%d%% → <%d%% HP", 7 * lv, 7 * (lv + 1));
            case "dnova": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "dlen": return String.format(Locale.US, "+%d%% → +%d%%", 18 * lv, 18 * (lv + 1));
            case "meteor": return String.format(Locale.US, "%d → %d ур.", lv, lv + 1);
            case "chrono": return String.format(Locale.US, "−%d%% → −%d%% скорость", 6 * lv, 6 * (lv + 1));
            default: return "";
        }
    }

    /** Можно ли взять ещё раз. */
    public boolean canTake(GameView g) {
        GameView.St st = g.st;
        switch (id) {
            case "bspd": return st.bv < 2.2;
            case "bsz": return st.bsz < 2;
            case "arm": return st.arm > .45;
            case "lucky": return st.lucky < .8;
            case "metabolism": return st.metabolism < 4;
            case "shots": return st.shots < 5;
            case "orb": return st.orbs < 6;
            case "obd": return st.orbs > 0 && st.obd < 3.5;
            case "nova": return st.nova < 5;
            case "pierce": return st.pierce < 4;
            case "crit": return st.crit < .75;
            case "regen": return st.regen < 6;
            case "dcd": return st.dcd > 1;
            case "vamp": return st.vamp < 1.5;
            case "ram": return st.ram < 4;
            case "thorn": return st.thorn < 4;
            case "aura": return st.aura < 4;
            case "rico": return st.rico < 4;
            case "shield": return st.shield < 3;
            case "bers": return st.bers < 3;
            case "burst": return st.burst < 4;
            case "shotgun": return st.shotgun < 3;
            case "drill": return st.drill < 3;
            case "dome": return st.dome < 4;
            case "gravity": return st.gravity < 3;
            case "drone": return st.drone < 4;
            case "bolt": return st.bolt < 5;
            case "boom": return st.boom < 4;
            case "missile": return st.missile < 3;
            case "rev": return st.rev < 2;
            case "dodge": return st.dodge < .35;
            case "bounty": return st.bounty < 5;
            case "knock": return st.knock < 4;
            case "reap": return st.reap < 4;
            case "zoom": return st.zoom < 4;
            case "chill": return st.chill < 3;
            case "ignite": return st.ignite < 3;
            case "arc": return st.arc < 4;
            case "exec": return st.exec < 3;
            case "dnova": return st.dnova < 3;
            case "dlen": return st.dlen < 4;
            case "meteor": return st.meteor < 4;
            case "chrono": return st.chrono < 3;
            default: return true;
        }
    }

    /** Применить эффект. */
    public void apply(GameView g) {
        GameView.St st = g.st;
        switch (id) {
            case "dmg": st.dmg *= 1.3; break;
            case "rate": st.rate *= .78; break;
            case "spd": st.spd *= 1.14; break;
            case "mag": st.mag *= 1.7; break;
            case "hp": st.hpm += 30; g.hp = Math.min(st.hpm, g.hp + 60); break;
            case "xpm": st.xpm *= 1.3; break;
            case "bspd": st.bv *= 1.25; break;
            case "bsz": st.bsz *= 1.25; break;
            case "arm": st.arm *= .88; break;
            case "lucky": st.lucky += .2; break;
            case "metabolism": st.metabolism += .4; break;
            case "shots": st.shots++; break;
            case "orb": st.orbs++; break;
            case "obd": st.obd *= 1.5; break;
            case "nova": st.nova++; g.nvT = Math.min(g.nvT, 1); break;
            case "pierce": st.pierce++; break;
            case "crit": st.crit += .15; break;
            case "regen": st.regen += 1.5; break;
            case "dcd": st.dcd *= .75; break;
            case "vamp": st.vamp += .5; break;
            case "ram": st.ram++; break;
            case "thorn": st.thorn++; break;
            case "aura": st.aura++; break;
            case "rico": st.rico++; break;
            case "shield": st.shield++; g.shc = st.shield; g.shT = 0; break;
            case "bers": st.bers++; break;
            case "burst": st.burst++; break;
            case "shotgun": st.shotgun++; break;
            case "drill": st.drill++; break;
            case "dome": st.dome++; break;
            case "gravity": st.gravity++; break;
            case "drone": st.drone++; break;
            case "bolt": st.bolt++; break;
            case "boom": st.boom++; break;
            case "missile": st.missile++; break;
            case "rev": st.rev++; break;
            case "dodge": st.dodge += .06; break;
            case "bounty": st.bounty++; break;
            case "knock": st.knock++; break;
            case "reap": st.reap++; break;
            case "zoom": st.zoom++; break;
            case "chill": st.chill++; break;
            case "ignite": st.ignite++; break;
            case "arc": st.arc++; break;
            case "exec": st.exec++; break;
            case "dnova": st.dnova++; break;
            case "dlen": st.dlen++; break;
            case "meteor": st.meteor++; break;
            case "chrono": st.chrono++; break;
        }
    }

    public static final String[] RAR = {"Обычное", "Редкое", "Эпик"};
    public static final int[] RARC = {0xFFA9B8FF, 0xFF4FE3FF, 0xFFFFB13D};

    public static List<Upgrade> all() {
        List<Upgrade> l = new ArrayList<>();
        l.add(new Upgrade("dmg", 0, "Мощь", 0xFFFF8A63, "Каждый выстрел бьёт сильнее."));
        l.add(new Upgrade("rate", 0, "Скорострел", 0xFFFFC76B, "Искра стреляет заметно чаще."));
        l.add(new Upgrade("spd", 0, "Ветер", 0xFF8FB4FF, "Легче уворачиваться от роя."));
        l.add(new Upgrade("mag", 0, "Магнит", 0xFFC9F57A, "Кристаллы сами летят к тебе."));
        l.add(new Upgrade("hp", 0, "Сердце", 0xFFFF6A86, "Максимум растёт и часть здоровья лечится."));
        l.add(new Upgrade("xpm", 0, "Мудрость", 0xFFD9FF5F, "Кристаллы дают больше опыта."));
        l.add(new Upgrade("bspd", 0, "Быстрый заряд", 0xFFFFD0A0, "Снаряды летят дальше и быстрее."));
        l.add(new Upgrade("bsz", 0, "Крупный калибр", 0xFFFFC2F0, "Проще попадать по мелким врагам."));
        l.add(new Upgrade("arm", 0, "Броня", 0xFFB8C4D9, "Удары врагов слабее."));
        l.add(new Upgrade("lucky", 0, "Удача", 0xFFFFF08A, "Иногда из врага падает вдвое больше."));
        l.add(new Upgrade("metabolism", 0, "Метаболизм", 0xFFA6FFB1, "Пассивное лечение вне боя."));
        l.add(new Upgrade("shots", 1, "Веер", 0xFF8FF0D4, "Больше снарядов за выстрел."));
        l.add(new Upgrade("orb", 1, "Клинки", 0xFFB58CFF, "Клинки кружат вокруг и рубят врагов."));
        l.add(new Upgrade("obd", 1, "Закалка клинков", 0xFFD6B8FF, "Клинки режут сильнее."));
        l.add(new Upgrade("nova", 1, "Нова", 0xFFFF7AD9, "Взрывная волна отбрасывает и ранит."));
        l.add(new Upgrade("pierce", 1, "Пробой", 0xFFFFFFFF, "Снаряды проходят сквозь врагов."));
        l.add(new Upgrade("crit", 1, "Крит", 0xFFFFB347, "Удачные попадания бьют вдвое."));
        l.add(new Upgrade("regen", 1, "Регенерация", 0xFF6DFFB0, "Раны затягиваются сами."));
        l.add(new Upgrade("dcd", 1, "Лёгкие ноги", 0xFF5FD0FF, "Рывок восстанавливается быстрее."));
        l.add(new Upgrade("vamp", 1, "Вампир", 0xFFFF5C7C, "Каждое убийство лечит."));
        l.add(new Upgrade("ram", 1, "Таран", 0xFFFF9D5F, "Во время рывка враги получают урон."));
        l.add(new Upgrade("thorn", 1, "Шипы", 0xFFFF6B9A, "Тот, кто тебя тронул, получает урон."));
        l.add(new Upgrade("aura", 1, "Ледяная аура", 0xFF8FE8FF, "Холод замедляет всех вокруг тебя."));
        l.add(new Upgrade("rico", 1, "Рикошет", 0xFFA6FFCF, "Снаряд после попадания летит в другого врага."));
        l.add(new Upgrade("shield", 1, "Щит", 0xFF78DCFF, "Щит восстанавливается каждые 12 секунд."));
        l.add(new Upgrade("bers", 1, "Берсерк", 0xFFFF5C5C, "Чем хуже дела, тем злее удары."));
        l.add(new Upgrade("burst", 1, "Двойной выстрел", 0xFFFFD2A6, "Шанс выстрелить второй пулей."));
        l.add(new Upgrade("shotgun", 1, "Дробовик", 0xFFFFB26B, "Выстрел бьёт веером дробин."));
        l.add(new Upgrade("drill", 1, "Дрель", 0xFFFFE0A3, "Каждое повторное попадание бьёт сильнее."));
        l.add(new Upgrade("dome", 1, "Купол", 0xFFA2C7FF, "Купол восстанавливается вне боя."));
        l.add(new Upgrade("gravity", 1, "Тяжесть", 0xFFB1A3FF, "Враги вокруг двигаются медленнее."));
        l.add(new Upgrade("drone", 2, "Дрон-стрелок", 0xFF6DFFF3, "Летает рядом и сам стреляет по врагам."));
        l.add(new Upgrade("bolt", 2, "Молния", 0xFFE3F0FF, "Молния бьёт случайных врагов вокруг."));
        l.add(new Upgrade("boom", 2, "Взрывные снаряды", 0xFFFF8A3D, "Каждое попадание взрывается."));
        l.add(new Upgrade("missile", 2, "Ракета", 0xFFFF5F3D, "Самонаводящаяся ракета летит в случайного врага."));
        l.add(new Upgrade("rev", 2, "Второй шанс", 0xFFFFC76B, "Один раз вернёт тебя с половиной здоровья и взрывом."));
        l.add(new Upgrade("dodge", 0, "Уклонение", 0xFF9FE8FF, "Иногда удар врага проходит мимо."));
        l.add(new Upgrade("bounty", 0, "Охотник", 0xFFFFE066, "Враги чаще роняют звёзды."));
        l.add(new Upgrade("knock", 0, "Отдача", 0xFFB0C4FF, "Снаряды сильнее отбрасывают врагов."));
        l.add(new Upgrade("reap", 0, "Жатва", 0xFFA6FFB1, "Каждые 20 убийств лечат тебя."));
        l.add(new Upgrade("zoom", 1, "Дальний обзор", 0xFF7AD7FF, "Камера отдаляется — рой виден раньше."));
        l.add(new Upgrade("chill", 1, "Ледяные пули", 0xFF8FE8FF, "Попадания замораживают врагов."));
        l.add(new Upgrade("ignite", 1, "Зажигалка", 0xFFFF8A3D, "Попадания поджигают врагов."));
        l.add(new Upgrade("arc", 1, "Дуга", 0xFFE3F0FF, "Попадание перескакивает молнией на соседа."));
        l.add(new Upgrade("exec", 1, "Палач", 0xFFFF5C5C, "Раненые враги добиваются сразу."));
        l.add(new Upgrade("dnova", 1, "Рывок-нова", 0xFF5FD0FF, "Рывок рождает взрывную волну."));
        l.add(new Upgrade("dlen", 1, "Длинный рывок", 0xFF6DFFF3, "Рывок уносит тебя дальше."));
        l.add(new Upgrade("meteor", 2, "Метеоритный дождь", 0xFFFF7A3D, "С неба падают метеориты на врагов."));
        l.add(new Upgrade("chrono", 2, "Хроно-поле", 0xFFC9B8FF, "Все враги двигаются медленнее."));
        return l;
    }
}
