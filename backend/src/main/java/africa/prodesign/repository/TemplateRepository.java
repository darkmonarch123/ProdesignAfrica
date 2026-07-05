package africa.prodesign.repository;

import africa.prodesign.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, String> {
    Page<Template> findByBedrooms(int bedrooms, Pageable pageable);
}
