package cmsc436.umd.edu.sway;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cmsc436.frontendhelper.TrialMode;
import edu.umd.cmsc436.sheets.Sheets;

public class FragmentPagerSupport extends AppCompatActivity {
    static final String ACTION_HELP = "edu.umd.cmsc436.balance.action.HELP";
    static final String ACTION_PRACTICE = "edu.umd.cmsc436.balance.action.PRACTICE";
    static final String ACTION_TRIAL = "edu.umd.cmsc436.balance.action.TRIAL";
    static final String ACTION_HISTORY = "edu.umd.cmsc436.balance.action.HISTORY";  // new addition to front end

    MyAdapter mAdapter;
    ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pager);

        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        Intent intent = getIntent();
        String action = intent.getAction();
        switch(action) {
            case ACTION_PRACTICE:
            case ACTION_HELP:
                addFragments();
                break;
            case ACTION_TRIAL:
                Sheets.TestType testType = TrialMode.getAppendage(intent);
                Log.e("INSTR", "TEST_TYPE: "+testType.name());
                switch (testType) {
                    case SWAY_OPEN_APART:
                        mAdapter.addFragment(LastFragment.newInstance(R.array.test_instr_text_1));
                        break;
                    case SWAY_OPEN_TOGETHER:
                        mAdapter.addFragment(LastFragment.newInstance(R.array.test_instr_text_2));
                        break;
                    case SWAY_CLOSED:
                        mAdapter.addFragment(LastFragment.newInstance(R.array.test_instr_text_3));
                        break;
                    default:
                        break;
                }
                break;
            case ACTION_HISTORY:
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("ACTIVITY_RESULT","AC CALLED: "+data.getFloatExtra(TrialMode.KEY_SCORE,-1));
        setResult(resultCode, data);
        finish();
    }

    private void addFragments() {
        mAdapter.addFragment(PageFragment.newInstance(R.array.instruction_overview));
        mAdapter.addFragment(PageFragment.newInstance(R.array.test_instr_text_1));
        mAdapter.addFragment(PageFragment.newInstance(R.array.test_instr_text_2));
        mAdapter.addFragment(PageFragment.newInstance(R.array.test_instr_text_3));
        mAdapter.addFragment(LastFragment.newInstance(R.array.practice_text));
    }

    public static class MyAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments = new ArrayList<>();

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment) {
            fragments.add(fragment);
            notifyDataSetChanged();
        }
    }

    public static class PageFragment extends Fragment {
        public static final String ARG_ID = "ARG_ID";
        private int mId;

        public static PageFragment newInstance(int id) {
            PageFragment fragment = new PageFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_ID, id);
            fragment.setArguments(args);
            return fragment;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mId = getArguments().getInt(ARG_ID);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_page, container, false);

            Resources res = getResources();
            String[] text = res.getStringArray(mId);

            TextView titleTextView = (TextView) view.findViewById(R.id.page_title);
            titleTextView.setText(text[0]);

            TextView instructionsTextView = (TextView) view.findViewById(R.id.page_text);
            instructionsTextView.setText(text[1]);

            return view;
        }
    }

    public static class LastFragment extends Fragment {
        public static final String ARG_ID = "ARG_ID";
        private int mId;

        public static LastFragment newInstance(int id) {
            Bundle args = new Bundle();
            args.putInt(ARG_ID, id);
            LastFragment fragment = new LastFragment();
            fragment.setArguments(args);
            return fragment;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mId = getArguments().getInt(ARG_ID);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_last, container, false);

            Resources res = getResources();
            String[] text = res.getStringArray(mId);

            TextView titleTextView = (TextView) view.findViewById(R.id.instruction_title);
            titleTextView.setText(text[0]);

            TextView instructionsTextView = (TextView) view.findViewById(R.id.text);
            instructionsTextView.setText(text[1]);

            Button startButton = (Button) view.findViewById(R.id.start_button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent currIntent = getActivity().getIntent();
                    Intent intent = new Intent(getActivity(), SwayMain.class);
                    intent.putExtras(currIntent);
                    intent.setAction(currIntent.getAction());
                    startActivity(intent);
                    getActivity().finish();
                }
            });

            return view;
        }
    }

}
