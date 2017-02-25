package me.faithfull;

import lombok.AllArgsConstructor;
import me.faithfull.domain.InternalPostRepository;
import me.faithfull.domain.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Will Faithfull
 */
@BasePathAwareController
@ResponseBody
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class PostController {

    private final InternalPostRepository postRepository;

    @RequestMapping(value = "/bypass/{id}", method = RequestMethod.GET)
    public PersistentEntityResource getPostWithoutSecurityRestrictions(@PathVariable("id")Post post,
                                                                       PersistentEntityResourceAssembler pera) {
        return pera.toResource(post);
    }

    @RequestMapping(value = "/bypass/", method = RequestMethod.GET)
    public Resources getPostsWithoutSecurityRestrictions(PersistentEntityResourceAssembler pera) {
        Iterable<Post> posts = postRepository.findAll();
        List<Resource<Object>> resources = new ArrayList<>();
        for (Post post : posts) {
            resources.add(post == null ? null : pera.toResource(post));
        }
        return new Resources(resources, new Link(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString()));
    }

}
