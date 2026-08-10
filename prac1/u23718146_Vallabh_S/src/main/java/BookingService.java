package u23718146_vallabh_s;

import javax.management.QueryEval;
import javax.persistence.*;

import u23718146_vallabh_s.Booking;
import u23718146_vallabh_s.Equipment;
import u23718146_vallabh_s.Researcher;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;


public class BookingService {
    
    // wrap the emf to make management easier for ourselves. 
    // all interaction with db is in this class
    private final EntityManagerFactory emf;

    public BookingService(){
        emf = Persistence.createEntityManagerFactory("prac1");
    }

    public void close(){
        emf.close();
    }

    // Researcher methods

    public Researcher registerResearcher(String name, String dept, String email){
        EntityManager em = emf.createEntityManager();
        try {
            Researcher r = new Researcher(name, dept, email);
            em.getTransaction().begin();
            em.persist(r);
            em.getTransaction().commit();
            return r;
        } finally {
            em.close();
        }
    }

    public Researcher findResearcher(Long id){
        EntityManager em = emf.createEntityManager();
        try{
            /// .find() -> search using class PK
            return em.find(Researcher.class, id);
        } finally {
            em.close();
        }
    }

    // Equipment methods
    public Equipment registerEquipment(String name, String category, LocalDate purchaseDate,
                                        Integer replacementCost, Equipment.Status status) {
        EntityManager em = emf.createEntityManager();
        try {
            Equipment e = new Equipment(name, category, purchaseDate, replacementCost, status);
            em.getTransaction().begin();
            em.persist(e);
            em.getTransaction().commit();
            return e;
        } finally {
            em.close();
        }
    }

    public Equipment findEquipment(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Equipment.class, id);
        } finally {
            em.close();
        }
    }

    // Bookings methods

    // most of the rules are enforced here
    public Booking createBooking(LocalDate date, LocalTime startTime, LocalTime endTime,
                                 String researchPurpose, Long researcherId, long equipmentId){

        EntityManager em = emf.createEntityManager();
        try {
            Researcher researcher = em.find(Researcher.class, researcherId);
            Equipment equipment = em.find(Equipment.class, equipmentId);

            // dont allow bookings with non existent researchers or equipment
            if (researcher == null) {
                throw new IllegalArgumentException("No researcher found with ID " + researcherId);
            }
            if (equipment == null) {
                throw new IllegalArgumentException("No equipment found with ID " + equipmentId);
            }

            if (equipment.getStatus() == Equipment.Status.UNAVAILABLE) {
                throw new IllegalArgumentException("This equipment is not available at the moment.");
            }

            // valid times
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)){
                throw new IllegalArgumentException("Start time must be before end time");
            }

            // cant create bookings in the past
            if (date == null || date.isBefore(LocalDate.now())){
                throw new IllegalArgumentException("Cannot create bookings in the past");
            }

            // researcher cannot have more than 3 active bookings at a time 
            Query activeCount = em.createQuery(
                "SELECT COUNT(b FROM Booking b WHERE b.researcher = :r AND b.status == :status");
            /// :var to set them later
            activeCount.setParameter("r", researcher);
            activeCount.setParameter("status", Booking.Status.ACTIVE);

            Long count = (Long) activeCount.getSingleResult();
            if (count >= 3){
                throw new IllegalArgumentException("This researcher already has 3 active bookings.");
            }

            // ensure no overlapping bookings (same date and overlapping time range that havent been cancelled)
            Query conflictQuery = em.createQuery(
                "SELECT b FROM Booking b WHERE b.equipment = :eq AND b.date = :d AND b.date = :d AND b.status <> :cancelled AND b.startTime < :end AND B.endTime > :start"
            );
            conflictQuery.setParameter("eq", equipment);
            conflictQuery.setParameter("d", date);
            conflictQuery.setParameter("cancelled", Booking.Status.CANCELLED);
            conflictQuery.setParameter("start", startTime);
            conflictQuery.setParameter("end", endTime);

            /// .getResultList() -> get all results as a list obv
            List<?> conflicts = conflictQuery.getResultList(); 

            if (!conflicts.isEmpty()){
                throw new IllegalArgumentException("This equipment is already booked during that time");
            }

            //a researcher may not have multiple bookings for the same equipment on the same date
            Query sameDayQuery = em.createQuery(
                "SELECT b from Booking b WHERE b.researcher = :r AND b.equipment = :eq AND b.date = :d AND b.status <> :cancelled"
            );
            sameDayQuery.setParameter("r", researcher);
            sameDayQuery.setParameter("eq", equipment);
            sameDayQuery.setParameter("d", date);
            sameDayQuery.setParameter("cancelled", Booking.Status.CANCELLED);
            if (!sameDayQuery.getResultList().isEmpty()){
                throw new IllegalArgumentException("This researcher has already booked this equipment on this date");
            }

            // yay we finally passed all the rules 

            Booking booking = new Booking(date, startTime, endTime, Booking.Status.ACTIVE, researchPurpose, researcher, equipment);

            em.getTransaction().begin();
            em.persist(booking);
            em.getTransaction().commit();
            return booking;

        } finally {
            em.close();
        }
    }

}
