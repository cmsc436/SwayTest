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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cmsc436.frontendhelper.TrialMode;
import edu.umd.cmsc436.sheets.Sheets;

// Sorry Lauren, I know this is your precious code
// but I need to add some stuff to it :]
// ~Sam
// P.S I Promise to Comment
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
        //String action = "edu.umd.cmsc436.balance.action.HELP";
//        String action = "edu.umd.cmsc436.balance.action.TRIAL";
        switch(action) {
            case ACTION_HELP:
                addFragments();
                break;
            case ACTION_PRACTICE:
                Intent newIntent = new Intent(this, SwayMain.class);
                newIntent.putExtras(intent);
                startActivity(newIntent);
                break;
            case ACTION_TRIAL:
                Sheets.TestType testType = TrialMode.getAppendage(intent);
                Log.e("INSTR", "TEST_TYPE: "+testType.name());
//                Sheets.TestType testType = Sheets.TestType.SWAY_OPEN_APART;
                switch (testType) {
                    case SWAY_OPEN_APART:
                        mAdapter.addFragment(LastFragment.newInstance(0, R.array.test_instr_text_1));

                        break;
                    case SWAY_OPEN_TOGETHER:
                        mAdapter.addFragment(LastFragment.newInstance(0, R.array.test_instr_text_2));
                        break;
                    case SWAY_CLOSED:
                        mAdapter.addFragment(LastFragment.newInstance(0, R.array.test_instr_text_3));
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

        setResult(resultCode,data);
        finish();
    }

    private void addFragments() {
        mAdapter.addFragment(FirstFragment.newInstance(0));
        mAdapter.addFragment(PageFragment.newInstance(1, R.array.test_instr_text_1));
        mAdapter.addFragment(PageFragment.newInstance(2, R.array.test_instr_text_2));
        mAdapter.addFragment(PageFragment.newInstance(3, R.array.test_instr_text_3));
        mAdapter.addFragment(LastFragment.newInstance(4, R.array.practice_text));
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

    public static class FirstFragment extends Fragment {
        public static final String ARG_PAGE = "ARG_PAGE";
        private int mPage;

        public static FirstFragment newInstance(int page) {
            FirstFragment fragment = new FirstFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_PAGE, page);
            fragment.setArguments(args);
            return fragment;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPage = getArguments().getInt(ARG_PAGE);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_first, container, false);
            return view;
        }
    }

    public static class LastFragment extends Fragment {
        public static final String ARG_PAGE = "ARG_PAGE";
        public static final String ARG_ID = "ARG_ID";
        private int mPage;
        private int mId;

        public static LastFragment newInstance(int page, int id) {
            Bundle args = new Bundle();
            args.putInt(ARG_PAGE, page);
            args.putInt(ARG_ID, id);
            LastFragment fragment = new LastFragment();
            fragment.setArguments(args);
            return fragment;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPage = getArguments().getInt(ARG_PAGE);
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
                    // START SWAY TEST ACTIVITY HERE
                    Intent intent = new Intent(getActivity(), SwayMain.class);
                    intent.putExtras(getActivity().getIntent());
                    startActivityForResult(intent,Info.ACTIVITY_FOR_RESULT);
                }
            });

            return view;
        }
    }

    public static class PageFragment extends Fragment {
        public static final String ARG_PAGE = "ARG_PAGE";
        public static final String ARG_ID = "ARG_ID";
        private int mPage;
        private int mId;

        public static PageFragment newInstance(int page, int id) {
            PageFragment fragment = new PageFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_PAGE, page);
            args.putInt(ARG_ID, id);
            fragment.setArguments(args);
            return fragment;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPage = getArguments().getInt(ARG_PAGE);
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
}
