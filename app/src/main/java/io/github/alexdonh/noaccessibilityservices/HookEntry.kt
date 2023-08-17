package io.github.alexdonh.noaccessibilityservices

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.os.Build
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.registerModuleAppActivities
import com.highcapable.yukihookapi.hook.type.android.ContentResolverClass
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.java.*
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }

    override fun onHook() = encase {
        loadZygote {
            onAppLifecycle {
                onCreate { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) registerModuleAppActivities() }
            }

            // https://android.googlesource.com/platform/frameworks/base/+/master/packages/SettingsLib/src/com/android/settingslib/accessibility/AccessibilityUtils.java
            findClass("com.android.settingslib.accessibility.AccessibilityUtils").hook {
                injectMember {
                    method {
                        name = "getEnabledServicesFromSettings"
                        param(ContextClass, IntType)
                        returnType = SetClass
                    }
                    replaceTo(emptySet<ComponentName>())
                }
            }.ignoredHookClassNotFoundFailure()

            findClass("android.provider.Settings${'$'}Secure").hook {
                injectMember {
                    method {
                        name = "getStringForUser"
                        param(ContentResolverClass, StringClass, IntType)
                        returnType = StringClass
                    }
                    afterHook {
                        when {
                            args(1).string() == "enabled_accessibility_services" -> {
                                result = ""
                            }
                            args(1).string() == "accessibility_enabled" -> {
                                result = "0"
                            }
                        }
                    }
                }
            }

            findClass("android.view.accessibility.AccessibilityManager").hook {
                injectMember {
                    method {
                        name = "getInstalledAccessibilityServiceList"
                        emptyParam()
                        returnType = ListClass
                    }
                    replaceTo(listOf<AccessibilityServiceInfo>())
                }

                injectMember {
                    method {
                        name = "getEnabledAccessibilityServiceList"
                        param(IntType)
                        returnType = ListClass
                    }
                    replaceTo(listOf<AccessibilityServiceInfo>())
                }

                injectMember {
                    method {
                        name = "isEnabled"
                        emptyParam()
                        returnType = BooleanType
                    }
                    replaceToFalse()
                }
            }
        }
    }
}