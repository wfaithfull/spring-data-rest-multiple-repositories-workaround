package me.faithfull.domain;

import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Will Faithfull
 */
@RepositoryRestResource(exported = true, path = "posts")
@Primary
@PreAuthorize("hasRole('ADMINISTRATOR')")
public interface ExportedPostRepository extends PagingAndSortingRepository<Post, Long> {
}
