# شرح المشروع والملفات

## تطبيق التلميذ

المسار: application/AbdallahGuennoun. كود Kotlin موجود داخل app/src/main/java/com/otmanelabouze/abdallahguennoun.

| الملف أو المجلد | الدور |
|---|---|
| MainActivity.kt | نقطة بدء التطبيق والتنقل الأولي والتعامل مع فتح الإشعارات |
| AuthManager.kt / AuthScreens.kt | المصادقة وواجهات الدخول |
| ProfileScreens.kt / ProfilePhotoStore.kt | بيانات الحساب وصورة التلميذ |
| StudentPortal.kt / CampusDashboard.kt | بوابة التلميذ والصفحة الرئيسية |
| DocumentRequests.kt | تقديم طلبات الوثائق وتتبعها |
| MediaScreens.kt | عرض المحتوى والوسائط |
| SchoolWebScreen.kt / SchoolWebPolicy.kt | المتصفح الداخلي وسياسة الروابط |
| SchoolNotifications.kt | استقبال الإشعارات والأذونات |
| AppSettings.kt / SchoolDesign.kt | الإعدادات والمظهر المشترك |
| ExamSchedule.kt / ExamCountdownCard.kt | تواريخ الامتحانات وعرض العد التنازلي |
| app/src/main/res | الصور والفيديو والأيقونات والنصوص وملفات XML |
| app/src/main/AndroidManifest.xml | تعريف التطبيق والشاشات والخدمات والأذونات |
| app/src/test / app/src/androidTest | اختبارات وحدة واختبارات تحتاج جهاز Android |
| app/build.gradle.kts | إصدارات التطبيق وSDK وتبعيات الوحدة |
| gradle/libs.versions.toml | فهرس إصدارات المكتبات والإضافات |
| gradle/wrapper / gradlew / gradlew.bat | تشغيل Gradle بالإصدار المحدد للمشروع |

## موقع الإدارة

المسار: administration/Admin-Site-Web. المجلد public هو جذر ملفات الموقع المنشورة.

| الملفات | الدور |
|---|---|
| index.html / app.js | الصفحة الرئيسية ومراجعة التسجيلات |
| workspace.js | إدارة الأقسام وبيانات الالتحاق |
| documents.html / documents.js / document-model.js | أنواع الوثائق وطلباتها ومعالجتها |
| media.html / media.js | الأخبار والتوجيه واستعمال الزمن |
| firebase-config.js | تهيئة اتصال الويب بـFirebase |
| appearance.js / ملفات CSS | المظهر والتنسيق |
| admin-navigation.js / admin-navigation.css | التنقل بين صفحات الإدارة |
| firebase.json / .firebaserc | إعداد الاستضافة ومعرّف مشروع Firebase |

## البيانات والخادم

داخل application/AbdallahGuennoun/firebase:

- firestore.rules: قواعد الوصول والتحقق من البيانات.
- firestore.indexes.json: فهارس استعلامات Firestore.
- functions/index.js: ربط أحداث النشر بوظائف إرسال الإشعارات.
- functions/publication.js: منطق تحديد أحداث النشر.
- functions/package.json وpnpm-lock.yaml: تعريف المكتبات وتثبيت إصداراتها.
- tests: اختبارات قواعد Firestore باستخدام المحاكي.

المجموعات الأساسية تشمل users للتلاميذ، roles للأدوار، classes للأقسام، news وguidance وtimetables للمحتوى، وdocumentTypes وdocumentRequests للوثائق. قواعد Firestore هي المرجع للصلاحيات؛ إخفاء زر في الواجهة لا يمنح أو يمنع الوصول وحده.

دورة الوثيقة: تقديم الطلب، تحضيره، جاهزيته، ثم تسليمه؛ الرفض متاح وفق القواعد. هذه الخدمة لتتبع طلب إداري، أما PDF استعمال الزمن فهو محتوى منفصل.

راجع [الفهرس الكامل](../FILE_INDEX.md) لكل ملف على حدة.
