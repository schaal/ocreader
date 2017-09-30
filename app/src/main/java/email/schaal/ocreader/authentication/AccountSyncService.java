package email.schaal.ocreader.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AccountSyncService extends Service {
    private NextcloudAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new NextcloudAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
