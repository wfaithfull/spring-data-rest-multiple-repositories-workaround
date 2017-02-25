package me.faithfull.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Will Faithfull
 */
@Entity @Getter @Setter
public class Post {

    @Id @GeneratedValue
    Long id;
    String title;
    String content;

}
