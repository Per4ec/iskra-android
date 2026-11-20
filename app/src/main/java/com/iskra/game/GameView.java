package com.iskra.game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * ИСКРА — игровой движок и рендер на Canvas (порт оригинальной HTML-игры).
 * Все экраны (меню, бой, прокачка, магазин, настройки) рисуются на одном View.
 */
public class GameView extends View implements Choreographer.FrameCallback {

    static final float TAU = 6.2832f;
    static final Random R = new Random();

    // ---------- состояния ----------
    static final int ST_MENU = 0, ST_HELP = 1, ST_LVL = 2, ST_PAUSE = 3,
            ST_OVER = 4, ST_SET = 5, ST_SHOP = 6, ST_WP = 7, ST_PLAY = 8, ST_DEAD = 9;
    int S = ST_MENU;

    // ---------- сущности ----------
    static class Enemy {
        int t; float x, y, r, hp, mh, sp, dm, xp, kx, ky, f, ob, mx, my, fz, bn, bd, ps, pd, mk, ct, bt, s, h;
        boolean el, d; int st;
    }

    static class Bullet {
        float x, y, vx, vy, l, m, dm, sz;
        int p, rc, hu;
        boolean ln, rk, bm, db, sg, ms, rv, dr;
        List<Enemy> ids = new ArrayList<>();
        List<int[]> fx;
        Enemy tgt;
    }

    static class EB { float x, y, vx, vy, l, m; }

    static class Gem { float x, y, sx, sy, tx, ty, w, v; boolean m, d; boolean init; }

    static class Part { float x, y, vx, vy, l, m, r, h; }

    static class FText { float x, y, l; String s; int c; }

    static class Nova { float x, y, r, m, h, dm; }

    static class Bolt { float x1, y1, x2, y2, l; }

    static class Pt { float x, y; }

