package com.ahren.google.registration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends Activity {

    @BindView(R.id.gsf_id)
    TextView mGsfId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        findViewById(R.id.go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = RegActivity.getGSFID(MainActivity.this);
                if(id == null || id.isEmpty()){

                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Can't git GSF ID, Please open Google Play first, and click \"SIGN IN\"!")
                            .setNeutralButton("Google Play", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PackageManager manager = MainActivity.this.getPackageManager();
                                    Intent intent = manager.getLaunchIntentForPackage("com.android.vending");
//                                    ComponentName name = new ComponentName("com.google.android.gms", "com.google.android.gms.auth.uiflows.minutemaid.MinuteMaidActivity");
//                                    Intent intent = new Intent();
//                                    intent.setComponent(name);
                                    startActivity(intent);
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    MainActivity.this.finish();
                                }
                            }).create().show();
                }else{
                    Intent intent = new Intent(MainActivity.this, RegActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String text = getResources().getText(R.string.gsf_id) + RegActivity.getGSFID(this);
        mGsfId.setText(text);
    }
}
