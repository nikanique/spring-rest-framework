package io.github.nikanique.springrestframework.orm;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@NoArgsConstructor
@Component
public class SpecificationsBuilder {

    private EntityManagerFactory entityManagerFactory;

    @Autowired
    public SpecificationsBuilder(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }


    public Specification fromSearchCriteriaList(List<SearchCriteria> searchCriteriaList) {
        if (searchCriteriaList.isEmpty())
            return null;
        Specification specifications = new ModelSpecification(searchCriteriaList.get(0), entityManagerFactory);
        for (int i = 1; i < searchCriteriaList.size(); i++) {
            specifications = Specification.where(specifications).and(new ModelSpecification(searchCriteriaList.get(i), entityManagerFactory));
        }
        return specifications;
    }

    public Specification fromSearchCriteria(SearchCriteria searchCriteria) {
        return new ModelSpecification(searchCriteria, entityManagerFactory);
    }


}
