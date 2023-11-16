package net.rubyworks.urlshortener.command;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.rubyworks.urlshortener.command.domain.CShorten;
import net.rubyworks.urlshortener.command.domain.CShortenRepository;

@Component
public class CommandShortener {
    private CShortenRepository shortenRepository;

    private Map<String, CShorten> byId;
    private Map<String, CShorten> byUrl;

    @Autowired
    public void setShortenRepository(CShortenRepository shortenRepository) {
        this.shortenRepository = shortenRepository;

        var reserved = List.of(
                CShorten.of("Hi", "https://test.dii.im/"),
                CShorten.of("hi", "https://www.dii.im/")
                );

        this.shortenRepository.saveAll(reserved);
        byId = reserved.stream().collect(toMap(CShorten::getId, Function.identity()));
        byUrl = reserved.stream().collect(toMap(CShorten::getUrl, Function.identity()));
    }

    public CShorten create(String url, long count, long ttl) {
        return create(obtainId(), url, count, ttl);
    }

    public CShorten create(String id, String url, long count, long ttl) {
        if (byUrl.containsKey(url)) {
            return byUrl.get(url);
        }
        return shortenRepository.save(CShorten.of(id, url, count, ttl, false));
    }

    private static final int LOOP_COUNT_MAX = 3;
    private String obtainId() {
        var countForRandom = 3;
        var loopCount = 0;
        while(true) {
            loopCount++;
            var tempId = CShorten.random(countForRandom++);
            if (!shortenRepository.existsById(tempId)) {
                return tempId;
            }

            if (loopCount > LOOP_COUNT_MAX) {
                throw new RuntimeException("failed obtaining id.");
            }
        }
    }

    private CShorten find(String id) {
        if (byId.containsKey(id)) {
            return byId.get(id);
        }
        return shortenRepository.findById(id).orElse(null);
    }

    public CShorten findAndUpdate(String id) {
        var shorten = find(id);

        Optional.ofNullable(shorten)
        .filter(find -> !find.isFixed())
        .ifPresent(find -> {
            var accessCount = find.getCount();
            if (accessCount < 0) { // not used count-expiration
                shortenRepository.save(CShorten.copy(find)); // just update modifed-time
            } else {
                accessCount--;

                if (accessCount < 1) {
                    deleteBy(id);
                } else {
                    shortenRepository.save(CShorten.copy(find, accessCount));
                }
            }
        });

        return shorten;
    }

    public void deleteBy(String id) {
        if (!byId.containsKey(id)) {
            shortenRepository.deleteById(id);
        }
    }

    public List<CShorten> findAll() {
        var findAll = shortenRepository.findAll();
        return StreamSupport.stream(findAll.spliterator(), true).toList();
    }

    public void deleteAll(List<CShorten> list) {
        list.stream().map(CShorten::getId)
        .forEach(shortenRepository::deleteById);
    }

}
