package it.dhd.oxygencustomizer.customprefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import it.dhd.oxygencustomizer.R;

public class TopIntroPreference extends Preference {

    public TopIntroPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.top_intro_preference);
        setSelectable(false);
    }

    public TopIntroPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.top_intro_preference);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
    }
}