    // ---------- статы ----------
    static class St {
        float dmg = 10, rate = .5f, spd = 190, mag = 80, hpm = 100, crit, regen, dcd = 2.2f,
                vamp, xpm = 1, bsz = 1, bv = 1, lucky, arm = 1, obd = 1, metabolism, dodge, bers;
        int shots = 1, pierce, orbs, nova, ram, thorn, aura, rico, shield, burst, shotgun,
                drill, dome, gravity, drone, bolt, boom, missile, rev, regLvl, bounty, knock,
                zoom, chill, ignite, arc, exec, dnova, dlen, reap, meteor, chrono;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                for (java.lang.reflect.Field f : St.class.getDeclaredFields()) {
                    if (f.getType() == float.class) o.put(f.getName(), f.getFloat(this));
                    else if (f.getType() == int.class) o.put(f.getName(), f.getInt(this));
                }
            } catch (Exception ignored) {}
            return o;
        }

        static St fromJson(JSONObject o) {
            St s = new St();
            try {
                for (java.lang.reflect.Field f : St.class.getDeclaredFields()) {
                    if (!o.has(f.getName())) continue;
                    if (f.getType() == float.class) f.setFloat(s, (float) o.getDouble(f.getName()));
                    else if (f.getType() == int.class) f.setInt(s, o.getInt(f.getName()));
                }
            } catch (Exception ignored) {}
            return s;
        }
    }

    // ---------- враги ----------
    static final float[][] TY = {
            {11, 12, 78, 8, 1, 330},   // 0 сфера
            {22, 70, 48, 16, 4, 26},   // 1 тяж
            {13, 22, 62, 12, 2, 268},  // 2 налётчик
            {56, 900, 52, 26, 36, 352},// 3 босс
            {14, 30, 58, 10, 3, 200},  // 4 стрелок
            {9, 9, 190, 7, 1, 55},     // 5 рой
    };

    // ---------- фон ----------
    static class Bg { String n, b; float[] h; String g; int c; }
    static final Bg[] BG = new Bg[5];
    static {
        BG[0] = mkBg("Ночь", "#0a0716", new float[]{262, 196, 338}, "150,120,255", 0xFF8B6BFF);
        BG[1] = mkBg("Океан", "#04101a", new float[]{190, 172, 210}, "110,200,255", 0xFF25B4FF);
        BG[2] = mkBg("Лава", "#170805", new float[]{16, 2, 32}, "255,150,105", 0xFFFF6234);
        BG[3] = mkBg("Лес", "#051208", new float[]{132, 158, 96}, "140,250,175", 0xFF35D472);
        BG[4] = mkBg("Мрак", "#0c0c11", new float[]{222, 214, 232}, "190,196,228", 0xFF9AA0B8);
    }
    static Bg mkBg(String n, String b, float[] h, String g, int c) {
        Bg x = new Bg(); x.n = n; x.b = b; x.h = h; x.g = g; x.c = c; return x;
    }
    static final int[] HEROES = {28, 168, 258, 330, 52, 200};

    // ---------- время и мир ----------
    float T, gt, slow = 1, hs, sh, fl, ang, zm = 1, acc, nextBoss = 60, fcd, nvT, mtT = 3;
    int kills, lvl = 1, rolls, shc;
    float xp, hp = 100, inv, dashT, dcd, shT;
    final Pt P = new Pt();
    float pvx, pvy, pfx = 1, pfy;
    final ArrayList<Pt> tr = new ArrayList<>();
    final Pt cam = new Pt();
    St st = new St();
    final ArrayList<Enemy> E = new ArrayList<>();
    final ArrayList<Bullet> B = new ArrayList<>();
    final ArrayList<EB> EBb = new ArrayList<>();
    final ArrayList<Gem> G = new ArrayList<>();
    final ArrayList<Part> F = new ArrayList<>();
    final ArrayList<FText> FT = new ArrayList<>();
    final ArrayList<Nova> NV = new ArrayList<>();
    final ArrayList<Bolt> LN = new ArrayList<>();
    final HashMap<Long, ArrayList<Enemy>> GR = new HashMap<>();

    // ---------- мета ----------
    int stars;
    final HashMap<String, Integer> shopLv = new HashMap<>();
    final HashMap<String, Integer> taken = new HashMap<>();
    final List<ShopItems> SHOP = ShopItems.all();
    final List<Upgrade> UPALL = Upgrade.all();
    List<Upgrade> pick = new ArrayList<>();
    Weapons.Wpn WQ;
    final ArrayList<Weapons.Wpn> VT = new ArrayList<>();
    final ArrayList<Weapons.Wpn> OF = new ArrayList<>();
    final ArrayList<String> SEL = new ArrayList<>();
    static final int VMAX = 6;
    float best;
    boolean hasSave;

    // ---------- настройки ----------
    int cfgBg, cfgHero = HEROES[0], cfgFx = 1, cfgGrid = 1, cfgShake = 1, cfgNums = 1, cfgFps, cfgCap = 500;
    float cfgVol = .8f;
    float fxV = 1, fxW = 1;
    int fxNow = 1;
    static final float[] FXF = {.4f, .72f, 1};

    // ---------- ввод ----------
    boolean joyOn; int joyId = -1; float joyOx, joyOy, joyX, joyY;

    // ---------- разметка UI ----------
    static class UiBtn {
        RectF r = new RectF(); String tag; String label; Object data; boolean enabled = true; int color;
    }
    final ArrayList<UiBtn> ui = new ArrayList<>();
    int setPrev = ST_MENU, wpPrev = ST_LVL;

    // ---------- служебное ----------
    int W, H;
    float last;
    long lastNs, lastFrNs;
    int fps, fc; long ft0;
    long lastSaveMs;
    float deadTimer;
    boolean running;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint ps = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final HashMap<String, RadialGradient> haloCache = new HashMap<>();
    RadialGradient vignette, heroGrad;
    SharedPreferences pref;
    UiBtn volSlider;

    public GameView(Context ctx) {
        super(ctx);
        pref = ctx.getSharedPreferences("iskra", Context.MODE_PRIVATE);
        loadMeta();
        loadCfg();
        Sfx.setVolume(cfgVol);
        hasSave = pref.contains("run");
        ps.setStyle(Paint.Style.STROKE);
        pt.setTypeface(Typeface.DEFAULT_BOLD);
        pt.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
        P.x = P.y = 0;
        lastNs = System.nanoTime();
        Choreographer.getInstance().postFrameCallback(this);
        running = true;
    }

    // ======================= утилиты =======================
    static float clamp(float v, float a, float b) { return Math.min(b, Math.max(a, v)); }
    static float dist(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    static float rnd() { return R.nextFloat(); }
    static int hsl(float h, float s, float l) { return hsl(h, s, l, 255); }
    static int hsl(float h, float s, float l, int a) {
        s /= 100f; l /= 100f;
        float v = l + s * Math.min(l, 1 - l);
        float sv = v == 0 ? 0 : 2f * (1 - l / v);
        return Color.HSVToColor(a, new float[]{h, sv, v});
    }
    static int argb(String hex) { return Color.parseColor(hex); }
    static String fmt(float s) { return (int) (s / 60) + ":" + String.format(Locale.US, "%02d", (int) (s % 60)); }
    int xpNeed() { return Math.round(2 + lvl * 2 + lvl * lvl * .3f); }
    int rollCost() { return 3 * (rolls + 1); }

    // ======================= сохранения =======================
    void loadMeta() {
        best = pref.getFloat("best", 0);
        stars = pref.getInt("stars", 0);
        try {
            JSONObject lv = new JSONObject(pref.getString("shopLv", "{}"));
            Iterator<String> it = lv.keys();
            while (it.hasNext()) { String k = it.next(); shopLv.put(k, lv.getInt(k)); }
        } catch (Exception ignored) {}
    }

    void saveMeta() {
        try {
            JSONObject lv = new JSONObject();
            for (Map.Entry<String, Integer> e : shopLv.entrySet()) lv.put(e.getKey(), e.getValue());
            pref.edit().putFloat("best", best)
                    .putInt("stars", Math.max(0, Math.round(stars)))
                    .putString("shopLv", lv.toString()).apply();
        } catch (Exception ignored) {}
    }

    void loadCfg() {
        cfgBg = pref.getInt("cfgBg", 0);
        cfgHero = pref.getInt("cfgHero", HEROES[0]);
        cfgFx = pref.getInt("cfgFx", 1);
        cfgGrid = pref.getInt("cfgGrid", 1);
        cfgShake = pref.getInt("cfgShake", 1);
        cfgNums = pref.getInt("cfgNums", 1);
        cfgFps = pref.getInt("cfgFps", 0);
        cfgCap = pref.getInt("cfgCap", 500);
        cfgVol = pref.getFloat("cfgVol", .8f);
        boolean ok = false;
        for (int h : HEROES) if (h == cfgHero) { ok = true; break; }
        if (!ok) cfgHero = HEROES[0];
        cfgBg = (int) clamp(cfgBg, 0, BG.length - 1);
        if (cfgCap != 60 && cfgCap != 144) cfgCap = 500;
    }

    void saveCfg() {
        pref.edit().putInt("cfgBg", cfgBg).putInt("cfgHero", cfgHero).putInt("cfgFx", cfgFx)
                .putInt("cfgGrid", cfgGrid).putInt("cfgShake", cfgShake).putInt("cfgNums", cfgNums)
                .putInt("cfgFps", cfgFps).putInt("cfgCap", cfgCap).putFloat("cfgVol", cfgVol).apply();
    }

    void saveRun() {
        if (S != ST_PLAY && S != ST_PAUSE) return;
        try {
            JSONObject o = new JSONObject();
            o.put("v", 1).put("gt", gt).put("lvl", lvl).put("xp", xp).put("hp", hp)
                    .put("kills", kills).put("stars", stars).put("rolls", rolls)
                    .put("px", P.x).put("py", P.y).put("nextBoss", nextBoss)
                    .put("shc", shc).put("shT", shT).put("drT", drT).put("blT", blT)
                    .put("mtT", mtT).put("st", st.toJson());
            JSONObject tk = new JSONObject();
            for (Map.Entry<String, Integer> e : taken.entrySet()) tk.put(e.getKey(), e.getValue());
            o.put("taken", tk);
            o.put("wq", WQ == null ? JSONObject.NULL : wpnToJson(WQ));
            org.json.JSONArray vt = new org.json.JSONArray();
            for (Weapons.Wpn w : VT) vt.put(wpnToJson(w));
            o.put("wv", vt);
            pref.edit().putString("run", o.toString()).apply();
            hasSave = true;
        } catch (Exception ignored) {}
    }

    JSONObject wpnToJson(Weapons.Wpn w) throws Exception {
        JSONObject o = new JSONObject();
        o.put("u", w.u).put("id", w.id).put("t", w.t).put("cost", w.cost);
        org.json.JSONArray fx = new org.json.JSONArray();
        for (int[] f : w.fx) fx.put(new org.json.JSONArray().put(f[0]).put(f[1]));
        o.put("fx", fx);
        return o;
    }

    Weapons.Wpn wpnFromJson(JSONObject o) {
        Weapons.Wpn w = new Weapons.Wpn();
        w.u = o.optString("u"); w.id = o.optString("id");
        w.t = o.optInt("t", 1); w.cost = o.optInt("cost", 0);
        org.json.JSONArray fx = o.optJSONArray("fx");
        if (fx != null) for (int i = 0; i < fx.length(); i++) {
            org.json.JSONArray a = fx.optJSONArray(i);
            if (a != null && a.length() >= 2) w.fx.add(new int[]{a.optInt(0), a.optInt(1)});
        }
        return w;
    }

    boolean loadRun() {
        try {
            String s = pref.getString("run", null);
            if (s == null) return false;
            JSONObject o = new JSONObject(s);
            if (o.optInt("v") != 1) return false;
            reset();
            gt = (float) o.optDouble("gt", 0);
            lvl = o.optInt("lvl", 1); xp = (float) o.optDouble("xp", 0);
            hp = (float) o.optDouble("hp", 100); kills = o.optInt("kills", 0);
            stars = o.optInt("stars", stars); rolls = o.optInt("rolls", 0);
            P.x = (float) o.optDouble("px", 0); P.y = (float) o.optDouble("py", 0);
            cam.x = P.x; cam.y = P.y;
            nextBoss = o.optInt("nextBoss", 60);
            shc = o.optInt("shc", 0); shT = (float) o.optDouble("shT", 0);
            drT = (float) o.optDouble("drT", 0); blT = (float) o.optDouble("blT", 0);
            mtT = (float) o.optDouble("mtT", 3);
            JSONObject stj = o.optJSONObject("st");
            if (stj != null) st = St.fromJson(stj);
            JSONObject tk = o.optJSONObject("taken");
            if (tk != null) {
                Iterator<String> it = tk.keys();
                while (it.hasNext()) { String k = it.next(); taken.put(k, tk.optInt(k)); }
            }
            JSONObject wq = o.optJSONObject("wq");
            WQ = wq == null ? null : wpnFromJson(wq);
            org.json.JSONArray vt = o.optJSONArray("wv");
            if (vt != null) for (int i = 0; i < vt.length(); i++) {
                JSONObject w = vt.optJSONObject(i);
                if (w != null) VT.add(wpnFromJson(w));
            }
            return true;
        } catch (Exception e) { return false; }
    }

    void clearRun() { pref.edit().remove("run").apply(); hasSave = false; }

    // ======================= цикл =======================
    @Override
    public void doFrame(long nowNs) {
        if (!running) return;
        Choreographer.getInstance().postFrameCallback(this);
        if (cfgCap < 500 && nowNs - lastFrNs < 1_000_000_000L / cfgCap - 2_000_000L) return;
        lastFrNs = nowNs;
        float rd = Math.min(.05f, (nowNs - lastNs) / 1e9f);
        lastNs = nowNs;
        T += rd;
        float dt = rd;
        if (hs > 0) { hs -= rd; dt *= .04f; }
        dt *= slow;
        fxV += (cfgFx - fxV) * Math.min(1, rd * 7);
        fxNow = Math.round(fxV);
        fxW = FXF[(int) clamp(Math.round(fxV), 0, 2)];
        zm += (1f / (1f + .13f * st.zoom) - zm) * Math.min(1, rd * 3);

        if (S == ST_PLAY || S == ST_MENU || S == ST_DEAD) sim(dt);
        float tx = P.x + pvx * .25f, ty = P.y + pvy * .25f;
        cam.x += (tx - cam.x) * Math.min(1, rd * 6);
        cam.y += (ty - cam.y) * Math.min(1, rd * 6);
        invalidate();

        if (S == ST_PLAY && nowNs / 1_000_000 - lastSaveMs > 4000) {
            lastSaveMs = nowNs / 1_000_000;
            saveRun();
        }
        fc++;
        if (nowNs - ft0 > 500_000_000L) {
            fps = Math.round(fc * 1e9f / (nowNs - ft0));
            fc = 0; ft0 = nowNs;
        }
    }

    public void onActivityPause() {
        if (S == ST_PLAY) pauseGame();
        saveRun();
        saveMeta();
    }

    public void onActivityResume() {
        lastNs = System.nanoTime();
    }

    public void destroy() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        Sfx.release();
    }

    void pauseGame() {
        if (S == ST_PLAY) { S = ST_PAUSE; saveRun(); }
        else if (S == ST_PAUSE) S = ST_PLAY;
    }

    // ======================= ввод =======================
    UiBtn btnAt(float x, float y) {
        for (int i = ui.size() - 1; i >= 0; i--) {
            UiBtn b = ui.get(i);
            if (b.enabled && b.r.contains(x, y)) return b;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        float x = e.getX(), y = e.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                UiBtn b = btnAt(x, y);
                if (b != null) { onBtn(b); return true; }
                if (S == ST_PLAY) {
                    joyOn = true; joyId = e.getPointerId(0);
                    joyOx = joyX = x; joyOy = joyY = y;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (volSlider != null && volSlider.r.contains(x, y) && S == ST_SET) {
                    float k = clamp((x - volSlider.r.left) / volSlider.r.width(), 0, 1);
                    cfgVol = k; Sfx.setVolume(cfgVol);
                    return true;
                }
                if (joyOn && e.getPointerId(e.getActionIndex()) == joyId) { joyX = x; joyY = y; }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = e.getActionIndex();
                if (joyOn && e.getPointerId(idx) == joyId) joyOn = false;
                return true;
            }
        }
        return true;
    }

    void onBtn(UiBtn b) {
        String t = b.tag;
        switch (t) {
            case "start":
                clearRun();
                startGame();
                break;
            case "resume_run":
                if (loadRun()) { S = ST_PLAY; Sfx.snd(660, .3f, 0, .06f, 400); }
                break;
            case "help": S = ST_HELP; break;
            case "menu_from_help": case "menu_from_over":
                saveMeta(); S = ST_MENU; break;
            case "open_shop": S = ST_SHOP; Sfx.snd(620, .14f, 3, .05f, 180); break;
            case "close_shop": S = ST_MENU; saveMeta(); break;
            case "set": setPrev = ST_MENU; S = ST_SET; break;
            case "set_pause": setPrev = ST_PAUSE; S = ST_SET; break;
            case "set_back": S = setPrev; saveCfg(); break;
            case "pause": pauseGame(); break;
            case "resume": S = ST_PLAY; break;
            case "quit": saveRun(); saveMeta(); resetAll(); S = ST_MENU; break;
            case "again": startGame(); break;
            case "dash": dash(); break;
            case "roll": doRoll(); break;
            case "wp_open": wpPrev = ST_LVL; SEL.clear(); S = ST_WP; break;
            case "wp_close": S = wpPrev; break;
            case "wp_equip": wpEquip(); break;
            case "wp_vault": wpVault(); break;
            case "wp_sell": wpSell(); break;
            case "wp_merge": wpMerge(); break;
            default:
                if (t.startsWith("card")) {
                    int i = Integer.parseInt(t.substring(4));
                    if (i < pick.size()) choose(pick.get(i));
                } else if (t.startsWith("buy")) {
                    int i = Integer.parseInt(t.substring(3));
                    if (i < OF.size()) buyW(OF.get(i));
                } else if (t.startsWith("wsel:")) {
                    String u = t.substring(5);
                    int k = SEL.indexOf(u);
                    if (k >= 0) SEL.remove(k);
                    else { SEL.add(u); if (SEL.size() > 2) SEL.remove(0); }
                    Sfx.snd(520, .06f, 3, .03f, 100);
                } else if (t.startsWith("sh:")) {
                    buyShop(t.substring(3));
                } else if (t.startsWith("bg")) {
                    cfgBg = Integer.parseInt(t.substring(2));
                    saveCfg(); Sfx.snd(520 + cfgBg * 60, .1f, 0, .04f, 80);
                } else if (t.startsWith("hero")) {
                    cfgHero = Integer.parseInt(t.substring(4));
                    saveCfg(); Sfx.snd(400 + cfgHero * 2, .12f, 0, .04f, 90);
                } else if (t.startsWith("fx")) {
                    cfgFx = Integer.parseInt(t.substring(2)); saveCfg(); Sfx.snd(560, .08f, 3, .04f, 120);
                } else if (t.startsWith("cap")) {
                    cfgCap = Integer.parseInt(t.substring(3)); saveCfg(); Sfx.snd(560, .08f, 3, .04f, 120);
                } else if (t.startsWith("tg:")) {
                    String k = t.substring(3);
                    if (k.equals("grid")) cfgGrid ^= 1;
                    else if (k.equals("shake")) cfgShake ^= 1;
                    else if (k.equals("nums")) cfgNums ^= 1;
                    else if (k.equals("fps")) cfgFps ^= 1;
                    saveCfg(); Sfx.snd(560, .08f, 3, .04f, 120);
                }
                break;
        }
    }

    // ======================= поток игры =======================
    void reset() {
        st = new St();
        WQ = null; VT.clear(); OF.clear(); SEL.clear();
        zm = 1; mtT = 3; rolls = 0; taken.clear(); shc = 0; shT = 0;
        drT = 0; blT = 0; LN.clear(); gt = 0; kills = 0; lvl = 1; xp = 0; hp = 100;
        inv = 0; dashT = 0; dcd = 0; acc = 0; nextBoss = 60; fcd = 0; nvT = 0;
        slow = 1; sh = 0; ang = 0;
        E.clear(); B.clear(); EBb.clear(); G.clear(); F.clear(); FT.clear(); NV.clear();
        P.x = P.y = pvx = pvy = 0; tr.clear();
        cam.x = cam.y = 0;
    }

    void resetAll() { reset(); }

    void startGame() {
        Sfx.snd(660, .15f, 3, .06f, 200);
        reset();
        applyShopAll();
        saveMeta();
        S = ST_PLAY;
    }

    void applyShopAll() {
        for (ShopItems s : SHOP) {
            int lv = shopLv.get(s.id) == null ? 0 : shopLv.get(s.id);
            for (int i = 0; i < lv; i++) s.fx(this);
        }
    }

    void applyUpg(String id) {
        taken.put(id, (taken.get(id) == null ? 0 : taken.get(id)) + 1);
        for (Upgrade u : UPALL) if (u.id.equals(id)) { u.apply(this); break; }
    }

    int upgLv(String id) { Integer v = taken.get(id); return v == null ? 0 : v; }

    // ---------- прокачка ----------
    void makeCards() {
        float[] wt = {10, 4, 1.3f};
        ArrayList<Upgrade> pool = new ArrayList<>();
        for (Upgrade u : UPALL) if (u.canTake(this)) pool.add(u);
        pick.clear();
        while (pick.size() < 3 && !pool.isEmpty()) {
            float tot = 0;
            for (Upgrade u : pool) {
                float w = wt[u.r];
                if (u.r == 2) w *= lvl < 6 ? 0 : 1;
                else if (u.r == 1 && lvl < 3) w *= .3f;
                tot += w;
            }
            float r = rnd() * tot;
            int i = 0;
            for (; i < pool.size() - 1; i++) {
                float w = wt[pool.get(i).r];
                if (pool.get(i).r == 2) w *= lvl < 6 ? 0 : 1;
                else if (pool.get(i).r == 1 && lvl < 3) w *= .3f;
                r -= w;
                if (r <= 0) break;
            }
            pick.add(pool.remove(i));
        }
    }

    void choose(Upgrade u) {
        if (S != ST_LVL) return;
        applyUpg(u.id);
        Sfx.snd(660, .2f, 3, .06f, 400);
        S = ST_PLAY;
        saveRun();
    }

    void doRoll() {
        if (S != ST_LVL) return;
        int cost = rollCost();
        if (kills < cost) {
            Sfx.snd(180, .15f, 1, .05f, -80);
            txt(P.x, P.y - 60, "НУЖНО " + cost + " УБИЙСТВ", 0xFFFF5C7C, true);
            return;
        }
        kills -= cost; rolls++;
        Sfx.snd(400, .1f, 3, .05f, 200);
        makeCards();
    }

    // ---------- магазин ----------
    void buyShop(String id) {
        ShopItems s = null;
        for (ShopItems q : SHOP) if (q.id.equals(id)) { s = q; break; }
        if (s == null) return;
        int lv = shopLv.get(id) == null ? 0 : shopLv.get(id);
        if (lv >= 20) return;
        int pr = s.price(lv);
        if (stars < pr) { Sfx.snd(170, .15f, 1, .05f, -60); return; }
        stars -= pr;
        shopLv.put(id, lv + 1);
        s.fx(this);
        saveMeta();
        Sfx.snd(880, .12f, 3, .05f, 240);
    }

    // ---------- оружейная ----------
    String rid() {
        return Long.toString(R.nextLong(), 36).replace("-", "") + Integer.toString(R.nextInt(1000), 36);
    }

    Weapons.Wpn mkW(int t) {
        Weapons.Wpn w = new Weapons.Wpn();
        List<String> keys = new ArrayList<>(Weapons.WD.keySet());
        w.u = rid();
        w.id = keys.get(R.nextInt(keys.size()));
        w.t = t;
        List<String> ef = new ArrayList<>(Weapons.EF.keySet());
        w.fx.add(new int[]{ef.indexOf(ef.get(R.nextInt(ef.size()))), 1});
        w.cost = Math.round(Weapons.TC[t - 1] * (.85f + rnd() * .4f));
        return w;
    }

    void genOffers() {
        OF.clear();
        float[] w = {70, 20 + lvl * .7f, 6 + lvl * .5f, 1 + lvl * .25f};
        float tot = w[0] + w[1] + w[2] + w[3];
        for (int i = 0; i < 5; i++) {
            float r = rnd() * tot;
            int t = 0;
            while (t < 3 && (r -= w[t]) > 0) t++;
            OF.add(mkW(t + 1));
        }
    }

    Weapons.Wpn findW(String u) {
        if (WQ != null && WQ.u.equals(u)) return WQ;
        for (Weapons.Wpn w : VT) if (w.u.equals(u)) return w;
        return null;
    }

    void buyW(Weapons.Wpn o) {
        if (o.sold) return;
        if (kills < o.cost || (WQ != null && VT.size() >= VMAX)) { Sfx.snd(170, .15f, 1, .05f, -60); return; }
        kills -= o.cost;
        o.sold = true;
        Weapons.Wpn w = o.copy();
        w.sold = false;
        if (WQ == null) WQ = w; else VT.add(w);
        SEL.clear();
        Sfx.snd(760, .14f, 3, .06f, 300);
        saveRun();
    }

    ArrayList<Weapons.Wpn> selWpns() {
        ArrayList<Weapons.Wpn> l = new ArrayList<>();
        for (String u : SEL) { Weapons.Wpn w = findW(u); if (w != null) l.add(w); }
        return l;
    }

    void wpEquip() {
        ArrayList<Weapons.Wpn> ws = selWpns();
        if (ws.size() != 1) return;
        Weapons.Wpn w = ws.get(0);
        if (WQ != null && WQ.u.equals(w.u)) {
            if (VT.size() < VMAX) { VT.add(WQ); WQ = null; SEL.clear(); Sfx.snd(400, .1f, 3, .05f, 100); }
        } else {
            int i = VT.indexOf(w);
            if (i >= 0) {
                Weapons.Wpn old = WQ;
                WQ = VT.remove(i);
                if (old != null) VT.add(old);
                SEL.clear();
                Sfx.snd(700, .12f, 3, .05f, 200);
            }
        }
        saveRun();
    }

    void wpVault() {
        ArrayList<Weapons.Wpn> ws = selWpns();
        if (ws.size() != 1 || WQ == null || !WQ.u.equals(ws.get(0).u)) return;
        if (VT.size() < VMAX) { VT.add(WQ); WQ = null; SEL.clear(); Sfx.snd(400, .1f, 3, .05f, 100); saveRun(); }
    }

    void wpSell() {
        ArrayList<Weapons.Wpn> ws = selWpns();
        if (ws.size() != 1) return;
        Weapons.Wpn w = ws.get(0);
        if (WQ != null && WQ.u.equals(w.u)) WQ = null;
        else VT.remove(w);
        kills += w.cost >> 1;
        SEL.clear();
        Sfx.snd(300, .12f, 1, .04f, 100);
        saveRun();
    }

    void wpMerge() {
        ArrayList<Weapons.Wpn> ws = selWpns();
        if (ws.size() != 2) return;
        Weapons.Wpn a = ws.get(0), b = ws.get(1);
        if (!a.id.equals(b.id) || a.t != b.t || a.t >= 4) { Sfx.snd(170, .15f, 1, .05f, -60); return; }
        Weapons.Wpn n = new Weapons.Wpn();
        n.u = rid(); n.id = a.id; n.t = a.t + 1; n.cost = a.cost + b.cost;
        for (int[] f : a.fx) n.fx.add(new int[]{f[0], f[1]});
        for (int[] g : b.fx) {
            int[] m = null;
            for (int[] f : n.fx) if (f[0] == g[0]) { m = f; break; }
            if (m != null) m[1] = Math.min(2, m[1] + g[1]);
            else if (n.fx.size() < 2) n.fx.add(new int[]{g[0], g[1]});
        }
        boolean eq = WQ != null && (WQ.u.equals(a.u) || WQ.u.equals(b.u));
        VT.remove(a); VT.remove(b);
        if (eq) WQ = n; else VT.add(n);
        SEL.clear();
        SEL.add(n.u);
        Sfx.snd(500, .15f, 3, .06f, 400);
        saveRun();
    }

    // ======================= бой =======================
    void spawn(int t, Float sx, Float sy) {
        float[] T_ = TY[t];
        float a = rnd() * TAU;
        float d = Math.max(W, H) / zm * .62f + 60;
        float dl = lvl - 1;
        float sc = 1 + Math.min(6, gt / 160) + dl * .08f + dl * dl * .002f;
        boolean el = t != 3 && t != 5 && lvl >= 9 && rnd() < Math.min(.09, (lvl - 8) * .007);
        float hpv = T_[1] * (t == 3 ? 1 + gt / 220 + dl * .08f : sc) * (el ? 3 : 1);
        Enemy e = new Enemy();
        e.t = t;
        e.x = sx != null ? sx : P.x + (float) Math.cos(a) * d;
        e.y = sy != null ? sy : P.y + (float) Math.sin(a) * d;
        e.r = T_[0] * (el ? 1.5f : 1);
        e.hp = e.mh = hpv;
        e.el = el;
        e.sp = T_[2] * (.9f + rnd() * .2f) * (1 + Math.min(.4f, dl * .01f));
        e.dm = T_[3] * (1 + dl * .017f) * (el ? 1.4f : 1);
        e.xp = T_[4] * (el ? 3 : 1);
        e.h = T_[5];
        e.ct = 1 + rnd() * 2;
        e.bt = 1 + rnd() * 2;
        e.s = rnd() * 9;
        E.add(e);
    }

    int pickType() {
        float r = rnd();
        float d = gt;
        float dd = ((int) Math.floor(gt) % 75) / 75f;
        if (lvl >= 12 && r < .06f) return 5;
        if (lvl >= 6 && d > 45 && r < .09f) return 4;
        if ((d > 30 || lvl >= 4) && r < .14f && dd > .3f) return 1;
        if ((d > 50 || lvl >= 7) && r < .24f) return 2;
        return 0;
    }

    Enemy nearest(float px, float py, float rg, List<Enemy> ex) {
        Enemy be = null;
        float bd = rg * rg;
        for (Enemy e : E) {
            if (e.d || ex.contains(e)) continue;
            float d = (e.x - px) * (e.x - px) + (e.y - py) * (e.y - py);
            if (d < bd) { bd = d; be = e; }
        }
        return be;
    }

    interface NearFn { void apply(Enemy e); }

    void grid() {
        GR.clear();
        for (Enemy e : E) {
            long k = (long) Math.floor(e.x / 52) * 7919 + (long) Math.floor(e.y / 52);
            ArrayList<Enemy> a = GR.get(k);
            if (a == null) { a = new ArrayList<>(); GR.put(k, a); }
            a.add(e);
        }
    }

    void near(float px, float py, NearFn fn) {
        long cx = (long) Math.floor(px / 52), cy = (long) Math.floor(py / 52);
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) {
                ArrayList<Enemy> a = GR.get((cx + i) * 7919 + cy + j);
                if (a != null) for (Enemy e : a) fn.apply(e);
            }
    }

    void aoe(float px, float py, float rad, float dm, Enemy skip) {
        float fx = px, fy = py;
        near(px, py, o -> {
            if (o != skip && !o.d && dist(o.x - fx, o.y - fy) < rad + o.r)
                hurt(o, dm, (o.x - fx) * 2, (o.y - fy) * 2);
        });
    }

    void burst(float px, float py, float h, int n, float v) {
        n = (int) Math.ceil(n * fxW);
        for (int i = 0; i < n; i++) {
            float a = rnd() * TAU, s = v * (.3f + rnd());
            Part f = new Part();
            f.x = px; f.y = py;
            f.vx = (float) Math.cos(a) * s; f.vy = (float) Math.sin(a) * s;
            f.l = f.m = .7f; f.h = h; f.r = 2 + rnd() * 3;
            F.add(f);
        }
    }

    void starDrop(float px, float py, int n) {
        for (int i = 0; i < n; i++) {
            float a = rnd() * TAU, s = 60 + rnd() * 180;
            Part f = new Part();
            f.x = px; f.y = py;
            f.vx = (float) Math.cos(a) * s; f.vy = (float) Math.sin(a) * s;
            f.l = f.m = .8f; f.h = 38; f.r = 3 + rnd() * 2;
            F.add(f);
        }
    }

    void txt(float px, float py, String s, int c) { txt(px, py, s, c, false); }

    void txt(float px, float py, String s, int c, boolean force) {
        if ((cfgNums != 0 || force) && FT.size() < 40) {
            FText f = new FText();
            f.x = px + (rnd() - .5f) * 16; f.y = py - 10; f.s = s; f.c = c; f.l = .7f;
            FT.add(f);
        }
    }

    void ouch(float dm) {
        if (st.dodge > 0 && rnd() < st.dodge) {
            inv = .45f;
            txt(P.x, P.y - 50, "УКЛОН", 0xFF8FE8FF, true);
            return;
        }
        if (shc > 0) {
            shc--; shT = 12; inv = .8f; sh = 8;
            burst(P.x, P.y, 195, 16, 220);
            Sfx.snd(600, .2f, 0, .06f, -300);
            return;
        }
        hp -= dm * st.arm;
        inv = .82f; sh = 12; fl = 1; shT = Math.max(shT, 1.4f);
        Sfx.snd(110, .25f, 2, .07f, -60);
        burst(P.x, P.y, 355, 10, 160);
        if (hp <= 0) {
            if (st.rev > 0) {
                st.rev--;
                hp = st.hpm * .5f; inv = 2.5f;
                Nova n = new Nova(); n.x = P.x; n.y = P.y; n.m = 520; n.h = 50; n.dm = st.dmg * 6;
                NV.add(n);
                sh = 30;
                txt(P.x, P.y - 60, "ВТОРОЙ ШАНС!", 0xFFFFC76B, true);
                Sfx.snd(300, .5f, 0, .08f, 900);
            } else die();
        }
    }

    void hurt(Enemy e, float d, float kx, float ky) {
        boolean cr = rnd() < st.crit;
        if (cr) d *= 2;
        if (e.mk > 0) d *= 1.3f;
        if (st.bers > 0 && hp < st.hpm * .5f) d *= 1 + .35f * st.bers;
        e.hp -= d;
        if (st.exec > 0 && e.t != 3 && e.hp > 0 && e.hp < e.mh * .07f * st.exec) e.hp = 0;
        e.f = .07f;
        e.kx += kx; e.ky += ky;
        txt(e.x, e.y - e.r, Math.round(d) + (cr ? "!" : ""), cr ? 0xFFFFB347 : (e.t == 3 ? 0xFFFFC76B : 0xFFFFFFFF));
        if (e.hp <= 0 && !e.d) kill(e);
    }

    void kill(Enemy e) {
        e.d = true;
        kills++;
        if (st.reap > 0 && kills % 20 == 0 && S == ST_PLAY) {
            hp = Math.min(st.hpm, hp + st.hpm * .06f * st.reap);
            txt(P.x, P.y - 50, "ЖАТВА", 0xFFA6FFB1, true);
        }
        if (S == ST_PLAY) hp = Math.min(st.hpm, hp + st.vamp);
        burst(e.x, e.y, e.h, e.t == 3 ? 60 : 10, e.t == 3 ? 420 : 180);
        if (e.t == 3) {
            sh = 26; hs = .12f;
            hp = Math.min(st.hpm, hp + 30);
            starDrop(e.x, e.y, 10);
            stars += 5;
            Nova n = new Nova(); n.x = e.x; n.y = e.y; n.m = 420; n.h = 50; n.dm = 0;
            NV.add(n);
            for (int i = 0; i < 12; i++) {
                Gem g = new Gem();
                g.x = e.x + (rnd() - .5f) * 120; g.y = e.y + (rnd() - .5f) * 120; g.v = 3;
                G.add(g);
            }
        } else {
            Gem g = new Gem();
            g.x = e.x + (rnd() - .5f) * 14; g.y = e.y + (rnd() - .5f) * 14; g.v = e.xp;
            G.add(g);
            if (rnd() < st.lucky) { Gem g2 = new Gem(); g2.x = e.x + 9; g2.y = e.y + 9; g2.v = e.xp; G.add(g2); }
            if (rnd() < .06f + .05f * st.bounty || e.t == 1) { starDrop(e.x, e.y, 2); stars += 1; }
        }
        Sfx.snd(180 + rnd() * 80, .09f, 1, .025f, -100);
    }

    void die() {
        S = ST_DEAD;
        slow = .22f; sh = 30; deadTimer = 1.5f;
        burst(P.x, P.y, 50, 70, 420);
        burst(P.x, P.y, cfgHero, 40, 300);
        Sfx.snd(120, .7f, 2, .08f, -80);
        clearRun();
    }

    void finishOver() {
        slow = 1;
        if (gt > best) { best = gt; pref.edit().putFloat("best", best).apply(); }
        saveMeta();
        S = ST_OVER;
    }

    float[] inp() {
        float ix = 0, iy = 0;
        if (S == ST_MENU) {
            ix = (float) Math.cos(T * .8f); iy = (float) Math.sin(T * 1.3f);
        } else if (joyOn) {
            float dx = joyX - joyOx, dy = joyY - joyOy;
            float d = dist(dx, dy);
            if (d > 8) {
                float k = Math.min(1, (float) Math.pow((d - 8) / 47, .78));
                ix = dx / d * k; iy = dy / d * k;
            }
        }
        float l = dist(ix, iy);
        if (l > 1) { ix /= l; iy /= l; }
        return new float[]{ix, iy};
    }

    void dash() {
        if (S != ST_PLAY || dcd > 0) return;
        dashT = .2f + .035f * st.dlen;
        inv = Math.max(inv, .4f);
        dcd = st.dcd;
        if (st.dnova > 0) {
            Nova n = new Nova(); n.x = P.x; n.y = P.y; n.m = 210; n.h = 190; n.dm = st.dmg * 1.6f * st.dnova;
            NV.add(n);
        }
        Sfx.snd(300, .2f, 2, .05f, 600);
    }

    Float aimAngle() {
        Enemy be = null;
        float bd = 680 * 680;
        for (Enemy e : E) {
            float d = (e.x - P.x) * (e.x - P.x) + (e.y - P.y) * (e.y - P.y);
            if (d < bd) { bd = d; be = e; }
        }
        return be == null ? null : (float) Math.atan2(be.y - P.y, be.x - P.x);
    }

    // ---------- стрельба ----------
    void fireW(float a0) {
        Weapons.Def d = Weapons.WD.get(WQ.id);
        int t = WQ.t;
        float dm = d.dm * Weapons.TD[t - 1];
        int cnt = (d.cnt > 0 ? d.cnt : 1) + (d.cnt > 0 && t > 2 ? 1 : 0) + st.shots - 1;
        int rc = st.rico;
        for (int[] f : WQ.fx) if (fxKey(f[0]).equals("rico")) rc += f[1];
        fcd = st.rate * d.r * Weapons.TR[t - 1];
        for (int i = 0; i < cnt; i++) {
            float a = a0 + (d.spr > 0 ? (rnd() - .5f) * d.spr : (i - (cnt - 1) / 2f) * .17f);
            float v = d.s * st.bv * (d.spr > 0 && d.cnt > 0 ? .85f + rnd() * .3f : 1);
            Bullet b = new Bullet();
            b.x = P.x; b.y = P.y;
            b.vx = (float) Math.cos(a) * v; b.vy = (float) Math.sin(a) * v;
            b.l = b.m = d.l;
            b.p = d.p > 50 ? 99 : d.p + st.pierce + (t > 2 ? 1 : 0);
            b.rc = rc;
            b.dm = dm;
            b.hu = d.h;
            b.fx = WQ.fx;
            b.ln = d.ln; b.rk = d.rk; b.bm = d.bm; b.sz = d.sz;
            B.add(b);
        }
    }

    static String[] fxKeys;
    String fxKey(int idx) {
        if (fxKeys == null) fxKeys = Weapons.EF.keySet().toArray(new String[0]);
        return idx >= 0 && idx < fxKeys.length ? fxKeys[idx] : "";
    }

    void chain(Enemy e, float d, float r) {
        List<Enemy> ex = new ArrayList<>();
        ex.add(e);
        Enemy t = nearest(e.x, e.y, r, ex);
        if (t != null) {
            Bolt b = new Bolt();
            b.x1 = e.x; b.y1 = e.y; b.x2 = t.x; b.y2 = t.y; b.l = .18f;
            LN.add(b);
            hurt(t, d, 0, 0);
        }
    }

    void wfx(Bullet b, Enemy e, float dm) {
        if (b.fx == null) return;
        for (int[] fx : b.fx) {
            String k = fxKey(fx[0]);
            int l = fx[1];
            switch (k) {
                case "freeze": e.fz = Math.max(e.fz, 1.1f + .5f * l); break;
                case "shock": chain(e, dm * (.45f + .2f * l), 150 + 40 * l); break;
                case "burn": e.bn = 2.6f; e.bd = Math.max(e.bd, dm * .28f * l); break;
                case "poison": e.ps = 4; e.pd = Math.max(e.pd, dm * .16f * l); break;
                case "blast":
                    aoe(b.x, b.y, 52 + 18 * l, dm * .45f * l, e);
                    burst(b.x, b.y, 30, 6, 160);
                    break;
                case "leech": if (S == ST_PLAY) hp = Math.min(st.hpm, hp + .5f * l); break;
                case "push": e.kx += b.vx * .9f * l; e.ky += b.vy * .9f * l; break;
                case "mark": e.mk = 3.5f; break;
                case "stun": e.fz = Math.max(e.fz, 1.4f + .6f * l); break;
                case "bleed": e.ps = 3; e.pd = Math.max(e.pd, dm * .2f * l); break;
                case "vuln": e.mk = Math.max(e.mk, 3); break;
                case "quake":
                    aoe(b.x, b.y, 70 + 20 * l, dm * .3f * l, e);
                    burst(b.x, b.y, 25, 4, 140);
                    break;
                case "chain": chain(e, dm * (.5f + .2f * l), 200 + 50 * l); break;
                case "frost": e.fz = Math.max(e.fz, .8f + .3f * l); aoe(b.x, b.y, 38, dm * .2f * l, e); break;
                case "heat": e.bn = 3.5f; e.bd = Math.max(e.bd, dm * .35f * l); break;
                case "split":
                    if (rnd() < .3f + .2f * l)
                        for (int s = -1; s <= 1; s += 2) {
                            float a = (float) Math.atan2(b.vy, b.vx) + s * .6f;
                            Bullet nb = new Bullet();
                            nb.x = b.x; nb.y = b.y;
                            nb.vx = (float) Math.cos(a) * 520; nb.vy = (float) Math.sin(a) * 520;
                            nb.l = nb.m = .5f; nb.p = 0; nb.rc = 0; nb.dm = .35f * b.dm; nb.hu = b.hu;
                            nb.ids.add(e);
                            B.add(nb);
                        }
                    break;
            }
        }
    }

    void onHit(Bullet b, Enemy e, float dm) {
        if (st.chill > 0) e.fz = Math.max(e.fz, .7f + .3f * st.chill);
        if (st.ignite > 0) { e.bn = 2.2f; e.bd = Math.max(e.bd, st.dmg * .2f * st.ignite); }
        if (st.arc > 0 && rnd() < .12f * st.arc) chain(e, dm * .5f, 150);
        if (b.rk) {
            aoe(b.x, b.y, 95, dm * .7f, e);
            Nova n = new Nova(); n.x = b.x; n.y = b.y; n.m = 95; n.h = 16; n.dm = 0;
            NV.add(n);
            burst(b.x, b.y, 16, 14, 260);
            sh = Math.max(sh, 7);
            b.l = 0;
        }
        wfx(b, e, dm);
    }

    // ======================= симуляция =======================
    float drT, blT;

    void sim(float dt) {
        boolean play = S == ST_PLAY;
        boolean dead = S == ST_DEAD;
        boolean menu = S == ST_MENU;

        if (play) {
            gt += dt;
            hp = Math.min(st.hpm, hp + st.regen * dt + st.metabolism * dt + gt * 0.006f * dt);
        }
        acc += dt * (1 + Math.min(3.5f, gt / 70));
        if (acc >= 1) {
            acc -= 1;
            if (G.size() < 70 && !E.isEmpty()) {
                Enemy e = E.get(R.nextInt(E.size()));
                Gem g = new Gem();
                g.x = e.x; g.y = e.y; g.v = 1;
                G.add(g);
            }
        }
        inv = Math.max(0, inv - dt);
        dcd = Math.max(0, dcd - dt);
        fl = Math.max(0, fl - dt * 2);
        if (dead) { deadTimer -= dt; if (deadTimer <= 0) finishOver(); }

        if (!dead) {
            float[] i = inp();
            float ix = i[0], iy = i[1];
            if (ix != 0 || iy != 0) { pfx = ix; pfy = iy; }

            if (dashT > 0) {
                dashT -= dt;
                float l = dist(pfx, pfy);
                if (l > 0) {
                    P.x += pfx / l * 820 * dt;
                    P.y += pfy / l * 820 * dt;
                }
                if (rnd() < .8f) burst(P.x, P.y, cfgHero, 2, 90);
                if (st.ram > 0) {
                    float fx = P.x, fy = P.y;
                    near(P.x, P.y, e -> {
                        if (!e.d && e.ob <= 0 && dist(e.x - fx, e.y - fy) < e.r + 22) {
                            e.ob = .2f;
                            hurt(e, st.dmg * (1 + st.ram * 1.5f), (e.x - fx) * 3, (e.y - fy) * 3);
                        }
                    });
                }
            } else {
                float sp = st.spd * (menu ? .8f : 1);
                pvx += (ix * sp - pvx) * Math.min(1, dt * 11);
                pvy += (iy * sp - pvy) * Math.min(1, dt * 11);
                P.x += pvx * dt;
                P.y += pvy * dt;
            }
            tr.add(0, newPt(P.x, P.y));
            if (tr.size() > 16) tr.remove(tr.size() - 1);

            fcd -= dt;
            if (fcd <= 0) {
                Float a0 = aimAngle();
                if (a0 != null) {
                    fcd = st.rate;
                    if (WQ != null) fireW(a0);
                    else {
                        for (int s = 0; s < st.shots; s++) {
                            float a = a0 + (s - (st.shots - 1) / 2f) * .17f;
                            Bullet b = new Bullet();
                            b.x = P.x; b.y = P.y;
                            b.vx = (float) Math.cos(a) * 640 * st.bv;
                            b.vy = (float) Math.sin(a) * 640 * st.bv;
                            b.l = b.m = 1.1f; b.p = st.pierce; b.rc = st.rico;
                            B.add(b);
                        }
                    }
                    if (st.burst > 0 && rnd() < Math.min(.6f, .15f * st.burst)) {
                        float a = a0 + (rnd() - .5f) * .2f;
                        Bullet b = new Bullet();
                        b.x = P.x; b.y = P.y;
                        b.vx = (float) Math.cos(a) * 640 * st.bv;
                        b.vy = (float) Math.sin(a) * 640 * st.bv;
                        b.l = b.m = 1.1f; b.p = st.pierce; b.rc = st.rico; b.db = true;
                        B.add(b);
                    }
                    if (st.shotgun > 0)
                        for (int s = 0; s < st.shotgun; s++) {
                            float a = a0 + (s - st.shotgun / 2f) * .22f;
                            Bullet b = new Bullet();
                            b.x = P.x; b.y = P.y;
                            b.vx = (float) Math.cos(a) * 560 * st.bv;
                            b.vy = (float) Math.sin(a) * 560 * st.bv;
                            b.l = b.m = .8f; b.sg = true;
                            B.add(b);
                        }
                    if (st.missile > 0)
                        for (int s = 0; s < st.missile; s++) {
                            Enemy t2 = nearest(P.x, P.y, 500, new ArrayList<Enemy>());
                            if (t2 != null) {
                                float q = (float) Math.atan2(t2.y - P.y, t2.x - P.x);
                                Bullet b = new Bullet();
                                b.x = P.x; b.y = P.y;
                                b.vx = (float) Math.cos(q) * 420;
                                b.vy = (float) Math.sin(q) * 420;
                                b.l = b.m = 1.4f; b.ms = true; b.tgt = t2;
                                B.add(b);
                            }
                        }
                    if (WQ == null || Weapons.WD.get(WQ.id).r > .2f || rnd() < .15f)
                        Sfx.snd(520 + rnd() * 60, .06f, 1, .012f, -200);
                } else fcd = .1f;
            }

            ang += dt * 3.2f;
            if (st.orbs > 0)
                for (int o = 0; o < st.orbs; o++) {
                    float a = ang + o * TAU / st.orbs;
                    float bx = P.x + (float) Math.cos(a) * 80, by = P.y + (float) Math.sin(a) * 80;
                    near(bx, by, e -> {
                        if (e.ob <= 0 && !e.d && (e.x - bx) * (e.x - bx) + (e.y - by) * (e.y - by) < (e.r + 13) * (e.r + 13)) {
                            e.ob = .3f;
                            hurt(e, st.dmg * .8f * st.obd, (e.x - P.x) * 2, (e.y - P.y) * 2);
                        }
                    });
                }

            if (st.nova > 0) {
                nvT -= dt;
                if (nvT <= 0) {
                    nvT = Math.max(2.5f, 9 - st.nova * 1.4f);
                    Nova n = new Nova();
                    n.x = P.x; n.y = P.y; n.m = 300; n.h = 310; n.dm = st.dmg * 2.4f;
                    NV.add(n);
                    Sfx.snd(150, .5f, 0, .07f, 300);
                }
            }
            if (st.drone > 0) {
                drT -= dt;
                if (drT <= 0) {
                    drT = Math.max(.45f, 1.3f - st.drone * .17f);
                    for (int o = 0; o < st.drone; o++) {
                        float a = ang * .7f + o * TAU / st.drone;
                        float dx = P.x + (float) Math.cos(a) * 54, dy = P.y + (float) Math.sin(a) * 54;
                        Enemy t2 = nearest(dx, dy, 620, new ArrayList<Enemy>());
                        if (t2 != null) {
                            float q = (float) Math.atan2(t2.y - dy, t2.x - dx);
                            Bullet b = new Bullet();
                            b.x = dx; b.y = dy;
                            b.vx = (float) Math.cos(q) * 700;
                            b.vy = (float) Math.sin(q) * 700;
                            b.l = b.m = .9f; b.dr = true;
                            B.add(b);
                        }
                    }
                }
            }
            if (st.bolt > 0) {
                blT -= dt;
                if (blT <= 0) {
                    blT = Math.max(1.2f, 4 - st.bolt * .5f);
                    ArrayList<Enemy> c = new ArrayList<>();
                    for (Enemy e : E) if (!e.d && dist(e.x - P.x, e.y - P.y) < 540) c.add(e);
                    for (int q = 0; q < 1 + st.bolt && !c.isEmpty(); q++) {
                        Enemy t2 = c.get(R.nextInt(c.size()));
                        Bolt b = new Bolt();
                        b.x1 = t2.x + (rnd() - .5f) * 90; b.y1 = t2.y - 460;
                        b.x2 = t2.x; b.y2 = t2.y; b.l = .25f;
                        LN.add(b);
                        hurt(t2, st.dmg * 3.2f, 0, 0);
                        burst(t2.x, t2.y, 55, 8, 200);
                    }
                    if (!c.isEmpty()) Sfx.snd(1200, .15f, 2, .03f, -900);
                }
            }
            if (st.meteor > 0) {
                mtT -= dt;
                if (mtT <= 0) {
                    mtT = Math.max(2.2f, 7 - st.meteor * 1.1f);
                    ArrayList<Enemy> c = new ArrayList<>();
                    for (Enemy e : E) if (!e.d && dist(e.x - P.x, e.y - P.y) < 600) c.add(e);
                    if (!c.isEmpty()) {
                        Enemy t2 = c.get(R.nextInt(c.size()));
                        Bolt b = new Bolt();
                        b.x1 = t2.x + 60; b.y1 = t2.y - 520; b.x2 = t2.x; b.y2 = t2.y; b.l = .3f;
                        LN.add(b);
                        Nova n = new Nova();
                        n.x = t2.x; n.y = t2.y; n.m = 130; n.h = 22;
                        n.dm = st.dmg * 4.5f * (1 + .3f * st.meteor);
                        NV.add(n);
                        sh = Math.max(sh, 8);
                        Sfx.snd(80, .5f, 2, .06f, -30);
                    }
                }
            }
            if (st.shield > 0 && shc < st.shield) {
                shT -= dt;
                if (shT <= 0) { shc++; Sfx.snd(800, .2f, 0, .05f, 400); }
            }
        }

        if (play || menu) {
            float rate = menu ? 2 : clamp(1 / 1.9f + gt * .013f + (lvl - 1) * .04f, 1, 5.5f);
            int cap = menu ? 26 : (int) Math.min(90, 32 + lvl * 3.5f);
            acc += dt * rate;
            while (acc >= 1) {
                acc -= 1;
                if (E.size() < cap) {
                    int ty = menu ? 0 : pickType();
                    spawn(ty, null, null);
                    if (ty == 5) for (int q = 0; q < 2; q++) spawn(5, null, null);
                }
            }
            if (play && gt >= nextBoss) {
                nextBoss += 75;
                spawn(3, null, null);
                txt(P.x, P.y - 100, "БОСС!", 0xFFFF5C7C, true);
                sh = 14;
                Sfx.snd(90, .8f, 2, .08f, -30);
            }
        }

        grid();
        for (Enemy e : E) {
            if (e.d) continue;
            float dx = P.x - e.x, dy = P.y - e.y;
            float d = dist(dx, dy);
            if (d == 0) d = 1;
            float ux = dx / d, uy = dy / d;
            float sp = e.sp;
            if (e.fz > 0) { e.fz -= dt; sp *= .45f; }
            if (e.mk > 0) e.mk -= dt;
            if (e.bn > 0) {
                e.bn -= dt; e.hp -= e.bd * dt;
                if (rnd() < dt * 8) burst(e.x, e.y, 18, 1, 50);
                if (e.hp <= 0) { kill(e); continue; }
            }
            if (e.ps > 0) {
                e.ps -= dt; e.hp -= e.pd * dt;
                if (e.hp <= 0) { kill(e); continue; }
            }
            if (st.chrono > 0) sp *= 1 - .06f * st.chrono;

            if (e.t == 2) {
                e.ct -= dt;
                if (e.st == 0 && e.ct <= 0) { e.st = 1; e.ct = .5f; }
                else if (e.st == 1 && e.ct <= 0) { e.st = 2; e.ct = .55f; e.mx = ux; e.my = uy; }
                else if (e.st == 2 && e.ct <= 0) { e.st = 0; e.ct = 2 + rnd() * 1.5f; }
                if (e.st == 1) sp = 0;
                if (e.st == 2) { sp = 0; e.x += e.mx * 400 * dt; e.y += e.my * 400 * dt; }
            }
            if (e.t == 3) {
                e.bt -= dt;
                if (e.bt <= 0 && !dead) {
                    e.bt = 2.4f;
                    int n = 14;
                    float o = rnd() * TAU;
                    for (int i = 0; i < n; i++) {
                        float a = o + i * TAU / n;
                        EB b = new EB();
                        b.x = e.x; b.y = e.y;
                        b.vx = (float) Math.cos(a) * 170; b.vy = (float) Math.sin(a) * 170;
                        b.l = b.m = 6;
                        EBb.add(b);
                    }
                    float a = (float) Math.atan2(dy, dx);
                    for (int i = -1; i <= 1; i++) {
                        EB b = new EB();
                        b.x = e.x; b.y = e.y;
                        b.vx = (float) Math.cos(a + i * .2f) * 260;
                        b.vy = (float) Math.sin(a + i * .2f) * 260;
                        b.l = b.m = 5;
                        EBb.add(b);
                    }
                }
            }
            if (e.t == 4) {
                sp = d > 300 ? e.sp : (d < 230 ? -e.sp : 0);
                e.bt -= dt;
                if (e.bt <= 0 && !dead && d < 620) {
                    e.bt = 2.4f;
                    EB b = new EB();
                    b.x = e.x; b.y = e.y;
                    b.vx = ux * 230; b.vy = uy * 230;
                    b.l = b.m = 4;
                    EBb.add(b);
                }
            }
            if (st.aura > 0 && d < 100 + st.aura * 35) sp *= 1 - Math.min(.6f, .18f + st.aura * .1f);
            if (st.gravity > 0 && d < 200) sp *= 1 - Math.min(.4f, .08f * st.gravity);

            e.x += ux * sp * dt + e.kx * dt;
            e.y += uy * sp * dt + e.ky * dt;
            e.kx *= .9f; e.ky *= .9f;
            e.f -= dt; e.ob -= dt;
            near(e.x, e.y, o -> {
                if (o != e && o.t != 3 && !o.d && !e.d) {
                    float ox = e.x - o.x, oy = e.y - o.y;
                    float dd = dist(ox, oy);
                    if (dd == 0) dd = 1;
                    float mn = (e.r + o.r) * .8f;
                    if (dd < mn) {
                        float pp = (mn - dd) * .5f / dd;
                        e.x += ox * pp; e.y += oy * pp;
                    }
                }
            });
            if (play && inv <= 0 && d < e.r + 9) {
                e.kx -= ux * 300; e.ky -= uy * 300;
                if (st.thorn > 0) hurt(e, st.dmg * 2.5f * st.thorn, -ux * 400, -uy * 400);
                ouch(e.dm);
            }
        }

        for (Bullet b : B) {
            b.l -= dt;
            if (b.bm) {
                if (!b.rv && b.l < b.m * .55f) { b.rv = true; b.ids.clear(); }
                if (b.rv) {
                    float q = (float) Math.atan2(P.y - b.y, P.x - b.x);
                    float sp = dist(b.vx, b.vy);
                    float k = Math.min(1, dt * 6);
                    b.vx += ((float) Math.cos(q) * sp - b.vx) * k;
                    b.vy += ((float) Math.sin(q) * sp - b.vy) * k;
                    if (dist(P.x - b.x, P.y - b.y) < 24) b.l = 0;
                }
            }
            if (b.ms && b.tgt != null && !b.tgt.d) {
                float q = (float) Math.atan2(b.tgt.y - b.y, b.tgt.x - b.x);
                float sp = dist(b.vx, b.vy);
                float k = Math.min(1, dt * 3);
                b.vx += ((float) Math.cos(q) * sp - b.vx) * k;
                b.vy += ((float) Math.sin(q) * sp - b.vy) * k;
            }
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            float bx = b.x, by = b.y;
            near(b.x, b.y, e -> {
                if (b.l > 0 && !e.d && !b.ids.contains(e)
                        && (e.x - bx) * (e.x - bx) + (e.y - by) * (e.y - by) < (e.r + 7 * st.bsz * (b.sz > 0 ? b.sz : 1)) * (e.r + 7 * st.bsz * (b.sz > 0 ? b.sz : 1))) {
                    b.ids.add(e);
                    float dm = st.dmg * (b.dr ? .6f : 1) * (b.sg ? .4f : 1) * b.dm;
                    if (st.drill > 0 && b.ids.size() > 1) dm *= 1 + 0.25f * st.drill * (b.ids.size() - 1);
                    float kn = .25f * (1 + .5f * st.knock);
                    hurt(e, dm, b.vx * kn, b.vy * kn);
                    if (b.p < 50) burst(b.x, b.y, b.hu > 0 ? b.hu : 50, 3, 120);
                    if (st.boom > 0) aoe(b.x, b.y, 48 + st.boom * 8, st.dmg * .4f * st.boom, e);
                    onHit(b, e, dm);
                    if (b.rc > 0) {
                        List<Enemy> ex = new ArrayList<>(b.ids);
                        ex.add(e);
                        Enemy t2 = nearest(b.x, b.y, 380, ex);
                        if (t2 != null) {
                            float a = (float) Math.atan2(t2.y - b.y, t2.x - b.x);
                            float v = dist(b.vx, b.vy);
                            b.vx = (float) Math.cos(a) * v;
                            b.vy = (float) Math.sin(a) * v;
                            b.rc--; b.p++;
                            b.l = Math.max(b.l, .5f);
                        }
                    }
                    if (--b.p < 0) b.l = 0;
                }
            });
        }

        for (Nova n : NV) {
            float pr = n.r;
            n.r += dt * n.m * 2.2f;
            if (n.dm > 0)
                for (Enemy e : E) {
                    float d = dist(e.x - n.x, e.y - n.y);
                    if (!e.d && d >= pr - e.r && d < n.r + e.r)
                        hurt(e, n.dm, (e.x - n.x) * 3, (e.y - n.y) * 3);
                }
        }

        for (EB b : EBb) {
            b.l -= dt;
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            if (play && inv <= 0 && (b.x - P.x) * (b.x - P.x) + (b.y - P.y) * (b.y - P.y) < 169) {
                b.l = Math.min(b.l, .12f);
                ouch(10 * (1 + (lvl - 1) * .03f));
            }
        }

        for (Gem g : G) {
            if (!g.init) { g.sx = g.tx = g.x; g.sy = g.ty = g.y; g.w = 0; g.init = true; }
            if (!dead && !g.m) {
                float ddx = P.x - g.x, ddy = P.y - g.y;
                float dd = dist(ddx, ddy);
                if (dd < st.mag + 70 || g.w > 0) {
                    g.w += dt;
                    float k = Math.min(1, g.w * 3.2f);
                    g.ty = P.y + (g.y - P.y) * .35f;
                    g.tx = P.x + (g.x - P.x) * .35f;
                    g.x += (g.tx - g.x) * 6 * dt * k;
                    g.y += (g.ty - g.y) * 6 * dt * k;
                    if (g.w > .16f || dd < st.mag * .55f) g.m = true;
                }
            }
            if (!dead && g.m) {
                float dx = P.x - g.x, dy = P.y - g.y;
                float d = dist(dx, dy);
                if (d == 0) d = 1;
                float s = (320 + (st.mag - d) * 2.2f + gt * 2.4f) * dt;
                g.x += dx / d * s;
                g.y += dy / d * s;
            }
            float d2 = dist(P.x - g.x, P.y - g.y);
            if (!dead && d2 < 24) {
                g.d = true;
                if (!menu) {
                    xp += g.v * st.xpm;
                    Sfx.snd(900 + Math.min(600, xp * 20), .05f, 0, .03f, 0);
                }
            }
        }

        int need = xpNeed();
        if (play && xp >= need) {
            xp -= need;
            lvl++;
            S = ST_LVL;
            hs = .1f;
            hp = Math.min(st.hpm, hp + st.hpm * .1f);
            Sfx.snd(523, .15f, 3, .06f, 0);
            genOffers();
            makeCards();
        }

        for (Part f : F) {
            f.l -= dt;
            f.x += f.vx * dt; f.y += f.vy * dt;
            f.vx *= .94f; f.vy *= .94f;
        }
        for (FText f : FT) { f.l -= dt; f.y -= 40 * dt; }

        packEnemies();
        packBullets();
        for (int i = EBb.size() - 1; i >= 0; i--) if (EBb.get(i).l <= .02f) EBb.remove(i);
        for (int i = G.size() - 1; i >= 0; i--) if (G.get(i).d) G.remove(i);
        for (int i = F.size() - 1; i >= 0; i--) if (F.get(i).l <= 0) F.remove(i);
        for (int i = FT.size() - 1; i >= 0; i--) if (FT.get(i).l <= 0) FT.remove(i);
        for (int i = NV.size() - 1; i >= 0; i--) if (NV.get(i).r >= NV.get(i).m) NV.remove(i);
        for (int i = LN.size() - 1; i >= 0; i--) { LN.get(i).l -= dt; if (LN.get(i).l <= 0) LN.remove(i); }
    }

    static Pt newPt(float x, float y) { Pt p = new Pt(); p.x = x; p.y = y; return p; }

    void packEnemies() {
        for (int i = E.size() - 1; i >= 0; i--) {
            Enemy e = E.get(i);
            if (e.d || (e.x - P.x) * (e.x - P.x) + (e.y - P.y) * (e.y - P.y) >= 4840000) E.remove(i);
        }
    }

    void packBullets() {
        for (int i = B.size() - 1; i >= 0; i--) if (B.get(i).l <= .02f) B.remove(i);
    }

    // ======================= рендер =======================
    @Override
    protected void onDraw(Canvas c) {
        W = getWidth();
        H = getHeight();
        if (W == 0 || H == 0) return;
        ui.clear();
        volSlider = null;
        drawWorld(c);
        drawHud(c);
        switch (S) {
            case ST_MENU: drawMenu(c); break;
            case ST_HELP: drawHelp(c); break;
            case ST_LVL: drawLvl(c); break;
            case ST_PAUSE: drawPause(c); break;
            case ST_OVER: drawOver(c); break;
            case ST_SET: drawSet(c); break;
            case ST_SHOP: drawShop(c); break;
            case ST_WP: drawWp(c); break;
        }
    }

    void halo(Canvas c, float x, float y, float h, float r, float a) {
        if (a <= 0.01f) return;
        int rr = Math.max(4, (Math.round(r / 4) * 4));
        String key = (int) h + "_" + rr;
        RadialGradient g = haloCache.get(key);
        if (g == null) {
            int ac = (int) clamp(a * 255, 0, 255);
            g = new RadialGradient(0, 0, rr,
                    new int[]{hsl(h, 100, 72, ac), hsl(h, 100, 60, (int) (ac * .35f)), hsl(h, 100, 50, 0)},
                    new float[]{0, .3f, 1}, Shader.TileMode.CLAMP);
            if (haloCache.size() > 200) haloCache.clear();
            haloCache.put(key, g);
        }
        p.setShader(g);
        c.drawCircle(x, y, rr, p);
        p.setShader(null);
    }

    void drawWorld(Canvas c) {
        Bg th = BG[cfgBg];
        int bgc = argb(th.b);
        c.save();
        float shx = 0, shy = 0;
        if (sh > .3f) {
            if (cfgShake != 0) { shx = (rnd() - .5f) * sh; shy = (rnd() - .5f) * sh; }
            sh *= .9f;
        }
        c.translate(shx, shy);
        p.setShader(null);
        p.setColor(bgc);
        c.drawRect(-40, -40, W + 40, H + 40, p);

        // фоновые градиентные пятна
        int nb = new int[]{1, 2, 4}[(int) clamp(fxNow, 0, 2)];
        for (int i = 0; i < nb; i++) {
            float span = Math.max(W, H) * .45f;
            float px = mod((i * .33f + .1f) * W - cam.x * .12f * (i + 1), W * 1.4f) - W * .2f;
            float py = mod((i * .27f + .15f) * H - cam.y * .12f * (i + 1), H * 1.4f) - H * .2f;
            halo(c, px, py, th.h[i % th.h.length], span, .1f + fxW * .06f);
        }

        float zW = W / zm, zH = H / zm;
        c.save();
        c.translate(W / 2, H / 2);
        c.scale(zm, zm);
        c.translate(-Math.round(cam.x), -Math.round(cam.y));

        // сетка
        if (cfgGrid != 0) {
            int stp = 80;
            float x0 = (float) Math.floor((cam.x - zW / 2) / stp) * stp;
            float y0 = (float) Math.floor((cam.y - zH / 2) / stp) * stp;
            String[] gc = th.g.split(",");
            int g1c = Color.argb(15, Integer.parseInt(gc[0].trim()), Integer.parseInt(gc[1].trim()), Integer.parseInt(gc[2].trim()));
            int g2c = Color.argb(36, Integer.parseInt(gc[0].trim()), Integer.parseInt(gc[1].trim()), Integer.parseInt(gc[2].trim()));
            float y1 = cam.y - zH / 2 - 20, y2 = cam.y + zH / 2 + 20;
            float xa = cam.x - zW / 2 - 20, xb = cam.x + zW / 2 + 20;
            ps.setStrokeWidth(1);
            for (int pass = 0; pass < 2; pass++) {
                ps.setColor(pass == 0 ? g1c : g2c);
                path.reset();
                for (float gx = x0; gx < cam.x + zW / 2 + stp; gx += stp)
                    if ((Math.round(gx / stp) % 5 != 0) == (pass == 0)) { path.moveTo(gx, y1); path.lineTo(gx, y2); }
                for (float gy = y0; gy < cam.y + zH / 2 + stp; gy += stp)
                    if ((Math.round(gy / stp) % 5 != 0) == (pass == 0)) { path.moveTo(xa, gy); path.lineTo(xb, gy); }
                c.drawPath(path, ps);
            }
        }

        // кристаллы
        for (Gem g : G) {
            boolean big = g.v >= 3;
            halo(c, g.x, g.y, big ? 30 : 55, big ? 34 : 22, .9f);
            p.setColor(big ? 0xFFFFF3B0 : 0xFFD9FFC4);
            c.save();
            c.translate(g.x, g.y);
            c.rotate(T * 3);
            c.drawRect(-3, -3, 3, 3, p);
            c.restore();
        }

        // новы
        for (Nova n : NV) {
            float k = 1 - n.r / n.m;
            ps.setColor(hsl(n.h, 100, 75, (int) clamp(k * 255, 0, 255)));
            ps.setStrokeWidth(10 * k + 2);
            c.drawCircle(n.x, n.y, n.r, ps);
        }

        // враги
        for (Enemy e : E) {
            if (Math.abs(e.x - cam.x) > zW / 2 + 90 || Math.abs(e.y - cam.y) > zH / 2 + 90) continue;
            if (fxNow > 0 || e.t == 3) halo(c, e.x, e.y, e.h, e.r * 2.8f, e.t == 3 ? 1 : .45f + fxW * .3f);
            c.save();
            c.translate(e.x, e.y);
            float rot = e.t == 2 ? (float) Math.atan2(P.y - e.y, P.x - e.x) : (e.t == 1 ? T : T * .5f);
            int n = e.t == 0 ? 14 : e.t == 1 ? 6 : e.t == 2 ? 3 : e.t == 4 ? 4 : e.t == 5 ? 5 : 12;
            path.reset();
            for (int i = 0; i < n; i++) {
                float a = rot + i / (float) n * TAU;
                float r = e.r;
                if (e.t == 0) r *= 1 + .14f * (float) Math.sin(a * 5 + T * 9 + e.s);
                if (e.t == 3) r *= i % 2 == 1 ? .8 : 1.08f;
                if (e.t == 2 && i > 0) r *= .8f;
                float px = (float) Math.cos(a) * r, py = (float) Math.sin(a) * r;
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            path.close();
            p.setColor(e.f > 0 ? Color.WHITE : hsl(e.h, 85, e.t == 3 ? 32 : (e.st == 1 ? 80 : 52)));
            c.drawPath(path, p);
            ps.setColor(hsl(e.h, 100, 82));
            ps.setStrokeWidth(2);
            c.drawPath(path, ps);
            if (e.el) {
                ps.setColor(0xFFFFC76B);
                ps.setStrokeWidth(3.5f);
                c.drawPath(path, ps);
            }
            // глаза
            float a = (float) Math.atan2(P.y - e.y, P.x - e.x);
            float er = e.r * .32f;
            float ex = (float) Math.cos(a) * e.r * .25f, ey = (float) Math.sin(a) * e.r * .25f;
            float ca = (float) Math.cos(a + 1.57f), sa = (float) Math.sin(a + 1.57f);
            p.setColor(Color.WHITE);
            for (int s = -1; s <= 1; s += 2)
                c.drawCircle(ex + ca * er * s, ey + sa * er * s, er * .6f, p);
            p.setColor(0xFF10001F);
            for (int s = -1; s <= 1; s += 2)
                c.drawCircle(ex * 1.7f + ca * er * s, ey * 1.7f + sa * er * s, er * .3f, p);
            if (e.t == 3) {
                float w = e.r * 2 * clamp(e.hp / e.mh, 0, 1);
                p.setColor(0x80000000);
                c.drawRect(-e.r, -e.r * 1.5f, e.r, -e.r * 1.5f + 6, p);
                p.setColor(0xFFFF5C7C);
                c.drawRect(-e.r, -e.r * 1.5f, -e.r + w, -e.r * 1.5f + 6, p);
            }
            if (e.t == 2 && e.st == 1) {
                ps.setColor(0xCCFF78FF);
                ps.setStrokeWidth(2);
                c.drawLine(0, 0, (float) Math.cos(a) * 300, (float) Math.sin(a) * 300, ps);
            }
            c.restore();
        }

        // вражеские пули
        for (EB b : EBb) {
            float life = b.l / (b.m > 0 ? b.m : 1);
            float k = clamp(life * 2.2f, 0, 1);
            halo(c, b.x, b.y, 355, 16 * k + 4, k);
            p.setColor(Color.WHITE);
            p.setAlpha((int) (k * 255));
            c.drawCircle(b.x, b.y, 4 * (0.5f + 0.5f * k), p);
            p.setAlpha(255);
        }

        // молнии
        for (Bolt n : LN) {
            ps.setColor(Color.argb((int) clamp(n.l * 4 * 255, 0, 255), 210, 235, 255));
            ps.setStrokeWidth(3);
            path.reset();
            path.moveTo(n.x1, n.y1);
            for (int i = 1; i < 7; i++) {
                float t = i / 7f;
                path.lineTo(n.x1 + (n.x2 - n.x1) * t + (rnd() - .5f) * 40,
                        n.y1 + (n.y2 - n.y1) * t + (rnd() - .5f) * 40);
            }
            path.lineTo(n.x2, n.y2);
            c.drawPath(path, ps);
            halo(c, n.x2, n.y2, 205, 60, Math.min(1, n.l * 3));
        }

        // пули
        for (Bullet b : B) {
            float life = clamp(b.l / (b.m > 0 ? b.m : 1.1f), 0, 1);
            float k = clamp(life * 2.2f, 0, 1);
            float sz = st.bsz * (b.sz > 0 ? b.sz : 1);
            float hu = b.hu > 0 ? b.hu : (b.dr ? 175 : (b.ms ? 20 : (b.sg ? 30 : 38)));
            halo(c, b.x, b.y, hu, 22 * sz * k + 4, .9f * k);
            p.setAlpha((int) (k * 255));
            if (b.ln) {
                ps.setColor(hsl(hu, 100, 82));
                ps.setStrokeWidth(3 * sz);
                c.drawLine(b.x - b.vx * .04f, b.y - b.vy * .04f, b.x, b.y, ps);
            } else {
                p.setColor(Color.WHITE);
                c.drawCircle(b.x, b.y, 3.5f * sz * (.5f + .5f * k), p);
            }
            p.setAlpha(255);
        }

        // игрок
        if (S != ST_DEAD) {
            if (fxNow > 0)
                for (int i = 0; i < tr.size(); i++)
                    halo(c, tr.get(i).x, tr.get(i).y, cfgHero, 22 - i, (.5f - i * .03f) * fxW);
            if (st.aura > 0) {
                ps.setColor(0x298CDAFF);
                ps.setStrokeWidth(2);
                c.drawCircle(P.x, P.y, 100 + st.aura * 35, ps);
            }
            if (st.orbs > 0)
                for (int i = 0; i < st.orbs; i++) {
                    float a = ang + i * TAU / st.orbs;
                    float bx = P.x + (float) Math.cos(a) * 80, by = P.y + (float) Math.sin(a) * 80;
                    halo(c, bx, by, 258, 30, 1);
                    c.save();
                    c.translate(bx, by);
                    c.rotate(a * 2);
                    p.setColor(0xFFE9D8FF);
                    path.reset();
                    path.moveTo(0, -12); path.lineTo(5, 0); path.lineTo(0, 12); path.lineTo(-5, 0);
                    path.close();
                    c.drawPath(path, p);
                    c.restore();
                }
            for (int i = 0; i < st.drone; i++) {
                float a = ang * .7f + i * TAU / st.drone;
                float dx = P.x + (float) Math.cos(a) * 54, dy = P.y + (float) Math.sin(a) * 54;
                halo(c, dx, dy, 170, 22, .9f);
                p.setColor(0xFFD9FFFB);
                c.save();
                c.translate(dx, dy);
                c.rotate(a);
                c.drawRect(-4, -4, 4, 4, p);
                c.restore();
            }
            if (shc > 0) {
                ps.setColor(0xBF78DCFF);
                ps.setStrokeWidth(2.5f);
                c.drawCircle(P.x, P.y, 24 + (float) Math.sin(T * 6) * 1.5f, ps);
                halo(c, P.x, P.y, 195, 46, .35f);
            }
            boolean bl = inv > 0 && Math.sin(T * 50) > 0;
            halo(c, P.x, P.y, cfgHero, 70, bl ? .4f : 1);
            p.setShader(heroGrad());
            p.setAlpha(bl ? 128 : 255);
            c.drawCircle(P.x, P.y, 10 + (float) Math.sin(T * 10) * .8f, p);
            p.setShader(null);
            p.setAlpha(255);
            ps.setColor(0x80FFFFFF);
            ps.setStrokeWidth(1.5f);
            c.drawCircle(P.x, P.y, 16 + (float) Math.sin(T * 4) * 2, ps);
        }

        // частицы
        for (Part f : F) {
            float a = f.l / f.m;
            halo(c, f.x, f.y, f.h, f.r * 5 * a + 4, a);
        }

        // всплывающий текст
        pt.setTextSize(15);
        for (FText f : FT) {
            pt.setAlpha((int) clamp(f.l * 2 * 255, 0, 255));
            pt.setColor(f.c);
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeWidth(3);
            c.drawText(f.s, f.x, f.y, pt);
            pt.setStyle(Paint.Style.FILL);
            c.drawText(f.s, f.x, f.y, pt);
        }
        pt.setAlpha(255);
        c.restore();

        // виньетка
        if (vignette == null)
            vignette = new RadialGradient(W / 2, H / 2, Math.min(W, H) * .3f,
                    new int[]{Color.argb(0, 0, 0, 0), Color.argb(184, 3, 2, 8)},
                    new float[]{0, 1}, Shader.TileMode.CLAMP);
        p.setShader(vignette);
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);

        // красная вспышка
        if (fl > 0) {
            p.setShader(new RadialGradient(W / 2, H / 2, Math.min(W, H) * .25f,
                    new int[]{Color.argb(0, 255, 40, 80), Color.argb((int) (fl * .55f * 255), 255, 40, 80)},
                    new float[]{0, 1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, p);
            p.setShader(null);
        }
        c.restore();

        // джойстик
        if (joyOn && S == ST_PLAY) {
            ps.setColor(0x4DFFFFFF);
            ps.setStrokeWidth(3);
            c.drawCircle(joyOx, joyOy, 55, ps);
            float dx = joyX - joyOx, dy = joyY - joyOy;
            float d = Math.min(55, dist(dx, dy));
            if (d == 0) d = 1;
            float a = (float) Math.atan2(dy, dx);
            p.setColor(0x59FFFFFF);
            c.drawCircle(joyOx + (float) Math.cos(a) * d, joyOy + (float) Math.sin(a) * d, 24, p);
        }
    }

    RadialGradient heroGrad() {
        if (heroGrad == null || heroGradCx != cfgHero) {
            heroGrad = new RadialGradient(0, 0, 11,
                    new int[]{Color.WHITE, hsl(cfgHero, 100, 82), hsl(cfgHero, 100, 58)},
                    new float[]{0, .6f, 1}, Shader.TileMode.CLAMP);
            heroGradCx = cfgHero;
        }
        return heroGrad;
    }
    int heroGradCx = -1;

    static float mod(float a, float n) {
        float r = a % n;
        return r < 0 ? r + n : r;
    }

    // ======================= HUD =======================
    void drawHud(Canvas c) {
        if (S == ST_MENU || S == ST_OVER || S == ST_HELP || S == ST_SHOP) return;
        float pad = 14;
        // уровень
        drawHex(c, pad + 26, pad + 30, 30, 0xD007060D, 0x22FFC76B);
        pt.setColor(0xFFF3EEFB);
        pt.setTextSize(22);
        c.drawText(String.valueOf(Math.max(1, lvl)), pad + 26, pad + 37, pt);
        // HP
        float bx = pad + 66;
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setTextSize(12);
        pt.setColor(hp < st.hpm * .3f ? 0xFFFF5C7C : 0xFFFFB2C8);
        c.drawText("♥", bx, pad + 22, pt);
        float bw = Math.min(W * .3f, 260);
        p.setColor(0xD005040B);
        c.drawRect(bx + 18, pad + 10, bx + 18 + bw, pad + 22, p);
        p.setColor(0xFFFF3D64);
        float hw = bw * clamp(hp / st.hpm, 0, 1);
        if (hw > 0) {
            p.setShader(new LinearGradient(bx + 18, 0, bx + 18 + bw, 0,
                    0xFFFF3D64, 0xFFFF9A6A, Shader.TileMode.CLAMP));
            c.drawRect(bx + 18, pad + 10, bx + 18 + hw, pad + 22, p);
            p.setShader(null);
        }
        pt.setColor(0xCCF3EEFB);
        c.drawText(Math.max(0, (int) Math.ceil(hp)) + " / " + (int) st.hpm, bx + 22, pad + 38, pt);
        // XP
        p.setColor(0x22F3EEFB);
        c.drawRect(bx + 18, pad + 42, bx + 18 + bw * .8f, pad + 46, p);
        float xw = bw * .8f * clamp(xp / xpNeed(), 0, 1);
        if (xw > 0) {
            p.setColor(0xFF8FF0D4);
            c.drawRect(bx + 18, pad + 42, bx + 18 + xw, pad + 46, p);
        }
        if (WQ != null) {
            pt.setColor(0xFFA98BFF);
            pt.setTextSize(11);
            Weapons.Def d = Weapons.WD.get(WQ.id);
            c.drawText(d.n + " " + Weapons.TN[WQ.t - 1], bx + 2, pad + 62, pt);
        }
        pt.setTextAlign(Paint.Align.CENTER);

        // таймер
        pt.setColor(0xFFF3EEFB);
        pt.setTextSize(26);
        c.drawText(fmt(gt), W / 2f, pad + 26, pt);
        if (cfgFps != 0) {
            pt.setTextSize(11);
            pt.setColor(0x55F3EEFB);
            c.drawText(fps + " FPS", W / 2f, pad + 44, pt);
        }

        // справа: убийства, звёзды, пауза
        pt.setTextSize(15);
        pt.setTextAlign(Paint.Align.RIGHT);
        pt.setColor(0xFFB2C8FF);
        c.drawText("☠ " + kills, W - pad - 52, pad + 20, pt);
        pt.setColor(0xFFFFC76B);
        c.drawText("★ " + Math.round(stars), W - pad - 52, pad + 42, pt);
        pt.setTextAlign(Paint.Align.CENTER);
        UiBtn pz = btn("pause", "", W - pad - 38, pad, W - pad - 6, pad + 32);
        p.setColor(0x73090712);
        c.drawRoundRect(pz.r, 4, 4, p);
        ps.setColor(0x55F3EEFB);
        ps.setStrokeWidth(1.5f);
        c.drawRoundRect(pz.r, 4, 4, ps);
        p.setColor(0x99F3EEFB);
        c.drawRect(W - pad - 26, pad + 8, W - pad - 22, pad + 24, p);
        c.drawRect(W - pad - 18, pad + 8, W - pad - 14, pad + 24, p);

        // рывок
        if (S == ST_PLAY) {
            float dr = 40;
            float cx = W - pad - 26, cy = H - pad - 30;
            UiBtn db = btn("dash", "", cx - dr, cy - dr, cx + dr, cy + dr);
            float k = dcd > 0 ? 1 - dcd / st.dcd : 1;
            p.setColor(0xBF0A0714);
            c.drawCircle(cx, cy, dr, p);
            if (k < 1) {
                p.setColor(0x668FF0D4);
                c.drawArc(cx - dr, cy - dr, cx + dr, cy + dr, -90, k * 360, true, p);
            }
            ps.setColor(0x598FF0D4);
            ps.setStrokeWidth(2);
            c.drawCircle(cx, cy, dr, ps);
            pt.setTextSize(11);
            pt.setColor(0xFFDFFFF5);
            c.drawText("РЫВОК", cx, cy + 4, pt);
        }
    }

    void drawHex(Canvas c, float cx, float cy, float r, int fill, int stroke) {
        path.reset();
        for (int i = 0; i < 6; i++) {
            float a = -90 + i * 60;
            float px = cx + (float) Math.cos(Math.toRadians(a)) * r;
            float py = cy + (float) Math.sin(Math.toRadians(a)) * r;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
        p.setColor(fill);
        c.drawPath(path, p);
        if (stroke != 0) {
            ps.setColor(stroke);
            ps.setStrokeWidth(2);
            c.drawPath(path, ps);
        }
    }

    // ---------- кнопки UI ----------
    UiBtn btn(String tag, String label, float l, float t, float r, float b) {
        UiBtn x = new UiBtn();
        x.tag = tag; x.label = label;
        x.r.set(l, t, r, b);
        ui.add(x);
        return x;
    }

    void drawBtn(Canvas c, UiBtn b, boolean primary, int accent) {
        if (!b.enabled) p.setColor(0x33000000);
        else if (primary) p.setColor(0x2AFFFFFF);
        else p.setColor(0x08FFFFFF);
        c.drawRoundRect(b.r, 6, 6, p);
        ps.setColor(primary ? accent : 0x4DF3EEFB);
        ps.setStrokeWidth(1.5f);
        c.drawRoundRect(b.r, 6, 6, ps);
        pt.setTextSize(15);
        pt.setColor(b.enabled ? 0xFFF3EEFB : 0x55F3EEFB);
        c.drawText(b.label, b.r.centerX(), b.r.centerY() + 5, pt);
    }

    // ======================= экраны =======================
    void dim(Canvas c, float a) {
        p.setColor(Color.argb((int) (a * 255), 4, 3, 10));
        c.drawRect(0, 0, W, H, p);
    }

    void panel(Canvas c, RectF r) {
        p.setColor(0xEB090712);
        c.drawRoundRect(r, 8, 8, p);
        ps.setColor(0x29F3EEFB);
        ps.setStrokeWidth(1);
        c.drawRoundRect(r, 8, 8, ps);
    }

    void title(Canvas c, String s, float y, float size, int color) {
        pt.setTextSize(size);
        pt.setColor(color);
        c.drawText(s, W / 2f, y, pt);
    }

    void drawMenu(Canvas c) {
        dim(c, .35f);
        float cx = W / 2f;
        // лучи логотипа
        c.save();
        c.translate(cx, H * .22f);
        ps.setStrokeCap(Paint.Cap.ROUND);
        for (int ring = 0; ring < 2; ring++) {
            float dir = ring == 0 ? 1 : -1;
            float sp = ring == 0 ? 26 : 18;
            float len = ring == 0 ? 44 : 34;
            ps.setColor(ring == 0 ? 0xD9FFC76B : 0x80A98BFF);
            ps.setStrokeWidth(ring == 0 ? 3 : 2);
            for (int i = 0; i < 8; i++) {
                float a = T * dir / sp * TAU / 4 + i * TAU / 8;
                c.drawLine((float) Math.cos(a) * 30, (float) Math.sin(a) * 30,
                        (float) Math.cos(a) * len, (float) Math.sin(a) * len, ps);
            }
        }
        halo(c, 0, 0, 32, 40, .9f);
        c.restore();

        // заголовок
        pt.setTextSize(Math.min(W, H) * .16f);
        pt.setShader(new LinearGradient(0, H * .12f, 0, H * .34f,
                new int[]{0xFFFFFDF6, 0xFFFFE0A3, 0xFFFF8A6B, 0xFFC07BFF},
                new float[]{0, .4f, .7f, 1}, Shader.TileMode.CLAMP));
        pt.setShadowLayer(18, 0, 0, 0x6BFF8C5A);
        c.drawText("ИСКРА", cx, H * .3f, pt);
        pt.setShadowLayer(0, 0, 0, 0);
        pt.setShader(null);
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("В Ы Ж И В И   В   Р О Е", cx, H * .35f, pt);

        pt.setTextSize(15);
        pt.setColor(0x9EF3EEFB);
        c.drawText("Ты — последний огонёк. Рой уже близко. Продержись как можно дольше.", cx, H * .43f, pt);

        float bw = Math.min(320, W * .5f), bh = 52, gap = 14;
        float top = H * .5f;
        int i = 0;
        if (hasSave) {
            UiBtn r = btn("resume_run", "Продолжить забег", cx - bw / 2, top, cx + bw / 2, top + bh);
            drawBtn(c, r, true, 0xFF8FF0D4);
            top += bh + gap; i++;
        }
        UiBtn b1 = btn("start", "В бой", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b1, true, 0xFFFFC76B); top += bh + gap;
        UiBtn b2 = btn("help", "Как играть", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b2, false, 0); top += bh + gap;
        UiBtn b3 = btn("open_shop", "Магазин", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b3, false, 0); top += bh + gap;
        UiBtn b4 = btn("set", "Настройки", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b4, false, 0); top += bh + gap;
        if (best > 0) {
            pt.setTextSize(13);
            pt.setColor(0x57F3EEFB);
            c.drawText("Рекорд · " + fmt(best), cx, top + 16, pt);
        }
    }

    void drawHelp(Canvas c) {
        dim(c, .6f);
        float pw = Math.min(W * .86f, 640), ph = H * .86f;
        RectF pr = new RectF(W / 2f - pw / 2, H / 2f - ph / 2, W / 2f + pw / 2, H / 2f + ph / 2);
        panel(c, pr);
        title(c, "Как играть", pr.top + 48, 30, 0xFFF3EEFB);
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("П Я Т Ь   П Р А В И Л   В Ы Ж И В А Н И Я", W / 2f, pr.top + 72, pt);
        String[][] rules = {
            {"Двигайся.", "Веди пальцем по экрану (виртуальный джойстик)."},
            {"Стрельба.", "Автоматически по ближайшему врагу."},
            {"Собирай кристаллы.", "Новый уровень даёт выбор из трёх усилений."},
            {"Переброс карточек.", "Каждый переброс стоит всё больше убийств: 3, 6, 9…"},
            {"Рывок.", "Кнопка справа внизу — ускорение и защита на миг."},
            {"Звёзды — для магазина.", "Трать их на постоянные усиления."},
            {"Оружие.", "В оружейной покупай пушки за убийства и скрещивай одинаковые."},
        };
        int[] cc = {0xFF8FF0D4, 0xFFFFC76B, 0xFFC9F57A, 0xFFFFC76B, 0xFFFF5C7C, 0xFFFF9A6A, 0xFFA98BFF};
        float y = pr.top + 108;
        for (int i = 0; i < rules.length; i++) {
            pt.setTextAlign(Paint.Align.LEFT);
            pt.setTextSize(15);
            pt.setColor(cc[i]);
            c.drawText("◆", pr.left + 30, y, pt);
            pt.setColor(0xFFFFFFFF);
            c.drawText(rules[i][0], pr.left + 56, y, pt);
            pt.setTextSize(13);
            pt.setColor(0xB0F3EEFB);
            c.drawText(rules[i][1], pr.left + 56, y + 20, pt);
            y += 46;
        }
        pt.setTextAlign(Paint.Align.CENTER);
        UiBtn ok = btn("menu_from_help", "Понятно", W / 2f - 140, pr.bottom - 74, W / 2f + 140, pr.bottom - 26);
        drawBtn(c, ok, true, 0xFFFFC76B);
    }

    void drawLvl(Canvas c) {
        dim(c, .55f);
        title(c, "Новый уровень " + lvl, H * .14f, 34, 0xFFF3EEFB);
        pt.setTextSize(13);
        pt.setColor(0x57F3EEFB);
        c.drawText(lvl % 5 == 0 ? "В Ы Б Е Р И   О Д Н О   И З   Т Р Ё Х" : "В Ы Б Е Р И   О Д Н О   И З   Т Р Ё Х", W / 2f, H * .2f, pt);
        // карточки
        float cw = Math.min(W * .26f, 250), ch = Math.min(H * .58f, 330);
        float gap = Math.min(W * .04f, 40);
        float top = H * .26f;
        for (int i = 0; i < pick.size(); i++) {
            Upgrade u = pick.get(i);
            float l = W / 2f + (i - 1) * (cw + gap) - cw / 2;
            RectF r = new RectF(l, top, l + cw, top + ch);
            UiBtn b = btn("card" + i, "", r.left, r.top, r.right, r.bottom);
            b.data = u;
            p.setColor(0xF7161226);
            c.drawRoundRect(r, 6, 6, p);
            ps.setColor(Upgrade.RARC[u.r]);
            ps.setStrokeWidth(2);
            c.drawRoundRect(r, 6, 6, ps);
            pt.setTextSize(10);
            pt.setColor(Upgrade.RARC[u.r]);
            c.drawText(Upgrade.RAR[u.r].toUpperCase(Locale.US), r.centerX(), r.top + 22, pt);
            // иконка-гекс
            drawHex(c, r.centerX(), r.top + 62, 26, blend(u.c, 0xFF160C2A), u.c);
            pt.setTextSize(24);
            pt.setColor(0xFF120A24);
            c.drawText(u.n.substring(0, 1), r.centerX(), r.top + 70, pt);
            pt.setTextSize(16);
            pt.setColor(0xFFFFFFFF);
            c.drawText(u.n, r.centerX(), r.top + 112, pt);
            pt.setTextSize(12);
            pt.setColor(u.c);
            c.drawText(u.s(upgLv(u.id)), r.centerX(), r.top + 134, pt);
            pt.setTextSize(11);
            pt.setColor(0x9EF3EEFB);
            drawMultiline(c, u.d, r.centerX(), r.top + 156, r.width() - 24, 15);
            int lv = upgLv(u.id);
            pt.setTextSize(10);
            pt.setColor(0x9EF3EEFB);
            c.drawText(lv > 0 ? "Ур. " + lv + " → " + (lv + 1) : "НОВОЕ", r.centerX(), r.bottom - 16, pt);
        }
        float bw = 230, bh = 44, by = top + ch + 22;
        UiBtn rl = btn("roll", "Перебросить · " + rollCost() + " убийств", W / 2f - bw - 12, by, W / 2f - 12, by + bh);
        rl.enabled = kills >= rollCost();
        drawBtn(c, rl, false, 0);
        int q = 0;
        for (Weapons.Wpn o : OF) if (!o.sold) q++;
        UiBtn wb = btn("wp_open", "Оружейная" + (q > 0 ? " · " + q : ""), W / 2f + 12, by, W / 2f + bw + 12, by + bh);
        drawBtn(c, wb, false, 0xFFA98BFF);
    }

    void drawPause(Canvas c) {
        dim(c, .6f);
        title(c, "Пауза", H * .3f, 42, 0xFFF3EEFB);
        float bw = 280, bh = 52, cx = W / 2f, top = H * .42f;
        UiBtn b1 = btn("resume", "Продолжить", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b1, true, 0xFFFFC76B); top += bh + 14;
        UiBtn b2 = btn("set_pause", "Настройки", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b2, false, 0); top += bh + 14;
        UiBtn b3 = btn("quit", "В меню", cx - bw / 2, top, cx + bw / 2, top + bh);
        drawBtn(c, b3, false, 0);
    }

    void drawOver(Canvas c) {
        dim(c, .65f);
        float pw = Math.min(W * .6f, 420), ph = H * .8f;
        RectF pr = new RectF(W / 2f - pw / 2, H / 2f - ph / 2, W / 2f + pw / 2, H / 2f + ph / 2);
        panel(c, pr);
        title(c, "Рой победил", pr.top + 52, 30, 0xFFFF5C7C);
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("Н О   Н Е   О К О Н Ч А Т Е Л Ь Н О", W / 2f, pr.top + 76, pt);
        String[][] rows = {
            {"Время", fmt(gt)}, {"Убито", String.valueOf(kills)},
            {"Уровень", String.valueOf(lvl)}, {"Звёзд за забег", String.valueOf(Math.round(stars))},
            {"Рекорд", fmt(best)},
        };
        float y = pr.top + 122;
        pt.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < rows.length; i++) {
            pt.setTextSize(15);
            pt.setColor(0x9EF3EEFB);
            c.drawText(rows[i][0], pr.left + 40, y, pt);
            pt.setTextAlign(Paint.Align.RIGHT);
            pt.setColor(i >= 3 ? 0xFFFFC76B : 0xFFF3EEFB);
            pt.setTextSize(19);
            c.drawText(rows[i][1], pr.right - 40, y, pt);
            pt.setTextAlign(Paint.Align.LEFT);
            p.setColor(0x14F3EEFB);
            c.drawRect(pr.left + 40, y + 8, pr.right - 40, y + 9, p);
            y += 40;
        }
        pt.setTextAlign(Paint.Align.CENTER);
        UiBtn a = btn("again", "Ещё раз", W / 2f - 220, pr.bottom - 78, W / 2f - 14, pr.bottom - 30);
        drawBtn(c, a, true, 0xFFFFC76B);
        UiBtn m = btn("menu_from_over", "В меню", W / 2f + 14, pr.bottom - 78, W / 2f + 220, pr.bottom - 30);
        drawBtn(c, m, false, 0);
    }

    // ---------- настройки ----------
    void drawSet(Canvas c) {
        dim(c, .6f);
        float pw = Math.min(W * .9f, 620), ph = H * .92f;
        RectF pr = new RectF(W / 2f - pw / 2, H / 2f - ph / 2, W / 2f + pw / 2, H / 2f + ph / 2);
        panel(c, pr);
        title(c, "Настройки", pr.top + 40, 26, 0xFFF3EEFB);
        pt.setTextSize(11);
        pt.setColor(0x57F3EEFB);
        c.drawText("В К У С   И   О Щ У Щ Е Н И Я", W / 2f, pr.top + 60, pt);

        float y = pr.top + 92;
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("ВНЕШНИЙ ВИД", pr.left + 30, y, pt);
        y += 18;
        // фон арены
        pt.setColor(0xDFF3EEFB);
        c.drawText("Фон арены", pr.left + 30, y + 26, pt);
        float bx = pr.right - 30 - 5 * 46;
        for (int i = 0; i < BG.length; i++) {
            UiBtn b = btn("bg" + i, "", bx + i * 46, y + 8, bx + i * 46 + 36, y + 44);
            p.setShader(new android.graphics.SweepGradient(b.r.centerX(), b.r.centerY(),
                    new int[]{BG[i].c, argb(BG[i].b)}, null));
            c.drawOval(b.r, p);
            p.setShader(null);
            if (cfgBg == i) {
                ps.setColor(Color.WHITE);
                ps.setStrokeWidth(3);
                c.drawOval(b.r, ps);
            }
        }
        y += 62;
        // цвет искры
        c.drawText("Цвет искры", pr.left + 30, y + 26, pt);
        bx = pr.right - 30 - HEROES.length * 46;
        for (int i = 0; i < HEROES.length; i++) {
            UiBtn b = btn("hero" + HEROES[i], "", bx + i * 46, y + 8, bx + i * 46 + 36, y + 44);
            p.setColor(hsl(HEROES[i], 100, 62));
            c.drawOval(b.r, p);
            if (cfgHero == HEROES[i]) {
                ps.setColor(Color.WHITE);
                ps.setStrokeWidth(3);
                c.drawOval(b.r, ps);
            }
        }
        y += 62;
        sectionToggle(c, pr, y, "tg:grid", "Сетка на полу", cfgGrid != 0); y += 46;
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("ЭФФЕКТЫ", pr.left + 30, y, pt); y += 20;
        pt.setColor(0xDFF3EEFB);
        pt.setTextSize(14);
        c.drawText("Качество", pr.left + 30, y + 24, pt);
        seg(c, pr.right - 30 - 3 * 84, y, 84, 36, new String[]{"Мало", "Норма", "Макс"}, cfgFx, "fx"); y += 52;
        sectionToggle(c, pr, y, "tg:shake", "Тряска экрана", cfgShake != 0); y += 46;
        sectionToggle(c, pr, y, "tg:nums", "Цифры урона", cfgNums != 0); y += 46;
        sectionToggle(c, pr, y, "tg:fps", "Счётчик FPS", cfgFps != 0); y += 46;
        pt.setTextSize(14);
        pt.setColor(0xDFF3EEFB);
        c.drawText("Лимит FPS", pr.left + 30, y + 24, pt);
        seg(c, pr.right - 30 - 3 * 84, y, 84, 36, new String[]{"60", "144", "Макс"},
                cfgCap == 60 ? 0 : (cfgCap == 144 ? 1 : 2), "cap"); y += 56;
        // громкость
        pt.setColor(0xDFF3EEFB);
        c.drawText("Громкость", pr.left + 30, y + 24, pt);
        float sw = pr.width() - 220;
        RectF sr = new RectF(pr.right - 30 - sw - 50, y + 14, pr.right - 30 - 50, y + 26);
        p.setColor(0x2EF3EEFB);
        c.drawRoundRect(sr, 6, 6, p);
        p.setColor(0xFFFFC76B);
        float fw = sw * cfgVol;
        if (fw > 4) c.drawRoundRect(new RectF(sr.left, sr.top, sr.left + fw, sr.bottom), 6, 6, p);
        UiBtn vs = btn("vol", "", sr.left - 16, y, sr.right + 16, y + 40);
        volSlider = vs;
        pt.setTextSize(14);
        pt.setColor(0xFFF3EEFB);
        c.drawText(Math.round(cfgVol * 100) + "%", pr.right - 34, y + 26, pt);
        y += 56;

        UiBtn ok = btn("set_back", "Готово", W / 2f - 140, pr.bottom - 66, W / 2f + 140, pr.bottom - 20);
        drawBtn(c, ok, true, 0xFFFFC76B);
    }

    void sectionToggle(Canvas c, RectF pr, float y, String tag, String name, boolean on) {
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setTextSize(14);
        pt.setColor(0xDFF3EEFB);
        c.drawText(name, pr.left + 30, y + 24, pt);
        UiBtn b = btn(tag, "", pr.right - 30 - 56, y + 6, pr.right - 30, y + 32);
        p.setColor(on ? 0x298FF0D4 : 0x14000000);
        c.drawRoundRect(b.r, 99, 99, p);
        ps.setColor(on ? 0x998FF0D4 : 0x33F3EEFB);
        ps.setStrokeWidth(2);
        c.drawRoundRect(b.r, 99, 99, ps);
        p.setColor(on ? 0xFF8FF0D4 : 0x66F3EEFB);
        float kx = on ? b.r.right - 15 : b.r.left + 15;
        c.drawCircle(kx, b.r.centerY(), 11, p);
    }

    void seg(Canvas c, float l, float t, float bw, float bh, String[] opts, int on, String tag) {
        for (int i = 0; i < opts.length; i++) {
            UiBtn b = btn(tag + (tag.equals("cap") ? (i == 0 ? 60 : i == 1 ? 144 : 500) : i), "", l + i * bw, t, l + (i + 1) * bw, t + bh);
            boolean o = i == on;
            p.setColor(o ? 0x29FFC76B : 0x08FFFFFF);
            c.drawRect(b.r, p);
            ps.setColor(o ? 0x99FFC76B : 0x2BF3EEFB);
            ps.setStrokeWidth(1);
            c.drawRect(b.r, ps);
            pt.setTextSize(12);
            pt.setColor(o ? 0xFFFFC76B : 0x9EF3EEFB);
            c.drawText(opts[i], b.r.centerX(), b.r.centerY() + 4, pt);
        }
    }

    // ---------- магазин ----------
    void drawShop(Canvas c) {
        dim(c, .75f);
        title(c, "Арсенал", H * .12f, 34, 0xFFF3EEFB);
        pt.setTextSize(12);
        pt.setColor(0x57F3EEFB);
        c.drawText("У Л У Ч Ш Е Н И Я   З А   З В Ё З Д Ы", W / 2f, H * .17f, pt);
        pt.setTextSize(22);
        pt.setColor(0xFFFFC76B);
        c.drawText("★ " + Math.round(stars), W - 60, H * .12f, pt);

        int cols = 5;
        float gap = 12;
        float cw = (W * .9f - (cols - 1) * gap) / cols;
        float ch = (H * .68f - 2 * gap) / 3;
        float l0 = W / 2f - (cols * cw + (cols - 1) * gap) / 2;
        float t0 = H * .22f;
        for (int i = 0; i < SHOP.size(); i++) {
            ShopItems s = SHOP.get(i);
            int lv = shopLv.get(s.id) == null ? 0 : shopLv.get(s.id);
            boolean max = lv >= 20;
            int pr = s.price(lv);
            float l = l0 + (i % cols) * (cw + gap);
            float t = t0 + (i / cols) * (ch + gap);
            RectF r = new RectF(l, t, l + cw, t + ch);
            UiBtn b = btn("sh:" + s.id, "", r.left, r.top, r.right, r.bottom);
            b.enabled = !max && stars >= pr;
            int ac = s.premium ? 0xFFA98BFF : 0xFFFFC76B;
            p.setColor(0xF71C1633);
            c.drawRoundRect(r, 10, 10, p);
            ps.setColor(b.enabled ? ac : 0x28F3EEFB);
            ps.setStrokeWidth(1.5f);
            c.drawRoundRect(r, 10, 10, ps);
            pt.setTextAlign(Paint.Align.LEFT);
            pt.setTextSize(13);
            pt.setColor(0xFFFFFFFF);
            drawMultilineL(c, s.n, r.left + 12, r.top + 22, r.width() - 20, 15);
            pt.setTextSize(11);
            pt.setColor(ac);
            c.drawText(max ? s.descCur(lv) : s.desc(lv), r.left + 12, r.top + 52, pt);
            pt.setTextSize(10);
            pt.setColor(0x9EF3EEFB);
            drawMultilineL(c, s.info, r.left + 12, r.top + 70, r.width() - 20, 12);
            // пипсы
            float pw = (r.width() - 24) / 20;
            for (int k = 0; k < 20; k++) {
                p.setColor(k < lv ? ac : 0x1AF3EEFB);
                c.drawRect(r.left + 12 + k * pw, r.bottom - 34, r.left + 12 + k * pw + pw - 1.5f, r.bottom - 28, p);
            }
            pt.setTextSize(11);
            pt.setColor(b.enabled ? 0xFFFFFFFF : 0xFFFFB2C8);
            c.drawText(max ? "MAX" : "★ " + pr + "  ·  ур. " + lv + "/20", r.left + 12, r.bottom - 12, pt);
            pt.setTextAlign(Paint.Align.CENTER);
        }
        UiBtn cl = btn("close_shop", "Закрыть", W / 2f - 130, H * .94f - 44, W / 2f + 130, H * .94f);
        drawBtn(c, cl, true, 0xFFFFC76B);
    }

    // ---------- оружейная ----------
    void weaponCard(Canvas c, RectF r, Weapons.Wpn w, String tag, boolean selected) {
        Weapons.Def d = Weapons.WD.get(w.id);
        UiBtn b = btn(tag, "", r.left, r.top, r.right, r.bottom);
        p.setColor(0xF71C1633);
        c.drawRoundRect(r, 8, 8, p);
        ps.setColor(selected ? d.c : 0x24F3EEFB);
        ps.setStrokeWidth(selected ? 3 : 1);
        c.drawRoundRect(r, 8, 8, ps);
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setTextSize(12);
        pt.setColor(0xFFFFFFFF);
        c.drawText(d.n, r.left + 10, r.top + 20, pt);
        pt.setTextSize(10);
        pt.setColor(d.c);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.t; i++) sb.append("★");
        c.drawText(sb.toString(), r.left + 10, r.top + 36, pt);
        float ey = r.top + 54;
        for (int[] f : w.fx) {
            if (ey > r.bottom - 34) break;
            String k = fxKey(f[0]);
            Weapons.FxDef fd = Weapons.EF.get(k);
            if (fd == null) continue;
            p.setColor(hsl(fd.h, 90, 62, 56));
            RectF chip = new RectF(r.left + 10, ey - 9, r.left + 10 + 8 + fd.n.length() * 7, ey + 5);
            c.drawRoundRect(chip, 99, 99, p);
            pt.setColor(0xFFFFFFFF);
            c.drawText(fd.n + (f[1] > 1 ? " II" : ""), r.left + 14, ey + 2, pt);
            ey += 18;
        }
        pt.setTextSize(9);
        pt.setColor(0x9EF3EEFB);
        c.drawText(Weapons.statsLine(this, w), r.left + 10, r.bottom - 22, pt);
        pt.setTextAlign(Paint.Align.CENTER);
    }

    void drawWp(Canvas c) {
        dim(c, .7f);
        title(c, "Оружейная", H * .1f, 28, 0xFFF3EEFB);
        pt.setTextSize(13);
        pt.setColor(0xFFFFC76B);
        c.drawText("Убийства · " + kills, W / 2f, H * .15f, pt);

        float gap = 10;
        float cw = Math.min((W * .92f - 4 * gap) / 5, 190);
        float ch = 108;
        float l0 = W / 2f - (5 * cw + 4 * gap) / 2;
        float y = H * .2f;
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setTextSize(11);
        pt.setColor(0x57F3EEFB);
        c.drawText("ВИТРИНА", l0, y - 8, pt);
        pt.setTextAlign(Paint.Align.CENTER);
        if (OF.isEmpty()) {
            pt.setColor(0x57F3EEFB);
            c.drawText("Витрина обновится после нового уровня", W / 2f, y + ch / 2, pt);
        }
        for (int i = 0; i < OF.size(); i++) {
            Weapons.Wpn o = OF.get(i);
            float l = l0 + i * (cw + gap);
            if (o.sold) {
                RectF r = new RectF(l, y, l + cw, y + ch);
                p.setColor(0x40000000);
                c.drawRoundRect(r, 8, 8, p);
                ps.setColor(0x20F3EEFB);
                c.drawRoundRect(r, 8, 8, ps);
                pt.setColor(0x4DF3EEFB);
                c.drawText("Продано", r.centerX(), r.centerY(), pt);
                continue;
            }
            weaponCard(c, new RectF(l, y, l + cw, y + ch), o, "buy" + i, false);
            pt.setTextSize(11);
            pt.setColor(kills >= o.cost ? 0xFFFFC76B : 0xFFFFB2C8);
            c.drawText("Купить · " + o.cost + " уб.", l + cw / 2, y + ch - 6, pt);
        }

        // в руках
        y += ch + 34;
        pt.setTextAlign(Paint.Align.LEFT);
        pt.setColor(0x57F3EEFB);
        c.drawText("В РУКАХ", l0, y - 8, pt);
        pt.setTextAlign(Paint.Align.CENTER);
        if (WQ != null) weaponCard(c, new RectF(l0, y, l0 + cw, y + ch), WQ, "wsel:" + WQ.u, SEL.contains(WQ.u));
        else {
            RectF r = new RectF(l0, y, l0 + cw, y + ch);
            ps.setColor(0x28F3EEFB);
            ps.setStrokeWidth(1);
            ps.setPathEffect(new android.graphics.DashPathEffect(new float[]{6, 6}, 0));
            c.drawRoundRect(r, 8, 8, ps);
            ps.setPathEffect(null);
            pt.setColor(0x57F3EEFB);
            c.drawText("Базовая «Искра»", r.centerX(), r.centerY(), pt);
        }
        // сейф
        pt.setTextAlign(Paint.Align.LEFT);
        c.drawText("СЕЙФ · " + VT.size() + "/" + VMAX, l0 + cw + gap + 10, y - 8, pt);
        pt.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < VMAX; i++) {
            float l = l0 + cw + gap + 10 + i * (cw * .8f + 8);
            if (i < VT.size()) {
                Weapons.Wpn w = VT.get(i);
                weaponCard(c, new RectF(l, y, l + cw * .8f, y + ch), w, "wsel:" + w.u, SEL.contains(w.u));
            } else {
                RectF r = new RectF(l, y, l + cw * .8f, y + ch);
                ps.setColor(0x1AF3EEFB);
                ps.setStrokeWidth(1);
                c.drawRoundRect(r, 8, 8, ps);
                pt.setColor(0x33F3EEFB);
                c.drawText("Пусто", r.centerX(), r.centerY(), pt);
            }
        }

        // кнопки действий
        ArrayList<Weapons.Wpn> ws = selWpns();
        float by = y + ch + 16, bh = 42;
        if (ws.size() == 1) {
            Weapons.Wpn w = ws.get(0);
            boolean isEq = WQ != null && WQ.u.equals(w.u);
            UiBtn b1 = btn(isEq ? "wp_vault" : "wp_equip", isEq ? "В сейф" : "Взять в руки",
                    W / 2f - 360, by, W / 2f - 130, by + bh);
            drawBtn(c, b1, true, 0xFFA98BFF);
            UiBtn b2 = btn("wp_sell", "Продать +" + (w.cost >> 1) + " уб.", W / 2f + 130, by, W / 2f + 360, by + bh);
            drawBtn(c, b2, false, 0);
        } else if (ws.size() == 2) {
            UiBtn bm = btn("wp_merge", "Скрестить", W / 2f - 120, by, W / 2f + 120, by + bh);
            bm.enabled = ws.get(0).id.equals(ws.get(1).id) && ws.get(0).t == ws.get(1).t && ws.get(0).t < 4;
            drawBtn(c, bm, true, 0xFFFFC76B);
        } else {
            pt.setTextSize(12);
            pt.setColor(0x57F3EEFB);
            c.drawText("Выбери одно оружие — действия, два одинаковых — скрещивание", W / 2f, by + 24, pt);
        }
        UiBtn cl = btn("wp_close", "Назад", W / 2f - 110, H - 54, W / 2f + 110, H - 12);
        drawBtn(c, cl, false, 0);
    }

    void drawMultiline(Canvas c, String s, float cx, float y, float maxW, float lh) {
        pt.setTextAlign(Paint.Align.CENTER);
        for (String line : wrap(s, maxW)) {
            c.drawText(line, cx, y, pt);
            y += lh;
        }
    }

    void drawMultilineL(Canvas c, String s, float x, float y, float maxW, float lh) {
        pt.setTextAlign(Paint.Align.LEFT);
        for (String line : wrap(s, maxW)) {
            c.drawText(line, x, y, pt);
            y += lh;
        }
    }

    java.util.List<String> wrap(String s, float maxW) {
        java.util.List<String> out = new ArrayList<>();
        String[] words = s.split(" ");
        String cur = "";
        for (String w : words) {
            String t = cur.isEmpty() ? w : cur + " " + w;
            if (pt.measureText(t) > maxW && !cur.isEmpty()) {
                out.add(cur);
                cur = w;
            } else cur = t;
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }

    static int blend(int c1, int c2) {
        float a = Color.alpha(c1) / 255f;
        int r = (int) (Color.red(c1) * a + Color.red(c2) * (1 - a));
        int g = (int) (Color.green(c1) * a + Color.green(c2) * (1 - a));
        int b = (int) (Color.blue(c1) * a + Color.blue(c2) * (1 - a));
        return Color.rgb(r, g, b);
    }
}
