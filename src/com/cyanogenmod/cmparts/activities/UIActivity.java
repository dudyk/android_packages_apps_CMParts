package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class UIActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	
	/* Preference Screens */
	private static final String STATUS_BAR_SCREEN = "status_bar_settings";
	private static final String DATE_PROVIDER_SCREEN = "date_provider_settings";
	private static final String NOTIFICATION_SCREEN = "notification_settings";
	private static final String EXTRAS_SCREEN = "tweaks_extras";
	
	private PreferenceScreen mStatusBarScreen;
    private PreferenceScreen mDateProviderScreen;
    private PreferenceScreen mNotificationScreen;
    private PreferenceScreen mExtrasScreen;
    
    /* Other */	
    private static final String PINCH_REFLOW_PREF = "pref_pinch_reflow";
    private static final String ROTATION_90_PREF = "pref_rotation_90";
    private static final String ROTATION_180_PREF = "pref_rotation_180";
    private static final String ROTATION_270_PREF = "pref_rotation_270";
    private static final String RENDER_EFFECT_PREF = "pref_render_effect";
    private static final String POWER_PROMPT_PREF = "power_dialog_prompt";
    
    private CheckBoxPreference mPinchReflowPref;
    private CheckBoxPreference mRotation90Pref;
    private CheckBoxPreference mRotation180Pref;
    private CheckBoxPreference mRotation270Pref;

    private CheckBoxPreference mPowerPromptPref;
    private ListPreference mRenderEffectPref;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.ui_title);
        addPreferencesFromResource(R.xml.ui_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        
        /* Preference Screens */
        mStatusBarScreen = (PreferenceScreen) prefSet.findPreference(STATUS_BAR_SCREEN);
        mDateProviderScreen = (PreferenceScreen) prefSet.findPreference(DATE_PROVIDER_SCREEN);
        mNotificationScreen = (PreferenceScreen) prefSet.findPreference(NOTIFICATION_SCREEN);
        mExtrasScreen = (PreferenceScreen) prefSet.findPreference(EXTRAS_SCREEN);
        
        /* Rotation */
        mRotation90Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_90_PREF);
        mRotation180Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_180_PREF);
        mRotation270Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_270_PREF);
        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_MODE, 5);
        mRotation90Pref.setChecked((mode & 1) != 0);
        mRotation180Pref.setChecked((mode & 2) != 0);
        mRotation270Pref.setChecked((mode & 4) != 0);

        /* Pinch reflow */
        mPinchReflowPref = (CheckBoxPreference) prefSet.findPreference(PINCH_REFLOW_PREF);
        mPinchReflowPref.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.WEB_VIEW_PINCH_REFLOW, 0) == 1);
        
        mPowerPromptPref = (CheckBoxPreference) prefSet.findPreference(POWER_PROMPT_PREF);
        mRenderEffectPref = (ListPreference) prefSet.findPreference(RENDER_EFFECT_PREF);
        mRenderEffectPref.setOnPreferenceChangeListener(this);
        updateFlingerOptions();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        
        /* Preference Screens */
        if (preference == mStatusBarScreen) {
        	startActivity(mStatusBarScreen.getIntent());
        }
        if (preference == mDateProviderScreen) {
        	startActivity(mDateProviderScreen.getIntent());
        }
        if (preference == mNotificationScreen) {
        	startActivity(mNotificationScreen.getIntent());
        }
        if (preference == mExtrasScreen) {
        	startActivity(mExtrasScreen.getIntent());
        }
        
        if (preference == mPinchReflowPref) {
            value = mPinchReflowPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.WEB_VIEW_PINCH_REFLOW, value ? 1 : 0);
        }

        if (preference == mPowerPromptPref) {
            value = mPowerPromptPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_DIALOG_PROMPT, value ? 1 : 0);
        }

        if (preference == mRotation90Pref ||
            preference == mRotation180Pref ||
            preference == mRotation270Pref) {
            int mode = 0;
            if (mRotation90Pref.isChecked()) mode |= 1;
            if (mRotation180Pref.isChecked()) mode |= 2;
            if (mRotation270Pref.isChecked()) mode |= 4;
            Settings.System.putInt(getContentResolver(),
                     Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
        }
        return true;
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRenderEffectPref) {
            writeRenderEffect(Integer.valueOf((String)newValue));
            return true;
        }
        return false;
    }
    
    // Taken from DevelopmentSettings
    private void updateFlingerOptions() {
        // magic communication with surface flinger.
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                int v;
                v = reply.readInt();
                // mShowCpuCB.setChecked(v != 0);
                v = reply.readInt();
                // mEnableGLCB.setChecked(v != 0);
                v = reply.readInt();
                // mShowUpdatesCB.setChecked(v != 0);
                v = reply.readInt();
                // mShowBackgroundCB.setChecked(v != 0);

                v = reply.readInt();
                mRenderEffectPref.setValue(String.valueOf(v));

                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }

    }

    private void writeRenderEffect(int id) {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(id);
                flinger.transact(1014, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

}
