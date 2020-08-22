package shehryar.paighaam;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permission {
    public static boolean permissionsValidate(int requestCode, Activity activity, String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> lPermissions = new ArrayList<>();
            for (String permission : permissions) {
                Boolean validaPermissao = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
                if (!validaPermissao) {
                    lPermissions.add(permission);
                }
            }
            if (lPermissions.isEmpty()) {
                return true;
            }
            String[] newPermissions = new String[lPermissions.size()];
            lPermissions.toArray(newPermissions);
            ActivityCompat.requestPermissions(activity, newPermissions, requestCode);
        }
        return true;
    }
}
