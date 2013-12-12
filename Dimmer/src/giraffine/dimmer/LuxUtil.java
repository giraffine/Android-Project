package giraffine.dimmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;

public class LuxUtil {

	private static Context mContext;
	private static String PREFER = "LuxLevel";
	public static void init(Context context)
	{
		mContext = context;
	}
	public static String dumpLuxLevel()
	{
		String result = "";
		Set<String> levelSet = mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).getStringSet("levelSet", null);
		if(levelSet != null)
		{
			ArrayList<Integer> data = new ArrayList<Integer>();
			Iterator<String> it = levelSet.iterator();
			while(it.hasNext())
				data.add(Integer.valueOf(it.next()));
			Collections.sort(data);
			result = data.toString();
		}
		return result;
	}
	public static boolean isLowestLevel(int level)
	{
		Set<String> levelSet = mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).getStringSet("levelSet", null);
		if(levelSet != null)
		{
			if(levelSet.size() < 5)	// data is too few to judge lowest
				return false;
			ArrayList<Integer> data = new ArrayList<Integer>();
			Iterator<String> it = levelSet.iterator();
			while(it.hasNext())
				data.add(Integer.valueOf(it.next()));
			Collections.sort(data);
			// TODO: may need check statistics to ensure the level is true lowest and not false alarm.
			if(level <= data.get(0))
				return true;
		}
		return false;
	}
	public static void setLuxLevel(int level)
	{
		levelExist(level);
		
		// maybe no need the total lux statistics
		int count = mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).getInt(String.valueOf(level), 0);
		count++;
		mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putInt(String.valueOf(level), count).commit();
	}
	private static boolean levelExist(int level)
	{
		Set<String> levelSet = mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).getStringSet("levelSet", null);
		if(levelSet == null)
		{
			levelSet = new TreeSet<String>();
			levelSet.add(String.valueOf(level));
			mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putStringSet("levelSet", levelSet).commit();
			return false;
		}
		if(levelSet.contains(String.valueOf(level)))
				return true;
		else
		{
			levelSet.add(String.valueOf(level));
			mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putStringSet("levelSet", levelSet).commit();
		}
		return false;
	}
}
