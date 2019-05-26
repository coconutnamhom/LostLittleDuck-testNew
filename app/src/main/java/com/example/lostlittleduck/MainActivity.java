package com.example.lostlittleduck;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.example.lostlittleduck.adapter.DeviceAdapter;
import com.example.lostlittleduck.adapter.ViewPagerAdapter;
import com.example.lostlittleduck.fragment.ConnectedBLEFragment;
import com.example.lostlittleduck.fragment.ScanBLEFragment;
import com.example.lostlittleduck.fragment.notiFragment;
import com.example.lostlittleduck.service.ExampleService;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
//        tabLayout.getTabAt(2)    .setIcon(R.drawable.noti);

        View rootView = getWindow().getDecorView().getRootView();
        startService(rootView);

        String menuFragment = getIntent().getStringExtra("menuFragment");
        if (menuFragment != null) {
            if (menuFragment.equals("notiFragment")) {
                viewPager.setCurrentItem(2);
                viewPager.getAdapter().notifyDataSetChanged();
            }
        }

    }


    private void setupViewPager(final ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ScanBLEFragment(), "Scan ");
        adapter.addFragment(new ConnectedBLEFragment(), "Pair");
        adapter.addFragment(new notiFragment(), "Noti");
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewPager.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void startService(View v) {

        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", "start");


        ContextCompat.startForegroundService(this, serviceIntent);
    }

}