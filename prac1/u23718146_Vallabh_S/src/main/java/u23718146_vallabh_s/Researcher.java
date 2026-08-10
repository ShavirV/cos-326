package u23718146_vallabh_s;

import java.io.Serializable;

import javax.annotation.processing.Generated;
import javax.persistence.*;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Shavir
 */

@Entity
public class Researcher implements Serializable{
    private static final Long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long researcherId;

    private String name;
    private String department;
    private String email;

    // one researcher may have many bookings
    @OneToMany(mappedBy = "researcher", cascade = CascadeType.PERSIST)
    private List<Booking> bookings = new ArrayList<>();

    public Researcher(String n, String d, String e){
        name = n;
        department = d;
        email = e;
    }

    public Researcher(){
    }

    public Long getId(){
        return researcherId;
    }

    public String getDepartment(){
        return department;
    }

    public void setDepartment(String d){
        department = d;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String e){
        email = e;
    }

    public List<Booking> getBookings() {
        return bookings;
    }
 
    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
 
    // Helper method to keep both sides of the relationship in sync
    public void addBooking(Booking booking) {
        bookings.add(booking);
        booking.setResearcher(this);
    }
    
}
