package me.faithfull;

import me.faithfull.domain.InternalPostRepository;
import me.faithfull.domain.Post;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Will Faithfull
 */
@SpringBootApplication
@Configuration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner bootstrapper(final InternalPostRepository postRepository) {
        return args -> {
            Post post = new Post();
            post.setTitle("Hello World");
            post.setContent("This is demonstrating an ugly hack to allow both an exported and internal repository.");
            postRepository.save(post);
        };
    }

}
