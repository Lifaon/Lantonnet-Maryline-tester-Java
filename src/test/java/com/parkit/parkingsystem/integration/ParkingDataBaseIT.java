package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.lang.Thread;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static final String reg_number ="ABCDEF";

    @Spy
    private static TicketDAO ticketDAO_spy;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO_spy = Mockito.spy(TicketDAO.class);
        ticketDAO_spy.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(reg_number);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final Date inTime = new Date();
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(reg_number);
        assertNull(ticket.getOutTime());
        assert(Math.abs(inTime.getTime() - ticket.getInTime().getTime()) < 5000);
        assertEquals(ticket.getVehicleRegNumber(), reg_number);
    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        // Ensure outTime is after inTime
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.err.println("Sleep error: " + ex);
        }
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final Date outTime = new Date();
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket(reg_number);
        assertNotNull(ticket.getOutTime());
        assert(Math.abs(outTime.getTime() - ticket.getOutTime().getTime()) < 5000);
        assert(ticket.getPrice() == 0.);
    }

    @Test
    public void testParkingLotExitRecurringUser(){
        testParkingLotExit();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO_spy);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(reg_number);
        final Date outTime = new Date(ticket.getInTime().getTime() + 60*60*1000);
        ticket.setOutTime(outTime);
        when(ticketDAO_spy.getTicket(reg_number)).thenReturn(ticket);

        parkingService.processExitingVehicle();

        ticket = ticketDAO.getTicket(reg_number);
        assertEquals(ticket.getPrice(), 0.95 * Fare.CAR_RATE_PER_HOUR);
    }
}
