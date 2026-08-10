package u23718146_vallabh_s;

import javax.persistence.*;
import java.util.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 *
 * @author Shavir
 */
public class U23718146_Vallabh_S {
public static void main(String[] args) {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("prac1");
    EntityManager em = emf.createEntityManager();

    try {
        em.getTransaction().begin();

        // --------------------------------------------------
        // Create and persist Researcher
        // --------------------------------------------------
        Researcher r = new Researcher("name", "dept", "em@dept");

        System.out.println("Creating researcher...");
        em.persist(r);
        System.out.println("Researcher persisted: " + r.getId());

        // --------------------------------------------------
        // Create and persist Equipment
        // --------------------------------------------------
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        Equipment e = new Equipment(
                "eq",
                "cat",
                date,
                200,
                Equipment.Status.AVAILABLE
        );

        System.out.println("Creating equipment...");
        em.persist(e);
        System.out.println("Equipment persisted: " + e.getId());

        // --------------------------------------------------
        // Create and persist Booking
        // --------------------------------------------------
        Booking b = new Booking(
                date,
                time,
                time,
                Booking.Status.ACTIVE,
                "research",
                r,
                e
        );

        System.out.println("Creating booking...");
        em.persist(b);
        System.out.println("Booking persisted: " + b.getBookingId());

        // --------------------------------------------------
        // Commit transaction
        // --------------------------------------------------
        em.getTransaction().commit();

        System.out.println("\n===== PERSISTENCE VALIDATION =====");

        if (r.getId() != null) {
            System.out.println("PASS: Researcher has ID: " + r.getId());
        } else {
            System.out.println("FAIL: Researcher ID was not generated.");
        }

        if (e.getId() != null) {
            System.out.println("PASS: Equipment has ID: " + e.getId());
        } else {
            System.out.println("FAIL: Equipment ID was not generated.");
        }

        if (b.getBookingId() != null) {
            System.out.println("PASS: Booking has ID: " + b.getBookingId());
        } else {
            System.out.println("FAIL: Booking ID was not generated.");
        }

        // --------------------------------------------------
        // Query number of bookings
        // --------------------------------------------------
        Query q1 = em.createQuery("SELECT COUNT(b) FROM Booking b");
        Long bookingCount = (Long) q1.getSingleResult();

        System.out.println("\nTotal Bookings: " + bookingCount);

        if (bookingCount > 0) {
            System.out.println("PASS: Booking exists in database.");
        } else {
            System.out.println("FAIL: No bookings found.");
        }

        // --------------------------------------------------
        // Retrieve booking and validate relationships
        // --------------------------------------------------
        Booking savedBooking = em.find(Booking.class, b.getBookingId());

        if (savedBooking != null) {
            System.out.println("PASS: Booking retrieved successfully.");
            System.out.println("Booking ID: " + savedBooking.getBookingId());
            System.out.println("Date: " + savedBooking.getDate());
            System.out.println("Start Time: " + savedBooking.getStartTime());
            System.out.println("End Time: " + savedBooking.getEndTime());
            System.out.println("Status: " + savedBooking.getStatus());
            System.out.println("Research Purpose: " + savedBooking.getResearchPurpose());
        } else {
            System.out.println("FAIL: Could not retrieve booking.");
        }

        System.out.println("\n===== ALL TESTS COMPLETE =====");

    } catch (Exception ex) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }

        System.out.println("ERROR: Transaction failed.");
        ex.printStackTrace();

    } finally {
        em.close();
        emf.close();
    }
}
}
