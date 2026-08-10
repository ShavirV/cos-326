package u23718146_vallabh_s;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import javax.annotation.processing.Generated;
import javax.persistence.*;

/**
 *
 * @author Shavir
 */
@Entity
public class Booking implements Serializable{
    private static final Long serialVersionUID = 1L;
    
    public enum Status  {
        ACTIVE,
        CONCLUDED,
        CANCELLED
    }

    @Id
    @GeneratedValue
    private Long bookingId;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String researchPurpose;

    @Enumerated(EnumType.STRING)
    private Status status;

    // many bookings may belong to one researcher
    // this side owns the FK
    @ManyToOne
    private Researcher researcher;
    
    // many bookings can be made for one piece of equipment
    @ManyToOne
    private Equipment equipment;
    
    protected Booking() {
    }

    public Booking(LocalDate date, LocalTime startTime, LocalTime endTime, Status status, String researchPurpose, Researcher researcher, Equipment equipment) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.researchPurpose = researchPurpose;
        this.researcher = researcher;
        this.equipment = equipment;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getResearchPurpose() {
        return researchPurpose;
    }

    public void setResearchPurpose(String researchPurpose) {
        this.researchPurpose = researchPurpose;
    }

    public Status getStatus() {
        return status;
    }
 
    public void setStatus(Status status) {
        this.status = status;
    }
 
    public Researcher getResearcher() {
        return researcher;
    }
 
    public void setResearcher(Researcher researcher) {
        this.researcher = researcher;
    }
 
    public Equipment getEquipment() {
        return equipment;
    }
 
    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

}
