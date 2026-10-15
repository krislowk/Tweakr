package com.krislowk.tweakr;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {
    private static final int PICK = 1;
    private TextView out;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int p = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p * 2, p, p);

        TextView title = new TextView(this);
        title.setText("Tweakr");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        Button pick = new Button(this);
        pick.setText("Pick APK");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });

        out = new TextView(this);
        out.setText("Pick an APK to inspect it.");
        out.setTextIsSelectable(true);
        out.setPadding(0, p, 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(out);

        root.addView(title);
        root.addView(pick);
        root.addView(scroll);
        setContentView(root);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK && res == RESULT_OK && data != null && data.getData() != null) {
            inspect(data.getData());
        }
    }

    private void inspect(Uri uri) {
        try {
            File f = new File(getCacheDir(), "picked.apk");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream o = new FileOutputStream(f)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            }

            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(
                    f.getAbsolutePath(), PackageManager.GET_PERMISSIONS);
            if (pi == null) {
                out.setText("Not a valid APK.");
                return;
            }
            pi.applicationInfo.sourceDir = f.getAbsolutePath();
            pi.applicationInfo.publicSourceDir = f.getAbsolutePath();

            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(pm.getApplicationLabel(pi.applicationInfo)).append("\n");
            sb.append("Package: ").append(pi.packageName).append("\n");
            sb.append("Version: ").append(pi.versionName).append("\n");
            sb.append("Size: ").append(f.length() / 1024).append(" KB\n\n");

            sb.append("Permissions:\n");
            if (pi.requestedPermissions != null) {
                for (String perm : pi.requestedPermissions) sb.append("  ").append(perm).append("\n");
            } else {
                sb.append("  none\n");
            }

            sb.append("\nFiles:\n");
            try (ZipFile z = new ZipFile(f)) {
                Enumeration<? extends ZipEntry> e = z.entries();
                while (e.hasMoreElements()) {
                    ZipEntry ze = e.nextElement();
                    sb.append("  ").append(ze.getName())
                      .append(" (").append(ze.getSize()).append(" B)\n");
                }
            }
            out.setText(sb.toString());
        } catch (Exception e) {
            out.setText("Error: " + e);
        }
    }
}
