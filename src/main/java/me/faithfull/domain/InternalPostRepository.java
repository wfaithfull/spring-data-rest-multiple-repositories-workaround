package me.faithfull.domain;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Will Faithfull
 */
@RepositoryRestResource(exported = false)
public interface InternalPostRepository extends PagingAndSortingRepository<Post, Long> {
}
