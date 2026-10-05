package lk.solar.microgrid.ui;

import android.content.Intent;
import lk.solar.microgrid.data.Reservation;

final class BookingIntents {
    private BookingIntents() { }
    static void addDetails(Intent intent, Reservation booking) {
        intent.putExtra("reservationJson", booking.source.toString());
        intent.putExtra("stationName", booking.stationLabel());
        intent.putExtra("stationAddress", booking.stationAddress);
        intent.putExtra("slotStartsAt", booking.slotStartsAt);
        intent.putExtra("slotEndsAt", booking.slotEndsAt);
        intent.putExtra("reservationCode", booking.reservationId);
        intent.putExtra("reservationDate", booking.reservationDate);
        intent.putExtra("energyAmountKwh", booking.energyAmountKwh);
        intent.putExtra("tradingType", booking.tradingType);
        intent.putExtra("status", booking.status);
        intent.putExtra("cancellationReason", booking.source.optString("cancellationReason", ""));
    }
}
