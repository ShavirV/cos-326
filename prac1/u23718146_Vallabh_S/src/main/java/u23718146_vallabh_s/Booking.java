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
    
    @Id
    @GeneratedValue
    private Long bookingId;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String researchPurpose;
    
    
    protected Booking() {
    }

    public Booking(LocalDate date, LocalTime startTime, LocalTime endTime, String researchPurpose) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.researchPurpose = researchPurpose;
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
}
