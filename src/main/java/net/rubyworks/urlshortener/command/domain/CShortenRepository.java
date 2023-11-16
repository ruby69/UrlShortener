package net.rubyworks.urlshortener.command.domain;

import org.springframework.data.repository.CrudRepository;

public interface CShortenRepository extends CrudRepository<CShorten, String> {

}
