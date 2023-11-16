package net.rubyworks.urlshortener.web;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.rubyworks.urlshortener.domain.Shorten;
import net.rubyworks.urlshortener.domain.ShortenRepository;

@Service
public class ShortenerService {
    private ShortenRepository shortenRepository;

    private Map<String, Shorten> byId;
    private Map<String, Shorten> byUrl;

    @Autowired
    public void setShortenRepository(ShortenRepository shortenRepository) {
        this.shortenRepository = shortenRepository;

        var reserved = List.of(
                Shorten.of("Hi", "https://test.dii.im/"),
                Shorten.of("hi", "https://www.dii.im/")
                );

        this.shortenRepository.saveAll(reserved);
        byId = reserved.stream().collect(toMap(Shorten::getId, Function.identity()));
        byUrl = reserved.stream().collect(toMap(Shorten::getUrl, Function.identity()));
    }

    public Shorten create(String url, long count, long ttl) {
        return create(obtainId(), url, count, ttl);
    }

    public Shorten create(String id, String url, long count, long ttl) {
        if (byUrl.containsKey(url)) {
            return byUrl.get(url);
        }
        return shortenRepository.save(Shorten.of(id, url, count, ttl, false));
    }

    private static final int LOOP_COUNT_MAX = 3;
    private String obtainId() {
        var countForRandom = 3;
        var loopCount = 0;
        while(true) {
            loopCount++;
            var tempId = Shorten.random(countForRandom++);
            if (!shortenRepository.existsById(tempId)) {
                return tempId;
            }

            if (loopCount > LOOP_COUNT_MAX) {
                throw new RuntimeException("failed obtaining id.");
            }
        }
    }

    private Shorten find(String id) {
        if (byId.containsKey(id)) {
            return byId.get(id);
        }
        return shortenRepository.findById(id).orElse(null);
    }

    public Shorten findAndUpdate(String id) {
        var shorten = find(id);

        Optional.ofNullable(shorten)
        .filter(find -> !find.isFixed())
        .ifPresent(find -> {
            var accessCount = find.getCount();
            if (accessCount < 0) { // not used count-expiration
                shortenRepository.save(Shorten.copy(find)); // just update modifed-time
            } else {
                accessCount--;

                if (accessCount < 1) {
                    deleteBy(id);
                } else {
                    shortenRepository.save(Shorten.copy(find, accessCount));
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

    public List<Shorten> findAll() {
        var findAll = shortenRepository.findAll();
        return StreamSupport.stream(findAll.spliterator(), true).toList();
    }

    public void deleteAll(List<Shorten> list) {
        list.stream().map(Shorten::getId)
        .forEach(shortenRepository::deleteById);
    }

    public List<Shorten> findByDuration(String duration) {
        var timeMillis = timeMillis(duration);
        var list = findAll().stream()
                .filter(it -> !it.isFixed() && it.getModifiedAt() > timeMillis)
                .sorted(Comparator.comparing(Shorten::getModifiedAt).reversed())
                .toList();


        return StreamSupport.stream(list.spliterator(), true).toList();
    }

    private long timeMillis(String duration) {
        var last = duration.substring(duration.length() - 1);
        var period = Long.parseLong(duration.substring(0, duration.length() - 1));

        var now = LocalDateTime.now();
        LocalDateTime time = null;
        if ("h".equals(last)) {         // hours
            time = now.minusHours(period);
        } else if ("d".equals(last)) {  // days
            time = now.minusDays(period);
        } else if ("w".equals(last)) {  // weeks
            time = now.minusWeeks(period);
        } else {
            time = now.minusMinutes(period); // minutes
        }
        return ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
