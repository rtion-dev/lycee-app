# ثانوية عبد الله كنون — التطبيق وموقع الإدارة

تطبيق Android للتلاميذ وموقع ويب للإدارة، مرتبطان بخدمات Firebase لتسجيل الدخول والبيانات والإشعارات.

**المطور:** RTION — Otman Elabouze · **إصدار التطبيق في المصدر:** 2.0 (6)

## مكونات المشروع

| الجزء | الوظيفة | التقنيات |
|---|---|---|
| [تطبيق Android](application/AbdallahGuennoun/) | حساب التلميذ، الأخبار، التوجيه، استعمال الزمن وطلب الوثائق | Kotlin، Jetpack Compose، XML |
| [موقع الإدارة](administration/Admin-Site-Web/) | مراجعة التسجيلات، إدارة الأقسام والمحتوى والوثائق | HTML، CSS، JavaScript |
| [خدمات Firebase](application/AbdallahGuennoun/firebase/) | قواعد البيانات والصلاحيات ووظائف إرسال الإشعارات | Firestore Rules، JavaScript، Node.js |

## تقسيم الملفات

~~~text
GitHub/
├── application/
│   └── AbdallahGuennoun/
│       ├── app/                 كود Android والواجهات والصور
│       ├── firebase/            القواعد والفهارس والوظائف والاختبارات
│       ├── gradle/              إعدادات المكتبات وGradle Wrapper
│       ├── gradlew / gradlew.bat
│       └── ملفات إعداد البناء
├── administration/
│   └── Admin-Site-Web/
│       ├── public/              صفحات الإدارة والسكربتات والتصميم
│       ├── firebase.json        إعداد Firebase Hosting
│       └── .firebaserc          ربط مشروع Firebase
├── docs/
├── README.md
├── FILE_INDEX.md
└── .gitignore
~~~

تم الاحتفاظ ببنية مشروع Android الداخلية، وتغيير اسم مجلد موقع الإدارة في هذه النسخة فقط. ملفات التطبيق والموقع المنسوخة مطابقة للأصل دون تعديل محتواها.

## كيف يعمل؟

1. يسجل التلميذ الدخول بحساب Google عبر Firebase Authentication ويكمل معلوماته.
2. تراجع الإدارة التسجيل وتدير الأقسام والمحتوى والطلبات من موقع الويب.
3. يقرأ التطبيق البيانات من Firestore حسب صلاحيات الحساب وقواعد الوصول.
4. يرسل التلميذ طلب الوثيقة ويتابع حالته، ثم يستلم الوثيقة من المؤسسة.
5. وظائف الخادم الموجودة في المصدر ترسل تنبيهات FCM عند نشر المحتوى، بعد إعدادها ونشرها.

التطبيق وموقع الإدارة يتصلان بخدمات Firebase؛ الموقع ليس خادماً محلياً يجب تشغيله على هاتف التلميذ. رفع الصور يعتمد أيضاً على خدمة خارجية غير مرفقة.

## البدء

### التطبيق

افتح المجلد التالي في Android Studio:

~~~text
application/AbdallahGuennoun
~~~

دع Android Studio يحدد مسار Android SDK وينزّل التبعيات. ثم من جذر المستودع في PowerShell:

~~~powershell
cd application/AbdallahGuennoun
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
~~~

راجع [دليل الإعداد](docs/SETUP.md) للإصدارات المحددة وإعداد Firebase. نواتج APK تظهر داخل app/build/outputs/apk. ملف APK المحفوظ من المشروع الأصلي موزع منفصلاً عن مجلد المصدر.

### موقع الإدارة

من جذر المستودع، إذا كان Python مثبتاً:

~~~powershell
cd administration/Admin-Site-Web
python -m http.server 8080 --directory public
~~~

افتح http://localhost:8080. الموقع ثابت ولا يحتاج npm build. تسجيل الدخول والبيانات يحتاجان إعداد Firebase وحساباً مخولاً.

## دليل القراءة

- [شرح البنية ودور الملفات](docs/ARCHITECTURE.md)
- [متطلبات التشغيل والبناء والنشر](docs/SETUP.md)
- [الخدمات الخارجية وحدود الحزمة](docs/EXTERNAL-SERVICES.md)
- [ما تم التحقق منه](docs/VERIFICATION.md)
- [فهرس كل الملفات](FILE_INDEX.md)

## ما تتضمنه النسخة

كود المصدر، الصور والفيديوهات اللازمة، الاختبارات، إعدادات البناء، Gradle Wrapper وملف قفل مكتبات وظائف الخادم. المكتبات المثبتة والكاش ومخرجات البناء والنسخ القديمة والإعدادات المحلية مستثناة؛ يعاد تنزيل التبعيات عند تجهيز بيئة التطوير.

إعدادات Firebase الخاصة بالعميل محفوظة كما كانت. الحزمة لا تنسخ الحسابات أو بيانات التلاميذ السحابية، ولا توفر خادم رفع الصور أو مفاتيح التوقيع. حالة الخدمات المنشورة لم تُفحص أثناء ترتيب الملفات.

## الحقوق

لم يُضف ترخيص جديد لهذا المشروع. راجع [إشعارات الطرف الثالث](application/AbdallahGuennoun/THIRD_PARTY_NOTICES.md)، وارجع إلى مالك المشروع بخصوص إعادة الاستخدام والنشر.
