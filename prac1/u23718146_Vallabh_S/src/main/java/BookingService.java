package u23718146_vallabh_s;

import javax.persistence.*;

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
            // .find() -> search using class PK
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
    
}
