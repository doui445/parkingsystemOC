package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    private final String vehicleRegistrationNumber = "ABCDEF";

    private ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtilMock;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.setDataBaseConfig(dataBaseTestConfig);
        ticketDAO = new TicketDAO();
        ticketDAO.setDataBaseConfig(dataBaseTestConfig);
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtilMock.readSelection()).thenReturn(1);
        when(inputReaderUtilMock.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        dataBasePrepareService.clearDataBaseEntries();

        parkingService = new ParkingService(inputReaderUtilMock, parkingSpotDAO, ticketDAO);
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception {
        when(inputReaderUtilMock.readSelection()).thenReturn(1);
        when(inputReaderUtilMock.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);

        ParkingService parkingService = new ParkingService(inputReaderUtilMock, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        Ticket ticket = ticketDAO.getTicket(vehicleRegistrationNumber);

        assertNotNull(ticket);

        int availableSpot = parkingSpotDAO.getNextAvailableSlot(ticket.getParkingSpot().getParkingType());
        int updatedSpot = ticket.getParkingSpot().getId();
        assertNotEquals(availableSpot, updatedSpot);
    }

    @Test
    public void testParkingLotExit() {

        int hoursOfParking = 3;
        long inHour = System.currentTimeMillis() - (hoursOfParking*60*60*1000);
        Date entryDateTime = new Date(inHour);

        int initialAvailableSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(vehicleRegistrationNumber);
        ticket.setInTime(entryDateTime);
        ticketDAO.saveTicket(ticket);

        long outHour = System.currentTimeMillis();
        parkingService.processExitingVehicle();

        Ticket registeredTicket = ticketDAO.getTicket(vehicleRegistrationNumber);
        int availableSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        assertEquals(initialAvailableSpot, availableSpot);

        assertEquals(Fare.CAR_RATE_PER_HOUR * hoursOfParking, registeredTicket.getPrice());

        assertEquals(outHour/100000, registeredTicket.getOutTime().getTime()/100000);
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        int hoursOfParking = 3;
        long inHour = System.currentTimeMillis() - (hoursOfParking*60*60*1000);

        Date entryDateTime = new Date(inHour);

        //registering first ticket
        parkingService.processIncomingVehicle();
        Ticket ticket1 = ticketDAO.getTicket(vehicleRegistrationNumber);
        ticket1.setInTime(entryDateTime);
        ticketDAO.saveTicket(ticket1);

        parkingService.processExitingVehicle();

        Ticket registeredTicket1 = ticketDAO.getTicket(vehicleRegistrationNumber);

        //registering second ticket
        parkingService.processIncomingVehicle();
        Ticket ticket2 = ticketDAO.getTicket(vehicleRegistrationNumber);
        ticket2.setInTime(entryDateTime);
        ticketDAO.saveTicket(ticket2);

        parkingService.processExitingVehicle();

        Ticket registeredTicket2 = ticketDAO.getTicket(vehicleRegistrationNumber);

        boolean cond = registeredTicket1.getPrice()>registeredTicket2.getPrice();
        //first ticket must be more expensive than second one because of the applied discount
        assertTrue(cond);
    }
}
