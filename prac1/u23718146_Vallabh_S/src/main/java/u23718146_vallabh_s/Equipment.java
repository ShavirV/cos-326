package u23718146_vallabh_s;

import java.io.Serializable;
import java.time.LocalDate;

import javax.annotation.processing.Generated;
import javax.persistence.*;

/**
 *
 * @author Shavir
 */

@Entity
public class Equipment implements Serializable{
    private static final Long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long equipmentId;

    private String name;
    private String category;
    private LocalDate purchaseDate;
    private Integer replacementCost;
    private String status;
    

    public Equipment(String n, String c, LocalDate d, Integer r, String s){
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
