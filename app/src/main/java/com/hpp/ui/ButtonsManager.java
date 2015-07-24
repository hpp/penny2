package com.hpp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;

import android.content.Context;
import android.widget.SeekBar;

/**
 * Created by izzy on 4/9/15.
 */
public class ButtonsManager {

    public ImageButton captureButton, audioButton, avButton, particlesButton,
                pyramidButton, cameraButton, rabbitButton;

    SharedPreferences mSharedPrefs;
    SharedPreferences.Editor mPrefsEditor;
    PenelopeMainActivity mMainActivity;
    Boolean particlesOpen = false;

    public ButtonsManager(PenelopeMainActivity mView, SharedPreferences sp){
        mSharedPrefs = sp;
        mPrefsEditor = sp.edit();
        mMainActivity = mView;

        captureButton = (ImageButton) mView.findViewById(R.id.capture_button);

        audioButton = (ImageButton) mView.findViewById(R.id.audioButton);
        setAudioButton(mSharedPrefs.getBoolean("turn_on_output_audio_key", false));
        audioButton.setOnClickListener(buttonClicked);

        avButton = (ImageButton) mView.findViewById(R.id.videoButton);
        setAVButton(mSharedPrefs.getBoolean("turn_on_video_preview_key", true));
        avButton.setOnClickListener(buttonClicked);

        particlesButton = (ImageButton) mView.findViewById(R.id.particlesButton);
        setParticlesButton(particlesOpen);
        particlesButton.setOnClickListener(buttonClicked);

        pyramidButton = (ImageButton) mView.findViewById(R.id.pyramidButton);
        setPyramidButton(mSharedPrefs.getBoolean("turn_on_visualization_key", true));
        pyramidButton.setOnClickListener(buttonClicked);

        cameraButton = (ImageButton) mView.findViewById(R.id.switchCameraButton);

        rabbitButton = (ImageButton) mView.findViewById(R.id.rabbitButton);
        rabbitButton.setOnClickListener(buttonClicked);
    }

    public void setVisibility(int viz){
        audioButton.setVisibility(viz);
        avButton.setVisibility(viz);
        captureButton.setVisibility(viz);
        particlesButton.setVisibility(viz);
        pyramidButton.setVisibility(viz);
        cameraButton.setVisibility(viz);
        rabbitButton.setVisibility(viz);
    }

    View.OnClickListener buttonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ImageButton button = (ImageButton) view;
            if (button == audioButton) {
                Boolean audioOn = !mSharedPrefs.getBoolean("turn_on_output_audio_key", false);
                setAudioButton(audioOn);
                mPrefsEditor.putBoolean("turn_on_output_audio_key", audioOn);
                mPrefsEditor.commit();
            } else if (button == avButton) {
                Boolean videoOn = mMainActivity.toggleCameraMode();
                setAVButton(videoOn);
                mPrefsEditor.putBoolean("turn_on_video_preview_key", videoOn);
                mPrefsEditor.commit();
            } else if (button == particlesButton) {
                setParticlesButton(true);
                showParticlesDialog();
            } else if (button == pyramidButton) {
                Boolean visualizationOn = !mSharedPrefs.getBoolean("turn_on_visualization_key", true);
                setPyramidButton(visualizationOn);
                mPrefsEditor.putBoolean("turn_on_visualization_key", visualizationOn);
                mPrefsEditor.commit();
            } else if (button == rabbitButton) {
                setRabbitButton(true);
                mMainActivity.launchEffectsMenu();
            }
        }

    };

    public void setAudioButton(Boolean val) {
        if (val){
            audioButton.setImageResource(R.drawable.audio_on);
        } else {
            audioButton.setImageResource(R.drawable.audio_off);
        }
    }

    public void setAVButton(Boolean val){
        if (val)
            avButton.setImageResource(R.drawable.av_video);
        else
            avButton.setImageResource(R.drawable.av_audio);
    }

    public void setParticlesButton(Boolean val){
        if (val)
            particlesButton.setImageResource(R.drawable.particles_open_96);
        else
            particlesButton.setImageResource(R.drawable.particles_closed_96);
    }

    public void setPyramidButton(Boolean val){
        if (val)
            pyramidButton.setImageResource(R.drawable.pyramid_open_96);
        else
            pyramidButton.setImageResource(R.drawable.pyramid_closed_96);
    }

    public void setRabbitButton(Boolean val){
        if (val)
            rabbitButton.setImageResource(R.drawable.rabbit_out);
        else
            rabbitButton.setImageResource(R.drawable.rabbit_hiding);
    }

    private Dialog showParticlesDialog(){
        final int numParticles = mSharedPrefs.getInt("vis_number_of_particles_key", 8000);
        final float sizeParticles = mSharedPrefs.getFloat("size_of_particles_key", 1.0f);
        final float opacityParticles = mSharedPrefs.getFloat("opacity_of_particles_key", 1.0f);

        LayoutInflater inflater = (LayoutInflater)mMainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.particles_dialog, (ViewGroup)mMainActivity.findViewById(R.id.particles_dialog_root_element));
        AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity)
                .setView(layout)
                .setTitle(R.string.particles_controls_dialog)
                .setMessage(numParticles + ", " + sizeParticles + "' " + 100.f * opacityParticles)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setParticlesButton(false);
                    }
                });

        final AlertDialog particlesDialog = builder.create();

        particlesDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wmlp = particlesDialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP;

        particlesDialog.show();

        final SeekBar numberParticlesSeekBar = (SeekBar) layout.findViewById(R.id.number_particles_seekbar);
        final SeekBar sizeParticlesSeekBar = (SeekBar) layout.findViewById(R.id.size_particles_seekbar);
        final SeekBar opacityParticlesSeekBar = (SeekBar) layout.findViewById(R.id.opacity_particles_seekbar);


        numberParticlesSeekBar.setProgress(numParticles);
        sizeParticlesSeekBar.setProgress((int)sizeParticles);
        opacityParticlesSeekBar.setProgress((int)(opacityParticles*100.0f));

        SeekBar.OnSeekBarChangeListener particleSeekBarsChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == numberParticlesSeekBar) {
                    particlesDialog.setMessage(progress +
                            ", " + sizeParticles +
                            ", " + opacityParticles);
                    mPrefsEditor.putInt("vis_number_of_particles_key", progress);
                    mPrefsEditor.commit();
                } else if (seekBar == sizeParticlesSeekBar) {
                    particlesDialog.setMessage(numParticles +
                            ", " + progress +
                            ", " + opacityParticles);
                    mPrefsEditor.putFloat("size_of_particles_key", (float) progress);
                    mPrefsEditor.commit();
                } else if (seekBar == opacityParticlesSeekBar) {
                    particlesDialog.setMessage(numParticles +
                            ", " + sizeParticles +
                            ", " + progress);
                    mPrefsEditor.putFloat("opacity_of_particles_key", (float) progress / 100.0f);
                    mPrefsEditor.commit();
                }


                // TODO make particle # interactive
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        numberParticlesSeekBar.setOnSeekBarChangeListener(particleSeekBarsChangeListener);
        sizeParticlesSeekBar.setOnSeekBarChangeListener(particleSeekBarsChangeListener);
        opacityParticlesSeekBar.setOnSeekBarChangeListener(particleSeekBarsChangeListener);

        return particlesDialog;
    }




}
