# מצב שבת לשעוני Wear OS (Galaxy Watch)

אפליקציית Wear OS: לחיצה אחת בערב שבת, והשעון מציג רק שעה, תאריך עברי, פרשה וזמני שבת - עמום, קבוע,
בלי תגובה להרמת יד או למגע, בלי התראות ורטט. לחיצה במוצאי שבת (או כיבוי אוטומטי בתום המשך שנבחר)
משחזרת את כל ההגדרות בדיוק כפי שהיו.

> **English:** A Wear OS app that turns the watch into a dim, always-on, touch-insensitive clock for
> Shabbat (time, Hebrew date, parsha, candle lighting / Shabbat end) and restores every setting afterwards.
> It is not on Google Play because it needs permissions that can only be granted over adb - see
> [Installation](#התקנה) (commands are copy-paste). UI is in Hebrew and English; cities are in Israel.

## חשוב לדעת לפני ההתקנה

- **נבדק רק על Galaxy Watch 7** (Wear OS 6 / One UI 8 Watch). נדרש Wear OS 5 ומעלה (Android 14).
  בשעונים אחרים ייתכן שחלק מההגדרות (הרם-יד, גע-להערה) לא יושפעו - מפתחות שלא קיימים במכשיר פשוט מדולגים.
- **האפליקציה לא בחנות Google Play.** היא צריכה הרשאות מערכת שרק adb יכול להעניק, ולכן ההתקנה נעשית
  פעם אחת מהמחשב (כ-10 דקות).
- האפליקציה היא כלי עזר טכני בלבד. **שאלות הלכתיות על שימוש בשעון חכם בשבת - לרב שלך.**
- אין איסוף מידע, אין חיבור לרשת, אין פרסומות. הקוד כולו כאן.

## מה האפליקציה עושה בהפעלה

1. שומרת את הערכים הנוכחיים של ההגדרות שהיא עומדת לשנות
2. מפעילה "נא לא להפריע" מלא (אין צלילים, רטט או התראות)
3. מכבה "הרם יד להערה" ו"גע להערה" ואת הבהירות האוטומטית, ומדליקה Always-On Display
4. מאריכה את "הצגת האפליקציה האחרונה" לכל משך השבת, כדי שהשעון לא יחזור לפני-השעון הרגילים
5. מתזמנת כיבוי אוטומטי (26 / 50 / 74 שעות לפי הבחירה)
6. מפעילה שירות רקע שמחזיר את מסך השעון אם נלחץ כפתור פיזי
7. פותחת את מסך השעון (עמום אחרי כמה שניות)

## מה מוצג על המסך

```
      🕯️🕯️
      21:31
  ט״ז תשרי תשפ״ז          ← מתקדם אחרי השקיעה
   פרשת בראשית            ← או שם החג / חול המועד / שבת מיוחדת
   צאת שבת 18:50          ← או "הדלקת נרות 17:45" לפני הכניסה
```

**עיר ומנהג הדלקה** נבחרים במסך הבית של האפליקציה: 23 ערים בישראל, והדלקת נרות 18 / 20 / 22 / 30 / 40
דקות לפני השקיעה (ברירת מחדל: ירושלים 40, חיפה 30, שאר הערים 20). צאת שבת מחושבת לפי צאת הכוכבים 8.5°.
הדלקת הנרות מחושבת מהשקיעה במישור (בגובה פני הים), כמקובל בלוחות. הזמנים נבדקו מול Hebcal ו-chabad.org
לשבתות אוקטובר 2026; ייתכן הפרש של דקה מלוח אחר בגלל עיגול. **לפני שסומכים על הזמנים - להשוות פעם אחת ללוח המקומי.**

## התקנה

### 1. במחשב
- להוריד את קובץ ה-APK ואת `setup.ps1` מעמוד ה-[Releases](../../releases/latest)
- להוריד [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) (מכיל את `adb`)
  ולחלץ את התיקייה `platform-tools` ליד `setup.ps1`

### 2. בשעון
1. הגדרות → אודות השעון → פרטי תוכנה → 7 הקשות על "גרסת תוכנה" (נפתחות "אפשרויות מפתח")
2. אפשרויות מפתח → ADB debugging ✓ → Wireless debugging ✓
3. השעון והמחשב על אותה רשת Wi-Fi
4. Wireless debugging → Pair new device: יופיעו כתובת IP, פורט צימוד וקוד בן 6 ספרות.
   במסך הראשי של Wireless debugging מופיע פורט החיבור (שונה מפורט הצימוד)

### 3. הרצה (Windows, PowerShell)
```powershell
.\setup.ps1 -Apk .\shabbat-watch-0.3.0.apk -Ip <IP> -Port <פורט חיבור> -PairPort <פורט צימוד> -Code <קוד>
```
הסקריפט מצמיד, מתקין ומעניק את ההרשאות. בסיום האפליקציה נפתחת בשעון; אם אין אזהרה אדומה במסך הבית - הכל הוענק.

אם PowerShell חוסם הרצת סקריפטים: `powershell -ExecutionPolicy Bypass -File .\setup.ps1 ...`

### Mac / Linux (או ידנית)
```bash
adb pair <IP>:<PAIR_PORT> <CODE>
adb connect <IP>:<PORT>
adb install -r shabbat-watch-0.3.0.apk
adb shell pm grant il.hiya.shabbatwatch android.permission.WRITE_SECURE_SETTINGS
adb shell cmd notification allow_dnd il.hiya.shabbatwatch
adb shell appops set il.hiya.shabbatwatch SYSTEM_ALERT_WINDOW allow
adb shell appops set il.hiya.shabbatwatch WRITE_SETTINGS allow
adb shell pm grant il.hiya.shabbatwatch android.permission.POST_NOTIFICATIONS
adb shell dumpsys deviceidle whitelist +il.hiya.shabbatwatch
```

אחרי ההתקנה אפשר לכבות את Wireless debugging. ההרשאות נשמרות גם אחרי עדכון גרסה.

## שימוש

1. לפתוח "מצב שבת" בשעון, לבחור עיר ומנהג הדלקה (פעם אחת)
2. בערב שבת: לבחור משך (שבת / שבת + חג / שלושה ימים) → "הפעל מצב שבת"
3. במוצאי שבת: לפתוח את האפליקציה → "כבה מצב שבת". אם לא כיבית - הכיבוי האוטומטי ישחזר הכל בתום המשך

## הסרה

**לכבות את מצב שבת לפני ההסרה** - אחרת ההגדרות שהאפליקציה שינתה לא ישוחזרו. אחר כך להסיר כרגיל מהשעון,
או `adb uninstall il.hiya.shabbatwatch`.

## מגבלות ידועות

- **צעדים ודופק** ממשיכים להימדד ברקע (Samsung Health). אין ממשק לאפליקציה צד-שלישית לעצור אותם.
  אפשרות ידנית: Samsung Health → הגדרות → דופק "ידני בלבד", סטרס כבוי, זיהוי אימון כבוי.
- **Bluetooth** נשאר דלוק. "נא לא להפריע" חוסם את ההתראות; מצב טיסה ידני אפשרי לפי בחירה.
- **כפתור פיזי** מעיר את המסך למצב בהיר לכמה שניות (ואז חוזר לעמום). לחיצת Home יוצאת מהשעון
  והשירות מחזיר אותו תוך כמה שניות.
- **כשהשעון לא על היד** המערכת מכבה את התצוגה הקבועה (התנהגות של Wear OS).
- **אתחול השעון באמצע שבת** לא נבדק.
- ערים מחוץ לישראל אינן נתמכות כרגע.

## בנייה מהקוד

דרישות: JDK 17, Android SDK (`platform-tools`, `platforms;android-34`, `build-tools;34.0.0`).
```powershell
.\scripts\watch-connect.ps1 -Ip <IP> -Port <PORT> -PairPort <PAIR_PORT> -Code <CODE>
.\scripts\install.ps1        # בונה debug ומתקין
.\scripts\watch-grant.ps1    # הרשאות חד-פעמיות
```
לבניית release חתום: להעתיק את `keystore.properties.example` ל-`keystore.properties` ולמלא.

אם ההגדרות לא משתנות בשעון שלך, `scripts\discover-settings.ps1` מגלה אילו מפתחות משתנים כשמזיזים מתג
בהגדרות השעון; את המפתח מוסיפים ל-`Constants.MANAGED_SETTINGS`.

מבנה: `clock/` מסך השעון ולוח עברי · `mode/` הפעלה/כיבוי, הגדרות מערכת, DND, שירות שומר · `ui/` מסך הבית ובחירת עיר.

## רישיון

[MIT](LICENSE). חישוב הזמנים באמצעות [KosherJava Zmanim](https://github.com/KosherJava/zmanim) (LGPL 2.1).
