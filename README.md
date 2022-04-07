modify-string-res-key-plugin
===
A library that can dynamically modify the library's string resource key during Android build time.

Android's library module may contain a large number of string resources, for example:

```xml
<!--xxlibrary/src/main/res/values/strings.xml-->
<resources>
    <string name="app_name">My Application</string>
    <string name="app_toast_library">library_toast</string>
    <string name="name">name</string>
    <string>...</string>
</resources>

```

Therefore, we can refer to the string in the xml file through @string/XXX, or use R.string.XXX in the java or kotlin code to get the id of the string.
However, since different modules may have strings with the same key, the strings in the module may be overwritten.
The plugin can modify the key of the string according to the specified rules when android builds the library. For example, the login module can add the prefix "login_" to the key of the string.

For example, there is a `<string name="name">name</string>` in the login module, and we can add a login prefix to it through the rules made, then the string will become `<string name during the android build process ="login_name">`name</string>, where the string is quoted will also become @string/login_name and R.string.login_name.

Download
--------

```groovy
// In build.gradle of project
repositories {
    mavenCentral()
}
dependencies {
    implementation 'io.github.brooks0129:modify-string-res-key-plugin:1.0.0'
}
```

```groovy
// In build.gradle of android library (not app)
apply plugin: "modify-string-res-key"
modify_string_key {
    // {original} represents the original string
    transform = "rule_{original}"
    // Strings in allowlist will not be converted
    allowlist = ["XX","XXX"]
}
```

Requires attention:
1. Do not apply the plugin to the app module.
2. There will be three-party dependent libraries in the source code library, and the built-in strings in them will not be converted (only the strings in the library source code will be converted).



License
=======

    Copyright 2022 Sander Lee

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
