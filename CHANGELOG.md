# Changelog

כל שינוי משמעותי לפרויקט מתועד כאן.
פורמט: [Keep a Changelog](https://keepachangelog.com) · גרסאות: [SemVer](https://semver.org).

## [Unreleased]

## [0.3.0] - 2026-09-30

גרסה ראשונה להפצה ציבורית (APK חתום ב-GitHub Releases).

### Added (תוסף)
- בחירת עיר (23 ערים בישראל) ומנהג הדלקת נרות (18/20/22/30/40 דק') במסך הבית - [SettingsScreens](app/src/main/java/il/hiya/shabbatwatch/ui/SettingsScreens.kt), [LocationStore](app/src/main/java/il/hiya/shabbatwatch/mode/LocationStore.kt)
- סקריפט התקנה למשתמש קצה: צימוד, התקנה והרשאות בפקודה אחת - [setup.ps1](scripts/setup.ps1)
- בניית release חתומה מתוך `keystore.properties` - [build.gradle.kts](app/build.gradle.kts)
- רישיון MIT ומדריך התקנה למשתמשים - [README](README.md)

### Changed (שונה)
- אזהרת הרשאות חסרות מפנה למדריך ההתקנה במקום לסקריפט פיתוח - [HomeScreen](app/src/main/java/il/hiya/shabbatwatch/ui/HomeScreen.kt)
- חתימת האפליקציה הוחלפה למפתח release: מעבר מגרסת debug מחייב הסרה והתקנה מחדש (פעם אחת)

## [0.2.1] - 2026-09-27

### Changed (שונה)
- הדלקת נרות 20 דקות לפני השקיעה במקום 30 (בחירת המשתמש) - [Constants](app/src/main/java/il/hiya/shabbatwatch/Constants.kt)

## [0.2.0] - 2026-09-27

### Added (תוסף)
- מסך השעון מציג פרשת השבוע או שם החג (כולל חול המועד ושבתות מיוחדות) - [ShabbatCalendar](app/src/main/java/il/hiya/shabbatwatch/clock/ShabbatCalendar.kt)
- הדלקת נרות לפני הכניסה, וצאת שבת / צאת החג במהלכה (צאת הכוכבים 8.5°), לפי מודיעין - [ShabbatTimes](app/src/main/java/il/hiya/shabbatwatch/clock/ShabbatTimes.kt)
- אמוג'י נרות 🕯️🕯️ בראש המסך, עם אייקון לבן מצויר אם הפונט חסר את הגליף - [ClockScreen](app/src/main/java/il/hiya/shabbatwatch/clock/ClockScreen.kt)
- התאריך העברי מתקדם אחרי השקיעה
- ספריית KosherJava zmanim 2.5.0 (LGPL); זמני מודיעין אומתו מול לוח chabad.org (הדלקה 30 דק' לפני השקיעה)

## [0.1.0] - 2026-09-27

הותקן ונבדק על Galaxy Watch 7 (SM-L310, Wear OS 6 / One UI 8 Watch).

### Added (תוסף)
- מסך בית: בחירת משך (שבת 26 ש' / שבת+חג 50 ש' / שלושה ימים 74 ש') וכפתור הפעלה/כיבוי - [HomeScreen](app/src/main/java/il/hiya/shabbatwatch/ui/HomeScreen.kt)
- מסך שעון לשבת: שעה ותאריך עברי, Ambient עמום קבוע, בולע מגע - [ShabbatClockActivity](app/src/main/java/il/hiya/shabbatwatch/clock/ShabbatClockActivity.kt)
- הפעלה: DND מלא, ביטול הרם-יד / גע-להערה / סיבוב מסגרת / הקשה כפולה, AOD דלוק, שמירת ערכים מקוריים ושחזור בכיבוי - [ShabbatModeController](app/src/main/java/il/hiya/shabbatwatch/mode/ShabbatModeController.kt)
- שירות שומר (Ongoing Activity) שמשאיר את השעון ב-Ambient ומחזיר אותו אם נלחץ כפתור Home - [ShabbatGuardService](app/src/main/java/il/hiya/shabbatwatch/mode/ShabbatGuardService.kt)
- כיבוי אוטומטי בתום המשך שנבחר, וחזרה למצב שבת אחרי אתחול
- לוח עברי ללא תלות חיצונית עם בדיקות יחידה - [HebrewDate](app/src/main/java/il/hiya/shabbatwatch/clock/HebrewDate.kt)
- סקריפטים: חיבור לשעון, מתן הרשאות חד-פעמי, גילוי מפתחות הגדרות, התקנה - [scripts/](scripts/)
- Hook לבדיקה בבניית debug בלבד: הפעלה עם כיבוי אוטומטי בעוד N שניות

### Fixed (תוקן)
- ההפעלה נעצרה באמצע כי הקורוטינה של הממשק בוטלה כשהמצב התחלף - עברה ל-scope בלתי-ניתן לביטול
- Wear OS החזיר לפני-השעון אחרי 20 שניות ב-Ambient - נפתר ע"י Ongoing Activity
- המסך נדלק כל דקה ב-Doze: הוסר `turnScreenOn`, והשומר מפעיל מחדש רק כשהמסך אינטראקטיבי
