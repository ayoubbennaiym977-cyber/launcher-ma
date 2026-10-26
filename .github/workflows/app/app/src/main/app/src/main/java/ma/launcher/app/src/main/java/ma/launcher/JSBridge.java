package ma.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JSBridge {
    private final Context ctx;
    public JSBridge(Context c) { ctx = c; }

    @JavascriptInterface
    public String getApps() {
        JSONArray arr = new JSONArray();
        PackageManager pm = ctx.getPackageManager();
        Set<String> seen = new HashSet<>();
        List<ResolveInfo> list = new ArrayList<>();

        // تطبيقات التلفاز أولًا
        Intent tv = new Intent(Intent.ACTION_MAIN, null);
        tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        list.addAll(pm.queryIntentActivities(tv, 0));

        // ثم التطبيقات العادية
        Intent normal = new Intent(Intent.ACTION_MAIN, null);
        normal.addCategory(Intent.CATEGORY_LAUNCHER);
        list.addAll(pm.queryIntentActivities(normal, 0));

        for (ResolveInfo r : list) {
            try {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(ctx.getPackageName())) continue; // لا تعرض نفسك
                if (seen.contains(pkg)) continue;
                seen.add(pkg);

                JSONObject o = new JSONObject();
                o.put("n", r.loadLabel(pm).toString());
                o.put("p", pkg);
                o.put("i", "data:image/png;base64," + icon(r.loadIcon(pm)));
                arr.put(o);
            } catch (Exception e) { /* تجاهل */ }
        }
        return arr.toString();
    }

    @JavascriptInterface
    public void launch(String pkg) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            }
        } catch (Exception e) { /* تجاهل */ }
    }

    @JavascriptInterface
    public void open(String pkgs, String url) {
        // جرّب كل حزمة معطاة
        if (pkgs != null && !pkgs.isEmpty()) {
            for (String p : pkgs.split(",")) {
                try {
                    Intent i = ctx.getPackageManager().getLaunchIntentForPackage(p.trim());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(i);
                        return;
                    }
                } catch (Exception e) { /* جرّب التالي */ }
            }
        }
        // احتياط: افتح الرابط
        if (url != null && !url.isEmpty()) {
            try {
                Intent w = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                w.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(w);
            } catch (Exception e) { /* تجاهل */ }
        }
    }

    private String icon(Drawable d) {
        try {
            int w = d.getIntrinsicWidth(), h = d.getIntrinsicHeight();
            if (w <= 0) w = 128;
            if (h <= 0) h = 128;
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, bo);
            return Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return ""; }
    }
}
