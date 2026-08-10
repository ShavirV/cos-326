package u23718146_vallabh_s;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import javax.annotation.processing.Generated;
import javax.persistence.*;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Shavir
 */

@Entity
public class Equipment implements Serializable{
    private static final Long serialVersionUID = 1L;

    public enum Status{
        AVAILABLE,
        UNAVAILABLE
    }

    @Id
    @GeneratedValue
    private Long equipmentId;

    private String name;
    private String category;
    private LocalDate purchaseDate;
    private Integer replacementCost;
    
    @Enumerated(EnumType.STRING)
    private Status status;

    //one piece of equipment may have many bookings
    //mappedBy= "equipment" means that the Booking class owns the fk
    @OneToMany(mappedBy = "equipment", cascade = CascadeType.PERSIST)
    private List<Booking> bookings = new ArrayList<>();

    public Equipment(){
    }

    public Equipment(String n, String c, LocalDate d, Integer r, Status s){
        name = n;
        category = c;
        purchaseDate = d;
        replacementCost = r;
        status = s;
    }

    public Long getId(){
        return equipmentId;
    }

    public void setId(long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Integer getReplacementCost() {
        return replacementCost;
    }

    public void setReplacementCost(Integer replacementCost) {
        this.replacementCost = replacementCost;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

        public List<Booking> getBookings() {
        return bookings != null ? bookings : new ArrayList<>();
    }
 
    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
 
    public void addBooking(Booking booking) {
        bookings.add(booking);
        booking.setEquipment(this);
    }

    
}
