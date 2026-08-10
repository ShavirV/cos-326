package u23718146_vallabh_s;

import java.io.Serializable;

import javax.annotation.processing.Generated;
import javax.persistence.*;

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

    public Researcher(String n, String d, String e){
        name = n;
        department = d;
        email = e;
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
    
}
