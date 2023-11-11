package net.rubyworks.urlshortener.domain;

import org.springframework.data.repository.CrudRepository;

public interface ShortenRepository extends CrudRepository<Shorten, String> {

}
