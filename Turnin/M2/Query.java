package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  /********CLEAR TABLES********/
  private static final String CLEAR_RESERVATIONS_SQL = "DELETE FROM RESERVATIONS_nagatac";
  private PreparedStatement clearReservationsTableStmt;
  
  private static final String CLEAR_USERS_SQL = "DELETE FROM USERS_nagatac";
  private PreparedStatement clearUsersTableStmt;
  /****************************/

  /***********LOGIN************/
  private static final String GET_LOGIN_SQL = "SELECT * FROM USERS_nagatac WHERE username = ?";
  private PreparedStatement getLoginStmt;
  /****************************/

  /********CREATE USER*********/
  private static final String CHECK_USERNAME_SQL = "SELECT username FROM USERS_nagatac WHERE username = ?";
  private PreparedStatement checkUsernameStmt;
  
  private static final String CREATE_USER_SQL = "INSERT INTO USERS_nagatac (username, password, balance) VALUES(?, ?, ?)";
  private PreparedStatement createUserStmt;
  /****************************/

  /**********SEARCH************/
  private static final String SEARCH_DIRECT_FLIGHTS_SQL = 
      "SELECT TOP (?) " +
          "F.fid AS fid, F.day_of_month AS day_of_month, F.carrier_id AS carrier_id, " + 
          "F.flight_num AS flight_num, F.origin_city AS origin_city, F.dest_city AS dest_city, " + 
          "F.actual_time AS actual_time, F.capacity AS capacity, F.price AS price " +  
      "FROM Flights AS F " + 
      "WHERE F.origin_city = ? AND F.dest_city = ? AND F.day_of_month = ? AND F.canceled = 0 " + 
      "ORDER BY F.actual_time ASC, F.fid ASC";
  private PreparedStatement searchDirectFlightsStmt;

  private static final String SEARCH_ALL_FLIGHTS_SQL = 
      "SELECT * " + 
      "FROM (SELECT TOP (?) * " + 
            "FROM (SELECT " + 
                      "F.fid AS F1_fid, F.day_of_month AS F1_day_of_month, F.carrier_id AS F1_carrier_id, " + 
                      "F.flight_num AS F1_flight_num, F.origin_city AS F1_origin_city, F.dest_city AS F1_dest_city, " + 
                      "F.actual_time AS F1_actual_time, F.capacity AS F1_capacity, F.price AS F1_price, " + 

                      "NULL AS F2_fid, NULL AS F2_day_of_month, NULL AS F2_carrier_id, NULL AS F2_flight_num, " + 
                      "NULL AS F2_origin_city, NULL AS F2_dest_city, NULL AS F2_actual_time, NULL AS F2_capacity, " + 
                      "NULL AS F2_price, " + 

                      "1 AS DIRECT " + 
                  "FROM FLIGHTS AS F " + 
                  "WHERE F.origin_city = ? AND F.dest_city = ? AND F.day_of_month = ? AND canceled = 0 " +
                  "UNION " + 
                  "SELECT " + 
                      "F1.fid AS F1_fid, F1.day_of_month AS F1_day_of_month, F1.carrier_id AS F1_carrier_id, " + 
                      "F1.flight_num AS F1_flight_num, F1.origin_city AS F1_origin_city, F1.dest_city AS F1_dest_city, " + 
                      "F1.actual_time AS F1_actual_time, F1.capacity AS F1_capacity, F1.price AS F1_price, " + 

                      "F2.fid AS F2_fid, F2.day_of_month AS F2_day_of_month, F2.carrier_id AS F2_carrier_id, " + 
                      "F2.flight_num AS F2_flight_num, F2.origin_city AS F2_origin_city, F2.dest_city AS F2_dest_city, " + 
                      "F2.actual_time AS F2_actual_time, F2.capacity AS F2_capacity, F2.price AS F2_price, " + 

                      "0 AS DIRECT " + 
                  "FROM FLIGHTS AS F1, FLIGHTS AS F2 " + 
                  "WHERE F1.origin_city = ? AND F1.dest_city = F2.origin_city AND F2.dest_city = ? AND " + 
                      "F1.day_of_month = ? AND F1.day_of_month = F2.day_of_month AND " + 
                      "F1.canceled = 0 AND F2.canceled = 0 " + 
            ") AS CAPPED_SET ORDER BY DIRECT DESC, (F1_actual_time + ISNULL(F2_actual_time, 0)), F1_fid, F2_fid" + 
      ") AS SORTED_RESULTS ORDER BY (F1_actual_time + ISNULL(F2_actual_time, 0)), F1_fid, F2_fid";
  private PreparedStatement searchAllFlightsStmt;
  /****************************/

  /***********BOOK*************/
  private static final String CHECK_BOOKED_SQL = "SELECT COUNT(*) AS count " + 
                                                 "FROM RESERVATIONS_nagatac AS R, FLIGHTS AS F " + 
                                                 "WHERE R.fid1 = F.fid AND R.username = ? AND F.day_of_month = ?";
  private PreparedStatement checkBookedStmt;

  private static final String GET_FLIGHT_CAPACITY_SQL = "SELECT COUNT(*) AS count FROM RESERVATIONS_nagatac WHERE fid1 = ? OR fid2 = ?"; 
  private PreparedStatement getFlightCapacityStmt;

  private static final String GET_RESERVATION_COUNT_SQL = "SELECT COUNT(*) AS count FROM RESERVATIONS_nagatac";
  private PreparedStatement getReservationCountStmt;

  private static final String ADD_RESERVATION_SQL = "INSERT INTO RESERVATIONS_nagatac VALUES(?, ?, ?, ?, ?)";
  private PreparedStatement addReservationStmt;
  /****************************/

  /************PAY*************/
  private static final String GET_RES_COUNT_SQL = "SELECT COUNT(*) AS count FROM RESERVATIONS_nagatac WHERE username = ?";
  private PreparedStatement getResCountStmt;

  private static final String GET_PAID_SQL = "SELECT paid, fid1, fid2 FROM RESERVATIONS_nagatac WHERE rid = ?";
  private PreparedStatement getPaidStmt;

  private static final String GET_PRICE_SQL = "SELECT price FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement getPriceStmt;

  private static final String GET_BALANCE_SQL = "SELECT balance FROM USERS_nagatac WHERE username = ?";
  private PreparedStatement getBalanceStmt;

  private static final String UPDATE_PAID_SQL = "UPDATE RESERVATIONS_nagatac SET paid = 1 WHERE username = ? AND rid = ?";
  private PreparedStatement updatePaidStmt;

  private static final String UPDATE_BALANCE_SQL = "UPDATE USERS_nagatac SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalanceStmt;
  /****************************/

  /*******RESERVATIONS*********/
  private static final String GET_RES_SQL = "SELECT * FROM RESERVATIONS_nagatac WHERE username = ? ORDER BY rid";
  private PreparedStatement getResStmt;

  private static final String GET_FLIGHT_DETAILS_SQL = "SELECT * FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement getFlightDetailsStmt;
  /****************************/

  //
  // Instance variables
  //
  private String currUser;
  Itinerary itineraries[];

  protected Query() throws SQLException, IOException {
    currUser = null;
    itineraries = null;
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // TODO: YOUR CODE HERE

      clearReservationsTableStmt.executeUpdate();
      clearUsersTableStmt.executeUpdate();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE

    // CLEAR TABLES
    clearReservationsTableStmt = conn.prepareStatement(CLEAR_RESERVATIONS_SQL);
    clearUsersTableStmt = conn.prepareStatement(CLEAR_USERS_SQL);

    // LOGIN
    getLoginStmt = conn.prepareStatement(GET_LOGIN_SQL);

    // CREATE USER
    checkUsernameStmt = conn.prepareStatement(CHECK_USERNAME_SQL);
    createUserStmt = conn.prepareStatement(CREATE_USER_SQL);

    // SEARCH
    searchDirectFlightsStmt = conn.prepareStatement(SEARCH_DIRECT_FLIGHTS_SQL);
    searchAllFlightsStmt = conn.prepareStatement(SEARCH_ALL_FLIGHTS_SQL);

    // BOOK
    checkBookedStmt = conn.prepareStatement(CHECK_BOOKED_SQL);
    getFlightCapacityStmt = conn.prepareStatement(GET_FLIGHT_CAPACITY_SQL);
    getReservationCountStmt = conn.prepareStatement(GET_RESERVATION_COUNT_SQL);
    addReservationStmt = conn.prepareStatement(ADD_RESERVATION_SQL);

    // PAY
    getResCountStmt = conn.prepareStatement(GET_RES_COUNT_SQL);
    getPaidStmt = conn.prepareStatement(GET_PAID_SQL);
    getPriceStmt = conn.prepareStatement(GET_PRICE_SQL);
    getBalanceStmt = conn.prepareStatement(GET_BALANCE_SQL);
    updateBalanceStmt = conn.prepareStatement(UPDATE_BALANCE_SQL);
    updatePaidStmt = conn.prepareStatement(UPDATE_PAID_SQL);
    
    // RESERVATIONS
    getResStmt = conn.prepareStatement(GET_RES_SQL);
    getFlightDetailsStmt = conn.prepareStatement(GET_FLIGHT_DETAILS_SQL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE

    try {
      if (currUser != null) {
        return "User already logged in\n";
      }

      // get username and password
      getLoginStmt.clearParameters();
      getLoginStmt.setString(1, username.toLowerCase());
      ResultSet results = getLoginStmt.executeQuery();
  
      String foundUsername = "";
      byte[] foundPassword = new byte[144];
  
      while (results.next()) {
        foundUsername = results.getString("username");
        foundPassword = results.getBytes("password");
      }

      results.close();
      
      // check if username and password match
      if (!foundUsername.equals(username.toLowerCase())) {
        return "Login failed\n";
      } else {
        if (PasswordUtils.plaintextMatchesHash(password, foundPassword)) {
          currUser = username;
          return "Logged in as " + username + "\n";
        } else {
          return "Login failed\n";
        }
      }
    } catch (SQLException e){
      return "Login failed\n";
    }    
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE

    try {
      if (initAmount < 0) {
        return "Failed to create user\n";
      }
      
      // check if username exists
      if(checkUsername(username) == true) {
        return "Failed to create user\n";
      } else {
        byte[] hashedPassword = PasswordUtils.hashPassword(password);
        createUser(username, hashedPassword, initAmount);
        return "Created user " + username + "\n";
      }
    } catch (SQLException e){
      return "Failed to create user\n";
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return, must be positive
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    StringBuffer sb = new StringBuffer();

    if (numberOfItineraries < 1 || originCity.equals(destinationCity) || dayOfMonth < 1 || dayOfMonth > 31) {
      return "Failed to search\n";
    }

    try {      
      itineraries = new Itinerary[numberOfItineraries];
      int itnCount = 0;
      ResultSet results;

      // search direct flights
      if (directFlight) {
        searchDirectFlightsStmt.clearParameters();
        searchDirectFlightsStmt.setInt(1, numberOfItineraries);
        searchDirectFlightsStmt.setString(2, originCity);
        searchDirectFlightsStmt.setString(3, destinationCity);
        searchDirectFlightsStmt.setInt(4, dayOfMonth);
        results = searchDirectFlightsStmt.executeQuery();

        while(results.next()) { // add flights to itineraries
          Flight flight = getFlight(results, "");

          itineraries[itnCount] = new Itinerary(flight);
          itnCount++;
        }

        results.close();
      } else { // search indirect and direct
        searchAllFlightsStmt.clearParameters();
        searchAllFlightsStmt.setInt(1, numberOfItineraries);
        searchAllFlightsStmt.setString(2, originCity);
        searchAllFlightsStmt.setString(3, destinationCity);
        searchAllFlightsStmt.setInt(4, dayOfMonth);
        searchAllFlightsStmt.setString(5, originCity);
        searchAllFlightsStmt.setString(6, destinationCity);
        searchAllFlightsStmt.setInt(7, dayOfMonth);
        results = searchAllFlightsStmt.executeQuery();

        while(results.next()) { // add flights to itineraries
          Flight flight1 = getFlight(results, "F1_");
          
          if (results.getInt("DIRECT") == 1) {
            itineraries[itnCount] = new Itinerary(flight1);
          } else {
            Flight flight2 = getFlight(results, "F2_");
            itineraries[itnCount] = new Itinerary(flight1, flight2);
          }

          itnCount++;
        }

        results.close();
      }

      if (itnCount == 0) {
        return "No flights match your selection\n";
      }

      // add to return string
      for (int i = 0; i < itnCount; i++) {
        sb.append("Itinerary " + i + ": " + itineraries[i].toString());
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }

    return sb.toString();

    // *****UNSAFE*****
    // try {
    //   // one hop itineraries
    //   String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
    //     + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
    //     + "FROM Flights " + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'"
    //     + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
    //     + "ORDER BY actual_time ASC";

    //   Statement searchStatement = conn.createStatement();
    //   ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

    //   while (oneHopResults.next()) {
    //     int result_dayOfMonth = oneHopResults.getInt("day_of_month");
    //     String result_carrierId = oneHopResults.getString("carrier_id");
    //     String result_flightNum = oneHopResults.getString("flight_num");
    //     String result_originCity = oneHopResults.getString("origin_city");
    //     String result_destCity = oneHopResults.getString("dest_city");
    //     int result_time = oneHopResults.getInt("actual_time");
    //     int result_capacity = oneHopResults.getInt("capacity");
    //     int result_price = oneHopResults.getInt("price");

    //     sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
    //               + result_flightNum + " Origin: " + result_originCity + " Destination: "
    //               + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
    //               + " Price: " + result_price + "\n");
    //   }
    //   oneHopResults.close();
    // } catch (SQLException e) {
    //   e.printStackTrace();
    // }
    // return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE

    if (currUser == null) {
      return "Cannot book reservations, not logged in\n";
    } 

    if (itineraries == null || itineraryId < 0 || itineraryId > itineraries.length) {
      return "No such itinerary " + itineraryId + "\n";
    }

    Itinerary itn = itineraries[itineraryId];
    int rid = 0;
    boolean direct = true;
    boolean deadlock = true;

    while (deadlock) {
      deadlock = false;

      try {
        conn.setAutoCommit(false);

        checkBookedStmt.clearParameters();
        checkBookedStmt.setString(1, currUser.toLowerCase());
        checkBookedStmt.setInt(2, itn.f1.dayOfMonth);
        ResultSet results = checkBookedStmt.executeQuery();
        results.next();

        if (results.getInt("count") > 0) {
          results.close();
          conn.rollback();
          conn.setAutoCommit(true);
          return "You cannot book two flights in the same day\n";
        }

        getFlightCapacityStmt.clearParameters();
        getFlightCapacityStmt.setInt(1, itn.f1.fid);
        getFlightCapacityStmt.setInt(2, itn.f1.fid);
        results = getFlightCapacityStmt.executeQuery();
        results.next();

        if ((itn.f1.capacity - results.getInt("count")) <= 0) {
          results.close();
          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        }

        if (itn.f2 != null) {
          direct = false;

          getFlightCapacityStmt.clearParameters();
          getFlightCapacityStmt.setInt(1, itn.f2.fid);
          getFlightCapacityStmt.setInt(2, itn.f2.fid);
          results = getFlightCapacityStmt.executeQuery();
          results.next();

          if ((itn.f2.capacity - results.getInt("count")) <= 0) {
            results.close();
            conn.rollback();
            conn.setAutoCommit(true);
            return "Booking failed\n";
          }
        }

        results = getReservationCountStmt.executeQuery();
        results.next();
        rid = results.getInt("count") + 1;
        results.close();

        addReservationStmt.clearParameters();
        addReservationStmt.setInt(1, rid);
        addReservationStmt.setString(2, currUser.toLowerCase());
        addReservationStmt.setInt(3, itn.f1.fid);
        if (direct) {
          addReservationStmt.setNull(4, Types.INTEGER);
        } else {
          addReservationStmt.setInt(4, itn.f2.fid);
        }
        addReservationStmt.setInt(5, 0);
        addReservationStmt.executeUpdate();

        conn.commit();
        conn.setAutoCommit(true);

        return "Booked flight(s), reservation ID: " + rid + "\n";

      } catch (SQLException e1) {
        deadlock = isDeadlock(e1);

        if (deadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e2.printStackTrace();
          }
        }
      }
    }
    return "Booking failed\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE

    if (currUser == null) {
      return "Cannot pay, not logged in\n";
    }

    boolean deadlock = true;

    while (deadlock) {
      deadlock = false;

      try {
        conn.setAutoCommit(false);

        int currBalance = 0;
        int updatedBalance = 0;
        int price = 0;

        getResCountStmt.setString(1, currUser.toLowerCase());

        ResultSet results = getResCountStmt.executeQuery();
        results.next();

        if (reservationId > results.getInt("count")) {
          results.close();
          conn.commit();
          conn.setAutoCommit(true);

          return "Cannot find unpaid reservation " + reservationId + " under user: " + currUser + "\n";
        }

        getPaidStmt.clearParameters();
        getPaidStmt.setInt(1, reservationId);
        results = getPaidStmt.executeQuery();
        results.next();

        if (results.getInt("paid") == 1) {
          results.close();
          conn.commit();
          conn.setAutoCommit(true);
          
          return "Cannot find unpaid reservation " + reservationId + " under user: " + currUser + "\n";
        }        

        int flight1 = results.getInt("fid1");
        int flight2 = results.getInt("fid2");

        getPriceStmt.clearParameters();
        getPriceStmt.setInt(1, flight1);
        results = getPriceStmt.executeQuery();
        results.next();

        price += results.getInt("price");

        if (flight2 != 0) {
          getPriceStmt.clearParameters();
          getPriceStmt.setInt(1, flight2);
          results = getPriceStmt.executeQuery();
          results.next();

          price += results.getInt("price");
        }

        getBalanceStmt.clearParameters();
        getBalanceStmt.setString(1, currUser.toUpperCase());
        results = getBalanceStmt.executeQuery();
        results.next();
        currBalance = results.getInt("balance");
        results.close();

        if (currBalance < price) {
          conn.commit();
          conn.setAutoCommit(true);

          return "User has only " + currBalance + " in account but itinerary costs " + price + "\n";
        }

        updatePaidUtil(currUser.toLowerCase(), reservationId);

        updatedBalance = currBalance - price;
        updateBalanceUtil(currUser.toLowerCase(), updatedBalance);

        conn.commit();
        conn.setAutoCommit(true);

        return "Paid reservation: " + reservationId + " remaining balance: " + updatedBalance + "\n";
      } catch (SQLException e1) {
        deadlock = isDeadlock(e1);
  
        if (deadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e2.printStackTrace();
          }
        }
      }
    }
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE

    if (currUser == null) {
      return "Cannot view reservations, not logged in\n";
    }

    boolean deadlock = true;

    while (deadlock) {
      deadlock = false;

      try {
        conn.setAutoCommit(false);

        StringBuffer sb = new StringBuffer();
        sb.append("");

        getResStmt.clearParameters();
        getResStmt.setString(1, currUser.toLowerCase());
        ResultSet results = getResStmt.executeQuery();

        while (results.next()) {
          int rid = results.getInt("rid");
          String paid = "";

          if (results.getInt("paid") == 0) {
            paid = "false";
          } else {
            paid = "true";
          }

          int fid1 = results.getInt("fid1");
          int fid2 = results.getInt("fid2");
          
          String f1 = getFlightUtil(fid1);

          sb.append("Reservation " + rid + " paid: " + paid + ":\n");
          sb.append(f1 + "\n");

          if (fid2 != 0) {
            String f2 = getFlightUtil(fid2);
            sb.append(f2 + "\n");
          }
        }
        results.close();

        conn.commit();
        conn.setAutoCommit(true);

        if (sb.toString() == "") {
          return "No reservations found";
        } else {
          return sb.toString();
        }
      } catch (SQLException e1) {
        deadlock = isDeadlock(e1);
  
        if (deadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e2.printStackTrace();
          }
        }
      }
    }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * Utility function for checking if username exists
   */
  private boolean checkUsername(String username) throws SQLException{
    checkUsernameStmt.clearParameters();
    checkUsernameStmt.setString(1, username);
    ResultSet results = checkUsernameStmt.executeQuery();
    String foundUsername = "";

    while (results.next()) {
      foundUsername = results.getString(username);
    }

    results.close();

    return foundUsername.equals(username);
  }

  /**
   * Utility function for inserting new user into Users
   */
  private void createUser(String username, byte[] password, int initAmount) throws SQLException{
    createUserStmt.clearParameters();
    createUserStmt.setString(1, username.toLowerCase());
    createUserStmt.setBytes(2, password);
    createUserStmt.setInt(3, initAmount);
    createUserStmt.executeUpdate();
  }

  /**
   * Utility function to get a new Flight Object from a ResultSet
   */
  private Flight getFlight(ResultSet r, String prefix) throws SQLException {
    Flight flight = new Flight(r.getInt(prefix + "fid"),
                               r.getInt(prefix + "day_of_month"),
                               r.getString(prefix + "carrier_id"),
                               r.getString(prefix + "flight_num"),
                               r.getString(prefix + "origin_city"),
                               r.getString(prefix + "dest_city"),
                               r.getInt(prefix + "actual_time"),
                               r.getInt(prefix + "capacity"),
                               r.getInt(prefix + "price"));
    return flight;
  }

  /**
   * Utility function for updating reservation paid status
   */
  private void updatePaidUtil(String username, int rid) throws SQLException{
    updatePaidStmt.clearParameters();
    updatePaidStmt.setString(1, username);
    updatePaidStmt.setInt(2, rid);
    updatePaidStmt.executeUpdate();
  }

  /**
   * Utility function for updating user balance
   */
  private void updateBalanceUtil(String username, int updatedBalance) throws SQLException{
    updateBalanceStmt.clearParameters();
    updateBalanceStmt.setInt(1, updatedBalance);
    updateBalanceStmt.setString(2, currUser.toLowerCase());
    updateBalanceStmt.executeUpdate();
  }

  /*
   * Utility function for getting flight data and returning string format
   */
  private String getFlightUtil(int fid) {
    Flight f;

    try {
      getFlightDetailsStmt.setInt(1, fid);
      ResultSet results = getFlightDetailsStmt.executeQuery();
      results.next();

      f = getFlight(results, "");

      results.close();
    } catch (SQLException e) {
      return "";
    }

    return f.toString();
  }

  /**
   * A class to store information about a single flight
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  /**
   * A class to store information about a single itinerary (direct or indirect)
   */
  class Itinerary {
    public Flight f1;
    public Flight f2;
    public boolean indirect;

    Itinerary(Flight f1) { // direct
      this.f1 = f1;
      this.f2 = null;
      this.indirect = false;
    }

    Itinerary(Flight f1, Flight f2) { // indirect
      this.f1 = f1;
      this.f2 = f2;
      this.indirect = true;
    }
    
    @Override
    public String toString() {
      int numFlights = 1;
      int time = f1.time;
      StringBuilder sb = new StringBuilder();

      if (this.indirect == true) {
        numFlights = 2;
        time = f1.time + f2.time;
      }

      // add flights to string
      sb.append(numFlights + " flight(s), " + time + " minutes\n" + f1.toString() + "\n");

      if (this.indirect == true) {
        sb.append(f2.toString() + "\n");
      }

      return sb.toString();
    }
  }
}
