package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().getTime());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        double duration = (double) (outHour - inHour) / (1000*60*60);

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                if (duration < 0.5) {
                    ticket.setPrice(0);
                } else {
                    ticket.setPrice((double)Math.round(duration * Fare.CAR_RATE_PER_HOUR * 1000) / 1000);
                }
                break;
            }
            case BIKE: {
                if (duration < 0.5) {
                    ticket.setPrice(0);
                } else {
                    ticket.setPrice((double)Math.round(duration * Fare.BIKE_RATE_PER_HOUR * 1000) / 1000);
                }
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }

        if (discount) {
            ticket.setPrice((double)Math.round(ticket.getPrice() * 0.95 * 1000) / 1000);
        }
    }

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}