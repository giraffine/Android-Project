package giraffine.dimmer;

import android.content.Context;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class LuxUtil {

    private static Context mContext;
    private static String PREFER = "LuxLevel";
    private static String LEVELSET = "levelSet";
    private static Set<String> mLevelSet = null;
    private static Comparator<String> mComparator = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return Integer.valueOf(a) - Integer.valueOf(b);
        }
    };

    public static void init(Context context) {
        mContext = context;
    }

    public static String dumpLuxLevel() {
        String result = "";
        if (mLevelSet == null)
            mLevelSet = mContext.getSharedPreferences(PREFER, Context.MODE_PRIVATE).getStringSet(LEVELSET, null);
        if (mLevelSet != null) {
            ArrayList<String> array = Collections.list(Collections.enumeration(mLevelSet));
            Collections.sort(array, mComparator);
            result = array.toString();
        }
        return result;
    }

    public static boolean isLowestLevel(int level) {
        if (mLevelSet == null)
            mLevelSet = mContext.getSharedPreferences(PREFER, Context.MODE_PRIVATE).getStringSet(LEVELSET, null);
        if (mLevelSet != null) {
            if (mLevelSet.size() < 5)    // data is too few to judge lowest
                return false;
            ArrayList<String> array = Collections.list(Collections.enumeration(mLevelSet));
            Collections.sort(array, mComparator);
            // TODO: may need check statistics to ensure the level is true lowest and not false alarm.
            if (level <= Integer.valueOf(array.get(0)))
                return true;
        }
        return false;
    }

    public static boolean getBoundaryLevel(Point bound) {
        if (mLevelSet == null)
            mLevelSet = mContext.getSharedPreferences(PREFER, Context.MODE_PRIVATE).getStringSet(LEVELSET, null);
        if (mLevelSet != null) {
            ArrayList<String> array = Collections.list(Collections.enumeration(mLevelSet));
            Collections.sort(array, mComparator);
            bound.x = Integer.valueOf(array.get(0));
            bound.y = Integer.valueOf(array.get(array.size() - 1));
            return true;
        }
        return false;
    }

    public static void setLuxLevel(int level) {
        levelExist(level);

        // maybe no need the total lux statistics
/*		// this will cause app ANR on Galaxy S3
		int count = mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).getInt(String.valueOf(level), 0);
		count++;
		mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putInt(String.valueOf(level), count).commit();
*/
    }

    private static boolean levelExist(int level) {
        if (mLevelSet == null)
            mLevelSet = mContext.getSharedPreferences(PREFER, Context.MODE_PRIVATE).getStringSet(LEVELSET, null);
        if (mLevelSet == null) {
            mLevelSet = new TreeSet<String>();
            mLevelSet.add(String.valueOf(level));
            mContext.getSharedPreferences(PREFER, Context.MODE_WORLD_WRITEABLE).edit().putStringSet(LEVELSET, mLevelSet).commit();
            return false;
        }
        if (mLevelSet.contains(String.valueOf(level)))
            return true;
        else {
            mLevelSet.add(String.valueOf(level));
            mContext.getSharedPreferences(PREFER, Context.MODE_WORLD_WRITEABLE).edit().putStringSet(LEVELSET, mLevelSet).commit();
        }
        return false;
    }
}
