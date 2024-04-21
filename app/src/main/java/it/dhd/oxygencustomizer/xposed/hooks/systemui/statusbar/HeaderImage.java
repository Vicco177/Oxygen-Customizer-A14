package it.dhd.oxygencustomizer.xposed.hooks.systemui.statusbar;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static it.dhd.oxygencustomizer.utils.Constants.Preferences.QsHeaderImage.*;
import static it.dhd.oxygencustomizer.xposed.XPrefs.Xprefs;
import static it.dhd.oxygencustomizer.xposed.hooks.systemui.OpUtils.getPrimaryColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.dhd.oxygencustomizer.BuildConfig;
import it.dhd.oxygencustomizer.utils.Constants;
import it.dhd.oxygencustomizer.xposed.ResourceManager;
import it.dhd.oxygencustomizer.xposed.XposedMods;
import it.dhd.oxygencustomizer.xposed.hooks.systemui.SettingsLibUtilsProvider;

public class HeaderImage extends XposedMods {

    private static final String listenPackage = Constants.Packages.SYSTEM_UI;
    private final String TAG = "Oxygen Customizer " + this.getClass().getSimpleName() + ": ";

    private final int MAX_TINT_OPACITY = 250;

    // QS Header Image
    private FadingEdgeLayout mQsHeaderLayout = null;
    private ImageView mQsHeaderImageView = null;
    private int qshiValue;
    private boolean qshiEnabled;
    private boolean qshiLandscapeEnabled;
    private boolean qshiZoomToFit;
    private int qshiHeightPortrait;
    private int qshiAlpha;
    private int qshiTint;
    private int qshiTintCustom;
    private int qshiPaddingSide;
    private int qshiPaddingTop;
    private int qshiMinHeight = 50;
    private int qshiDefaultHeight = 200;
    private int qshiTintIntensity = 50;

    private int mColorAccent;
    private int mColorTextPrimary;
    private int mColorTextPrimaryInverse;
    private int bottomFadeAmount = 0;

    public HeaderImage(Context context) {
        super(context);
    }


    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;

