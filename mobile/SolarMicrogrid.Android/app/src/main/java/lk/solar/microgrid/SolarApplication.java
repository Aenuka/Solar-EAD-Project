package lk.solar.microgrid;

import android.app.Application;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.ReservationRepository;

public final class SolarApplication extends Application {
    private AccountRepository accounts;
    private ReservationRepository reservations;

    @Override public void onCreate() {
        super.onCreate();
        accounts = new AccountRepository(this, BuildConfig.API_BASE_URL);
        reservations = new ReservationRepository(accounts.api(), accounts);
    }
    public AccountRepository accounts() { return accounts; }
    public ReservationRepository reservations() { return reservations; }
}