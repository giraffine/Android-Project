package giraffine.dimmer;

import android.content.Intent;
import android.service.quicksettings.TileService;

public class DimmerTileService extends TileService {
    @Override
    public void onClick() {
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        startServiceIntent.setAction(DimmerService.SWITCHDIM);
        startService(startServiceIntent);

    }
}
