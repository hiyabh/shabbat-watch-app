package il.hiya.shabbatwatch.mode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import il.hiya.shabbatwatch.ManagedSetting
import il.hiya.shabbatwatch.SettingsNamespace

/**
 * Reads and writes system settings (global / secure / system).
 * Requires WRITE_SECURE_SETTINGS, granted once via adb (scripts/watch-grant.ps1).
 */
class SystemSettingsGateway(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun read(namespace: SettingsNamespace, key: String): String? = when (namespace) {
        SettingsNamespace.GLOBAL -> Settings.Global.getString(resolver, key)
        SettingsNamespace.SECURE -> Settings.Secure.getString(resolver, key)
        SettingsNamespace.SYSTEM -> Settings.System.getString(resolver, key)
    }

    fun write(namespace: SettingsNamespace, key: String, value: String): Boolean = try {
        when (namespace) {
            SettingsNamespace.GLOBAL -> Settings.Global.putString(resolver, key, value)
            SettingsNamespace.SECURE -> Settings.Secure.putString(resolver, key, value)
            SettingsNamespace.SYSTEM -> Settings.System.putString(resolver, key, value)
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "Cannot write $namespace/$key - permission missing", e)
        false
    }

    /**
     * Writes the Shabbat value of every setting that exists on this device and returns a snapshot
     * of the original values so they can be restored exactly. Missing keys are skipped.
     */
    fun applyShabbatValues(settings: List<ManagedSetting>): Map<String, String> {
        val snapshot = mutableMapOf<String, String>()
        for (setting in settings) {
            val original = read(setting.namespace, setting.key) ?: continue
            snapshot[snapshotKey(setting.namespace, setting.key)] = original
            write(setting.namespace, setting.key, setting.shabbatValue)
        }
        return snapshot
    }

    fun restore(snapshot: Map<String, String>) {
        for ((snapshotKey, value) in snapshot) {
            val (namespace, key) = parseSnapshotKey(snapshotKey) ?: continue
            write(namespace, key, value)
        }
    }

    companion object {
        private const val TAG = "SystemSettingsGateway"
        private const val SNAPSHOT_SEPARATOR = ":"

        fun snapshotKey(namespace: SettingsNamespace, key: String): String =
            "${namespace.name}$SNAPSHOT_SEPARATOR$key"

        fun parseSnapshotKey(snapshotKey: String): Pair<SettingsNamespace, String>? {
            val idx = snapshotKey.indexOf(SNAPSHOT_SEPARATOR)
            if (idx <= 0) return null
            val namespace = runCatching { SettingsNamespace.valueOf(snapshotKey.substring(0, idx)) }
                .getOrNull() ?: return null
            return namespace to snapshotKey.substring(idx + 1)
        }
    }
}
