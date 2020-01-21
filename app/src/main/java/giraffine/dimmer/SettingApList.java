package giraffine.dimmer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingApList extends DialogPreference {

    private Context mContext = null;
    private ApAdapter mApAdapter = null;
    private List<PackageInfo> mListAll = null;

    private class ApItem {
        ApplicationInfo appInfo;
        Boolean checked;
        String label;

        public ApItem(Context context, ApplicationInfo info, boolean b) {
            appInfo = info;
            checked = Boolean.valueOf(b);
            label = (String) appInfo.loadLabel(context.getPackageManager());
        }
    }

    private List<ApItem> mListApItem = null;

    public SettingApList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.setting_ap_list);
        mContext = context;
    }

    @Override
    public void onBindDialogView(View view) {
        if (mListApItem == null)
            mListApItem = new ArrayList<ApItem>();
        else
            mListApItem.clear();
        if (mApAdapter == null)
            mApAdapter = new ApAdapter(mContext);

        ListView listview = (ListView) view.findViewById(R.id.apListView);
        listview.setAdapter(mApAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApItem item = mListApItem.get(position);
                if (item.checked) {
                    item.checked = false;
                    view.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    item.checked = true;
                    view.setBackgroundColor(Color.LTGRAY);
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mListAll == null)
                    mListAll = mContext.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);

                for (PackageInfo ap : mListAll) {
                    if (ap.activities != null && ap.applicationInfo.enabled) {
                        Set<String> settinglist = Prefs.getApList();
                        if (settinglist != null && settinglist.contains(ap.packageName))
                            mListApItem.add(new ApItem(mContext, ap.applicationInfo, true));
                        else
                            mListApItem.add(new ApItem(mContext, ap.applicationInfo, false));
                    }
                }
                Collections.sort(mListApItem, mComparator);
                mHandler.sendEmptyMessage(MSG_REFRESH_LIST);
            }
        }).start();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Set<String> result = new HashSet<String>();
            for (ApItem item : mListApItem) {
                if (item.checked)
                    result.add(item.appInfo.packageName);
            }
            Prefs.setApList(result);
            setSummary(getSummary(result, mContext.getPackageManager()));
        }
    }

    public static String getSummary(Set<String> list, PackageManager pm) {
        String summary = "";
        if (list == null)
            return summary;

        for (String name : list) {
            try {
                summary += pm.getApplicationLabel(pm.getApplicationInfo(name, 0)) + ", ";
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(Dimmer.TAG, "can't find " + name);
            }
        }
        int end = summary.lastIndexOf(", ");
        if (end != -1)
            summary = summary.substring(0, end);
        return summary;
    }

    private Comparator<ApItem> mComparator = new Comparator<ApItem>() {
        @Override
        public int compare(ApItem a, ApItem b) {
            if (a.checked && !b.checked)
                return -1;
            else if (!a.checked && b.checked)
                return 1;
            else
                return a.label.compareToIgnoreCase(b.label);
        }
    };

    public class ApAdapter extends BaseAdapter {
        private Context mContext = null;
        private LayoutInflater mInflater;

        private class ViewHolder {
            TextView name;
            ImageView icon;
        }

        public ApAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mListApItem.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mListApItem.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.setting_ap_list_item, null);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.apIcon);
                holder.name = (TextView) convertView.findViewById(R.id.apName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(mListApItem.get(position).label);
            holder.icon.setImageDrawable(mListApItem.get(position).appInfo.loadIcon(mContext.getPackageManager()));
            if (mListApItem.get(position).checked)
                convertView.setBackgroundColor(Color.LTGRAY);
            else
                convertView.setBackgroundColor(Color.TRANSPARENT);

            return convertView;
        }
    }

    private static final int MSG_REFRESH_LIST = 0;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_LIST:
                    mApAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
}
