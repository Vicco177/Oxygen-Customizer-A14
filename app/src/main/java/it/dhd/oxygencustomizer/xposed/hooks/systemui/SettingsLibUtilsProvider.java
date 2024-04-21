package it.dhd.oxygencustomizer.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.dhd.oxygencustomizer.utils.Constants;
import it.dhd.oxygencustomizer.xposed.XPLauncher;
import it.dhd.oxygencustomizer.xposed.XposedMods;

public class SettingsLibUtilsProvider extends XposedMods {
    private static final String listenPackage = Constants.Packages.SYSTEM_UI;
    private static Class<?> UtilsClass = null;
    private static Class<?> AbsSettingsValueProxy = null;
    private static Class<?> BrightnessUtils = null;
    private static Class<?> CoUIColors = null;

    public static ColorStateList getColorAttr(int resID, Context context) {
        if (UtilsClass == null) return null;

        return (ColorStateList) callStaticMethod(UtilsClass, "getColorAttr", context, resID);
    }

    public static int getColorStateListDefaultColor(Context context, int resID) {
        if (UtilsClass == null) return 0;

        return (int) callStaticMethod(UtilsClass, "getColorStateListDefaultColor", context, resID);
    }

    public static int getColorAttrDefaultColor(Context context, int resID) {
        if (UtilsClass == null) return 0;

        try {
            return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", context, resID, 0);
        } catch (Throwable ignored) { //13 QPR1
            return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", resID, context);
        }
    }

    public static int getColorAttrDefaultColor(int resID, Context context) {
        if (UtilsClass == null) return 0;

        try {
            return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", resID, context);
        } catch (Throwable throwable) {
            try {
                return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", context, resID);
            } catch (Throwable throwable1) {
                return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", context, resID, 0);
            }
        }
    }

    public static int getSecureIntValue(Context context, String str, int i) {
        if (AbsSettingsValueProxy == null) return i;

        return (int) callStaticMethod(AbsSettingsValueProxy, "getSecureIntValue", context, str, i);
    }

    public static float convertGammaToLinearFloat(int i, float f, float f2) {
        if (BrightnessUtils == null) return 0f;

        return (float) callStaticMethod(BrightnessUtils, "convertGammaToLinearFloat", i, f, f2);
    }

    public static int getGammaMax() {
        if (BrightnessUtils == null) return 0;

        return getStaticIntField(BrightnessUtils, "GAMMA_SPACE_MAX");
    }

    public static int getThemeAttr(Context context, int attr) {
        return getThemeAttr(context, attr, 0);
    }

    public static int getThemeAttr(Context context, int attr, int defaultValue) {
        if (UtilsClass == null) return 0;
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int theme = ta.getResourceId(0, defaultValue);
        ta.recycle();
        return theme;
    }

    public SettingsLibUtilsProvider(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
        AbsSettingsValueProxy = findClass("com.oplusos.systemui.common.settingsvalue.AbsSettingsValueProxy", lpparam.classLoader);
        BrightnessUtils = findClass("com.android.settingslib.display.BrightnessUtils", lpparam.classLoader);
        CoUIColors = findClass("com.coui.appcompat.contextutil.COUIContextUtil", lpparam.classLoader);
    }
}
