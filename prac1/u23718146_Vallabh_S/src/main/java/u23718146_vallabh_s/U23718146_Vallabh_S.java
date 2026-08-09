package u23718146_vallabh_s;

import javax.persistence.*;
import java.util.*;

/**
 *
 * @author Shavir
 */
public class U23718146_Vallabh_S {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("prac1");
        EntityManager em = emf.createEntityManager();
        System.out.println("Connected OK");
        em.close();
        emf.close();
    }
}
