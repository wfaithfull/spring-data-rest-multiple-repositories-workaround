package me.faithfull;

import me.faithfull.hack.ExportAwareRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * @author Will Faithfull
 */
@Configuration
public class RepositoryRestConfiguration extends RepositoryRestMvcConfiguration {

    @Autowired
    ApplicationContext context;

    /**
     * We replace the stock repostiories with our modified subclass.
     */
    @Override
    public Repositories repositories() {
        return new ExportAwareRepositories(context);
    }
}
