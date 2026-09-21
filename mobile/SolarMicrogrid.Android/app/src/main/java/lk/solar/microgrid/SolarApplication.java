package lk.solar.microgrid;

import android.app.Application;
import lk.solar.microgrid.data.AccountRepository;

public final class SolarApplication extends Application {
    private AccountRepository accounts;
    @Override public void onCreate() {
        super.onCreate();
        accounts = new AccountRepository(this, BuildConfig.API_BASE_URL);
    }
    public AccountRepository accounts() { return accounts; }
}
