# الإعداد والتشغيل

الأوامر أدناه تعليمات للمطور ولم تُنفذ على الخدمات السحابية أثناء تنظيم النسخة. ابدأ كل مجموعة أوامر من جذر المستودع ما لم يذكر غير ذلك.

## Android

هذه القيم مأخوذة من ملفات المشروع المحفوظة، وليست اقتراحاً للترقية:

| الإعداد | القيمة |
|---|---|
| Gradle | 9.7.1 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.10 |
| Gradle daemon JVM | 25 |
| Java source/target | 17 |
| compileSdk | 37، minor API 1 |
| targetSdk / minSdk | 37 / 26 |
| ABI | arm64-v8a |
| الإصدار | 2.0، versionCode 6 |

افتح application/AbdallahGuennoun في Android Studio وثبّت SDK المطابق. ملف local.properties خاص بجهازك؛ ينشئه Android Studio، أو تحدد فيه sdk.dir بمسار SDK المحلي. Gradle Wrapper وملفات تعريف المكتبات موجودة؛ الكاش والمكتبات المثبتة ليست مرفقة.

~~~powershell
cd application/AbdallahGuennoun
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
~~~

على Linux/macOS امنح gradlew إذن التنفيذ ثم شغّل ./gradlew. اختبارات الجهاز تستخدم :app:connectedDebugAndroidTest وتحتاج جهازاً أو محاكياً مناسباً. نسخة التوزيع النهائية تحتاج توقيع المالك؛ APK المحفوظ من مجلد debug لا يُقدّم هنا باعتباره إصدار release.

## Firebase

إعداد العميل محفوظ في application/AbdallahGuennoun/app/google-services.json وadministration/Admin-Site-Web/public/firebase-config.js، وربط Hosting في .firebaserc. عند نقل المشروع إلى Firebase آخر حدّث هذه الإعدادات معاً.

جهّز Google Authentication والنطاقات المسموح بها وبصمات شهادة Android. حساب الإدارة يحتاج دوراً مخولاً في roles وفق firestore.rules. اضبط الأقسام والموسم وبيانات المؤسسة في المشروع المناسب. بيانات السحابة لا تأتي مع ملفات المصدر.

## معاينة موقع الإدارة

~~~powershell
cd administration/Admin-Site-Web
python -m http.server 8080 --directory public
~~~

افتح http://localhost:8080. تحتاج خدمات تسجيل الدخول والبيانات اتصالاً وإعداد النطاق المناسب. لا يلزم تثبيت مكتبات npm للموقع الثابت.

## نشر الاستضافة والقواعد

بعد تثبيت Firebase CLI وتسجيل الدخول للحساب المخول، استبدل YOUR_FIREBASE_PROJECT بمعرّف المشروع المقصود:

~~~powershell
cd administration/Admin-Site-Web
firebase deploy --only hosting --project YOUR_FIREBASE_PROJECT
~~~

وللقواعد والفهارس، ابدأ من جذر المستودع:

~~~powershell
cd application/AbdallahGuennoun
firebase deploy --only firestore --project YOUR_FIREBASE_PROJECT
~~~

## وظائف الإشعارات والاختبارات

وظائف الخادم تحدد Node.js 22. باستخدام pnpm المثبت في بيئة التطوير:

~~~powershell
cd application/AbdallahGuennoun/firebase/functions
pnpm install --frozen-lockfile
pnpm test
~~~

نشر الوظائف بعد تجهيز مشروع Firebase والخطة المناسبة، من application/AbdallahGuennoun:

~~~powershell
firebase deploy --only functions:school-notifications --project YOUR_FIREBASE_PROJECT
~~~

اختبارات القواعد، من جذر المستودع:

~~~powershell
cd application/AbdallahGuennoun/firebase/tests
npm install
npm test
~~~

هذه الاختبارات مهيأة لمشروع المحاكي demo-rtion-documents. نجاح اختبار منطق الإشعارات لا يثبت وصول Push إلى الهاتف؛ ذلك يحتاج نشر الوظائف واختباراً فعلياً.
