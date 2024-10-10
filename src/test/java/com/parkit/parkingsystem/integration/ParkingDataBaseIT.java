package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.Fare;
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

import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static final String reg_number ="ABCDEF";

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(reg_number);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
        Connection con = null;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.DELETE_TEST_TICKET);
            ps.setString(1, reg_number);
            ps.executeUpdate();
            dataBaseTestConfig.closePreparedStatement(ps);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final Date inTime = new Date();
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(reg_number);
        assert(Math.abs(inTime.getTime() - ticket.getInTime().getTime()) < 5000);
        assert(ticket.getOutTime() == null);
        assert(ticket.getVehicleRegNumber() == reg_number);
    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final Date outTime = new Date();
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket(reg_number);
        assert(ticket.getOutTime() != null);
        assert(Math.abs(outTime.getTime() - ticket.getOutTime().getTime()) < 5000);
        assert(ticket.getPrice() == 0.);
    }

    @Test
    public void testParkingLotExitRecurringUser(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000));
        // TODO : mock time
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket(reg_number);
        // assert(ticket.getPrice() == 0.95 * Fare.CAR_RATE_PER_HOUR);
    }
}
