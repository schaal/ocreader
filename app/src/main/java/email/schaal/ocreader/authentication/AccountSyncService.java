package email.schaal.ocreader.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AccountSyncService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new NextcloudAccountAuthenticator(this).getIBinder();
    }
}
