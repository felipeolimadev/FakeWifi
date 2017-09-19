package eu.chylek.adam.fakewifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainFragment extends Fragment {
    private class PInfo {
        private String appname = "";
        private String pname = "";
    }

    SharedPreferences pref;
    ListView app_list;        //listview with checkboxes which will contain apps
    ToggleButton masterSwitch;
    ToggleButton debugSwitch;

    ArrayList<PInfo> pinfos;    //PInfo object for each app

    public MainFragment() {
    }

    private void init(View view) {
        pref = this.getContext().getSharedPreferences("pref", Context.MODE_PRIVATE);
        app_list = view.findViewById(R.id.appList);

        pinfos = getInstalledApps(true);
        //sort the pinfo objects by name
        Collections.sort(pinfos, new Comparator<PInfo>() {
            @Override
            public int compare(PInfo lhs, PInfo rhs) {
                return lhs.appname.compareTo(rhs.appname);
            }
        });

        //add apps to installed_apps list
        ArrayList<String> installed_apps = new ArrayList<String>();
        for (int i = 0; i < pinfos.size(); i++)
            installed_apps.add(pinfos.get(i).appname);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_multiple_choice,
                installed_apps);
        app_list.setAdapter(adapter);
        app_list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        for (int i = 0; i < pinfos.size(); i++)
            app_list.setItemChecked(i, pref.getBoolean(pinfos.get(i).pname, false));

        masterSwitch = view.findViewById(R.id.masterswitch);
        masterSwitch.setChecked(pref.getBoolean("master", true));
        debugSwitch = view.findViewById(R.id.debugswitch);
        debugSwitch.setChecked(pref.getBoolean("debug", true));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        init(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setMenuVisibility(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actionbar, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.act_deselect:
                all(false);
                return true;

            case R.id.act_select:
                all(true);
                return true;

            case R.id.act_invert:
                invert();
                return true;
            case R.id.act_about:
                about();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void about() {
        Fragment newFragment = new AboutFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment, newFragment);
        transaction.addToBackStack(null);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
    }

    public ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
        ArrayList<PInfo> res = new ArrayList<>();

        Context context = getContext();
        List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if ((!getSysPackages) && (p.versionName == null))
                continue;
            PInfo newInfo = new PInfo();
            newInfo.appname = p.applicationInfo.loadLabel(context.getPackageManager()).toString();
            newInfo.pname = p.packageName;
            res.add(newInfo);
        }
        return res;
    }

    //save all apps' names along with true or false for ticked or not ticked
    public void save() {
        SharedPreferences.Editor editor = pref.edit();
        for (int i = 0; i < pinfos.size(); i++)
            editor.putBoolean(pinfos.get(i).pname, app_list.isItemChecked(i));
        editor.putBoolean("master", this.masterSwitch.isChecked());
        editor.putBoolean("debug", this.debugSwitch.isChecked());
        editor.commit(); // do not use apply, otherwise the Xposed part of the module won't update its settings
        fixPreferencePermission(getActivity());
    }

    /**
     * workaround for android N and later - preference files have to be in MODE_PRIVATE
     * so we need to override the permissions after each save so that Xposed part can read them.
     *
     * @param ctxt
     */
    public static void fixPreferencePermission(Context ctxt){
        Log.d("PREFDIR",ctxt.getApplicationInfo().dataDir);
        File prefsDir = new File(ctxt.getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, "pref.xml");
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false);
        }
    }
    //invert selected
    public void invert() {
        for (int i = 0; i < pinfos.size(); i++) {
            app_list.setItemChecked(i, !app_list.isItemChecked(i));
        }
    }

    public void all(boolean select) {
        for (int i = 0; i < pinfos.size(); i++) {
            app_list.setItemChecked(i, select);
        }
    }


    @Override
    public void onPause() {
        save();
        super.onPause();
    }
}