        qshiEnabled = Xprefs.getBoolean(QS_HEADER_IMAGE_ENABLED, false);
        qshiValue = Xprefs.getInt(QS_HEADER_IMAGE_VALUE, -1);
        qshiLandscapeEnabled = Xprefs.getBoolean(QS_HEADER_IMAGE_LANDSCAPE_ENABLED, false);
        qshiAlpha = Xprefs.getSliderInt(QS_HEADER_IMAGE_ALPHA, 255);
        qshiTint = Integer.parseInt(Xprefs.getString(QS_HEADER_IMAGE_TINT, "0"));
        qshiTintCustom = Xprefs.getInt(QS_HEADER_IMAGE_TINT_CUSTOM, 0XFFFFFFFF);
        qshiTintIntensity = Xprefs.getSliderInt(QS_HEADER_IMAGE_TINT_INTENSITY, 50);
        qshiHeightPortrait = Xprefs.getSliderInt(QS_HEADER_IMAGE_HEIGHT_PORTRAIT, qshiDefaultHeight);
        qshiZoomToFit = Xprefs.getBoolean(QS_HEADER_IMAGE_ZOOM_TO_FIT, false);
        qshiPaddingSide = Xprefs.getSliderInt(QS_HEADER_IMAGE_PADDING_SIDE, 0);
        qshiPaddingTop = Xprefs.getSliderInt(QS_HEADER_IMAGE_PADDING_TOP, -16);
        bottomFadeAmount = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                Xprefs.getSliderInt(QS_HEADER_IMAGE_BOTTOM_FADE, 40),
                mContext.getResources().getDisplayMetrics()
        );

        if (Key.length > 0) {
            for (String qsPref : QS_PREFS) {
                if (Key[0].equals(qsPref)) {
                    updateQSHeaderImage();
                }
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> OplusQSContainerImpl = findClass("com.oplus.systemui.qs.OplusQSContainerImpl", lpparam.classLoader);;
        Class<?> QuickStatusBarHeader = findClass("com.oplus.systemui.qs.OplusQuickStatusBarHeader", lpparam.classLoader);;

        try {
            log(TAG + "Hooking");
            hookAllMethods(OplusQSContainerImpl, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    log(TAG + "onFinishInflate");
                    FrameLayout mQuickStatusBarHeader = (FrameLayout) param.thisObject;
                    mQuickStatusBarHeader.setBackgroundColor(Color.TRANSPARENT);

                    mQsHeaderLayout = new FadingEdgeLayout(mContext);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiHeightPortrait, mContext.getResources().getDisplayMetrics()));
                    layoutParams.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingSide, mContext.getResources().getDisplayMetrics());
                    layoutParams.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingSide, mContext.getResources().getDisplayMetrics());
                    layoutParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingTop, mContext.getResources().getDisplayMetrics());

                    mQsHeaderLayout.setLayoutParams(layoutParams);
                    mQsHeaderLayout.setVisibility(View.GONE);

                    mQsHeaderImageView = new ImageView(mContext);
                    mQsHeaderImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mQsHeaderLayout.addView(mQsHeaderImageView);

                    mQuickStatusBarHeader.addView(mQsHeaderLayout, 0);

                    updateQSHeaderImage();

                }
            });

            hookAllMethods(OplusQSContainerImpl, "updateResources", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    updateQSHeaderImage();
                }
            });

            hookAllMethods(QuickStatusBarHeader, "onMeasure", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    View mDatePrivacyView = (View) getObjectField(param.thisObject, "mPrivacyContainer");
                    int mTopViewMeasureHeight = getIntField(param.thisObject, "mTopViewMeasureHeight");

                    if ((int) callMethod(mDatePrivacyView, "getMeasuredHeight") != mTopViewMeasureHeight) {
                        setObjectField(param.thisObject, "mTopViewMeasureHeight", callMethod(mDatePrivacyView, "getMeasuredHeight"));
                        callMethod(param.thisObject, "updateAnimators");
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName);
    }

    private void updateQSHeaderImage() {
        if (mQsHeaderLayout == null || mQsHeaderImageView == null) {
            return;
        }

        if (!qshiEnabled) {
            mQsHeaderLayout.setVisibility(View.GONE);
            return;
        }

        loadImageOrGif(mQsHeaderImageView);

        // Alpha
        mQsHeaderImageView.setImageAlpha(qshiAlpha);

        // Tint
        mColorAccent = getPrimaryColor(mContext);
        mColorTextPrimary = SettingsLibUtilsProvider.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorPrimary);
        mColorTextPrimaryInverse = SettingsLibUtilsProvider.getColorAttrDefaultColor(
                mContext, android.R.attr.textColorPrimaryInverse);


        ViewGroup.MarginLayoutParams qshiParams = (ViewGroup.MarginLayoutParams) mQsHeaderLayout.getLayoutParams();
        //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiHeightPortrait, mContext.getResources().getDisplayMetrics()));
        qshiParams.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingSide, mContext.getResources().getDisplayMetrics());
        qshiParams.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingSide, mContext.getResources().getDisplayMetrics());
        qshiParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiPaddingTop, mContext.getResources().getDisplayMetrics());
        qshiParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, qshiHeightPortrait, mContext.getResources().getDisplayMetrics());
        mQsHeaderLayout.setLayoutParams(qshiParams);
        mQsHeaderLayout.requestLayout();

        Configuration config = mContext.getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE && !qshiLandscapeEnabled) {
            mQsHeaderLayout.setVisibility(View.GONE);
        } else {
            mQsHeaderLayout.setVisibility(View.VISIBLE);
        }

        mQsHeaderLayout.setFadeEdges(false, false, bottomFadeAmount != 0, false);
        mQsHeaderLayout.setFadeSizes(0, 0, bottomFadeAmount, 0);

    }

    private void addOrRemoveProperty(View view, int property, boolean flag) {
        Object vParam = view.getLayoutParams();
        if (vParam instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) vParam;
            if (flag) {
                layoutParams.addRule(property);
            } else {
                layoutParams.removeRule(property);
            }
            view.setLayoutParams(layoutParams);
        } else if (vParam instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) vParam;
            if (flag) {
                layoutParams.gravity = property;
            } else {
                layoutParams.gravity = Gravity.NO_GRAVITY;
            }
            view.setLayoutParams(layoutParams);
        }
    }

    private void loadImageOrGif(ImageView iv) {
        AtomicBoolean applyTint = new AtomicBoolean(false);
        int tintColor;
        if (qshiTint == 0) {
            tintColor = -1;
            mQsHeaderImageView.setColorFilter(null);
        } else if (qshiTint == 1) {
            tintColor = mColorAccent;
        } else if (qshiTint == 2) {
            tintColor = mColorTextPrimary;
        } else if (qshiTint == 3) {
            tintColor = mColorTextPrimaryInverse;
        } else if (qshiTint == 4) {
            // validate color and limit custom tint opacity to MAX_TINT_OPACITY
            tintColor = getValidCustomTint(qshiTintCustom);
        } else {
            tintColor = -1;
        }
        if (qshiValue != -1) {
            @SuppressLint("DiscouragedApi") int resId = ResourceManager.modRes.getIdentifier("qs_header_image_" + qshiValue,"drawable", BuildConfig.APPLICATION_ID);
            Drawable drw = ResourcesCompat.getDrawable(ResourceManager.modRes,
                    resId,
                    mContext.getTheme());
            iv.post(()->loadImageMain(iv, drw));
            applyTint.set(true);
        } else {
            try {
                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(() -> {
                    File Android = new File(Environment.getExternalStorageDirectory() + "/Android");

                    if (Android.isDirectory()) {
                        try {
                            ImageDecoder.Source source = ImageDecoder.createSource(new File(Environment.getExternalStorageDirectory() + "/.oxygen_customizer/header_image.png"));

                            Drawable drawable = ImageDecoder.decodeDrawable(source);
                            iv.post(() -> loadImageMain(iv, drawable));

                            if (drawable instanceof AnimatedImageDrawable) {
                                if (tintColor != -1) {
                                    int fadeFilter = ColorUtils.blendARGB(Color.TRANSPARENT, tintColor, qshiTintIntensity / 100f);
                                    ColorFilter colorFilter = new PorterDuffColorFilter(fadeFilter, PorterDuff.Mode.SRC_ATOP);
                                    drawable.setColorFilter(colorFilter);
                                } else {
                                    drawable.setColorFilter(null);
                                }
                                ((AnimatedImageDrawable) drawable).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                                ((AnimatedImageDrawable) drawable).start();
                                applyTint.set(false);
                            } else {
                                applyTint.set(true);
                            }
                        } catch (Throwable ignored) {}

                        executor.shutdown();
                        executor.shutdownNow();
                    }
                }, 0, 5, TimeUnit.SECONDS);

            } catch (Throwable ignored) {
            }
            if (applyTint.get()) {
                if (tintColor != -1) {
                    int fadeFilter = ColorUtils.blendARGB(Color.TRANSPARENT, tintColor, qshiTintIntensity / 100f);
                    mQsHeaderImageView.setColorFilter(fadeFilter, PorterDuff.Mode.SRC_ATOP);
                }
            } else {
                mQsHeaderImageView.setColorFilter(null);
            }
        }
        if (!qshiZoomToFit) {
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setAdjustViewBounds(false);
            iv.setCropToPadding(false);
        }
    }

    private void loadImageMain(ImageView iv, Drawable drawable) {
        iv.setImageDrawable(drawable);
    }

    private int getValidCustomTint(int customTint) {
        int alpha = Color.alpha(customTint);
        int red = Color.red(customTint);
        int blue = Color.blue(customTint);
        int green = Color.green(customTint);

        if (alpha < 0 || red < 0 || red > 255 || blue < 0 || blue > 255
                || green < 0 || green > 255) {
            return -1;
        }

        //limit tint opacity level (alpha <= MAX_TINT_OPACITY)
        if (alpha > MAX_TINT_OPACITY) {
            alpha = MAX_TINT_OPACITY;
            return Color.argb(alpha, red, green, blue);
        }
        else {
            return customTint;
        }
    }
}
