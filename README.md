# JARVIS AI - Android 14 (API 34) Complete Project
फोन मॉडल: Redmi 12C (MediaTek Helio G85)
ऑपरेटिंग सिस्टम: Android 14 (MIUI / HyperOS)

नमस्ते! यह आपके JARVIS AI का सम्पूर्ण, 100% कार्यशील Android Kotlin प्रोजेक्ट है।

## फ़ोल्डर संरचना (Folder Structure):
├── .github/workflows/
│   └── build_apk.yml            <-- फ्री क्लाउड APK कम्पाइलर (GitHub Actions)
├── app/
│   ├── build.gradle.kts         <-- App Gradle (SDK 34 / Android 14)
│   └── src/main/
│       ├── AndroidManifest.xml  <-- अनुमतियां (Mic, Internet, Accessibility)
│       ├── java/com/jarvis/ai/
│       │   ├── MainActivity.kt               <-- मुख्य स्क्रीन, हिन्दी स्पीच और AI
│       │   └── JarvisAccessibilityService.kt <-- स्क्रीन स्क्रॉल, टाइपिंग, बैक व लॉक
│       └── res/
│           ├── layout/activity_main.xml      <-- आयरन मैन HUD लेआउट
│           ├── xml/accessibility_service_config.xml
│           ├── drawable/ic_arc_reactor.xml   <-- आर्क रिएक्टर वेक्टर ग्राफिक
│           ├── anim/rotate_arc.xml           <-- रोटेशन एनिमेशन
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── build.gradle.kts             <-- प्रोजेक्ट Gradle
├── settings.gradle.kts          <-- सेटिंग्स व रिपोजिटरी
└── gradle.properties            <-- JVM व AndroidX सेटिंग्स

## केवल फोन से बिना लैपटॉप APK कैसे बनाएं? (Easiest Method):
1. अपने फोन के Chrome ब्राउज़र में https://github.com पर जाएं और फ्री अकाउंट बनाएं।
2. "New Repository" बनाएं (नाम रखें: JarvisAI)।
3. इस ZIP फ़ाइल को अपने फोन में अनज़िप करें और "Upload files" बटन दबाकर सभी फ़ाइलें GitHub पर अपलोड कर दें।
4. अपलोड होते ही GitHub का "Actions" टैब स्वचालित रूप से 2 मिनट में असली "Jarvis-AI-Redmi12C.apk" बनाकर डाउनलोड लिंक दे देगा!
5. APK को अपने Redmi 12C में इनस्टॉल करें।

## Android 14 Accessibility अनुमति कैसे दें? (Redmi 12C):
Settings > Apps > Manage Apps > JARVIS AI > ऊपर 3 बिंदु (⋮) दबाएं > "Allow restricted settings" > फिर Accessibility में जाकर Jarvis को ON करें!
