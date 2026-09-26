package lk.solar.microgrid;

import android.app.Application;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.ReservationRepository;
import lk.solar.microgrid.data.OperatorRepository;

public final class SolarApplication extends Application {
    private AccountRepository accounts;
    private ReservationRepository reservations;
    private OperatorRepository operators;

    @Override public void onCreate() {
        super.onCreate();
        accounts = new AccountRepository(this, BuildConfig.API_BASE_URL);
        reservations = new ReservationRepository(accounts.api(), accounts);
        operators = new OperatorRepository(this, accounts.api());
    }
    public AccountRepository accounts() { return accounts; }
    public ReservationRepository reservations() { return reservations; }
    public OperatorRepository operators() { return operators; }
}