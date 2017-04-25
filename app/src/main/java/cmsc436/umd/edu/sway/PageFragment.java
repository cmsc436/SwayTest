package cmsc436.umd.edu.sway;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class PageFragment extends android.support.v4.app.Fragment {
        TextView textView;


    public PageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String message="";
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_page_layout,container,false);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        Bundle bundle = getArguments();
        String msg = Integer.toString(bundle.getInt("count"));
        switch(msg){
            case "1":
                 message = "\t\t\t\t\t\t\t\tTEST 1 \nInstructions: Please have legs opened \nand aligned with your shoulders.";
                break;
            case "2":
                message = "\t\t\t\t\t\t\t\tTEST 2 \nInstructions: Please have your legs closed \nand eyes opened";
                break;
            case "3":
                message = "                TEST 3 \nInstructions: Please have your legs closed \nand eyes closed";
                break;
            default:
//                message = "This is the end of the SWAY TEST";

                startActivity(new Intent(getContext(), SwayMain.class));
        }


        textView.setText(message+" .");

        return view;
    }

}
